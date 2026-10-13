package com.cybershield.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import java.util.ArrayList;
import java.util.List;

/* Register once in MainActivity:  webView.addJavascriptInterface(new GuardianBridge(this), "GuardianAndroid"); */
public class GuardianBridge {

    private final Activity a;
    public GuardianBridge(Activity a) { this.a = a; }

    /* Starts the watcher service if Guardian is switched on but the service is not running
       (after a reboot, an update, or a system kill). Safe to call often. */
    static void ensureServiceRunning(android.content.Context c) {
        try {
            if (!GuardianAlert.sp(c).getBoolean("enabled", false)) return;
            if (GuardianAlert.serviceAlive(c)) return;
            Intent i = new Intent(c, GuardianService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception e) {
            GuardianAlert.log(c, 0, "Could not start Guardian service: " + e);
        }
    }

    @JavascriptInterface public String getGuardianState() { return GuardianAlert.state(a); }

    @JavascriptInterface public void setGuardianEnabled(boolean on) {
        GuardianAlert.sp(a).edit().putBoolean("enabled", on).apply();
        Intent i = new Intent(a, GuardianService.class);
        if (on) { if (Build.VERSION.SDK_INT >= 26) a.startForegroundService(i); else a.startService(i); }
        else a.stopService(i);
    }

    @JavascriptInterface public void saveGuardianSettings(String owner, String guardiansCsv) {
        GuardianAlert.sp(a).edit().putString("owner", owner.trim()).putString("guardians", guardiansCsv).apply();
    }

    @JavascriptInterface public void removeTrusted(String n) {
        List<String> t = GuardianAlert.list(a, "trusted");
        t.remove(n);
        GuardianAlert.putList(a, "trusted", t);
    }

    @JavascriptInterface public void testGuardianAlert() {
        GuardianAlert.sms(a, "CyberShield test: Guardian Alert is set up for " + GuardianAlert.owner(a) + ". No action needed.");
        GuardianAlert.log(a, 0, "Test SMS sent to guardians.");
    }

    @JavascriptInterface public void clearGuardianLog() { GuardianAlert.sp(a).edit().remove("log").apply(); }

    @JavascriptInterface public void askGuardianPermission(final String key) {
        a.runOnUiThread(() -> {
            if ("phone".equals(key)) {
                List<String> p = new ArrayList<>();
                p.add(Manifest.permission.READ_PHONE_STATE); p.add(Manifest.permission.READ_CALL_LOG);
                p.add(Manifest.permission.READ_CONTACTS); p.add(Manifest.permission.ANSWER_PHONE_CALLS);
                if (Build.VERSION.SDK_INT >= 33) p.add("android.permission.POST_NOTIFICATIONS");
                a.requestPermissions(p.toArray(new String[0]), 1201);
            } else if ("sms".equals(key)) {
                a.requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 1202);
            } else if ("usage".equals(key)) {
                a.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } else if ("overlay".equals(key)) {
                a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName())));
            }
        });
    }
}
