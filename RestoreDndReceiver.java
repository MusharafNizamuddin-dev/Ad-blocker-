package com.cybershield.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * RestoreDndReceiver
 * --------------------
 * Fires automatically 15 minutes after Silence was turned on, and turns
 * Do Not Disturb back off so the phone returns to normal.
 */
public class RestoreDndReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (manager.isNotificationPolicyAccessGranted()) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            Toast.makeText(context, "CyberShield: Silence period ended, notifications restored.", Toast.LENGTH_SHORT).show();
        }
    }
}
