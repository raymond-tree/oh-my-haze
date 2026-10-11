package com.ohmyhaze.ui

import com.ohmyhaze.R
import com.ohmyhaze.monitoring.UsAqiCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AqiGuidanceTest {
    @Test
    fun eachCategoryMapsToItsApprovedTwoRecommendationResources() {
        val expected = mapOf(
            UsAqiCategory.GOOD to listOf(R.string.recommendation_good_1, R.string.recommendation_good_2),
            UsAqiCategory.MODERATE to listOf(R.string.recommendation_moderate_1, R.string.recommendation_moderate_2),
            UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS to listOf(
                R.string.recommendation_sensitive_1,
                R.string.recommendation_sensitive_2,
            ),
            UsAqiCategory.UNHEALTHY to listOf(
                R.string.recommendation_unhealthy_1,
                R.string.recommendation_unhealthy_2,
            ),
            UsAqiCategory.VERY_UNHEALTHY to listOf(
                R.string.recommendation_very_unhealthy_1,
                R.string.recommendation_very_unhealthy_2,
            ),
            UsAqiCategory.HAZARDOUS to listOf(
                R.string.recommendation_hazardous_1,
                R.string.recommendation_hazardous_2,
            ),
        )

        expected.forEach { (category, recommendationResources) ->
            assertEquals(category.name, recommendationResources, AqiPresentation.forCategory(category).recommendationRes)
        }
        val smokeOrParticleGuidanceCategories = expected
            .filterValues { R.string.recommendation_hazardous_2 in it }
            .keys
        assertEquals(setOf(UsAqiCategory.HAZARDOUS), smokeOrParticleGuidanceCategories)
    }
}
