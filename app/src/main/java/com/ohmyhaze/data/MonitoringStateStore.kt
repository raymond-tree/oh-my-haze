package com.ohmyhaze.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import com.ohmyhaze.monitoring.AlertBaseline
import com.ohmyhaze.monitoring.AlertNotification
import com.ohmyhaze.monitoring.AlertPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One local Preferences DataStore for the selected monitoring state. */
class MonitoringStateStore private constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val state: Flow<MonitoringState> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(Preferences::toMonitoringState)

    suspend fun saveLocation(location: SavedMonitoringLocation) {
        StateChangeGate.mutex.withLock {
            mutate { current ->
                if (current.selectedLocation == location) {
                    current
                } else {
                    current.copy(
                        selectedLocation = location,
                        estimate = null,
                        baselineAqi = null,
                        baselineModelValidAt = null,
                        lastSuccessfulCheckAt = null,
                        checkStatus = CheckStatus.NEVER_CHECKED,
                        lastCheckError = null,
                        requestRevision = current.requestRevision + 1,
                    )
                }
            }
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        StateChangeGate.mutex.withLock {
            mutate { current ->
                if (current.monitoringEnabled == enabled) current
                else current.copy(monitoringEnabled = enabled, requestRevision = current.requestRevision + 1)
            }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        StateChangeGate.mutex.withLock {
            mutate { current ->
                if (current.notificationsEnabled == enabled) current
                else current.copy(notificationsEnabled = enabled, requestRevision = current.requestRevision + 1)
            }
        }
    }

    suspend fun setSensitivityFloor(floor: Int) {
        require(floor in MonitoringState.SENSITIVITY_FLOORS) { "Unsupported sensitivity floor" }
        StateChangeGate.mutex.withLock {
            mutate { current ->
                if (current.sensitivityFloor == floor) current
                else current.copy(sensitivityFloor = floor, requestRevision = current.requestRevision + 1)
            }
        }
    }

    suspend fun snapshotForRequest(source: RequestSource): RequestSnapshot? = StateChangeGate.mutex.withLock {
        val current = state.first()
        val location = current.selectedLocation ?: return@withLock null
        if (source == RequestSource.AUTOMATIC && !current.monitoringEnabled) return@withLock null
        RequestSnapshot(location, current.requestRevision, source)
    }

    /**
     * Applies a network result only if the durable revision and saved location still match.
     * Network I/O must happen before this call; the shared lock covers only the state transaction.
     */
    suspend fun applyResponse(
        snapshot: RequestSnapshot,
        result: ApiCheckResult,
        notify: (AlertNotification) -> Unit = {},
    ): ApplyResponseOutcome =
        StateChangeGate.mutex.withLock {
            var outcome = ApplyResponseOutcome.SUPERSEDED
            var pendingNotification: AlertNotification? = null
            dataStore.updateData { preferences ->
                val current = preferences.toMonitoringState()
                val requestStillCurrent = current.requestRevision == snapshot.requestRevision &&
                    current.selectedLocation == snapshot.location &&
                    (snapshot.source == RequestSource.MANUAL || current.monitoringEnabled)
                if (!requestStillCurrent) {
                    outcome = ApplyResponseOutcome.SUPERSEDED
                    preferences
                } else {
                    val updated = when (result) {
                        is ApiCheckResult.Failure -> {
                            outcome = ApplyResponseOutcome.FAILURE_RECORDED
                            current.copy(
                                checkStatus = result.status,
                                lastCheckError = result.safeMessage?.let(::safeError),
                            )
                        }

                        is ApiCheckResult.Success -> {
                            val estimate = result.estimate
                            val modelTimeValid = !estimate.modelValidAt.isAfter(
                                result.checkedAt.plus(MAX_FUTURE_MODEL_AGE),
                            )
                            val withinDisplayWindow = !estimate.modelValidAt.isBefore(
                                result.checkedAt.minus(MAX_DISPLAY_AGE),
                            )
                            val newerThanDisplayed = current.estimate == null ||
                                estimate.modelValidAt.isAfter(current.estimate.modelValidAt)
                            val canReplaceEstimate = modelTimeValid && withinDisplayWindow && newerThanDisplayed
                            outcome = if (canReplaceEstimate) {
                                ApplyResponseOutcome.ESTIMATE_UPDATED
                            } else {
                                ApplyResponseOutcome.CHECK_RECORDED
                            }
                            val alertTransition = if (canReplaceEstimate &&
                                !estimate.modelValidAt.isBefore(result.checkedAt.minus(AlertPolicy.ELIGIBILITY_AGE))
                            ) {
                                AlertPolicy.evaluate(
                                    estimate = estimate,
                                    previous = current.baselineAqi?.let { aqi ->
                                        current.baselineModelValidAt?.let { AlertBaseline(aqi, it) }
                                    },
                                    sensitivityFloor = current.sensitivityFloor,
                                    notificationsEnabled = current.notificationsEnabled,
                                    now = result.checkedAt,
                                )
                            } else {
                                null
                            }
                            pendingNotification = alertTransition?.notification
                            current.copy(
                                estimate = if (canReplaceEstimate) estimate else current.estimate,
                                baselineAqi = alertTransition?.baseline?.aqi ?: current.baselineAqi,
                                baselineModelValidAt = alertTransition?.baseline?.modelValidAt ?: current.baselineModelValidAt,
                                lastSuccessfulCheckAt = listOfNotNull(current.lastSuccessfulCheckAt, result.checkedAt).maxOrNull(),
                                checkStatus = CheckStatus.AVAILABLE,
                                lastCheckError = null,
                            )
                        }
                    }
                    updated.toPreferences()
                }
            }
            pendingNotification?.let { alert -> runCatching { notify(alert) } }
            outcome
        }

    private suspend fun mutate(transform: (MonitoringState) -> MonitoringState): MonitoringState =
        dataStore.updateData { current -> transform(current.toMonitoringState()).toPreferences() }
            .toMonitoringState()

    companion object {
        private const val STORE_NAME = "monitoring_state"
        private val MAX_DISPLAY_AGE: Duration = Duration.ofHours(18)
        private val MAX_FUTURE_MODEL_AGE: Duration = Duration.ofMinutes(5)

        fun create(context: Context, scope: CoroutineScope): MonitoringStateStore {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { appContext.preferencesDataStoreFile(STORE_NAME) },
            )
            return MonitoringStateStore(dataStore)
        }

        internal fun createForTest(file: File, scope: CoroutineScope): MonitoringStateStore {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            return MonitoringStateStore(dataStore)
        }

        private fun safeError(value: String): String = value
            .filter { it == '\t' || it.code >= 0x20 }
            .replace(Regex("\\s+"), " ")
            .take(120)
    }
}

/** Shared within the app process so configuration updates and response commits serialize. */
internal object StateChangeGate {
    val mutex = Mutex()
}

private val selectedLocationLabelKey = stringPreferencesKey("location_label")
private val selectedLocationLatitudeKey = doublePreferencesKey("location_latitude")
private val selectedLocationLongitudeKey = doublePreferencesKey("location_longitude")
private val selectedLocationSourceKey = stringPreferencesKey("location_source")
private val selectedLocationSelectedAtKey = longPreferencesKey("location_selected_at")
private val estimateSourceAqiKey = doublePreferencesKey("estimate_source_us_aqi")
private val estimateModelValidAtKey = longPreferencesKey("estimate_model_valid_at")
private val estimateRetrievedAtKey = longPreferencesKey("estimate_retrieved_at")
private val estimateTimezoneKey = stringPreferencesKey("estimate_timezone")
private val monitoringEnabledKey = booleanPreferencesKey("monitoring_enabled")
private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
private val sensitivityFloorKey = intPreferencesKey("sensitivity_floor")
private val requestRevisionKey = longPreferencesKey("request_revision")
private val baselineAqiKey = intPreferencesKey("baseline_aqi")
private val baselineModelValidAtKey = longPreferencesKey("baseline_model_valid_at")
private val lastSuccessfulCheckAtKey = longPreferencesKey("last_successful_check_at")
private val checkStatusKey = stringPreferencesKey("check_status")
private val lastCheckErrorKey = stringPreferencesKey("last_check_error")

private fun Preferences.toMonitoringState(): MonitoringState {
    val location = runCatching {
        val label = this[selectedLocationLabelKey] ?: return@runCatching null
        val latitude = this[selectedLocationLatitudeKey] ?: return@runCatching null
        val longitude = this[selectedLocationLongitudeKey] ?: return@runCatching null
        val source = this[selectedLocationSourceKey]?.let(LocationSelectionSource::valueOf) ?: return@runCatching null
        val selectedAt = this[selectedLocationSelectedAtKey]?.let(Instant::ofEpochMilli) ?: return@runCatching null
        SavedMonitoringLocation(label, latitude, longitude, source, selectedAt)
    }.getOrNull()

    val estimate = runCatching {
        val value = this[estimateSourceAqiKey] ?: return@runCatching null
        val modelTime = this[estimateModelValidAtKey]?.let(Instant::ofEpochMilli) ?: return@runCatching null
        val retrievedAt = this[estimateRetrievedAtKey]?.let(Instant::ofEpochMilli) ?: return@runCatching null
        val timezone = this[estimateTimezoneKey] ?: return@runCatching null
        AirQualityEstimate(value, modelTime, retrievedAt, timezone)
    }.getOrNull()

    val baselineAt = this[baselineModelValidAtKey]?.let { runCatching { Instant.ofEpochMilli(it) }.getOrNull() }
    val baselineAqi = this[baselineAqiKey]?.takeIf { it >= 0 }
    val hasCompleteBaseline = baselineAt != null && baselineAqi != null
    val revision = this[requestRevisionKey]?.takeIf { it >= 0L } ?: 0L
    val floor = this[sensitivityFloorKey]?.takeIf { it in MonitoringState.SENSITIVITY_FLOORS }
        ?: MonitoringState.DEFAULT_SENSITIVITY_FLOOR
    val status = this[checkStatusKey]?.let { runCatching { CheckStatus.valueOf(it) }.getOrNull() }
        ?: CheckStatus.NEVER_CHECKED

    return MonitoringState(
        selectedLocation = location,
        estimate = estimate,
        monitoringEnabled = this[monitoringEnabledKey] ?: true,
        notificationsEnabled = this[notificationsEnabledKey] ?: true,
        sensitivityFloor = floor,
        requestRevision = revision,
        baselineAqi = if (hasCompleteBaseline) baselineAqi else null,
        baselineModelValidAt = if (hasCompleteBaseline) baselineAt else null,
        lastSuccessfulCheckAt = this[lastSuccessfulCheckAtKey]?.let { runCatching { Instant.ofEpochMilli(it) }.getOrNull() },
        checkStatus = status,
        lastCheckError = this[lastCheckErrorKey]?.take(120),
    )
}

private fun MonitoringState.toPreferences(): MutablePreferences =
    mutablePreferencesOf().apply {
        selectedLocation?.let { location ->
            this[selectedLocationLabelKey] = location.label
            this[selectedLocationLatitudeKey] = location.latitude
            this[selectedLocationLongitudeKey] = location.longitude
            this[selectedLocationSourceKey] = location.selectionSource.name
            this[selectedLocationSelectedAtKey] = location.selectedAt.toEpochMilli()
        }
        estimate?.let { current ->
            this[estimateSourceAqiKey] = current.sourceUsAqi
            this[estimateModelValidAtKey] = current.modelValidAt.toEpochMilli()
            this[estimateRetrievedAtKey] = current.retrievedAt.toEpochMilli()
            this[estimateTimezoneKey] = current.providerTimezone
        }
        this[monitoringEnabledKey] = monitoringEnabled
        this[notificationsEnabledKey] = notificationsEnabled
        this[sensitivityFloorKey] = sensitivityFloor
        this[requestRevisionKey] = requestRevision
        baselineAqi?.let { this[baselineAqiKey] = it }
        baselineModelValidAt?.let { this[baselineModelValidAtKey] = it.toEpochMilli() }
        lastSuccessfulCheckAt?.let { this[lastSuccessfulCheckAtKey] = it.toEpochMilli() }
        this[checkStatusKey] = checkStatus.name
        lastCheckError?.let { this[lastCheckErrorKey] = it.take(120) }
    }
