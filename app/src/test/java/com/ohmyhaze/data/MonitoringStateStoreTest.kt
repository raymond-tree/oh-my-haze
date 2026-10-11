package com.ohmyhaze.data

import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringStateStoreTest {
    @Test
    fun defaultsHaveNoSavedLocationAndUseApprovedSensitivity() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)

        val state = store.state.first()
        assertNull(state.selectedLocation)
        assertEquals(101, state.sensitivityFloor)
        assertTrue(state.notificationsEnabled)
        assertEquals(0L, state.requestRevision)

        scope.cancel()
        file.delete()
    }

    @Test
    fun locationAndPreferencesSurviveRecreationAndOnlyMeaningfulChangesAdvanceRevision() = runBlocking<Unit> {
        val file = tempFile()
        val firstScope = testScope()
        val first = MonitoringStateStore.createForTest(file, firstScope)
        val selectedAt = Instant.now()
        first.saveLocation(
            SavedMonitoringLocation(
                label = "Kuala Lumpur",
                latitude = 3.139,
                longitude = 101.6869,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = selectedAt,
            ),
        )
        first.setMonitoringEnabled(false)
        first.setNotificationsEnabled(false)
        first.setSensitivityFloor(151)
        first.setSensitivityFloor(151)
        assertEquals(4L, first.state.first().requestRevision)
        firstScope.cancel()

        val reopenedScope = testScope()
        val reopened = MonitoringStateStore.createForTest(file, reopenedScope)
        val restored = reopened.state.first()
        assertEquals("Kuala Lumpur", restored.selectedLocation?.label)
        assertEquals(3.139, restored.selectedLocation?.latitude ?: -1.0, 0.000001)
        assertEquals(101.6869, restored.selectedLocation?.longitude ?: -1.0, 0.000001)
        assertFalse(restored.monitoringEnabled)
        assertFalse(restored.notificationsEnabled)
        assertEquals(151, restored.sensitivityFloor)
        assertEquals(4L, restored.requestRevision)
        reopenedScope.cancel()
        file.delete()
    }

    @Test
    fun estimateAndSuccessfulCheckTimeSurviveRecreation() = runBlocking<Unit> {
        val file = tempFile()
        val firstScope = testScope()
        val first = MonitoringStateStore.createForTest(file, firstScope)
        first.saveLocation(
            SavedMonitoringLocation(
                label = "Kuala Lumpur",
                latitude = 3.139,
                longitude = 101.6869,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = Instant.now(),
            ),
        )
        val snapshot = first.snapshotForRequest(RequestSource.MANUAL)!!
        val estimateTime = Instant.ofEpochMilli(Instant.now().minusSeconds(60).toEpochMilli())
        val checkedAt = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val estimate = AirQualityEstimate(
            sourceUsAqi = 105.0,
            modelValidAt = estimateTime,
            retrievedAt = checkedAt,
            providerTimezone = "Asia/Kuala_Lumpur",
        )
        first.applyResponse(snapshot, ApiCheckResult.Success(estimate, checkedAt))
        firstScope.cancel()

        val reopenedScope = testScope()
        val reopened = MonitoringStateStore.createForTest(file, reopenedScope)
        val restored = reopened.state.first()
        assertEquals(105, restored.estimate?.aqi)
        assertEquals(estimateTime, restored.estimate?.modelValidAt)
        assertEquals(checkedAt, restored.lastSuccessfulCheckAt)
        assertEquals(CheckStatus.AVAILABLE, restored.checkStatus)
        reopenedScope.cancel()
        file.delete()
    }

    @Test
    fun changingLocationClearsEstimateAndCheckStateAndAdvancesRevision() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(
            SavedMonitoringLocation(
                label = "Kuala Lumpur",
                latitude = 3.139,
                longitude = 101.6869,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = Instant.now(),
            ),
        )
        val snapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val checkedAt = Instant.now()
        val modelTime = checkedAt.minusSeconds(60)
        store.applyResponse(
            snapshot,
            ApiCheckResult.Success(
                AirQualityEstimate(
                    sourceUsAqi = 105.0,
                    modelValidAt = modelTime,
                    retrievedAt = checkedAt,
                    providerTimezone = "Asia/Kuala_Lumpur",
                ),
                checkedAt,
            ),
        )
        assertEquals(CheckStatus.AVAILABLE, store.state.first().checkStatus)

        store.saveLocation(
            SavedMonitoringLocation(
                label = "Shah Alam",
                latitude = 3.0733,
                longitude = 101.5185,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = Instant.now(),
            ),
        )

        val changed = store.state.first()
        assertEquals("Shah Alam", changed.selectedLocation?.label)
        assertNull(changed.estimate)
        assertNull(changed.baselineAqi)
        assertNull(changed.baselineModelValidAt)
        assertNull(changed.lastSuccessfulCheckAt)
        assertEquals(CheckStatus.NEVER_CHECKED, changed.checkStatus)
        assertEquals(2L, changed.requestRevision)

        scope.cancel()
        file.delete()
    }

    @Test
    fun twelveToEighteenHourEstimateCanUpdateDisplayWithoutChangingAlertBaseline() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(
            SavedMonitoringLocation(
                label = "Kuala Lumpur",
                latitude = 3.139,
                longitude = 101.6869,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = Instant.now(),
            ),
        )
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val initialSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val originalBaselineAt = now.minusSeconds(25 * 60 * 60)
        store.applyResponse(
            initialSnapshot,
            ApiCheckResult.Success(
                AirQualityEstimate(90.0, originalBaselineAt, now.minusSeconds(15 * 60 * 60), "Asia/Kuala_Lumpur"),
                now.minusSeconds(15 * 60 * 60),
            ),
        )
        val delayedSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val displayOnlyTime = now.minusSeconds(13 * 60 * 60)
        val deliveredAlerts = mutableListOf<com.ohmyhaze.monitoring.AlertNotification>()

        val outcome = store.applyResponse(
            delayedSnapshot,
            ApiCheckResult.Success(
                AirQualityEstimate(180.0, displayOnlyTime, now, "Asia/Kuala_Lumpur"),
                now,
            ),
            deliveredAlerts::add,
        )

        assertEquals(ApplyResponseOutcome.ESTIMATE_UPDATED, outcome)
        val state = store.state.first()
        assertEquals(displayOnlyTime, state.estimate?.modelValidAt)
        assertEquals(originalBaselineAt, state.baselineModelValidAt)
        assertEquals(90, state.baselineAqi)
        assertTrue(deliveredAlerts.isEmpty())

        scope.cancel()
        file.delete()
    }

    @Test
    fun committedCrossingDoesNotProduceCatchUpWhenSameModelTimeIsCheckedAgain() = runBlocking<Unit> {
        val file = tempFile()
        val scope = testScope()
        val store = MonitoringStateStore.createForTest(file, scope)
        store.saveLocation(
            SavedMonitoringLocation(
                label = "Kuala Lumpur",
                latitude = 3.139,
                longitude = 101.6869,
                selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
                selectedAt = Instant.now(),
            ),
        )
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val initialSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        store.applyResponse(
            initialSnapshot,
            ApiCheckResult.Success(
                AirQualityEstimate(80.0, now.minusSeconds(3_600), now.minusSeconds(1_800), "Asia/Kuala_Lumpur"),
                now.minusSeconds(1_800),
            ),
        )
        val crossingTime = now.minusSeconds(600)
        val firstCrossingSnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val crossing = ApiCheckResult.Success(
            AirQualityEstimate(130.0, crossingTime, now, "Asia/Kuala_Lumpur"),
            now,
        )
        // A process can die after the state commit and before its callback reaches Android.
        store.applyResponse(firstCrossingSnapshot, crossing)

        val retrySnapshot = store.snapshotForRequest(RequestSource.MANUAL)!!
        val notifications = mutableListOf<com.ohmyhaze.monitoring.AlertNotification>()
        val retryOutcome = store.applyResponse(retrySnapshot, crossing, notifications::add)

        assertEquals(ApplyResponseOutcome.CHECK_RECORDED, retryOutcome)
        assertTrue(notifications.isEmpty())
        assertEquals(130, store.state.first().baselineAqi)

        scope.cancel()
        file.delete()
    }

    private fun tempFile(): File = File.createTempFile("oh-my-haze", ".preferences_pb").apply { delete() }

    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
