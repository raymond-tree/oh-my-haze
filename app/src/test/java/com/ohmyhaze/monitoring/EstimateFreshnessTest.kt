package com.ohmyhaze.monitoring

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimateFreshnessTest {
    private val now = Instant.parse("2026-10-10T15:00:00Z")

    @Test
    fun separatesTwelveHourAlertEligibilityFromEighteenHourDisplayFreshness() {
        assertEquals(EstimateDisplayFreshness.FRESH, EstimateFreshness.displayStatus(now.minusSeconds(12 * 3600L), now))
        assertEquals(EstimateDisplayFreshness.FRESH, EstimateFreshness.displayStatus(now.minusNanos(1), now))
        assertEquals(
            EstimateDisplayFreshness.DISPLAY_ONLY,
            EstimateFreshness.displayStatus(now.minusSeconds(12 * 3600L).minusNanos(1), now),
        )
        assertEquals(
            EstimateDisplayFreshness.DISPLAY_ONLY,
            EstimateFreshness.displayStatus(now.minusSeconds(18 * 3600L), now),
        )
        assertEquals(
            EstimateDisplayFreshness.STALE,
            EstimateFreshness.displayStatus(now.minusSeconds(18 * 3600L).minusNanos(1), now),
        )
    }

    @Test
    fun alertEligibilityRequiresModelTimeNoMoreThanTwelveHoursOld() {
        assertTrue(EstimateFreshness.isAlertEligible(now.minusSeconds(12 * 3600L), now))
        assertTrue(EstimateFreshness.isAlertEligible(now, now))
        assertFalse(EstimateFreshness.isAlertEligible(now.minusSeconds(12 * 3600L).minusNanos(1), now))
        assertFalse(EstimateFreshness.isAlertEligible(now.minusSeconds(18 * 3600L), now))
    }
}
