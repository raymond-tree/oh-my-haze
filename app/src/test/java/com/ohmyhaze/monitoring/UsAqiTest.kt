package com.ohmyhaze.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UsAqiTest {
    @Test
    fun mapsAllUsAqiCategoryBoundaries() {
        val cases = listOf(
            0 to UsAqiCategory.GOOD,
            50 to UsAqiCategory.GOOD,
            51 to UsAqiCategory.MODERATE,
            100 to UsAqiCategory.MODERATE,
            101 to UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS,
            150 to UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS,
            151 to UsAqiCategory.UNHEALTHY,
            200 to UsAqiCategory.UNHEALTHY,
            201 to UsAqiCategory.VERY_UNHEALTHY,
            300 to UsAqiCategory.VERY_UNHEALTHY,
            301 to UsAqiCategory.HAZARDOUS,
            500 to UsAqiCategory.HAZARDOUS,
        )

        cases.forEach { (aqi, category) -> assertEquals(category, UsAqi.category(aqi)) }
    }

    @Test
    fun roundsNonnegativeHalvesUpAndPreservesAboveScaleDisplayValues() {
        assertEquals(100, UsAqi.roundedIndex(100.49))
        assertEquals(101, UsAqi.roundedIndex(100.5))
        assertEquals(101, UsAqi.roundedIndex(101.0))
        assertEquals("500", UsAqi.displayValue(500.0))
        assertEquals("500.5", UsAqi.displayValue(500.5))
        assertEquals(UsAqiCategory.HAZARDOUS, UsAqi.category(UsAqi.roundedIndex(500.5)))
    }

    @Test
    fun rejectsNegativeAndNonFiniteProviderValues() {
        assertThrows(IllegalArgumentException::class.java) { UsAqi.roundedIndex(-0.1) }
        assertThrows(IllegalArgumentException::class.java) { UsAqi.roundedIndex(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { UsAqi.roundedIndex(Double.POSITIVE_INFINITY) }
    }
}
