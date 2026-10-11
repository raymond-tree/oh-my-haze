package com.ohmyhaze.monitoring

import com.ohmyhaze.data.AirQualityEstimate
import java.time.Duration
import java.time.Instant

data class AlertBaseline(
    val aqi: Int,
    val modelValidAt: Instant,
)

data class AlertNotification(
    val aqi: Int,
    val category: UsAqiCategory,
    val modelValidAt: Instant,
)

data class AlertTransition(
    val baseline: AlertBaseline,
    val notification: AlertNotification?,
)

/** Pure policy for newer model-time crossings; provider retrieval time is never used as freshness. */
object AlertPolicy {
    const val DEFAULT_FLOOR = 101
    val SUPPORTED_FLOORS: Set<Int> = setOf(101, 151, 201, 301)
    val ELIGIBILITY_AGE: Duration = Duration.ofHours(12)

    fun evaluate(
        estimate: AirQualityEstimate,
        previous: AlertBaseline?,
        sensitivityFloor: Int,
        notificationsEnabled: Boolean,
        now: Instant,
    ): AlertTransition? {
        require(sensitivityFloor in SUPPORTED_FLOORS) { "Unsupported alert sensitivity floor" }
        val modelTime = estimate.modelValidAt
        if (modelTime.isAfter(now.plus(Duration.ofMinutes(5)))) return null
        if (modelTime.isBefore(now.minus(ELIGIBILITY_AGE))) return null
        if (previous != null && !modelTime.isAfter(previous.modelValidAt)) return null

        val nextBaseline = AlertBaseline(estimate.aqi, modelTime)
        val previousIsCurrent = previous != null &&
            !previous.modelValidAt.isBefore(now.minus(ELIGIBILITY_AGE)) &&
            !previous.modelValidAt.isAfter(now.plus(Duration.ofMinutes(5)))
        val crossedFloor = previous?.let {
            previousIsCurrent && it.aqi < sensitivityFloor && estimate.aqi >= sensitivityFloor
        } ?: false

        return AlertTransition(
            baseline = nextBaseline,
            notification = if (crossedFloor && notificationsEnabled) {
                AlertNotification(estimate.aqi, estimate.category, modelTime)
            } else {
                null
            },
        )
    }
}
