package com.ohmyhaze

import android.app.Application
import com.ohmyhaze.data.MonitoringStateStore
import com.ohmyhaze.data.OpenMeteoAirQualityClient
import com.ohmyhaze.location.CurrentLocationProvider
import com.ohmyhaze.monitoring.AirQualityWorker
import com.ohmyhaze.monitoring.MonitoringScheduler
import com.ohmyhaze.notifications.AirQualityNotifier
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OhMyHazeApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val monitoringStateStore: MonitoringStateStore by lazy {
        MonitoringStateStore.create(this, applicationScope)
    }

    val currentLocationProvider: CurrentLocationProvider by lazy {
        CurrentLocationProvider(this)
    }

    val airQualityClient: OpenMeteoAirQualityClient by lazy { OpenMeteoAirQualityClient() }

    val airQualityNotifier: AirQualityNotifier by lazy { AirQualityNotifier(this) }

    val monitoringScheduler: MonitoringScheduler by lazy {
        MonitoringScheduler(WorkManager.getInstance(this), AirQualityWorker::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        airQualityNotifier.ensureChannel()
        syncMonitoringSchedule()
    }

    /** Reconciles durable settings and unique WorkManager state after startup or location changes. */
    fun syncMonitoringSchedule() {
        applicationScope.launch {
            val state = monitoringStateStore.state.first()
            monitoringScheduler.setMonitoringEnabled(state.selectedLocation != null && state.monitoringEnabled)
        }
    }
}
