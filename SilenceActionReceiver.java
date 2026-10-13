package com.cybershield.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/**
 * SilenceActionReceiver
 * -----------------------
 * Runs when the user taps "Silence 15 min" on the flood alert notification.
 *
 * Turns on Do Not Disturb for 15 minutes so incoming texts stop making
 * noise or vibrating, then automatically turns it back off.
 *
 * Needs "Do Not Disturb access" - a permission granted once, separately
 * from Notification Access, in the phone's Settings.
 */
public class SilenceActionReceiver extends BroadcastReceiver {

    private static final long SILENCE_DURATION_MS = 15 * 60 * 1000; // 15 minutes
    private static final int ALERT_NOTIFICATION_ID = 9911;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        boolean hasDndAccess = Build.VERSION.SDK_INT < 23 || manager.isNotificationPolicyAccessGranted();

        if (!hasDndAccess) {
            Toast.makeText(context,
                    "Turn on 'Do Not Disturb access' for CyberShield, then tap Silence again.",
                    Toast.LENGTH_LONG).show();

            Intent settingsIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            return;
        }

        // Silence everything for SILENCE_DURATION_MS.
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        Toast.makeText(context, "Silenced for 15 minutes.", Toast.LENGTH_SHORT).show();
        manager.cancel(ALERT_NOTIFICATION_ID);

        // Schedule turning it back off automatically.
        Intent restoreIntent = new Intent(context, RestoreDndReceiver.class);
        int flags = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent restorePendingIntent = PendingIntent.getBroadcast(context, 0, restoreIntent, flags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerAt = System.currentTimeMillis() + SILENCE_DURATION_MS;
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, restorePendingIntent);
        }
    }
}
