package com.ohmyhaze.monitoring

import com.ohmyhaze.data.AirQualityEstimate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPolicyTest {
    private val now = Instant.parse("2026-10-10T04:00:00Z")

    @Test
    fun defaultFloorIs101AndSensitivityChoicesAreSupported() {
        assertEquals(101, AlertPolicy.DEFAULT_FLOOR)
        assertEquals(setOf(101, 151, 201, 301), AlertPolicy.SUPPORTED_FLOORS)
    }

    @Test
    fun firstAndExpiredSamplesSilentlyEstablishBaseline() {
        val first = AlertPolicy.evaluate(estimate(240, now.minusSeconds(600)), null, 101, true, now)
        assertEquals(240, first?.baseline?.aqi)
        assertNull(first?.notification)

        val expired = AlertBaseline(90, now.minusSeconds(12 * 60 * 60 + 1))
        val afterGap = AlertPolicy.evaluate(estimate(140, now.minusSeconds(300)), expired, 101, true, now)
        assertEquals(140, afterGap?.baseline?.aqi)
        assertNull(afterGap?.notification)
    }

    @Test
    fun crossesSelectedFloorOnceAndReportsReachedCategoryOnMultiFloorJump() {
        val result = AlertPolicy.evaluate(
            estimate(240, now.minusSeconds(60)),
            AlertBaseline(80, now.minusSeconds(3_600)),
            101,
            true,
            now,
        )

        assertEquals(UsAqiCategory.VERY_UNHEALTHY, result?.notification?.category)
        assertEquals(240, result?.notification?.aqi)
    }

    @Test
    fun everySensitivityFloorAlertsOnUpwardCrossingAndRearmsAfterRecovery() {
        for (floor in AlertPolicy.SUPPORTED_FLOORS) {
            val before = AlertBaseline(floor - 1, now.minusSeconds(3_600))
            val crossed = AlertPolicy.evaluate(estimate(floor, now.minusSeconds(1_800)), before, floor, true, now)
            assertEquals("floor $floor", floor, crossed?.notification?.aqi)

            val recovered = AlertPolicy.evaluate(
                estimate(floor - 1, now.minusSeconds(900)),
                crossed?.baseline,
                floor,
                true,
                now,
            )
            assertNull("floor $floor recovery", recovered?.notification)

            val recrossed = AlertPolicy.evaluate(
                estimate(floor, now.minusSeconds(300)),
                recovered?.baseline,
                floor,
                true,
                now,
            )
            assertEquals("floor $floor rearmed", floor, recrossed?.notification?.aqi)
        }
    }

    @Test
    fun duplicateOrOlderModelTimesNeverChangeBaselineOrNotify() {
        val baseline = AlertBaseline(90, now.minusSeconds(600))
        assertNull(AlertPolicy.evaluate(estimate(160, baseline.modelValidAt), baseline, 101, true, now))
        assertNull(AlertPolicy.evaluate(estimate(160, baseline.modelValidAt.minusSeconds(60)), baseline, 101, true, now))
    }

    @Test
    fun baselineAtTwelveHoursCanCrossOnANewerEligibleEstimateButOlderEstimateIsRejected() {
        val boundary = now.minusSeconds(12 * 60 * 60)
        val baseline = AlertBaseline(90, boundary)
        val atBoundary = AlertPolicy.evaluate(estimate(110, boundary.plusMillis(1)), baseline, 101, true, now)
        val beyondBoundary = AlertPolicy.evaluate(
            estimate(110, boundary.minusMillis(1)),
            AlertBaseline(90, now.minusSeconds(13 * 60 * 60)),
            101,
            true,
            now,
        )

        assertEquals(110, atBoundary?.baseline?.aqi)
        assertEquals(UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS, atBoundary?.notification?.category)
        assertNull(beyondBoundary)
    }

    @Test
    fun recoveryBelowFloorRearmsAndDisabledNotificationsDoNotQueueCatchUp() {
        val previous = AlertBaseline(110, now.minusSeconds(600))
        val recovery = AlertPolicy.evaluate(estimate(90, now.minusSeconds(300)), previous, 101, true, now)
        assertNull(recovery?.notification)
        assertEquals(90, recovery?.baseline?.aqi)

        val mutedCrossing = AlertPolicy.evaluate(estimate(130, now.minusSeconds(60)), recovery?.baseline, 101, false, now)
        assertNull(mutedCrossing?.notification)
        val laterEnabledReading = AlertPolicy.evaluate(
            estimate(140, now),
            mutedCrossing?.baseline,
            101,
            true,
            now,
        )
        assertNull(laterEnabledReading?.notification)
    }

    @Test
    fun changingSensitivityDoesNotAlertWithoutANewerCrossing() {
        val alreadyAboveNewFloor = AlertBaseline(160, now.minusSeconds(600))
        val nextHigher = AlertPolicy.evaluate(estimate(170, now.minusSeconds(60)), alreadyAboveNewFloor, 151, true, now)
        assertNull(nextHigher?.notification)
    }

    private fun estimate(aqi: Int, at: Instant) = AirQualityEstimate(
        sourceUsAqi = aqi.toDouble(),
        modelValidAt = at,
        retrievedAt = now,
        providerTimezone = "Asia/Kuala_Lumpur",
    )
}
