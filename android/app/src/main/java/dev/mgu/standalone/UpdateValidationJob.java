// Adapted from the user’s MGU Store Installer project (GPL-3.0).
// See licenses/MGU-Store-Installer-GPL-3.0.txt and THIRD-PARTY.md.
package dev.mgu.standalone;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class UpdateValidationJob extends JobService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Future<?> task;
    @Override public boolean onStartJob(JobParameters params) {
        task = worker.submit(() -> {
            if (AppUpdater.vehicleBusy()) { jobFinished(params, true); return; }
            try { AppUpdater.validateDownload(this); }
            catch (Exception error) { AppUpdater.rejectDownload(this, error.getMessage()); }
            finally { jobFinished(params, false); }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) { if (task != null) task.cancel(true); return true; }
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
