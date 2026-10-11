package com.ohmyhaze.monitoring

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ohmyhaze.data.RequestSource
import java.util.concurrent.TimeUnit

/** Owns only the unique WorkManager entries; worker state and networking live in the worker/store. */
class MonitoringScheduler(
    private val workManager: WorkManager,
    private val workerClass: Class<out ListenableWorker>,
) {
    fun setMonitoringEnabled(enabled: Boolean): Operation =
        if (enabled) {
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest(),
            )
        } else {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        }

    fun enqueueManualCheck(): Operation = workManager.enqueueUniqueWork(
        MANUAL_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        manualRequest(),
    )

    private fun periodicRequest(): PeriodicWorkRequest =
        PeriodicWorkRequest.Builder(workerClass, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(workDataOf(INPUT_REQUEST_SOURCE to RequestSource.AUTOMATIC.name))
            .build()

    private fun manualRequest(): OneTimeWorkRequest =
        OneTimeWorkRequest.Builder(workerClass)
            .setInputData(workDataOf(INPUT_REQUEST_SOURCE to RequestSource.MANUAL.name))
            .build()

    companion object {
        const val PERIODIC_WORK_NAME = "oh-my-haze-periodic-monitoring"
        const val MANUAL_WORK_NAME = "oh-my-haze-manual-check"
        const val INPUT_REQUEST_SOURCE = "request_source"
        const val CHECK_INTERVAL_MINUTES = 60L
    }
}
