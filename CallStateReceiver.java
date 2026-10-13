package com.cybershield.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Receives phone-state changes for Guardian Alert.
 * MUST be declared in AndroidManifest.xml with the PHONE_STATE action,
 * otherwise Android never calls it and Guardian Alert never arms.
 */
public class CallStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String incomingNumber =
                intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        GuardianAlert.onPhoneState(context, state, incomingNumber);

        // If the watcher service died (reboot, update, system kill), try to bring it back.
        // On Android 12+ this can be refused from the background; the failure is logged.
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            GuardianBridge.ensureServiceRunning(context);
        }
    }
}
