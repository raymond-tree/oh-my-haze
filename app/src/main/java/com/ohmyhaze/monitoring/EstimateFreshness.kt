package com.ohmyhaze.monitoring

import java.time.Duration
import java.time.Instant

enum class EstimateDisplayFreshness {
    FRESH,
    DISPLAY_ONLY,
    STALE,
}

/** Keeps user-facing display age separate from whether an estimate may affect alerts. */
object EstimateFreshness {
    private val ALERT_MAX_AGE: Duration = Duration.ofHours(12)
    private val DISPLAY_MAX_AGE: Duration = Duration.ofHours(18)

    fun displayStatus(modelValidAt: Instant, now: Instant): EstimateDisplayFreshness = when {
        modelValidAt.isBefore(now.minus(DISPLAY_MAX_AGE)) -> EstimateDisplayFreshness.STALE
        modelValidAt.isBefore(now.minus(ALERT_MAX_AGE)) -> EstimateDisplayFreshness.DISPLAY_ONLY
        else -> EstimateDisplayFreshness.FRESH
    }

    fun isAlertEligible(modelValidAt: Instant, now: Instant): Boolean =
        !modelValidAt.isBefore(now.minus(ALERT_MAX_AGE))
}
