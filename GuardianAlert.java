package com.cybershield.app;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Guardian Alert core: no audio, only phone events. Level 1 = unknown call answered,
   level 2 = payment app opened, level 3 = remote app / new app appeared. */
public class GuardianAlert {

    static final String PREFS = "guardian_prefs";
    static final long WINDOW_MS = 30L * 60 * 1000;

    /* Edit these lists to add more apps. */
    static final Set<String> PAY = new HashSet<>(Arrays.asList(
            "com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm",
            "in.org.npci.upiapp", "com.dreamplug.androidapp", "com.mobikwik_new",
            "com.freecharge.android", "com.sbi.upi", "com.csam.icici.bank.imobile",
            "com.snapwork.hdfc", "com.axis.mobile"));
    static final Set<String> REMOTE = new HashSet<>(Arrays.asList(
            "com.anydesk.anydeskandroid", "com.teamviewer.quicksupport.market",
            "com.teamviewer.host.market", "com.rustdesk", "com.sand.airdroid"));

    static SharedPreferences sp(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static boolean has(Context c, String p) { return c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    static String norm(String n) {
        if (n == null) return "";
        String d = n.replaceAll("\\D", "");
        return d.length() > 10 ? d.substring(d.length() - 10) : d;
    }

    static List<String> list(Context c, String k) {
        List<String> l = new ArrayList<>();
        String s = sp(c).getString(k, "");
        if (!s.isEmpty()) l.addAll(Arrays.asList(s.split(",")));
        return l;
    }

    static void putList(Context c, String k, List<String> l) {
        sp(c).edit().putString(k, TextUtils.join(",", l)).apply();
    }

    static String owner(Context c) {
        String o = sp(c).getString("owner", "");
        return o.isEmpty() ? "this phone's owner" : o;
    }

    static boolean isKnown(Context c, String number) {
        String n = norm(number);
        if (n.isEmpty()) return false;
        if (list(c, "trusted").contains(n)) return true;
        if (!has(c, Manifest.permission.READ_CONTACTS)) return false;
        try (Cursor cu = c.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                new String[]{ContactsContract.PhoneLookup._ID}, null, null, null)) {
            return cu != null && cu.getCount() > 0;
        } catch (Exception e) { return false; }
    }

    /* Called from CallStateReceiver on every phone-state broadcast. */
    static void onPhoneState(Context c, String state, String number) {
        SharedPreferences p = sp(c);
        if (!p.getBoolean("enabled", false) || state == null) return;
        log(c, 0, "Phone state: " + state + (number != null && !number.isEmpty() ? " (number visible)" : ""));
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) && number != null) {
            p.edit().putString("ring_number", number).apply();
        }
        String last = p.getString("g_last", TelephonyManager.EXTRA_STATE_IDLE);
        if (state.equals(last)) return;
        p.edit().putString("g_last", state).apply();
        long now = System.currentTimeMillis();

        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                && !TelephonyManager.EXTRA_STATE_RINGING.equals(last)) {
            log(c, 0, "Call started from this phone (outgoing). Not watching.");
        }

        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)
                && TelephonyManager.EXTRA_STATE_RINGING.equals(last)) {
            String num = p.getString("ring_number", "");
            if (isKnown(c, num)) { log(c, 0, "Known caller answered. Not watching."); return; }
            p.edit().putBoolean("armed", true).putBoolean("callActive", true)
                    .putLong("armStart", now).putInt("level", 1).putInt("smsLevel", 0)
                    .putLong("lastBrake", 0).putString("arm_number", num)
                    .remove("ring_number").apply();
            log(c, 1, "Unknown caller answered. Watching for payments.");
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state) && p.getBoolean("callActive", false)) {
            p.edit().putBoolean("callActive", false).putLong("armedUntil", now + WINDOW_MS).apply();
            if (p.getInt("level", 0) >= 2) {
                sms(c, "CyberShield: the unknown call on " + owner(c) + "'s phone has ended. Please still check in.");
            }
            log(c, p.getInt("level", 0), "Call ended. Watching 30 more minutes.");
        }
    }

    static boolean active(Context c) {
        SharedPreferences p = sp(c);
        return p.getBoolean("enabled", false) && p.getBoolean("armed", false)
                && (p.getBoolean("callActive", false) || System.currentTimeMillis() < p.getLong("armedUntil", 0));
    }

    static void fire(Context c, int level, String why) {
        SharedPreferences p = sp(c);
        if (level <= p.getInt("smsLevel", 0)) return;
        p.edit().putInt("level", level).putInt("smsLevel", level).apply();
        log(c, level, why);
        String who = owner(c);
        sms(c, level == 2
                ? "CyberShield alert: " + who + " is on a call with an unknown number and just opened a payment app. Call " + who + " now and tell them to hang up and not send money."
                : "CyberShield EMERGENCY: " + who + " is on an unknown call and a remote-control app or new app appeared. Call " + who + " NOW.");
        brake(c, level);
    }

    static void brake(Context c, int level) {
        sp(c).edit().putLong("lastBrake", System.currentTimeMillis()).apply();
        log(c, level, "Brake screen requested");
        Intent i = new Intent(c, GuardianBrakeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("level", level);
        try { c.startActivity(i); } catch (Exception e) { log(c, 0, "Brake screen blocked: " + e); }
    }

    static void sms(Context c, String text) {
        if (!has(c, Manifest.permission.SEND_SMS)) { log(c, 0, "SMS permission missing. Alert not sent."); return; }
        SmsManager m = Build.VERSION.SDK_INT >= 31 ? c.getSystemService(SmsManager.class) : SmsManager.getDefault();
        int sent = 0;
        for (String g : list(c, "guardians")) {
            try { m.sendMultipartTextMessage(g, null, m.divideMessage(text), null, null); sent++; }
            catch (Exception e) { log(c, 0, "SMS to a guardian failed: " + e); }
        }
        log(c, 0, "SMS sent to " + sent + " guardian(s)");
    }

    static void trust(Context c) {
        List<String> t = list(c, "trusted");
        String n = norm(sp(c).getString("arm_number", ""));
        if (!n.isEmpty() && !t.contains(n)) { t.add(n); putList(c, "trusted", t); }
        sp(c).edit().putBoolean("armed", false).putInt("level", 0).apply();
        log(c, 0, "Number marked as trusted. Alerts stopped.");
    }

    static void log(Context c, int level, String text) {
        try {
            JSONArray old = new JSONArray(sp(c).getString("log", "[]"));
            JSONArray out = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("t", System.currentTimeMillis()); o.put("l", level); o.put("x", text);
            out.put(o);
            for (int i = 0; i < old.length() && i < 59; i++) out.put(old.get(i));
            sp(c).edit().putString("log", out.toString()).apply();
        } catch (Exception ignored) {}
    }

    /* The service writes this every ~10 s so other code can tell whether it is alive. */
    static void heartbeat(Context c) { sp(c).edit().putLong("hb", System.currentTimeMillis()).apply(); }

    static boolean serviceAlive(Context c) {
        return System.currentTimeMillis() - sp(c).getLong("hb", 0) < 25000;
    }

    static boolean usageOk(Context c) {
        AppOpsManager a = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        return a.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), c.getPackageName())
                == AppOpsManager.MODE_ALLOWED;
    }

    static String state(Context c) {
        try {
            SharedPreferences p = sp(c);
            JSONObject o = new JSONObject(), pm = new JSONObject();
            o.put("enabled", p.getBoolean("enabled", false));
            o.put("owner", p.getString("owner", ""));
            o.put("guardians", new JSONArray(list(c, "guardians")));
            o.put("trusted", new JSONArray(list(c, "trusted")));
            o.put("armed", active(c));
            o.put("service", serviceAlive(c));
            pm.put("phone", has(c, Manifest.permission.READ_PHONE_STATE) && has(c, Manifest.permission.READ_CALL_LOG)
                    && has(c, Manifest.permission.READ_CONTACTS));
            pm.put("sms", has(c, Manifest.permission.SEND_SMS));
            pm.put("usage", usageOk(c));
            pm.put("overlay", Settings.canDrawOverlays(c));
            o.put("perms", pm);
            o.put("log", new JSONArray(p.getString("log", "[]")));
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }
}
