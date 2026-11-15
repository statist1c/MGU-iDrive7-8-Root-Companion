// SPDX-License-Identifier: GPL-3.0-only
// Adapted from the user’s MGU Store Installer project (GPL-3.0).
// See licenses/MGU-Store-Installer-GPL-3.0.txt and THIRD-PARTY.md.
package dev.mgu.standalone;

import android.Manifest;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** GitHub-distributed app updates. Downloads and installation always require user action. */
final class AppUpdater {
    static final String REPO = UpdatePolicy.REPOSITORY;
    static final String API = "https://api.github.com/repos/" + REPO + "/releases?per_page=100";
    static final long MAX_APK = 200L * 1024 * 1024;
    static final int CHECK_JOB = 7101, VALIDATE_JOB = 7102, NOTICE = 7103;
    static final String CHANNEL = "app-updates";

    static boolean vehicleBusy() { return BackendService.running || MainActivity.phoneNetworkBusy; }
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("app-updates", Context.MODE_PRIVATE); }
    static File apkFile(Context c) {
        File directory = c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) throw new IllegalStateException("Download storage is unavailable");
        return new File(directory, "mgu-app-update.apk");
    }
    private static final java.util.concurrent.atomic.AtomicBoolean checking = new java.util.concurrent.atomic.AtomicBoolean();
    static void checkInBackground(Context context) {
        Context app=context.getApplicationContext();
        if(!checking.compareAndSet(false,true))return;
        java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        worker.execute(()->{try{check(app,true);}catch(Exception error){prefs(app).edit().putString("status","Update check unavailable. Try Check now.").apply();}finally{checking.set(false);worker.shutdown();}});
    }
    static void schedule(Context c) {
        try {
            String downloaded = prefs(c).getString("download_release", "");
            String installed = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            if (!downloaded.isEmpty() && ReleaseVersion.parse(installed).compareTo(
                    ReleaseVersion.parse(new JSONObject(downloaded).getString("tag"))) >= 0) {
                long id = prefs(c).getLong("download_id", -1);
                prefs(c).edit().remove("download_release").remove("download_id").remove("release")
                        .putBoolean("verified", false).putString("status", "Installed version: " + installed).apply();
                if (id >= 0) c.getSystemService(DownloadManager.class).remove(id);
            }
        } catch (Exception ignored) { /* Missing metadata must not interrupt normal app startup. */ }
        JobScheduler jobs = c.getSystemService(JobScheduler.class);
        if (!prefs(c).getBoolean("automatic", true)) jobs.cancel(CHECK_JOB);
        JobInfo existing = jobs.getPendingJob(CHECK_JOB);
        if (prefs(c).getBoolean("automatic", true) && existing == null) {
            jobs.schedule(new JobInfo.Builder(CHECK_JOB, new ComponentName(c, UpdateJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                    .setPeriodic(24 * 60 * 60 * 1000L, 60 * 60 * 1000L).build());
        }
        long id = prefs(c).getLong("download_id", -1);
        if (id >= 0 && !prefs(c).getBoolean("verified", false)) scheduleValidation(c);
    }
    static void scheduleValidation(Context c) {
        c.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(VALIDATE_JOB,
                new ComponentName(c, UpdateValidationJob.class)).setMinimumLatency(0).build());
    }

    private static byte[] get(String address, int max) throws Exception {
        if (vehicleBusy()) throw new IOException("Wait for the vehicle operation to finish.");
        URL url = new URL(address);
        for (int redirects = 0; redirects < 6; redirects++) {
            String host = url.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equals(url.getProtocol()) || !(host.equals("api.github.com") || host.equals("github.com")
                    || host.equals("release-assets.githubusercontent.com") || host.equals("objects.githubusercontent.com"))) {
                throw new IOException("Refused update download host");
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "MGU-Root-Companion");
            connection.setRequestProperty("Accept", host.equals("api.github.com") ? "application/vnd.github+json" : "application/octet-stream");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status <= 399) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing redirect destination");
                    url = new URL(url, location); continue;
                }
                if (status == 404) return new byte[0];
                if (status != 200) throw new IOException(status == 403 || status == 429
                        ? "GitHub request limit reached. Try again later." : "GitHub returned HTTP " + status);
                try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                        if (out.size() + count > max) throw new IOException("Update metadata exceeds limit");
                        out.write(buffer, 0, count);
                    }
                    return out.toByteArray();
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many update redirects");
    }
    private static boolean assetUrl(String address) { return UpdatePolicy.assetUrl(address); }

    static synchronized JSONObject check(Context c, boolean notify) throws Exception {
        if (vehicleBusy()) { prefs(c).edit().putString("status", "Update checks paused during vehicle work.").apply(); return null; }
        byte[] bytes = get(API, 4 * 1024 * 1024);
        JSONArray releases = bytes.length == 0 ? new JSONArray() : new JSONArray(new String(bytes, StandardCharsets.UTF_8));
        PackageInfo installed = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
        ReleaseVersion current = ReleaseVersion.parse(installed.versionName);
        ReleaseVersion bestVersion = current;
        JSONObject best = null;
        boolean previews = prefs(c).getBoolean("previews", false);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.optJSONObject(i);
            if (release == null || release.optBoolean("draft")) continue;
            String tag = release.optString("tag_name");
            ReleaseVersion version;
            try { version = ReleaseVersion.parse(tag); } catch (IllegalArgumentException ignored) { continue; }
            if (!UpdatePolicy.eligibleVersion(tag, installed.versionName, release.optBoolean("draft"), release.optBoolean("prerelease"), previews)) continue;
            if (version.compareTo(bestVersion) <= 0) continue;
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) continue;
            // Exactly one universal APK avoids choosing the wrong architecture or split package.
            JSONObject apk = null; int count = 0;
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.optJSONObject(j);
                if (asset != null && asset.optString("name").endsWith(".apk")) { apk = asset; count++; }
            }
            if (count != 1 || apk == null || !assetUrl(apk.optString("browser_download_url"))) continue;
            long size = apk.optLong("size", -1);
            if (size <= 0 || size > MAX_APK) continue;
            String digest = apk.optString("digest", "");
            String checksumUrl = "";
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.optJSONObject(j);
                if (asset != null && (asset.optString("name").equals("SHA256SUMS.txt") || asset.optString("name").equals("MGU-iDrive7-8-Root-Companion-" + tag + "-SHA256SUMS.txt"))
                        && assetUrl(asset.optString("browser_download_url"))) checksumUrl = asset.optString("browser_download_url");
            }
            if (!digest.matches("sha256:[a-fA-F0-9]{64}") && checksumUrl.isEmpty()) continue;
            best = new JSONObject().put("tag", tag).put("name", apk.getString("name"))
                    .put("url", apk.getString("browser_download_url")).put("size", size)
                    .put("sha256", digest.matches("sha256:[a-fA-F0-9]{64}") ? digest.substring(7).toLowerCase(Locale.ROOT) : "")
                    .put("checksum_url", checksumUrl).put("notes", release.optString("body", ""));
            bestVersion = version;
        }
        SharedPreferences.Editor editor = prefs(c).edit().putLong("last_check", System.currentTimeMillis())
                .putString("status", best == null ? "No newer compatible release found." : "Update available: " + best.getString("tag"));
        if (best == null) editor.remove("release"); else editor.putString("release", best.toString());
        editor.apply();
        if (notify && best != null && !best.getString("tag").equals(prefs(c).getString("notified", ""))) {
            if (notifyUser(c, "MGU Root Companion update", best.getString("tag") + " is available. Tap to review and download.")) {
                prefs(c).edit().putString("notified", best.getString("tag")).apply();
            }
        }
        return best;
    }

    static synchronized void download(Context c, JSONObject release) throws Exception {
        if (vehicleBusy()) throw new IOException("Wait for the head-unit operation to finish.");
        long existing = prefs(c).getLong("download_id", -1);
        if (existing >= 0) {
            try (Cursor cursor = c.getSystemService(DownloadManager.class).query(new DownloadManager.Query().setFilterById(existing))) {
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED)
                        throw new IOException("An update is already downloading.");
                }
            }
        }
        if (!assetUrl(release.getString("url"))) throw new IOException("Invalid APK URL");
        String sha = release.optString("sha256");
        if (!sha.matches("[a-fA-F0-9]{64}")) {
            String address = release.optString("checksum_url");
            if (!assetUrl(address)) throw new IOException("Missing SHA-256 checksum");
            String sums = new String(get(address, 1024 * 1024), StandardCharsets.UTF_8);
            for (String line : sums.split("\\r?\\n")) {
                java.util.regex.Matcher match = java.util.regex.Pattern.compile("^([a-fA-F0-9]{64})[ \\t]+\\*?(.+)$").matcher(line);
                if (match.matches() && match.group(2).equals(release.getString("name"))) {
                    if (!sha.isEmpty()) throw new IOException("Duplicate APK checksum");
                    sha = match.group(1).toLowerCase(Locale.ROOT);
                }
            }
            if (!sha.matches("[a-fA-F0-9]{64}")) throw new IOException("No checksum for this APK");
        }
        prefs(c).edit().putBoolean("verified", false).remove("download_id").apply();
        if (existing >= 0) c.getSystemService(DownloadManager.class).remove(existing);
        File file = apkFile(c);
        if (file.exists() && !file.delete()) throw new IOException("Cannot replace old update file");
        JSONObject expected = new JSONObject(release.toString()).put("sha256", sha);
        prefs(c).edit().putString("download_release", expected.toString()).apply();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(release.getString("url")))
                .setTitle("MGU Root Companion " + release.getString("tag"))
                .setDescription("Downloading app update; installation needs your confirmation")
                .setMimeType("application/octet-stream").setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file));
        long id = c.getSystemService(DownloadManager.class).enqueue(request);
        prefs(c).edit().putLong("download_id", id).putString("status", "Downloading " + release.getString("tag") + "…").apply();
    }

    static synchronized boolean validateDownload(Context c) throws Exception {
        if (vehicleBusy()) return false;
        long id = prefs(c).getLong("download_id", -1);
        if (id < 0) return false;
        try (Cursor cursor = c.getSystemService(DownloadManager.class).query(new DownloadManager.Query().setFilterById(id))) {
            if (!cursor.moveToFirst()) throw new IOException("Download no longer exists. Download again.");
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_FAILED) throw new IOException("Download failed. Try again.");
            if (status != DownloadManager.STATUS_SUCCESSFUL) return false;
        }
        JSONObject release = new JSONObject(prefs(c).getString("download_release", "{}"));
        File file = apkFile(c);
        if (file.length() != release.getLong("size")) throw new IOException("APK size mismatch");
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] data = new byte[128 * 1024]; int count;
            while ((count = in.read(data)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                hash.update(data, 0, count);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte value : hash.digest()) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
        if (!hex.toString().equalsIgnoreCase(release.getString("sha256"))) throw new IOException("APK SHA-256 mismatch");
        PackageManager manager = c.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = manager.getPackageArchiveInfo(file.getPath(), flags);
        PackageInfo installed = manager.getPackageInfo(c.getPackageName(), flags);
        if (candidate == null || !c.getPackageName().equals(candidate.packageName)) throw new IOException("APK belongs to a different app");
        long candidateCode = Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode;
        long currentCode = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
        if (candidateCode <= currentCode || ReleaseVersion.parse(candidate.versionName).compareTo(ReleaseVersion.parse(installed.versionName)) <= 0
                || ReleaseVersion.parse(candidate.versionName).compareTo(ReleaseVersion.parse(release.getString("tag"))) != 0)
            throw new IOException("APK version does not match a newer release");
        Signature[] a = Build.VERSION.SDK_INT >= 28 ? candidate.signingInfo == null ? null : candidate.signingInfo.getApkContentsSigners() : candidate.signatures;
        Signature[] b = Build.VERSION.SDK_INT >= 28 ? installed.signingInfo == null ? null : installed.signingInfo.getApkContentsSigners() : installed.signatures;
        if (a == null || b == null || a.length == 0 || !new HashSet<>(Arrays.asList(a)).equals(new HashSet<>(Arrays.asList(b))))
            throw new IOException("APK signing certificate differs. Obtain a release signed with the same key; do not uninstall and lose your data.");
        boolean wasVerified = prefs(c).getBoolean("verified", false);
        prefs(c).edit().putBoolean("verified", true).putString("status", release.getString("tag") + " verified. Ready to install.").apply();
        if (!wasVerified) notifyUser(c, "App update ready", "Verified APK downloaded. Tap to review and install.");
        return true;
    }
    static synchronized void rejectDownload(Context c, String reason) {
        prefs(c).edit().putBoolean("verified", false).putString("status", "Update rejected: " + reason).apply();
        long id = prefs(c).getLong("download_id", -1);
        if (id >= 0) c.getSystemService(DownloadManager.class).remove(id);
        prefs(c).edit().remove("download_id").apply();
    }
    static boolean notifyUser(Context c, String title, String detail) {
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT));
        if ((Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                || !manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        Intent intent = new Intent(c, SettingsActivity.class).putExtra("show_app_updates", true);
        PendingIntent open = PendingIntent.getActivity(c, NOTICE, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        manager.notify(NOTICE, new Notification.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_update_status)
                .setContentTitle(title).setContentText(detail).setStyle(new Notification.BigTextStyle().bigText(detail))
                .setContentIntent(open).setAutoCancel(true).build());
        return true;
    }
    static void openInstaller(Activity activity) throws Exception {
        if (vehicleBusy()) throw new IOException("Wait for the head-unit operation to finish.");
        if (!prefs(activity).getBoolean("verified", false)) throw new IOException("Download and verify the update first.");
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
            return;
        }
        Uri uri = Uri.parse("content://" + activity.getPackageName() + ".updates/update.apk");
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("Verified app update", uri));
        activity.startActivity(intent);
    }
}
