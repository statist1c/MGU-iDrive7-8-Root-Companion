// Adapted from the user’s MGU Store Installer project (GPL-3.0).
// See licenses/MGU-Store-Installer-GPL-3.0.txt and THIRD-PARTY.md.
package dev.mgu.standalone;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Android persists and reschedules this best-effort daily job across reboots. */
public final class UpdateJob extends JobService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> task;
    @Override public boolean onStartJob(JobParameters params) {
        task = worker.submit(() -> {
            try {
                if (AppUpdater.vehicleBusy()) { jobFinished(params, true); return; }
                if (AppUpdater.prefs(this).getBoolean("automatic", true)) AppUpdater.check(this, true);
                jobFinished(params, false);
            } catch (Exception error) {
                AppUpdater.prefs(this).edit().putString("status", "Update check unavailable. Try Check now.").apply();
                jobFinished(params, true);
            }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (task != null) task.cancel(true);
        return true;
    }
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
