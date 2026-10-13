package com.cybershield.app;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Locale;

/*
 * Runs on a repeating ~15-minute alarm scheduled by
 * MainActivity.scheduleHoneypotChecks(). Mirrors the same
 * compare-against-baseline logic as
 * MainActivity.getHoneypotStatus() (the one the Honeypot
 * Vault page calls directly when the app is open), but reads
 * SharedPreferences and ContentResolver via the Context this
 * receiver is given, since it has no Activity/WebView of its
 * own. Fires a real notification the moment it finds a new
 * trigger, instead of waiting for someone to open the app
 * and check manually.
 */
public class HoneypotCheckReceiver extends BroadcastReceiver {

    private static final String HONEYPOT_PREFS =
            "cybershield_honeypot_persistence";

    private static final String HONEYPOT_BASELINE =
            "honeypot_baseline";

    private static final String HONEYPOT_LOG =
            "honeypot_log";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context == null) {
            return;
        }

        try {

            SharedPreferences prefs =
                    context.getSharedPreferences(
                            HONEYPOT_PREFS,
                            Context.MODE_PRIVATE
                    );

            JSONArray baseline =
                    new JSONArray(prefs.getString(HONEYPOT_BASELINE, "[]"));

            if (baseline.length() == 0) {
                return;
            }

            ContentResolver resolver = context.getContentResolver();

            JSONArray log =
                    new JSONArray(prefs.getString(HONEYPOT_LOG, "[]"));

            for (int i = 0; i < baseline.length(); i++) {

                JSONObject entry = baseline.getJSONObject(i);

                String name = entry.getString("name");
                String originalHash = entry.getString("hash");
                Uri uri = Uri.parse(entry.getString("uri"));

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

                    if (!alreadyLogged(log, name, "DELETED")) {

                        log.put(makeEvent(name, "DELETED"));

                        MainActivity.showHoneypotNotification(
                                context,
                                name,
                                "DELETED"
                        );
                    }

                } else {

                    String currentHash = hashFile(resolver, uri);

                    if (!currentHash.isEmpty()
                            && !currentHash.equals(originalHash)
                            && !alreadyLogged(log, name, "MODIFIED")) {

                        log.put(makeEvent(name, "MODIFIED"));

                        MainActivity.showHoneypotNotification(
                                context,
                                name,
                                "MODIFIED"
                        );
                    }
                }
            }

            while (log.length() > 50) {
                log.remove(0);
            }

            prefs.edit()
                    .putString(HONEYPOT_LOG, log.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private boolean alreadyLogged(JSONArray log, String name, String type) {

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

    private JSONObject makeEvent(String name, String type) {

        JSONObject o = new JSONObject();

        try {

            o.put("name", name);
            o.put("type", type);
            o.put("time", System.currentTimeMillis());

        } catch (Exception ignored) {
        }

        return o;
    }

    private String hashFile(ContentResolver resolver, Uri uri) {

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
}
