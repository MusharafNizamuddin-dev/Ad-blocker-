package com.cybershield.app;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

private WebView webView;

private ValueCallback<Uri[]> filePathCallback;

private static final int FILE_CHOOSER_REQUEST = 1001;

private static final int NOTIFICATION_PERMISSION_REQUEST = 2001;

private static final String SECURITY_CHANNEL_ID =
        "cybershield_security_alerts";

private static final String REVIEW_CHANNEL_ID =
        "cybershield_review_alerts";

private static final String SHIELD_PREFS =
        "cybershield_shield_persistence";

private static final String SHIELD_EVENTS =
        "shield_events";

private static final String TAPJACK_PREFS =
        "cybershield_tapjack_persistence";

private static final String TAPJACK_LOG =
        "tapjack_log";

private static final String TAPJACK_COUNT =
        "tapjack_count";

private static final String HONEYPOT_PREFS =
        "cybershield_honeypot_persistence";

private static final String HONEYPOT_BASELINE =
        "honeypot_baseline";

private static final String HONEYPOT_LOG =
        "honeypot_log";

private static final String[] HONEYPOT_FILE_NAMES = {
        "Bank_Login_Backup.txt",
        "Crypto_Wallet_Seed_Phrase.txt",
        "Saved_Passwords_Export.txt"
};

private static final String HONEYPOT_CHANNEL_ID =
        "cybershield_honeypot_alerts";

private static final int HONEYPOT_ALARM_REQUEST_CODE = 4001;

private static MainActivity instance;


/* =========================================================
 * EXISTING ACTIVITY SETUP
 * ========================================================= */

@Override
protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    instance = this;

    setContentView(
            getResources()
                    .getIdentifier(
                            "activity_main",
                            "layout",
                            getPackageName()
                    )
    );


    webView = findViewById(
            getResources()
                    .getIdentifier(
                            "webview",
                            "id",
                            getPackageName()
                    )
    );


    /*
     * If your project creates the WebView directly
     * instead of using activity_main.xml, this fallback
     * keeps the existing setup working.
     */
    if (webView == null) {

        webView = new WebView(this);

        setContentView(webView);
    }


    WebSettings webSettings =
            webView.getSettings();

    webSettings.setJavaScriptEnabled(true);

    webSettings.setDomStorageEnabled(true);

    webSettings.setAllowFileAccess(true);

    webSettings.setAllowContentAccess(true);


    webView.setWebChromeClient(
            new WebChromeClient() {

                @Override
                public boolean onShowFileChooser(
                        WebView webView,
                        ValueCallback<Uri[]> filePathCallback,
                        FileChooserParams fileChooserParams
                ) {

                    if (MainActivity.this.filePathCallback != null) {

                        MainActivity.this.filePathCallback
                                .onReceiveValue(null);
                    }

                    MainActivity.this.filePathCallback =
                            filePathCallback;


                    Intent intent =
                            fileChooserParams
                                    .createIntent();


                    try {

                        startActivityForResult(
                                intent,
                                FILE_CHOOSER_REQUEST
                        );

                    } catch (Exception e) {

                        MainActivity.this.filePathCallback
                                .onReceiveValue(null);

                        MainActivity.this.filePathCallback =
                                null;
                    }


                    return true;
                }
            }
    );


    /*
     * CyberShield JavaScript bridge.
     */
    webView.addJavascriptInterface(
            new Object() {

                @android.webkit.JavascriptInterface
                public void openNotificationSettings() {

                    runOnUiThread(() -> {

                        try {

                            Intent intent =
                                    new Intent(
                                            Settings
                                                    .ACTION_NOTIFICATION_LISTENER_SETTINGS
                                    );

                            startActivity(intent);

                        } catch (Exception e) {

                            android.widget.Toast.makeText(
                                    MainActivity.this,
                                    "Unable to open Notification Access settings.",
                                    android.widget.Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }

@android.webkit.JavascriptInterface
public void openExternalUrl(String url) {

    runOnUiThread(() -> {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {

            android.widget.Toast.makeText(
                    MainActivity.this,
                    "Unable to open this link.",
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    });
}
                @android.webkit.JavascriptInterface
                public boolean isNotificationAccessGranted() {

                    try {

                        NotificationManager notificationManager =
                                (NotificationManager)
                                        getSystemService(
                                                NOTIFICATION_SERVICE
                                        );


                        ComponentName componentName =
                                new ComponentName(
                                        MainActivity.this,
                                        LinkNotificationListener.class
                                );


                        return notificationManager
                                .isNotificationListenerAccessGranted(
                                        componentName
                                );

                    } catch (Exception e) {

                        return false;
                    }
                }
@android.webkit.JavascriptInterface
public void restoreShieldStateFromDashboard() {

    runOnUiThread(() -> {
        restoreShieldState();
    });

}
@android.webkit.JavascriptInterface
public void resetShieldState() {

    runOnUiThread(() -> {

        SharedPreferences prefs =
                getSharedPreferences(
                        SHIELD_PREFS,
                        MODE_PRIVATE
                );

        prefs.edit()
                .remove(SHIELD_EVENTS)
                .apply();

        String js =
                "if(typeof window.clearShieldDashboard === 'function')" +
                "{window.clearShieldDashboard();}" +
                "if(typeof window.clearLookalikeDashboard === 'function')" +
                "{window.clearLookalikeDashboard();}";

        if (webView != null) {
            webView.evaluateJavascript(
                    js,
                    null
            );
        }
    });
}
@android.webkit.JavascriptInterface
public String getOverlayRiskApps() {

    JSONArray result = new JSONArray();

    try {

        PackageManager pm = getPackageManager();

        AppOpsManager appOps =
                (AppOpsManager) getSystemService(APP_OPS_SERVICE);

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {

            try {

                boolean isSystemApp =
                        (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                if (isSystemApp) {
                    continue;
                }

                /*
                 * CyberShield itself may hold "Display over other
                 * apps" (for Call Shield auto-open) - don't list it.
                 */
                if (app.packageName.equals(getPackageName())) {
                    continue;
                }

                int mode;

                if (appOps != null) {

                    mode = appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                            app.uid,
                            app.packageName
                    );

                } else {

                    mode = AppOpsManager.MODE_IGNORED;
                }

                if (mode == AppOpsManager.MODE_ALLOWED) {

                    JSONObject entry = new JSONObject();

                    String label =
                            pm.getApplicationLabel(app).toString();

                    entry.put("name", label);
                    entry.put("package", app.packageName);

                    result.put(entry);
                }

            } catch (Exception ignored) {
            }
        }

    } catch (Exception ignored) {
    }

    return result.toString();
}

@android.webkit.JavascriptInterface
public String getTapjackingLog() {

    try {

        SharedPreferences prefs =
                getSharedPreferences(
                        TAPJACK_PREFS,
                        MODE_PRIVATE
                );

        return prefs.getString(TAPJACK_LOG, "[]");

    } catch (Exception e) {

        return "[]";
    }
}

@android.webkit.JavascriptInterface
public int getTapjackingCount() {

    try {

        SharedPreferences prefs =
                getSharedPreferences(
                        TAPJACK_PREFS,
                        MODE_PRIVATE
                );

        return prefs.getInt(TAPJACK_COUNT, 0);

    } catch (Exception e) {

        return 0;
    }
}

@android.webkit.JavascriptInterface
public void resetTapjackingLog() {

    runOnUiThread(() -> {

        SharedPreferences prefs =
                getSharedPreferences(
                        TAPJACK_PREFS,
                        MODE_PRIVATE
                );

        prefs.edit()
                .remove(TAPJACK_LOG)
                .remove(TAPJACK_COUNT)
                .apply();
    });
}

/* =========================================================
 * HONEYPOT VAULT — DEPLOY DECOY FILES
 *
 * Plants fake, clearly-labeled decoy files with enticing
 * names ("Bank_Login_Backup.txt" etc.) in the public
 * Downloads folder. None of them contain real data. A
 * SHA-256 fingerprint of each is saved as a baseline right
 * after creation. No legitimate app or process has any
 * reason to ever touch a fake password file sitting in
 * Downloads — so if one is later modified, renamed, or
 * deleted, that's a strong, low-noise signal something
 * scanned and tampered with it.
 *
 * Requires Android 10+ (scoped storage / MediaStore).
 * ========================================================= */

@android.webkit.JavascriptInterface
public String deployHoneypot() {

    JSONArray baseline = new JSONArray();

    try {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "{\"error\":\"needs_android_10_or_higher\"}";
        }

        ContentResolver resolver = getContentResolver();

        removeExistingHoneypotFiles(resolver);

        for (String fileName : HONEYPOT_FILE_NAMES) {

            try {

                String content =
                        "CyberShield Honeypot Decoy File\r\n" +
                        "--------------------------------\r\n" +
                        "This file contains no real data.\r\n" +
                        "It exists only to detect unauthorized scanning.\r\n" +
                        "Token: " + java.util.UUID.randomUUID() + "\r\n";

                ContentValues values = new ContentValues();

                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                );

                Uri uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

                if (uri == null) {
                    continue;
                }

                java.io.OutputStream out = resolver.openOutputStream(uri);

                if (out != null) {

                    out.write(content.getBytes("UTF-8"));
                    out.close();
                }

                String hash = hashHoneypotFile(resolver, uri);

                JSONObject entry = new JSONObject();

                entry.put("name", fileName);
                entry.put("uri", uri.toString());
                entry.put("hash", hash);
                entry.put("deployedTime", System.currentTimeMillis());

                baseline.put(entry);

            } catch (Exception ignored) {
            }
        }

        SharedPreferences prefs =
                getSharedPreferences(HONEYPOT_PREFS, MODE_PRIVATE);

        prefs.edit()
                .putString(HONEYPOT_BASELINE, baseline.toString())
                .remove(HONEYPOT_LOG)
                .apply();

    } catch (Exception ignored) {
    }

    return baseline.toString();
}

private void removeExistingHoneypotFiles(ContentResolver resolver) {

    try {

        SharedPreferences prefs =
                getSharedPreferences(HONEYPOT_PREFS, MODE_PRIVATE);

        JSONArray oldBaseline =
                new JSONArray(prefs.getString(HONEYPOT_BASELINE, "[]"));

        for (int i = 0; i < oldBaseline.length(); i++) {

            try {

                JSONObject entry = oldBaseline.getJSONObject(i);

                Uri uri = Uri.parse(entry.getString("uri"));

                resolver.delete(uri, null, null);

            } catch (Exception ignored) {
            }
        }

    } catch (Exception ignored) {
    }
}

private String hashHoneypotFile(ContentResolver resolver, Uri uri) {

    try {

        java.io.InputStream in = resolver.openInputStream(uri);

        if (in == null) {
            return "";
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] buffer = new byte[4096];
        int read;

        while ((read = in.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }

        in.close();

        byte[] hashBytes = digest.digest();

        StringBuilder sb = new StringBuilder();

        for (byte b : hashBytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }

        return sb.toString();

    } catch (Exception e) {

        return "";
    }
}

/* =========================================================
 * HONEYPOT VAULT — CHECK STATUS
 *
 * Compares each decoy file's current state against its
 * saved baseline. A file that's missing, or whose content no
 * longer matches its original fingerprint, gets logged as
 * tampered with.
 * ========================================================= */

@android.webkit.JavascriptInterface
public String getHoneypotStatus() {

    JSONObject result = new JSONObject();

    try {

        SharedPreferences prefs =
                getSharedPreferences(HONEYPOT_PREFS, MODE_PRIVATE);

        JSONArray baseline =
                new JSONArray(prefs.getString(HONEYPOT_BASELINE, "[]"));

        if (baseline.length() == 0) {

            result.put("status", "not_deployed");
            result.put("files", new JSONArray());
            result.put("log", new JSONArray(prefs.getString(HONEYPOT_LOG, "[]")));

            return result.toString();
        }

        ContentResolver resolver = getContentResolver();

        JSONArray log = new JSONArray(prefs.getString(HONEYPOT_LOG, "[]"));

        JSONArray fileStates = new JSONArray();

        boolean anyTriggered = false;

        for (int i = 0; i < baseline.length(); i++) {

            JSONObject entry = baseline.getJSONObject(i);

            String name = entry.getString("name");
            String originalHash = entry.getString("hash");
            Uri uri = Uri.parse(entry.getString("uri"));

            String state;
            boolean exists = true;

            try {

                java.io.InputStream test = resolver.openInputStream(uri);

                if (test == null) {
                    exists = false;
                } else {
                    test.close();
                }

            } catch (Exception e) {

                exists = false;
            }

            if (!exists) {

                state = "deleted";
                anyTriggered = true;

                if (!honeypotAlreadyLogged(log, name, "DELETED")) {
                    log.put(honeypotEvent(name, "DELETED"));
                    showHoneypotNotification(MainActivity.this, name, "DELETED");
                }

            } else {

                String currentHash = hashHoneypotFile(resolver, uri);

                if (!currentHash.equals(originalHash)) {

                    state = "modified";
                    anyTriggered = true;

                    if (!honeypotAlreadyLogged(log, name, "MODIFIED")) {
                        log.put(honeypotEvent(name, "MODIFIED"));
                        showHoneypotNotification(MainActivity.this, name, "MODIFIED");
                    }

                } else {

                    state = "intact";
                }
            }

            JSONObject fileEntry = new JSONObject();

            fileEntry.put("name", name);
            fileEntry.put("state", state);

            fileStates.put(fileEntry);
        }

        while (log.length() > 50) {
            log.remove(0);
        }

        prefs.edit().putString(HONEYPOT_LOG, log.toString()).apply();

        result.put("status", anyTriggered ? "triggered" : "armed");
        result.put("files", fileStates);
        result.put("log", log);

    } catch (Exception e) {

        try {
            result.put("status", "error");
        } catch (Exception ignored) {
        }
    }

    return result.toString();
}

private boolean honeypotAlreadyLogged(JSONArray log, String name, String type) {

    try {

        for (int i = 0; i < log.length(); i++) {

            JSONObject e = log.getJSONObject(i);

            if (e.getString("name").equals(name)
                    && e.getString("type").equals(type)) {

                return true;
            }
        }

    } catch (Exception ignored) {
    }

    return false;
}

private JSONObject honeypotEvent(String name, String type) {

    JSONObject o = new JSONObject();

    try {

        o.put("name", name);
        o.put("type", type);
        o.put("time", System.currentTimeMillis());

    } catch (Exception ignored) {
    }

    return o;
}

                /* ===== Phone security scan bridge ===== */

                @android.webkit.JavascriptInterface
                public String runSecurityScan() {
                    return runSecurityScanInternal();
                }

                @android.webkit.JavascriptInterface
                public void openSecurityFix(String target) {
                    runOnUiThread(() -> openSecurityFixInternal(target));
                }


            },
            "CyberShieldAndroid"
    );

    // Guardian Alert JavaScript bridge
    webView.addJavascriptInterface(new GuardianBridge(this), "GuardianAndroid");
    GuardianBridge.ensureServiceRunning(this);


    /*
     * Load the CyberShield home page.
     */
    webView.setWebViewClient(
            new WebViewClient() {

                @Override
                public void onPageFinished(
                        WebView view,
                        String url
                ) {

                    super.onPageFinished(
                            view,
                            url
                    );


                    /*
                     * Restore saved Link Shield detections
                     * after the WebView has finished loading.
                     */
                    restoreShieldState();


                    if (getIntent() != null
                            && getIntent().getBooleanExtra(
                                    "open_lookalike",
                                    false
                            )) {

                        view.postDelayed(
                                () -> {

                                    String js =
                                            "if(typeof window.openLookalikeDashboard === 'function')" +
                                            "{window.openLookalikeDashboard();}";

                                    view.evaluateJavascript(
                                            js,
                                            null
                                    );

                                },
                                500
                        );

                    } else if (getIntent() != null
                            && getIntent().getBooleanExtra(
                                    "open_link_shield",
                                    false
                            )) {

                        view.postDelayed(
                                () -> {

                                    String js =
                                            "if(typeof window.openLinkShieldDashboard === 'function')" +
                                            "{window.openLinkShieldDashboard();}";

                                    view.evaluateJavascript(
                                            js,
                                            null
                                    );

                                },
                                500
                        );
                    }
                }
            }
    );


    if (getIntent() != null
            && getIntent().getBooleanExtra(
                    "open_honeypot",
                    false
            )) {

        webView.loadUrl(
                "file:///android_asset/honeypot.html"
        );

    } else {

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }


    /*
     * Create notification channel.
     */
    createSecurityNotificationChannel();


    /*
     * Android 13+ notification permission.
     */
    requestNotificationPermission();


    /*
     * Periodic background Honeypot check so a trigger still
     * gets noticed (within ~15 min) even if the app is closed.
     */
    scheduleHoneypotChecks();
}


/* =========================================================
 * SECURITY NOTIFICATION CHANNEL
 * ========================================================= */

private void createSecurityNotificationChannel() {

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O) {

        NotificationChannel channel =
                new NotificationChannel(
                        SECURITY_CHANNEL_ID,
                        "CyberShield Security Alerts",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );


        channel.setDescription(
                "Alerts for suspicious links detected by CyberShield."
        );


        channel.enableVibration(true);


        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );


        if (manager != null) {

            manager.createNotificationChannel(
                    channel
            );
        }


        /*
         * Separate channel for "Needs Review" alerts so the user
         * can control/mute these independently from full
         * SUSPICIOUS alerts in system settings if they want to.
         */
        NotificationChannel reviewChannel =
                new NotificationChannel(
                        REVIEW_CHANNEL_ID,
                        "CyberShield Review Alerts",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );


        reviewChannel.setDescription(
                "Alerts for links that need review, detected by CyberShield."
        );


        reviewChannel.enableVibration(true);


        if (manager != null) {

            manager.createNotificationChannel(
                    reviewChannel
            );
        }


        /*
         * Separate channel for Honeypot Vault triggers.
         */
        NotificationChannel honeypotChannel =
                new NotificationChannel(
                        HONEYPOT_CHANNEL_ID,
                        "CyberShield Honeypot Alerts",
                        NotificationManager
                                .IMPORTANCE_HIGH
                );


        honeypotChannel.setDescription(
                "Alerts when a CyberShield Honeypot decoy file is tampered with."
        );


        honeypotChannel.enableVibration(true);


        if (manager != null) {

            manager.createNotificationChannel(
                    honeypotChannel
            );
        }

    }
}


/* =========================================================
 * PHONE SECURITY SCAN
 *
 * Read-only checks of phone settings and installed apps.
 * Nothing is changed or uploaded: results go to scan.html only.
 * ========================================================= */

private void scanAdd(
        JSONArray checks, String id, String title, String status,
        String detail, String fix, JSONArray apps
) throws Exception {

    JSONObject c = new JSONObject();
    c.put("id", id);
    c.put("title", title);
    c.put("status", status);
    c.put("detail", detail);

    if (fix != null) {
        c.put("fix", fix);
    }

    if (apps != null && apps.length() > 0) {
        c.put("apps", apps);
    }

    checks.put(c);
}

private JSONObject scanApp(PackageManager pm, ApplicationInfo ai) throws Exception {

    JSONObject o = new JSONObject();
    o.put("name", String.valueOf(pm.getApplicationLabel(ai)));
    o.put("pkg", ai.packageName);
    return o;
}

private boolean scanIsSystem(ApplicationInfo ai) {

    return (ai.flags & (ApplicationInfo.FLAG_SYSTEM
            | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
}

private String runSecurityScanInternal() {

    JSONObject out = new JSONObject();
    JSONArray checks = new JSONArray();
    int score = 100;
    int appCount = 0;

    PackageManager pm = getPackageManager();
    ContentResolver cr = getContentResolver();
    String self = getPackageName();

    /* 1. Screen lock */
    try {
        boolean locked = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.KeyguardManager km =
                    (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            locked = km != null && km.isDeviceSecure();
        }

        if (locked) {
            scanAdd(checks, "lock", "Screen lock", "ok",
                    "A PIN, pattern or password protects your phone.", null, null);
        } else {
            score -= 25;
            scanAdd(checks, "lock", "Screen lock", "bad",
                    "No screen lock is set. Anyone who picks up your phone can open your apps, messages and bank accounts.",
                    "security", null);
        }
    } catch (Exception ignored) {
    }

    /* 2. Security update age */
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Build.VERSION.SECURITY_PATCH != null) {

            Date patch = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(Build.VERSION.SECURITY_PATCH);

            long days = (System.currentTimeMillis() - patch.getTime()) / 86400000L;
            String when = "Last security update: " + Build.VERSION.SECURITY_PATCH
                    + " (" + Math.max(days, 0) + " days ago).";

            if (days > 365) {
                score -= 20;
                scanAdd(checks, "patch", "Security updates", "bad",
                        when + " Updates older than a year leave publicly known weaknesses open.",
                        "about", null);
            } else if (days > 180) {
                score -= 10;
                scanAdd(checks, "patch", "Security updates", "warn",
                        when + " Check for a newer update.", "about", null);
            } else {
                scanAdd(checks, "patch", "Security updates", "ok",
                        when + " Your phone is up to date.", null, null);
            }
        }
    } catch (Exception ignored) {
    }

    /* 3. Developer options and USB debugging */
    try {
        boolean dev = Settings.Global.getInt(
                cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        boolean adb = Settings.Global.getInt(
                cr, Settings.Global.ADB_ENABLED, 0) == 1;

        if (adb) {
            score -= 10;
            scanAdd(checks, "dev", "USB debugging", "warn",
                    "USB debugging is on. A computer connected by cable could control your phone. Turn it off unless you are developing apps.",
                    "developer", null);
        } else if (dev) {
            score -= 3;
            scanAdd(checks, "dev", "Developer options", "warn",
                    "Developer options are switched on. You can turn them off if you don't use them.",
                    "developer", null);
        } else {
            scanAdd(checks, "dev", "Developer options", "ok",
                    "Developer options and USB debugging are off.", null, null);
        }
    } catch (Exception ignored) {
    }

    /* 4. Rooted device */
    try {
        boolean root = Build.TAGS != null && Build.TAGS.contains("test-keys");
        String[] su = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk"};

        for (String p : su) {
            if (new java.io.File(p).exists()) {
                root = true;
            }
        }

        if (root) {
            score -= 20;
            scanAdd(checks, "root", "Device integrity", "bad",
                    "Signs of root access were found. Rooted phones lose Android's built-in app protections.",
                    null, null);
        } else {
            scanAdd(checks, "root", "Device integrity", "ok",
                    "No signs of root access.", null, null);
        }
    } catch (Exception ignored) {
    }

    /* 5. Accessibility services (how banking malware reads your screen) */
    try {
        JSONArray acc = new JSONArray();
        java.util.Set<String> seen = new java.util.HashSet<>();

        android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager)
                        getSystemService(ACCESSIBILITY_SERVICE);

        if (am != null) {
            for (android.accessibilityservice.AccessibilityServiceInfo info :
                    am.getEnabledAccessibilityServiceList(
                            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {

                try {
                    ApplicationInfo ai = info.getResolveInfo().serviceInfo.applicationInfo;

                    if (ai.packageName.equals(self) || scanIsSystem(ai)
                            || !seen.add(ai.packageName)) {
                        continue;
                    }

                    acc.put(scanApp(pm, ai));

                } catch (Exception ignored) {
                }
            }
        }

        if (acc.length() > 0) {
            score -= Math.min(30, 15 * acc.length());
            scanAdd(checks, "access", "Apps that can read your screen", "bad",
                    acc.length() + " app(s) have accessibility access. This lets an app see everything on screen and tap for you. Banking malware abuses it. Keep only apps you installed on purpose, such as a screen reader.",
                    "accessibility", acc);
        } else {
            scanAdd(checks, "access", "Apps that can read your screen", "ok",
                    "No third-party app has accessibility access.", null, null);
        }
    } catch (Exception ignored) {
    }

    /* 6. Device administrators */
    try {
        JSONArray adm = new JSONArray();

        android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager)
                        getSystemService(DEVICE_POLICY_SERVICE);

        List<ComponentName> admins = dpm != null ? dpm.getActiveAdmins() : null;

        if (admins != null) {
            for (ComponentName cn : admins) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(cn.getPackageName(), 0);

                    if (ai.packageName.equals(self) || scanIsSystem(ai)) {
                        continue;
                    }

                    adm.put(scanApp(pm, ai));

                } catch (Exception ignored) {
                }
            }
        }

        if (adm.length() > 0) {
            score -= Math.min(30, 15 * adm.length());
            scanAdd(checks, "admin", "Device administrator apps", "bad",
                    adm.length() + " app(s) are device administrators. They can lock the phone and are hard to uninstall. Remove any you don't recognise.",
                    "security", adm);
        } else {
            scanAdd(checks, "admin", "Device administrator apps", "ok",
                    "No third-party device administrators.", null, null);
        }
    } catch (Exception ignored) {
    }

    /* 7. Apps that can read all notifications (including OTP messages) */
    try {
        JSONArray lis = new JSONArray();
        java.util.Set<String> seen = new java.util.HashSet<>();
        String raw = Settings.Secure.getString(cr, "enabled_notification_listeners");

        if (raw != null && !raw.isEmpty()) {
            for (String part : raw.split(":")) {
                try {
                    ComponentName cn = ComponentName.unflattenFromString(part);

                    if (cn == null) {
                        continue;
                    }

                    ApplicationInfo ai = pm.getApplicationInfo(cn.getPackageName(), 0);

                    if (ai.packageName.equals(self) || scanIsSystem(ai)
                            || !seen.add(ai.packageName)) {
                        continue;
                    }

                    lis.put(scanApp(pm, ai));

                } catch (Exception ignored) {
                }
            }
        }

        if (lis.length() > 0) {
            score -= Math.min(20, 10 * lis.length());
            scanAdd(checks, "notif", "Apps that can read your notifications", "warn",
                    lis.length() + " app(s) can read every notification, including bank alerts and one-time passwords. Keep only apps that need it.",
                    "notifications", lis);
        } else {
            scanAdd(checks, "notif", "Apps that can read your notifications", "ok",
                    "No other app reads your notifications.", null, null);
        }
    } catch (Exception ignored) {
    }

    /* 8. Installed apps: remote-control tools, SMS access, install source */
    try {
        JSONArray remote = new JSONArray();
        JSONArray sms = new JSONArray();
        JSONArray side = new JSONArray();
        JSONArray installers = new JSONArray();
        int smsN = 0, sideN = 0, instN = 0;

        java.util.Set<String> remoteApps = new java.util.HashSet<>(java.util.Arrays.asList(
                "com.anydesk.anydeskandroid",
                "com.teamviewer.quicksupport.market",
                "com.teamviewer.teamviewer.market.mobile",
                "com.carriez.flutter_hbb",
                "com.sand.airdroid"));

        java.util.Set<String> stores = new java.util.HashSet<>(java.util.Arrays.asList(
                "com.android.vending",
                "com.sec.android.app.samsungapps",
                "com.xiaomi.mipicks",
                "com.heytap.market",
                "com.oppo.market",
                "com.vivo.appstore",
                "com.huawei.appmarket",
                "com.amazon.venezia"));

        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);

        String defaultSms = null;
        try {
            defaultSms = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
        } catch (Exception ignored) {
        }

        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {

            if (scanIsSystem(ai) || ai.packageName.equals(self)) {
                continue;
            }

            appCount++;
            String pkg = ai.packageName;

            try {
                if (remoteApps.contains(pkg)) {
                    remote.put(scanApp(pm, ai));
                }

                /* Install source */
                String installer;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    installer = pm.getInstallSourceInfo(pkg).getInstallingPackageName();
                } else {
                    installer = pm.getInstallerPackageName(pkg);
                }

                if (installer == null || !stores.contains(installer)) {
                    sideN++;
                    if (side.length() < 12) {
                        side.put(scanApp(pm, ai));
                    }
                }

                /* Permissions */
                PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
                String[] rp = pi.requestedPermissions;
                int[] rf = pi.requestedPermissionsFlags;

                if (rp == null || rf == null) {
                    continue;
                }

                boolean readsSms = false;
                boolean asksInstall = false;

                for (int i = 0; i < rp.length && i < rf.length; i++) {

                    boolean granted =
                            (rf[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;

                    if (granted && ("android.permission.READ_SMS".equals(rp[i])
                            || "android.permission.RECEIVE_SMS".equals(rp[i]))) {
                        readsSms = true;
                    }

                    if ("android.permission.REQUEST_INSTALL_PACKAGES".equals(rp[i])) {
                        asksInstall = true;
                    }
                }

                if (readsSms && !pkg.equals(defaultSms)) {
                    smsN++;
                    if (sms.length() < 12) {
                        sms.put(scanApp(pm, ai));
                    }
                }

                if (asksInstall && appOps != null
                        && appOps.checkOpNoThrow("android:request_install_packages",
                        ai.uid, pkg) == AppOpsManager.MODE_ALLOWED) {
                    instN++;
                    if (installers.length() < 12) {
                        installers.put(scanApp(pm, ai));
                    }
                }

            } catch (Exception ignored) {
            }
        }

        if (remote.length() > 0) {
            score -= 20;
            scanAdd(checks, "remote", "Remote-control apps", "bad",
                    "A screen-sharing or remote-control app is installed. Scammers on a phone call ask victims to install these. If you did not install it yourself, remove it now.",
                    "apps", remote);
        } else {
            scanAdd(checks, "remote", "Remote-control apps", "ok",
                    "No common remote-control apps found.", null, null);
        }

        if (smsN > 0) {
            score -= Math.min(15, 5 * smsN);
            scanAdd(checks, "sms", "Apps that can read your text messages", "warn",
                    smsN + " app(s) can read your SMS, including one-time passwords. Remove any that don't need it.",
                    "apps", sms);
        } else {
            scanAdd(checks, "sms", "Apps that can read your text messages", "ok",
                    "No third-party app can read your SMS.", null, null);
        }

        if (instN > 0) {
            score -= Math.min(10, 5 * instN);
            scanAdd(checks, "install", "Apps that can install other apps", "warn",
                    instN + " app(s) are allowed to install other apps. Malware uses this to add more malware.",
                    "apps", installers);
        } else {
            scanAdd(checks, "install", "Apps that can install other apps", "ok",
                    "No app has permission to install other apps.", null, null);
        }

        if (sideN > 0) {
            score -= Math.min(10, 2 * sideN);
            scanAdd(checks, "source", "Apps from outside app stores", "warn",
                    sideN + " app(s) were not installed from a trusted app store. That is fine for apps you installed on purpose, but check the list.",
                    "apps", side);
        } else {
            scanAdd(checks, "source", "Apps from outside app stores", "ok",
                    "All apps came from trusted app stores.", null, null);
        }

    } catch (Exception ignored) {
    }

    score = Math.max(0, Math.min(100, score));

    try {
        out.put("score", score);
        out.put("level", score >= 85 ? "good" : (score >= 60 ? "attention" : "risk"));
        out.put("checks", checks);
        out.put("appsScanned", appCount);
        out.put("device", Build.MANUFACTURER + " " + Build.MODEL);
        out.put("android", Build.VERSION.RELEASE);
    } catch (Exception ignored) {
    }

    return out.toString();
}

private void openSecurityFixInternal(String target) {

    try {
        Intent i;

        if (target != null && target.startsWith("app:")
                && target.substring(4).matches("[A-Za-z0-9._]+")) {

            i = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + target.substring(4))
            );

        } else if ("security".equals(target)) {
            i = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        } else if ("accessibility".equals(target)) {
            i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        } else if ("developer".equals(target)) {
            i = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        } else if ("notifications".equals(target)) {
            i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        } else if ("about".equals(target)) {
            i = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        } else if ("apps".equals(target)) {
            i = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
        } else {
            i = new Intent(Settings.ACTION_SETTINGS);
        }

        startActivity(i);

    } catch (Exception e) {

        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception ignored) {
        }
    }
}


/* =========================================================
 * NOTIFICATION PERMISSION
 * ========================================================= */

private void requestNotificationPermission() {

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {

        if (checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }
}


/* =========================================================
 * HONEYPOT BACKGROUND CHECK SCHEDULING
 *
 * Android does not allow true instant background checks
 * without a permanent foreground-service notification icon,
 * so this uses an inexact repeating alarm instead — roughly
 * every 15 minutes, no extra icon required. Safe to call on
 * every app launch: it just replaces the existing schedule,
 * never creates duplicates.
 * ========================================================= */

private void scheduleHoneypotChecks() {

    try {

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager)
                        getSystemService(ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        HoneypotCheckReceiver.class
                );

        int immutableFlag =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        ? PendingIntent.FLAG_IMMUTABLE
                        : 0;

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        HONEYPOT_ALARM_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | immutableFlag
                );

        alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()
                        + android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent
        );

    } catch (Exception ignored) {
    }
}


/* =========================================================
 * SUSPICIOUS NOTIFICATION
 * ========================================================= */

public static void showSuspiciousNotification(
        String url,
        String reason
) {

    showSuspiciousNotification(url, reason, null);
}


public static void showSuspiciousNotification(
        String url,
        String reason,
        String matchedDomain
) {

    if (instance == null) {
        return;
    }


    instance.runOnUiThread(() ->
            instance.sendSuspiciousNotification(
                    url,
                    reason,
                    matchedDomain
            )
    );
}


private void sendSuspiciousNotification(
        String url,
        String reason,
        String matchedDomain
) {

    boolean isLookalike =
            matchedDomain != null;


    NotificationManager manager =
            (NotificationManager)
                    getSystemService(
                            NOTIFICATION_SERVICE
                    );


    if (manager == null) {
        return;
    }


    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {

        if (checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
    }


    Intent intent =
            new Intent(
                    this,
                    MainActivity.class
            );


    intent.putExtra(
            "open_link_shield",
            true
    );

    if (isLookalike) {

        intent.putExtra(
                "open_lookalike",
                true
        );
    }


    intent.setFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
    );


    PendingIntent pendingIntent =
            PendingIntent.getActivity(
                    this,
                    3001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | getImmutableFlag()
            );


    android.app.Notification.Builder builder;


    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O) {

        builder =
                new android.app.Notification.Builder(
                        this,
                        SECURITY_CHANNEL_ID
                );

    } else {

        builder =
                new android.app.Notification.Builder(
                        this
                );


        builder.setPriority(
                android.app.Notification.PRIORITY_HIGH
        );
    }


    builder.setSmallIcon(
            getResources()
                    .getIdentifier(
                            "ic_link_suspicious",
                            "drawable",
                            getPackageName()
                    )
    );


    builder.setContentTitle(
            isLookalike
                    ? "Lookalike domain detected"
                    : "Suspicious link detected"
    );


    builder.setContentText(
            isLookalike
                    ? ("This link may be impersonating " + matchedDomain + ". Tap to review.")
                    : "A potentially risky link was detected. Tap to review."
    );


    builder.setStyle(
            new android.app.Notification.BigTextStyle()
                    .bigText(
                            (isLookalike
                                    ? "CyberShield detected a domain that may be impersonating " + matchedDomain + ".\n\n"
                                    : "CyberShield detected a potentially risky link.\n\n")
                                    + url
                                    + "\n\n"
                                    + reason
                                    + "\n\nTap to open CyberShield and review the detection."
                    )
    );


    builder.setContentIntent(
            pendingIntent
    );


    builder.setAutoCancel(true);


    builder.setCategory(
            android.app.Notification.CATEGORY_STATUS
    );


    builder.setVisibility(
            android.app.Notification.VISIBILITY_PRIVATE
    );


    manager.notify(
            (int) System.currentTimeMillis(),
            builder.build()
    );
}


/* =========================================================
 * REVIEW NOTIFICATION ("Needs Review" / CHECK status)
 * ========================================================= */

public static void showReviewNotification(
        String url,
        String reason
) {

    if (instance == null) {
        return;
    }


    instance.runOnUiThread(() ->
            instance.sendReviewNotification(
                    url,
                    reason
            )
    );
}


private void sendReviewNotification(
        String url,
        String reason
) {

    NotificationManager manager =
            (NotificationManager)
                    getSystemService(
                            NOTIFICATION_SERVICE
                    );


    if (manager == null) {
        return;
    }


    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {

        if (checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
    }


    Intent intent =
            new Intent(
                    this,
                    MainActivity.class
            );


    intent.putExtra(
            "open_link_shield",
            true
    );


    intent.setFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
    );


    PendingIntent pendingIntent =
            PendingIntent.getActivity(
                    this,
                    3002,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | getImmutableFlag()
            );


    android.app.Notification.Builder builder;


    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O) {

        builder =
                new android.app.Notification.Builder(
                        this,
                        REVIEW_CHANNEL_ID
                );

    } else {

        builder =
                new android.app.Notification.Builder(
                        this
                );


        builder.setPriority(
                android.app.Notification.PRIORITY_HIGH
        );
    }


    builder.setSmallIcon(
            getResources()
                    .getIdentifier(
                            "ic_link_review",
                            "drawable",
                            getPackageName()
                    )
    );


    builder.setContentTitle(
            "Link needs review"
    );


    builder.setContentText(
            "A link was flagged for review. Tap to check it."
    );


    builder.setStyle(
            new android.app.Notification.BigTextStyle()
                    .bigText(
                            "CyberShield found a link worth reviewing.\n\n"
                                    + url
                                    + "\n\n"
                                    + reason
                                    + "\n\nTap to open CyberShield and review the detection."
                    )
    );


    builder.setContentIntent(
            pendingIntent
    );


    builder.setAutoCancel(true);


    builder.setCategory(
            android.app.Notification.CATEGORY_STATUS
    );


    builder.setVisibility(
            android.app.Notification.VISIBILITY_PRIVATE
    );


    manager.notify(
            (int) System.currentTimeMillis(),
            builder.build()
    );
}


/* =========================================================
 * HONEYPOT TRIGGERED NOTIFICATION
 *
 * Called from the Honeypot Vault bridge check (app open) and
 * from HoneypotCheckReceiver (periodic background check).
 * ========================================================= */

public static void showHoneypotNotification(
        Context context,
        String fileName,
        String eventType
) {

    if (context == null) {
        return;
    }


    NotificationManager manager =
            (NotificationManager)
                    context.getSystemService(
                            NOTIFICATION_SERVICE
                    );

    if (manager == null) {
        return;
    }


    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {

        if (context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
    }


    Intent intent =
            new Intent(
                    context,
                    MainActivity.class
            );

    intent.putExtra(
            "open_honeypot",
            true
    );

    intent.setFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
    );


    int immutableFlag =
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    ? PendingIntent.FLAG_IMMUTABLE
                    : 0;

    PendingIntent pendingIntent =
            PendingIntent.getActivity(
                    context,
                    3002,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | immutableFlag
            );


    android.app.Notification.Builder builder;

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O) {

        builder =
                new android.app.Notification.Builder(
                        context,
                        HONEYPOT_CHANNEL_ID
                );

    } else {

        builder =
                new android.app.Notification.Builder(
                        context
                );

        builder.setPriority(
                android.app.Notification.PRIORITY_HIGH
        );
    }


    builder.setSmallIcon(
            context.getResources()
                    .getIdentifier(
                            "ic_link_suspicious",
                            "drawable",
                            context.getPackageName()
                    )
    );

    builder.setContentTitle(
            "Honeypot triggered"
    );

    builder.setContentText(
            fileName + " was " + eventType.toLowerCase(Locale.US) + ". Tap to review."
    );

    builder.setStyle(
            new android.app.Notification.BigTextStyle()
                    .bigText(
                            "A CyberShield honeypot decoy file was "
                                    + eventType.toLowerCase(Locale.US)
                                    + ":\n\n"
                                    + fileName
                                    + "\n\nNo legitimate app has any reason to touch this file. "
                                    + "Tap to open the Honeypot Vault and review."
                    )
    );

    builder.setContentIntent(pendingIntent);
    builder.setAutoCancel(true);

    builder.setCategory(
            android.app.Notification.CATEGORY_STATUS
    );

    builder.setVisibility(
            android.app.Notification.VISIBILITY_PRIVATE
    );

    manager.notify(
            (int) System.currentTimeMillis(),
            builder.build()
    );
}


/* =========================================================
 * PENDING INTENT FLAG
 * ========================================================= */

private int getImmutableFlag() {

    if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M) {

        return PendingIntent.FLAG_IMMUTABLE;

    } else {

        return 0;
    }
}


/* =========================================================
 * EXISTING RESUME
 * ========================================================= */

@Override
protected void onResume() {

    super.onResume();

    GuardianBridge.ensureServiceRunning(this);


    if (webView == null) {
        return;
    }


    webView.postDelayed(
            () -> {

                try {

                    NotificationManager
                            notificationManager =
                            (NotificationManager)
                                    getSystemService(
                                            NOTIFICATION_SERVICE
                                    );


                    ComponentName componentName =
                            new ComponentName(
                                    MainActivity.this,
                                    LinkNotificationListener.class
                            );


                    boolean granted =
                            notificationManager
                                    .isNotificationListenerAccessGranted(
                                            componentName
                                    );


                    String js =
                            "if(typeof window.updateNotificationAccessStatus === 'function')" +
                            "{window.updateNotificationAccessStatus(" +
                            granted +
                            ");}";


                    webView.evaluateJavascript(
                            js,
                            null
                    );

                } catch (Exception ignored) {
                }

            },
            250
    );
}


/* =========================================================
 * LINK RESULT
 * ========================================================= */

public static void showLinkResult(
        String url,
        String status,
        String reason
) {

    showLinkResult(url, status, reason, null);
}


public static void showLinkResult(
        String url,
        String status,
        String reason,
        String matchedDomain
) {

    /*
     * SAVE FIRST.
     *
     * This is important because the Activity/WebView may
     * not exist when the notification is later opened.
     */
    saveShieldDetection(
            url,
            status,
            reason,
            matchedDomain
    );


    if (instance == null) {
        return;
    }


    instance.runOnUiThread(() -> {

        if (instance.webView == null) {
            return;
        }


        String js =
                "if(typeof window.showLinkResult === 'function')" +
                "{window.showLinkResult(" +
                "'" + escapeJs(url) + "'," +
                "'" + escapeJs(status) + "'," +
                "'" + escapeJs(reason) + "'" +
                ");}";


        instance.webView.evaluateJavascript(
                js,
                null
        );


        if (matchedDomain != null) {

            String lookalikeJs =
                    "if(typeof window.showLookalikeResult === 'function')" +
                    "{window.showLookalikeResult(" +
                    "'" + escapeJs(url) + "'," +
                    "'" + escapeJs(reason) + "'," +
                    "'" + escapeJs(matchedDomain) + "'" +
                    ");}";

            instance.webView.evaluateJavascript(
                    lookalikeJs,
                    null
            );
        }
    });
}


/* =========================================================
 * PERSIST DETECTION
 * ========================================================= */

private static synchronized void saveShieldDetection(
        String url,
        String status,
        String reason,
        String matchedDomain
) {

    if (url == null) {
        url = "";
    }

    if (status == null) {
        status = "CHECK";
    }

    if (reason == null) {
        reason = "";
    }


    try {

        SharedPreferences prefs =
                getPersistencePreferences();


        String saved =
                prefs.getString(
                        SHIELD_EVENTS,
                        "[]"
                );


        JSONArray events =
                new JSONArray(saved);


        JSONObject event =
                new JSONObject();


        event.put(
                "url",
                url
        );


        event.put(
                "status",
                status
        );


        event.put(
                "isLookalike",
                matchedDomain != null
        );


        event.put(
                "matchedDomain",
                matchedDomain == null ? "" : matchedDomain
        );


        event.put(
                "reason",
                reason
        );


        event.put(
                "time",
                new java.text.SimpleDateFormat(
                        "hh:mm:ss a",
                        java.util.Locale.getDefault()
                ).format(
                        new java.util.Date()
                )
        );


        events.put(event);


        /*
         * Keep the same maximum amount shown by
         * the dashboard's recent activity list.
         */
        while (events.length() > 10) {

            JSONArray trimmed =
                    new JSONArray();

            int start =
                    events.length() - 10;


            for (int i = start;
                    i < events.length();
                    i++) {

                trimmed.put(
                        events.getJSONObject(i)
                );
            }


            events = trimmed;
        }


        prefs.edit()
                .putString(
                        SHIELD_EVENTS,
                        events.toString()
                )
                .apply();


    } catch (Exception ignored) {
    }
}


/* =========================================================
 * PERSISTENCE PREFERENCES
 * ========================================================= */

private static SharedPreferences getPersistencePreferences() {

    if (instance != null) {

        return instance.getSharedPreferences(
                SHIELD_PREFS,
                MODE_PRIVATE
        );
    }


    /*
     * This should normally not be reached because the
     * listener calls showLinkResult while MainActivity
     * exists. Kept safe for the existing architecture.
     */
    return null;
}


/* =========================================================
 * RESTORE SAVED DASHBOARD
 * ========================================================= */

private void restoreShieldState() {

    if (webView == null) {
        return;
    }


    try {

        SharedPreferences prefs =
                getSharedPreferences(
                        SHIELD_PREFS,
                        MODE_PRIVATE
                );


        String saved =
                prefs.getString(
                        SHIELD_EVENTS,
                        "[]"
                );


        JSONArray events =
                new JSONArray(saved);


        if (events.length() == 0) {
            return;
        }


        String encoded =
                Base64.encodeToString(
                        events.toString()
                                .getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                ),
                        Base64.NO_WRAP
                );


        String js =
                "(function(){" +

                "try{" +

                "var raw=atob('" +
                encoded +
                "');" +

                "var bytes=new Uint8Array(raw.length);" +

                "for(var i=0;i<raw.length;i++)" +
                               "{bytes[i]=raw.charCodeAt(i);}" +

                "var text=new TextDecoder('utf-8').decode(bytes);" +

                "var events=JSON.parse(text);" +

                "var links=document.getElementById('linksDetected');" +

                "var suspicious=document.getElementById('suspiciousCount');" +

                "var checks=document.getElementById('checkCount');" +

                "var safe=document.getElementById('safeCount');" +

                "var latest=document.getElementById('latestDetection');" +

                "var activity=document.getElementById('activityList');" +

                "var dashboard=document.getElementById('dashboard');" +

                "if(!links||!suspicious||!checks||!safe||!latest||!activity)return;" +

                "var suspiciousTotal=0;" +

                "var checksTotal=0;" +

                "var safeTotal=0;" +

                "activity.innerHTML='';" +

                "for(var i=0;i<events.length;i++){" +

                    "var e=events[i];" +

                    "if(e.status==='SUSPICIOUS')suspiciousTotal++;" +

                    "else if(e.status==='CHECK')checksTotal++;" +

                    "else if(e.status==='APPEARS SAFE')safeTotal++;" +

                "}" +

                "links.textContent=events.length;" +

                "suspicious.textContent=suspiciousTotal;" +

                "checks.textContent=checksTotal;" +

                "safe.textContent=safeTotal;" +

                "for(var j=0;j<events.length;j++){" +

                    "if(typeof window.addRecentActivity==='function'){" +

                        "window.addRecentActivity(" +

                            "events[j].url," +

                            "events[j].status," +

                            "events[j].reason," +

                            "events[j].time" +

                        ");" +

                    "}" +

                "}" +

                "var latestEvent=events[events.length-1];" +

                "if(typeof window.updateLatestDetection==='function'){" +

                    "window.updateLatestDetection(" +

                        "latestEvent.url," +

                        "latestEvent.status," +

                        "latestEvent.reason," +

                        "latestEvent.time" +

                    ");" +

                "}" +

                "if(dashboard)dashboard.style.display='block';" +

                "var lookalikeDashboard=document.getElementById('lookalikeDashboard');" +

                "var lookalikeCount=document.getElementById('lookalikeCount');" +

                "var lookalikeTotal=0;" +

                "var lastLookalikeEvent=null;" +

                "for(var k=0;k<events.length;k++){" +

                    "if(events[k].isLookalike){" +

                        "lookalikeTotal++;" +

                        "lastLookalikeEvent=events[k];" +

                        "if(typeof window.addLookalikeActivity==='function'){" +

                            "window.addLookalikeActivity(" +

                                "events[k].url," +

                                "events[k].matchedDomain," +

                                "events[k].time" +

                            ");" +

                        "}" +

                    "}" +

                "}" +

                "if(lookalikeCount)lookalikeCount.textContent=lookalikeTotal;" +

                "if(lastLookalikeEvent&&typeof window.updateLatestLookalikeDetection==='function'){" +

                    "window.updateLatestLookalikeDetection(" +

                        "lastLookalikeEvent.url," +

                        "lastLookalikeEvent.reason," +

                        "lastLookalikeEvent.matchedDomain," +

                        "lastLookalikeEvent.time" +

                    ");" +

                "}" +

                "if(lookalikeTotal>0&&lookalikeDashboard)lookalikeDashboard.style.display='block';" +

                "if(typeof window.saveShieldState==='function')" +

                    "{window.saveShieldState();}" +

                "}catch(e){console.log('Could not restore Link Shield state.');}" +

                "})();";


        webView.evaluateJavascript(
                js,
                null
        );
		
		
		    } catch (Exception ignored) {
    }
}


/* =========================================================
 * JS ESCAPING
 * ========================================================= */

private static String escapeJs(
        String value
) {

    if (value == null) {
        return "";
    }


    return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
}


/* =========================================================
 * NOTIFICATION TAP
 * ========================================================= */

@Override
protected void onNewIntent(
        Intent intent
) {

    super.onNewIntent(intent);


    setIntent(intent);


    if (intent != null
            && intent.getBooleanExtra(
                    "open_lookalike",
                    false
            )) {

        openLookalikeDashboard();

    } else if (intent != null
            && intent.getBooleanExtra(
                    "open_link_shield",
                    false
            )) {

        openLinkShieldDashboard();

    } else if (intent != null
            && intent.getBooleanExtra(
                    "open_honeypot",
                    false
            )) {

        openHoneypotVault();
    }
}


/* =========================================================
 * FILE CHOOSER RESULT
 * ========================================================= */

@Override
protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
) {

    super.onActivityResult(
            requestCode,
            resultCode,
            data
    );


    if (requestCode ==
            FILE_CHOOSER_REQUEST) {

        if (filePathCallback == null) {
            return;
        }


        Uri[] results =
                null;


        if (resultCode ==
                RESULT_OK) {

            if (data != null) {

                String dataString =
                        data.getDataString();


                if (dataString != null) {

                    results =
                            new Uri[]{
                                    Uri.parse(
                                            dataString
                                    )
                            };
                }
            }
        }


        filePathCallback
                .onReceiveValue(results);


        filePathCallback =
                null;
    }
}


/* =========================================================
 * TAP SHIELD (TAPJACKING / OVERLAY DETECTION)
 * ========================================================= */

@Override
public boolean dispatchTouchEvent(MotionEvent event) {

    if (event.getAction() == MotionEvent.ACTION_DOWN) {

        boolean fullyObscured =
                (event.getFlags()
                        & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0;

        boolean partiallyObscured = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            partiallyObscured =
                    (event.getFlags()
                            & MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0;
        }

        if (fullyObscured || partiallyObscured) {

            recordTapjackingEvent(fullyObscured);
        }
    }

    return super.dispatchTouchEvent(event);
}

private void recordTapjackingEvent(boolean fullyObscured) {

    try {

        SharedPreferences prefs =
                getSharedPreferences(
                        TAPJACK_PREFS,
                        MODE_PRIVATE
                );

        String saved =
                prefs.getString(
                        TAPJACK_LOG,
                        "[]"
                );

        JSONArray events =
                new JSONArray(saved);

        JSONObject event =
                new JSONObject();

        String time =
                new SimpleDateFormat(
                        "hh:mm:ss a",
                        Locale.getDefault()
                ).format(new Date());

        event.put("time", time);

        event.put(
                "type",
                fullyObscured
                        ? "FULLY_OBSCURED"
                        : "PARTIALLY_OBSCURED"
        );

        events.put(event);

        while (events.length() > 50) {
            events.remove(0);
        }

        int newCount =
                prefs.getInt(TAPJACK_COUNT, 0) + 1;

        prefs.edit()
                .putString(TAPJACK_LOG, events.toString())
                .putInt(TAPJACK_COUNT, newCount)
                .apply();

        runOnUiThread(() -> {

            android.widget.Toast.makeText(
                    MainActivity.this,
                    "⚠ Tap Shield: a tap was intercepted while another app was drawing over CyberShield.",
                    android.widget.Toast.LENGTH_LONG
            ).show();

            if (webView != null) {

                String js =
                        "if(typeof window.onTapjackingDetected === 'function')" +
                        "{window.onTapjackingDetected();}";

                webView.evaluateJavascript(js, null);
            }
        });

    } catch (Exception ignored) {
    }
}


/* =========================================================
 * BACK BUTTON
 * ========================================================= */

@Override
public void onBackPressed() {

    if (webView != null
            && webView.canGoBack()) {

        webView.goBack();

    } else {

        super.onBackPressed();
    }
}


/* =========================================================
 * OPEN LINK SHIELD DASHBOARD
 * ========================================================= */

private void openLinkShieldDashboard() {

    if (webView == null) {
        return;
    }


    webView.postDelayed(
            () -> {

                /*
                 * Restore again here so notification taps
                 * work even when a fresh Activity was created.
                 */
                restoreShieldState();


                String js =
                        "if(typeof window.openLinkShieldDashboard === 'function')" +
                        "{window.openLinkShieldDashboard();}";


                webView.evaluateJavascript(
                        js,
                        null
                );

            },
            300
    );
}


/* =========================================================
 * OPEN LOOKALIKE DASHBOARD
 * ========================================================= */

private void openLookalikeDashboard() {

    if (webView == null) {
        return;
    }


    webView.postDelayed(
            () -> {

                /*
                 * Restore again here so notification taps
                 * work even when a fresh Activity was created.
                 */
                restoreShieldState();


                String js =
                        "if(typeof window.openLookalikeDashboard === 'function')" +
                        "{window.openLookalikeDashboard();}";


                webView.evaluateJavascript(
                        js,
                        null
                );

            },
            300
    );
}


/* =========================================================
 * OPEN HONEYPOT VAULT
 *
 * Honeypot Vault is its own standalone page (not a toggled
 * section inside index.html like Link Shield/Lookalike), so
 * this just navigates the WebView straight to it.
 * ========================================================= */

private void openHoneypotVault() {

    if (webView == null) {
        return;
    }

    webView.postDelayed(
            () -> webView.loadUrl(
                    "file:///android_asset/honeypot.html"
            ),
            300
    );
}

}