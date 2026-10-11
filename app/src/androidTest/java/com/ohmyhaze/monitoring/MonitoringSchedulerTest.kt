package com.ohmyhaze.monitoring

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.ohmyhaze.data.RequestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class MonitoringSchedulerTest {
    @Test
    fun uniquePeriodicWorkUsesHourlyNetworkConstraintAndSupportsPauseAndResume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workManager = WorkManager.getInstance(context)
        val scheduler = MonitoringScheduler(workManager, NoOpWorker::class.java)
        NoOpWorker.reset()

        scheduler.setMonitoringEnabled(false).result.get(10, TimeUnit.SECONDS)
        scheduler.setMonitoringEnabled(true).result.get(10, TimeUnit.SECONDS)
        scheduler.setMonitoringEnabled(true).result.get(10, TimeUnit.SECONDS)

        val scheduled = workManager.getWorkInfosForUniqueWork(MonitoringScheduler.PERIODIC_WORK_NAME)
            .get(10, TimeUnit.SECONDS)
            .filter { it.state != androidx.work.WorkInfo.State.CANCELLED }
        assertEquals(1, scheduled.size)
        val work = scheduled.single()
        assertEquals(TimeUnit.HOURS.toMillis(1), work.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
        assertFalse(work.constraints.requiresCharging())
        assertFalse(work.constraints.requiresDeviceIdle())

        scheduler.setMonitoringEnabled(false).result.get(10, TimeUnit.SECONDS)
        val paused = workManager.getWorkInfosForUniqueWork(MonitoringScheduler.PERIODIC_WORK_NAME)
            .get(10, TimeUnit.SECONDS)
        assertTrue(paused.none { it.state == androidx.work.WorkInfo.State.ENQUEUED || it.state == androidx.work.WorkInfo.State.RUNNING })

        scheduler.setMonitoringEnabled(true).result.get(10, TimeUnit.SECONDS)
        val resumed = workManager.getWorkInfosForUniqueWork(MonitoringScheduler.PERIODIC_WORK_NAME)
            .get(10, TimeUnit.SECONDS)
            .filter { it.state != androidx.work.WorkInfo.State.CANCELLED }
        assertEquals(1, resumed.size)
        scheduler.setMonitoringEnabled(false).result.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun manualEntryPointUsesOneUniqueUnconstrainedRequestWithManualSource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(MonitoringScheduler.MANUAL_WORK_NAME).result.get(10, TimeUnit.SECONDS)
        val scheduler = MonitoringScheduler(workManager, NoOpWorker::class.java)
        NoOpWorker.reset()

        scheduler.enqueueManualCheck().result.get(10, TimeUnit.SECONDS)
        assertTrue(NoOpWorker.started.await(10, TimeUnit.SECONDS))
        val entries = workManager.getWorkInfosForUniqueWork(MonitoringScheduler.MANUAL_WORK_NAME)
            .get(10, TimeUnit.SECONDS)
            .filter { it.state != androidx.work.WorkInfo.State.CANCELLED }

        assertEquals(1, entries.size)
        assertEquals(RequestSource.MANUAL.name, NoOpWorker.lastRequestSource)
        assertEquals(NetworkType.NOT_REQUIRED, entries.single().constraints.requiredNetworkType)
        workManager.cancelUniqueWork(MonitoringScheduler.MANUAL_WORK_NAME).result.get(10, TimeUnit.SECONDS)
    }

    class NoOpWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
        override fun doWork(): Result {
            lastRequestSource = inputData.getString(MonitoringScheduler.INPUT_REQUEST_SOURCE)
            started.countDown()
            return Result.success()
        }

        companion object {
            @Volatile
            var lastRequestSource: String? = null
            lateinit var started: CountDownLatch

            fun reset() {
                lastRequestSource = null
                started = CountDownLatch(1)
            }
        }
    }
}
