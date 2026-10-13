package com.cybershield.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/* Foreground service: watches app launches while Guardian Alert is armed. */
public class GuardianService extends Service {

    private final Handler h = new Handler(Looper.getMainLooper());
    private long cursor = 0;            // timestamp of the newest app event already handled
    private String lastLogged = "";
    private String lastErr = "";
    private boolean lastArmed = false;
    private boolean usageWarned = false;
    private long lastHb = 0;
    private int tick = 0;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            try {
                step();
            } catch (Exception e) {
                String m = e.toString();
                if (!m.equals(lastErr)) {            // log each new error once, never hide it
                    lastErr = m;
                    GuardianAlert.log(GuardianService.this, 0, "Service error: " + m);
                }
            }
            h.postDelayed(this, GuardianAlert.active(GuardianService.this) ? 1500 : 4000);
        }
    };

    @Override public int onStartCommand(Intent i, int flags, int startId) {
        try {
            Notification n = note("Guardian Alert is on");
            if (Build.VERSION.SDK_INT >= 34) startForeground(7, n, 0x40000000); // SPECIAL_USE
            else startForeground(7, n);
        } catch (Exception e) {
            GuardianAlert.log(this, 0, "Service could not start in foreground: " + e);
            stopSelf();
            return START_NOT_STICKY;
        }
        GuardianAlert.heartbeat(this);
        GuardianAlert.log(this, 0, "Guardian service started");
        h.removeCallbacks(loop);
        h.post(loop);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        h.removeCallbacks(loop);
        GuardianAlert.log(this, 0, "Guardian service stopped");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    private Notification note(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("guardian", "Guardian Alert", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, "guardian");
        } else b = new Notification.Builder(this);
        return b.setContentTitle("CyberShield Guardian").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock).setOngoing(true).build();
    }

    private boolean isForeground(int type) {
        if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) return true;
        return Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_RESUMED;
    }

    private void step() {
        long now = System.currentTimeMillis();
        if (now - lastHb > 10000) {
            lastHb = now;
            GuardianAlert.heartbeat(this);
        }

        boolean armed = GuardianAlert.active(this);
        if (armed != lastArmed) {
            lastArmed = armed;
            lastLogged = "";
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(7,
                    note(armed ? "Unknown call: watching for payments" : "Guardian Alert is on"));
            GuardianAlert.log(this, 0, armed ? "Watching apps now" : "Watching ended");
        }

        if (!GuardianAlert.usageOk(this)) {
            if (!usageWarned) {
                usageWarned = true;
                GuardianAlert.log(this, 0, "Usage access is OFF. Cannot see which app opens.");
            }
            return;
        }
        usageWarned = false;

        scanEvents(armed);

        if (armed && ++tick % 4 == 0) scanNewApps();
    }

    /* Reads EVERY app-launch event since the last one handled, so a payment app is not
       hidden by a later system screen, and each launch is handled once. */
    private void scanEvents(boolean armed) {
        UsageStatsManager u = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (u == null) return;

        long now = System.currentTimeMillis();
        long from = cursor > 0 ? Math.max(cursor, now - 60000) : now - 3000;
        UsageEvents ev = u.queryEvents(from, now);
        if (ev == null) return;

        SharedPreferences p = GuardianAlert.sp(this);
        long armStart = p.getLong("armStart", 0);
        UsageEvents.Event e = new UsageEvents.Event();

        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            long ts = e.getTimeStamp();
            if (ts <= cursor || !isForeground(e.getEventType())) continue;
            cursor = ts;

            String pkg = e.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) continue;
            if (!armed || ts < armStart) continue;          // only apps opened after the call was answered

            if (!pkg.equals(lastLogged)) {
                lastLogged = pkg;
                GuardianAlert.log(this, 0, "App opened: " + pkg);
            }

            if (GuardianAlert.PAY.contains(pkg)) {
                GuardianAlert.log(this, 2, "Payment app matched: " + pkg);
                if (p.getInt("level", 0) < 2) {
                    GuardianAlert.fire(this, 2, "Payment app opened during an unknown call.");
                } else if (System.currentTimeMillis() - p.getLong("lastBrake", 0) > 60000) {
                    GuardianAlert.brake(this, 2);
                }
            } else if (GuardianAlert.REMOTE.contains(pkg)) {
                GuardianAlert.fire(this, 3, "Remote-control app opened during an unknown call: " + pkg);
            }
        }
    }

    private void scanNewApps() {
        long since = GuardianAlert.sp(this).getLong("armStart", 0);
        for (PackageInfo pi : getPackageManager().getInstalledPackages(0)) {
            if (pi.firstInstallTime > since && !pi.packageName.equals(getPackageName())) {
                GuardianAlert.fire(this, 3, "New app installed during an unknown call: " + pi.packageName);
                break;
            }
        }
    }
}
