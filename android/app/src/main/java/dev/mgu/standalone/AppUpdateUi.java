// SPDX-License-Identifier: GPL-3.0-only
// Adapted from the user’s MGU Store Installer project (GPL-3.0).
// See licenses/MGU-Store-Installer-GPL-3.0.txt and THIRD-PARTY.md.
package dev.mgu.standalone;

import android.Manifest;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;
import org.json.JSONObject;

final class AppUpdateUi {
    static void attach(SettingsActivity a, LinearLayout parent) {
        a.updateHeading(parent, "App updates");
        a.updateStatus = a.updateLabel(AppUpdater.prefs(a).getString("status", "Check GitHub releases for a newer app version."));
        CheckBox automatic = a.updateCheck("Check daily for app updates", AppUpdater.prefs(a).getBoolean("automatic", true));
        automatic.setOnCheckedChangeListener((button, enabled) -> {
            AppUpdater.prefs(a).edit().putBoolean("automatic", enabled).apply();
            AppUpdater.schedule(a);
        });
        CheckBox previews = a.updateCheck("Include preview releases", AppUpdater.prefs(a).getBoolean("previews", false));
        previews.setOnCheckedChangeListener((button, enabled) -> {
            AppUpdater.prefs(a).edit().putBoolean("previews", enabled).remove("release").remove("notified")
                    .putString("status", "Release channel changed. Tap Check now.").apply();
        });
        a.updateButton("Check now", true, () -> check(a, true));
        a.updateButton("Review available update", false, () -> {
            try {
                String release = AppUpdater.prefs(a).getString("release", "");
                if (release.isEmpty()) { check(a, true); return; }
                review(a, new JSONObject(release));
            } catch (Exception error) { a.message(error.getMessage()); }
        });
        a.updateButton("Install downloaded app update", false, () -> install(a));
        a.updateButton("App update notification permission", false, () -> {
            if (Build.VERSION.SDK_INT >= 33 && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 71);
            } else {
                a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName()));
            }
        });
        a.updateLabel("Checks use your phone’s internet. Android controls background timing. Downloads begin only when you choose; installation requires Android’s confirmation. Enable ‘Allow from this source’ only when prompted for an update.");
    }

    static void check(SettingsActivity a, boolean showResult) {
        if (a.updateBusy) return;
        if (AppUpdater.vehicleBusy()) { if (showResult) a.message("Wait for vehicle work to finish before checking updates."); return; }
        a.updateBusy = true;
        AppUpdater.prefs(a).edit().putString("status", "Checking GitHub releases…").apply();
        a.updateWorker.execute(() -> {
            try {
                JSONObject release = AppUpdater.check(a.getApplicationContext(), !showResult);
                a.updateHandler.post(() -> {
                    a.updateBusy = false;
                    if (a.isDestroyed() || !showResult) return;
                    if (release == null) a.message("No newer compatible release found. Releases need one universal APK and a SHA-256 checksum. Build artifacts alone are not releases.");
                    else review(a, release);
                });
            } catch (Exception error) {
                AppUpdater.prefs(a).edit().putString("status", "Update check failed: " + error.getMessage()).apply();
                a.updateHandler.post(() -> { a.updateBusy = false; if (!a.isDestroyed() && showResult) a.message(error.getMessage()); });
            }
        });
    }

    static void review(SettingsActivity a, JSONObject release) {
        String notes = release.optString("notes", "");
        if (notes.length() > 5000) notes = notes.substring(0, 5000) + "…";
        new AlertDialog.Builder(a).setTitle("MGU Root Companion " + release.optString("tag"))
                .setMessage(String.format(java.util.Locale.ROOT, "Download size: %.1f MB\n\n%s\n\nDownload and verify this update?", release.optLong("size") / 1048576.0, notes))
                .setNegativeButton("Later", null).setPositiveButton("Download", (d, w) -> {
                    if (a.updateBusy) return;
                    a.updateBusy = true;
                    a.updateWorker.execute(() -> {
                        try { AppUpdater.download(a.getApplicationContext(), release); }
                        catch (Exception error) { a.updateHandler.post(() -> { if (!a.isDestroyed()) a.message(error.getMessage()); }); }
                        finally { a.updateHandler.post(() -> a.updateBusy = false); }
                    });
                }).show();
    }

    static void install(SettingsActivity a) {
        if (a.updateBusy) return;
        a.updateBusy = true;
        a.updateWorker.execute(() -> {
            try {
                boolean ready = AppUpdater.validateDownload(a.getApplicationContext());
                a.updateHandler.post(() -> {
                    a.updateBusy = false;
                    if (a.isDestroyed()) return;
                    if (!ready) { a.message("No completed app update is available. Check for updates or wait for the download."); return; }
                    try { AppUpdater.openInstaller(a); }
                    catch (Exception error) { a.message(error.getMessage()); }
                });
            } catch (Exception error) {
                AppUpdater.rejectDownload(a.getApplicationContext(), error.getMessage());
                a.updateHandler.post(() -> { a.updateBusy = false; if (!a.isDestroyed()) a.message(error.getMessage()); });
            }
        });
    }
}
