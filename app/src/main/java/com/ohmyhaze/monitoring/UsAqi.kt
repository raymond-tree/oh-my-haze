package com.ohmyhaze.monitoring

import kotlin.math.floor

enum class UsAqiCategory(val minimumAqi: Int, val displayName: String) {
    GOOD(0, "Good"),
    MODERATE(51, "Moderate"),
    UNHEALTHY_FOR_SENSITIVE_GROUPS(101, "Unhealthy for Sensitive Groups"),
    UNHEALTHY(151, "Unhealthy"),
    VERY_UNHEALTHY(201, "Very Unhealthy"),
    HAZARDOUS(301, "Hazardous");

    companion object {
        fun fromAqi(aqi: Int): UsAqiCategory {
            require(aqi >= 0) { "US AQI must be non-negative" }
            return entries.last { aqi >= it.minimumAqi }
        }
    }
}

/** US AQI normalization shared by display, categories, and notification policy. */
object UsAqi {
    fun roundedIndex(sourceValue: Double): Int {
        require(sourceValue.isFinite() && sourceValue >= 0.0) {
            "US AQI must be finite and non-negative"
        }
        return if (sourceValue >= Int.MAX_VALUE.toDouble()) {
            Int.MAX_VALUE
        } else {
            floor(sourceValue + 0.5).toInt()
        }
    }

    fun category(roundedIndex: Int): UsAqiCategory = UsAqiCategory.fromAqi(roundedIndex)

    fun displayValue(sourceValue: Double): String =
        if (sourceValue > 500.0) sourceValue.toString() else roundedIndex(sourceValue).toString()
}
