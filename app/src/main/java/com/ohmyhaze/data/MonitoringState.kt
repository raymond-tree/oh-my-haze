package com.ohmyhaze.data

import java.time.Instant
import com.ohmyhaze.monitoring.UsAqi
import com.ohmyhaze.monitoring.UsAqiCategory

enum class LocationSelectionSource {
    CURRENT_LOCATION,
    BUNDLED_LOCALITY,
}

enum class RequestSource {
    MANUAL,
    AUTOMATIC,
}

enum class CheckStatus {
    NEVER_CHECKED,
    AVAILABLE,
    NETWORK_ERROR,
    HTTP_ERROR,
    INVALID_RESPONSE,
}

data class SavedMonitoringLocation(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val selectionSource: LocationSelectionSource,
    val selectedAt: Instant,
) {
    init {
        require(label.isNotBlank()) { "Location label must not be blank" }
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be finite WGS84" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be finite WGS84" }
    }
}

data class AirQualityEstimate(
    val sourceUsAqi: Double,
    val modelValidAt: Instant,
    val retrievedAt: Instant,
    val providerTimezone: String,
) {
    init {
        require(sourceUsAqi.isFinite() && sourceUsAqi >= 0.0) { "US AQI must be finite and non-negative" }
        require(providerTimezone.isNotBlank()) { "Provider timezone must not be blank" }
    }

    /** Rounded for category/threshold decisions; source values over 500 remain separately available. */
    val aqi: Int
        get() = UsAqi.roundedIndex(sourceUsAqi)

    val category: UsAqiCategory
        get() = UsAqi.category(aqi)

    val displayValue: String
        get() = UsAqi.displayValue(sourceUsAqi)
}

data class MonitoringState(
    val selectedLocation: SavedMonitoringLocation? = null,
    val estimate: AirQualityEstimate? = null,
    val monitoringEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val sensitivityFloor: Int = DEFAULT_SENSITIVITY_FLOOR,
    val requestRevision: Long = 0L,
    val baselineAqi: Int? = null,
    val baselineModelValidAt: Instant? = null,
    val lastSuccessfulCheckAt: Instant? = null,
    val checkStatus: CheckStatus = CheckStatus.NEVER_CHECKED,
    val lastCheckError: String? = null,
) {
    init {
        require(sensitivityFloor in SENSITIVITY_FLOORS) { "Unsupported sensitivity floor" }
        require(requestRevision >= 0L) { "Request revision must not be negative" }
        require((baselineAqi == null) == (baselineModelValidAt == null)) {
            "Alert baseline value and timestamp must be present together"
        }
        require(baselineAqi == null || baselineAqi >= 0) { "Alert baseline must be non-negative" }
    }

    companion object {
        const val DEFAULT_SENSITIVITY_FLOOR = 101
        val SENSITIVITY_FLOORS: Set<Int> = setOf(101, 151, 201, 301)
    }
}

data class RequestSnapshot(
    val location: SavedMonitoringLocation,
    val requestRevision: Long,
    val source: RequestSource,
)

sealed interface ApiCheckResult {
    data class Success(
        val estimate: AirQualityEstimate,
        val checkedAt: Instant,
    ) : ApiCheckResult

    data class Failure(
        val status: CheckStatus,
        val safeMessage: String? = null,
    ) : ApiCheckResult {
        init {
            require(status in setOf(CheckStatus.NETWORK_ERROR, CheckStatus.HTTP_ERROR, CheckStatus.INVALID_RESPONSE))
        }
    }
}

enum class ApplyResponseOutcome {
    SUPERSEDED,
    FAILURE_RECORDED,
    CHECK_RECORDED,
    ESTIMATE_UPDATED,
}
