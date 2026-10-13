package com.cybershield.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * SmsFloodDetectorService
 * ------------------------
 * Watches notifications from messaging apps (Google Messages, Samsung
 * Messages, etc.) and looks for the real pattern of an SMS bombing attack:
 * many DIFFERENT senders texting you within a short window.
 *
 * Honest limitation: this cannot stop those texts from being sent - that
 * happens on servers outside your phone (the sites the attacker used your
 * number on). What it CAN do is notice the pattern fast and give you a
 * one-tap way to silence your phone while you report it to your carrier.
 *
 * REQUIRES: the user must manually turn this on once, in
 * Settings > Apps > Special app access > Notification access > CyberShield.
 * (Same type of permission Link Shield already uses. Android requires each
 * listener service to be switched on separately, even inside the same app.)
 */
public class SmsFloodDetectorService extends NotificationListenerService {

    // Packages of common messaging apps we watch for incoming SMS notifications.
    private static final String[] WATCHED_PACKAGES = {
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging",     // Samsung Messages
            "com.android.mms",                   // Stock AOSP messaging
            "com.android.messaging"
    };

    private static final long WINDOW_MS = 3 * 60 * 1000;            // 3 minutes
    private static final int DISTINCT_SENDER_THRESHOLD = 4;         // 4+ different senders in the window = likely flood
    private static final long RE_ALERT_COOLDOWN_MS = 5 * 60 * 1000; // don't spam alerts more than once per 5 min

    private static final String PREFS = "sms_flood_detector";
    private static final String KEY_LAST_ALERT = "last_alert_time";
    private static final int ALERT_NOTIFICATION_ID = 9911;

    // sender label -> last time seen, kept in memory while this service is running
    private final TreeMap<String, Long> recentSenders = new TreeMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null) return;
        if (!isWatchedPackage(sbn.getPackageName())) return;

        String sender = extractSenderLabel(sbn);
        if (sender == null) sender = "unknown";

        long now = System.currentTimeMillis();
        recentSenders.put(sender, now);
        purgeOldEntries(now);

        if (recentSenders.size() >= DISTINCT_SENDER_THRESHOLD) {
            maybeAlert(now);
        }
    }

    private boolean isWatchedPackage(String pkg) {
        for (String watched : WATCHED_PACKAGES) {
            if (watched.equals(pkg)) return true;
        }
        return false;
    }

    private String extractSenderLabel(StatusBarNotification sbn) {
        try {
            android.os.Bundle extras = sbn.getNotification().extras;
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            return title != null ? title.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void purgeOldEntries(long now) {
        Iterator<Map.Entry<String, Long>> it = recentSenders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > WINDOW_MS) {
                it.remove();
            }
        }
    }

    private void maybeAlert(long now) {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lastAlert = prefs.getLong(KEY_LAST_ALERT, 0);

        if (now - lastAlert < RE_ALERT_COOLDOWN_MS) return; // already alerted recently

        prefs.edit().putLong(KEY_LAST_ALERT, now).apply();
        showFloodAlert();
    }

    private void showFloodAlert() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        String channelId = "sms_flood_alerts";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "SMS Flood Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Warns you when many texts arrive from different senders quickly.");
            manager.createNotificationChannel(channel);
        }

        Intent silenceIntent = new Intent(this, SilenceActionReceiver.class);
        int flags = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent silencePendingIntent = PendingIntent.getBroadcast(this, 0, silenceIntent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, channelId)
                : new Notification.Builder(this);

        builder.setContentTitle("Possible SMS bombing detected")
                .setContentText("Texts are arriving from several different senders quickly. Tap Silence to quiet your phone for 15 minutes.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_lock_silent_mode, "Silence 15 min", silencePendingIntent);

        manager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }
}
