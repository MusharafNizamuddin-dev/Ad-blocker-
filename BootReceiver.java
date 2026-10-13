package com.cybershield.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* Restores the Guardian watcher service after a reboot or an app update. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String a = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {

            if (GuardianAlert.sp(context).getBoolean("enabled", false)) {
                GuardianAlert.log(context, 0, "Phone restarted or app updated. Restoring Guardian.");
                GuardianBridge.ensureServiceRunning(context);
            }
        }
    }
}
