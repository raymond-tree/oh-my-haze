package com.ohmyhaze.monitoring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ohmyhaze.OhMyHazeApplication
import com.ohmyhaze.data.RequestSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared manual and periodic worker. Each request uses a saved-coordinate snapshot only. */
class AirQualityWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? OhMyHazeApplication ?: return Result.failure()
        val source = inputData.getString(MonitoringScheduler.INPUT_REQUEST_SOURCE)
            ?.let { runCatching { RequestSource.valueOf(it) }.getOrNull() }
            ?: RequestSource.AUTOMATIC
        val snapshot = app.monitoringStateStore.snapshotForRequest(source) ?: return Result.success()

        AirQualityRequestCoordinator.execute(
            snapshot = snapshot,
            fetch = {
                withContext(Dispatchers.IO) { app.airQualityClient.fetch(snapshot.location) }
            },
            commit = { response ->
                app.monitoringStateStore.applyResponse(snapshot, response) { alert ->
                    app.airQualityNotifier.notify(alert, snapshot.location.label)
                }
            },
        )
        // Failures are recorded as state and retried only at the next scheduled/manual check.
        return Result.success()
    }
}
