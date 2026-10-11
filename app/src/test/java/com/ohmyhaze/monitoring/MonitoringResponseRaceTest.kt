package com.ohmyhaze.monitoring

import com.ohmyhaze.data.AirQualityEstimate
import com.ohmyhaze.data.ApiCheckResult
import com.ohmyhaze.data.ApplyResponseOutcome
import com.ohmyhaze.data.CheckStatus
import com.ohmyhaze.data.LocationSelectionSource
import com.ohmyhaze.data.MonitoringStateStore
import com.ohmyhaze.data.RequestSnapshot
import com.ohmyhaze.data.RequestSource
import com.ohmyhaze.data.SavedMonitoringLocation
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringResponseRaceTest {
    @Test
    fun locationChangeDuringHeldRequestDiscardsEveryOldLocationField() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(location("Kuala Lumpur", 3.139, 101.6869))

        val started = CompletableDeferred<RequestSnapshot>()
        val responseGate = CompletableDeferred<ApiCheckResult>()
        val oldLocationAlerts = mutableListOf<AlertNotification>()
        val request = async {
            val snapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
            started.complete(snapshot)
            val response = FakeAirQualityClient { responseGate.await() }.fetch(snapshot.location)
            store.applyResponse(snapshot, response, oldLocationAlerts::add)
        }
        val oldSnapshot = started.await()

        store.saveLocation(location("Shah Alam", 3.0733, 101.5185))
        val stateAfterLocationChange = store.state.first()
        responseGate.complete(success(aqi = 220.0, modelTime = Instant.now().minusSeconds(60), checkedAt = Instant.now()))

        assertEquals(ApplyResponseOutcome.SUPERSEDED, request.await())
        assertEquals(stateAfterLocationChange, store.state.first())
        assertEquals("Shah Alam", stateAfterLocationChange.selectedLocation?.label)
        assertNull(stateAfterLocationChange.estimate)
        assertNull(stateAfterLocationChange.baselineAqi)
        assertNull(stateAfterLocationChange.baselineModelValidAt)
        assertNull(stateAfterLocationChange.lastSuccessfulCheckAt)
        assertEquals(CheckStatus.NEVER_CHECKED, stateAfterLocationChange.checkStatus)
        assertEquals(oldSnapshot.requestRevision + 1, stateAfterLocationChange.requestRevision)
        assertTrue(oldLocationAlerts.isEmpty())

        scope.cancel()
        file.delete()
    }

    @Test
    fun monitoringAndAlertPreferenceChangesInvalidateCapturedRequests() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(location("Kuala Lumpur", 3.139, 101.6869))
        val automaticSnapshot = store.snapshotForRequest(RequestSource.AUTOMATIC)!!
        store.setMonitoringEnabled(false)

        assertEquals(
            ApplyResponseOutcome.SUPERSEDED,
            store.applyResponse(automaticSnapshot, success(aqi = 130.0)),
        )
        assertEquals(CheckStatus.NEVER_CHECKED, store.state.first().checkStatus)
        assertNull(store.state.first().lastSuccessfulCheckAt)

        val manualSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        store.setNotificationsEnabled(false)
        assertEquals(
            ApplyResponseOutcome.SUPERSEDED,
            store.applyResponse(manualSnapshot, ApiCheckResult.Failure(CheckStatus.NETWORK_ERROR, "offline")),
        )

        val settingsSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        store.setSensitivityFloor(151)
        assertEquals(
            ApplyResponseOutcome.SUPERSEDED,
            store.applyResponse(settingsSnapshot, ApiCheckResult.Failure(CheckStatus.HTTP_ERROR, "http")),
        )
        val current = store.state.first()
        assertFalseMonitoringSettingsChangedState(current)

        scope.cancel()
        file.delete()
    }

    @Test
    fun sameRevisionManualAndScheduledResponsesCannotRegressEstimateWhenCompletedOutOfOrder() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(location("Kuala Lumpur", 3.139, 101.6869))
        val manualStarted = CompletableDeferred<RequestSnapshot>()
        val automaticStarted = CompletableDeferred<RequestSnapshot>()
        val manualResponse = CompletableDeferred<ApiCheckResult>()
        val automaticResponse = CompletableDeferred<ApiCheckResult>()

        val manual = async {
            val snapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
            manualStarted.complete(snapshot)
            store.applyResponse(
                snapshot,
                FakeAirQualityClient { manualResponse.await() }.fetch(snapshot.location),
            )
        }
        val automatic = async {
            val snapshot = store.snapshotForRequest(RequestSource.AUTOMATIC)!!
            automaticStarted.complete(snapshot)
            store.applyResponse(
                snapshot,
                FakeAirQualityClient { automaticResponse.await() }.fetch(snapshot.location),
            )
        }
        val manualSnapshot = manualStarted.await()
        val automaticSnapshot = automaticStarted.await()
        assertEquals(manualSnapshot.requestRevision, automaticSnapshot.requestRevision)
        assertEquals(manualSnapshot.location, automaticSnapshot.location)

        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        automaticResponse.complete(success(aqi = 155.0, modelTime = now.minusSeconds(1_800), checkedAt = now.minusSeconds(1)))
        assertEquals(ApplyResponseOutcome.ESTIMATE_UPDATED, automatic.await())
        manualResponse.complete(success(aqi = 220.0, modelTime = now.minusSeconds(3_600), checkedAt = now))
        assertEquals(ApplyResponseOutcome.CHECK_RECORDED, manual.await())

        val restored = store.state.first()
        assertEquals(155, restored.estimate?.aqi)
        assertEquals(now.minusSeconds(1_800), restored.estimate?.modelValidAt)
        assertEquals(now, restored.lastSuccessfulCheckAt)
        assertEquals(CheckStatus.AVAILABLE, restored.checkStatus)

        val duplicateSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val duplicateResult = store.applyResponse(
            duplicateSnapshot,
            success(aqi = 300.0, modelTime = now.minusSeconds(1_800), checkedAt = now.plusSeconds(1)),
        )
        assertEquals(ApplyResponseOutcome.CHECK_RECORDED, duplicateResult)
        assertEquals(155, store.state.first().estimate?.aqi)
        assertEquals(now.plusSeconds(1), store.state.first().lastSuccessfulCheckAt)

        scope.cancel()
        file.delete()
    }

    @Test
    fun persistedRevisionRejectsPreRestartResponseAfterStateChanges() = runBlocking<Unit> {
        val file = tempFile()
        val firstScope = testScope()
        val beforeRestart = MonitoringStateStore.createForTest(file, firstScope)
        beforeRestart.saveLocation(location("Kuala Lumpur", 3.139, 101.6869))
        val oldSnapshot = beforeRestart.snapshotForRequest(RequestSource.AUTOMATIC)!!
        beforeRestart.setSensitivityFloor(151)
        val persisted = beforeRestart.state.first()
        firstScope.cancel()

        val reopenedScope = testScope()
        val afterRestart = MonitoringStateStore.createForTest(file, reopenedScope)
        assertEquals(persisted, afterRestart.state.first())
        assertEquals(
            ApplyResponseOutcome.SUPERSEDED,
            afterRestart.applyResponse(oldSnapshot, success(aqi = 220.0)),
        )
        assertEquals(persisted, afterRestart.state.first())
        assertTrue(afterRestart.state.first().requestRevision > oldSnapshot.requestRevision)

        reopenedScope.cancel()
        file.delete()
    }

    @Test
    fun coalescedManualAndScheduledChecksFetchAndNotifyOnceForOneRevision() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(location("Kuala Lumpur", 3.139, 101.6869))
        val initial = store.snapshotForRequest(RequestSource.MANUAL)!!
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        store.applyResponse(initial, success(80.0, now.minusSeconds(3_600), now.minusSeconds(2_000)))

        val manualSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val automaticSnapshot = store.snapshotForRequest(RequestSource.AUTOMATIC)!!
        assertEquals(manualSnapshot.requestRevision, automaticSnapshot.requestRevision)
        val responseGate = CompletableDeferred<ApiCheckResult>()
        val requestStarted = CompletableDeferred<Unit>()
        val fetchCount = AtomicInteger()
        val alerts = mutableListOf<AlertNotification>()
        val fetch: suspend () -> ApiCheckResult = {
            fetchCount.incrementAndGet()
            requestStarted.complete(Unit)
            responseGate.await()
        }
        val manual = async {
            AirQualityRequestCoordinator.execute(manualSnapshot, fetch) { result ->
                store.applyResponse(manualSnapshot, result, alerts::add)
            }
        }
        requestStarted.await()
        val automatic = async {
            AirQualityRequestCoordinator.execute(automaticSnapshot, fetch) { result ->
                store.applyResponse(automaticSnapshot, result, alerts::add)
            }
        }
        yield()
        responseGate.complete(success(140.0, now.minusSeconds(1_000), now))

        assertEquals(ApplyResponseOutcome.ESTIMATE_UPDATED, manual.await())
        assertEquals(ApplyResponseOutcome.ESTIMATE_UPDATED, automatic.await())
        assertEquals(1, fetchCount.get())
        assertEquals(1, alerts.size)
        assertEquals(140, store.state.first().baselineAqi)

        scope.cancel()
        file.delete()
    }

    private class FakeAirQualityClient(private val response: suspend () -> ApiCheckResult) {
        suspend fun fetch(@Suppress("UNUSED_PARAMETER") location: SavedMonitoringLocation): ApiCheckResult = response()
    }

    private fun success(
        aqi: Double,
        modelTime: Instant = Instant.now().minusSeconds(60),
        checkedAt: Instant = Instant.now(),
    ) = ApiCheckResult.Success(
        estimate = AirQualityEstimate(
            sourceUsAqi = aqi,
            modelValidAt = modelTime,
            retrievedAt = checkedAt,
            providerTimezone = "Asia/Kuala_Lumpur",
        ),
        checkedAt = checkedAt,
    )

    private fun location(label: String, latitude: Double, longitude: Double) = SavedMonitoringLocation(
        label = label,
        latitude = latitude,
        longitude = longitude,
        selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
        selectedAt = Instant.now(),
    )

    private fun tempFile(): File = File.createTempFile("oh-my-haze-race", ".preferences_pb").apply { delete() }

    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun assertFalseMonitoringSettingsChangedState(state: com.ohmyhaze.data.MonitoringState) {
        assertEquals(false, state.monitoringEnabled)
        assertEquals(false, state.notificationsEnabled)
        assertEquals(151, state.sensitivityFloor)
        assertEquals(CheckStatus.NEVER_CHECKED, state.checkStatus)
        assertNull(state.estimate)
        assertNull(state.lastSuccessfulCheckAt)
        assertNull(state.lastCheckError)
        assertTrue(state.requestRevision >= 4L)
    }
}
