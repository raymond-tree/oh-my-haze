package com.ohmyhaze.ui

import com.ohmyhaze.R
import com.ohmyhaze.monitoring.UsAqi
import com.ohmyhaze.monitoring.UsAqiCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AqiPresentationTest {
    @Test
    fun eachCategoryMapsToItsSuppliedIllustrationAndGaugeSegment() {
        val expectedImages = mapOf(
            UsAqiCategory.GOOD to R.drawable.aqi_good,
            UsAqiCategory.MODERATE to R.drawable.aqi_moderate,
            UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS to R.drawable.aqi_sensitive,
            UsAqiCategory.UNHEALTHY to R.drawable.aqi_unhealthy,
            UsAqiCategory.VERY_UNHEALTHY to R.drawable.aqi_very_unhealthy,
            UsAqiCategory.HAZARDOUS to R.drawable.aqi_hazardous,
        )

        UsAqiCategory.entries.forEachIndexed { index, category ->
            val presentation = AqiPresentation.forCategory(category)
            assertEquals(expectedImages.getValue(category), presentation.illustrationRes)
            assertEquals(index, presentation.gaugeSegmentIndex)
        }
    }

    @Test
    fun everyCategoryHasTwoDistinctRecommendationStrings() {
        val recommendationPairs = UsAqiCategory.entries.map { category ->
            val recommendations = AqiPresentation.forCategory(category).recommendationRes
            assertEquals("$category should have two recommendations", 2, recommendations.size)
            assertEquals("$category recommendations should be distinct", 2, recommendations.distinct().size)
            recommendations
        }

        assertEquals(UsAqiCategory.entries.size, recommendationPairs.distinct().size)
        recommendationPairs.flatten().forEach { assertNotEquals(0, it) }
    }

    @Test
    fun categoryBoundariesResolveToMatchingIllustrationAndGauge() {
        val expected = listOf(
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
        )

        expected.forEach { (aqi, expectedCategory) ->
            val category = UsAqi.category(UsAqi.roundedIndex(aqi.toDouble()))
            assertEquals("AQI $aqi", expectedCategory, category)
            val presentation = AqiPresentation.forCategory(category)
            assertEquals("AQI $aqi illustration", expectedImages.getValue(expectedCategory), presentation.illustrationRes)
            assertEquals("AQI $aqi gauge segment", UsAqiCategory.entries.indexOf(expectedCategory), presentation.gaugeSegmentIndex)
        }
    }

    private val expectedImages = mapOf(
        UsAqiCategory.GOOD to R.drawable.aqi_good,
        UsAqiCategory.MODERATE to R.drawable.aqi_moderate,
        UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS to R.drawable.aqi_sensitive,
        UsAqiCategory.UNHEALTHY to R.drawable.aqi_unhealthy,
        UsAqiCategory.VERY_UNHEALTHY to R.drawable.aqi_very_unhealthy,
        UsAqiCategory.HAZARDOUS to R.drawable.aqi_hazardous,
    )
}
