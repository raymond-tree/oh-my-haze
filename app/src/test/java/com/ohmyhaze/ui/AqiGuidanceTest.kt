package com.ohmyhaze.ui

import com.ohmyhaze.R
import com.ohmyhaze.monitoring.UsAqiCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AqiGuidanceTest {
    @Test
    fun eachCategoryMapsToItsThreePosterRecommendationsAndOnlyCorrectsCopyThatNeedsIt() {
        val expected = mapOf(
            UsAqiCategory.GOOD to listOf(
                R.string.poster_good_recommendation_1,
                R.string.poster_good_recommendation_2,
                R.string.poster_good_recommendation_3,
            ),
            UsAqiCategory.MODERATE to listOf(
                R.string.poster_moderate_recommendation_1,
                R.string.poster_moderate_recommendation_2,
                R.string.poster_moderate_recommendation_3,
            ),
            UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS to listOf(
                R.string.poster_sensitive_recommendation_1,
                R.string.poster_sensitive_recommendation_2,
                R.string.poster_sensitive_recommendation_3,
            ),
            UsAqiCategory.UNHEALTHY to listOf(
                R.string.poster_unhealthy_recommendation_1,
                R.string.poster_unhealthy_recommendation_2,
                R.string.poster_unhealthy_recommendation_3,
            ),
            UsAqiCategory.VERY_UNHEALTHY to listOf(
                R.string.poster_very_unhealthy_recommendation_1,
                R.string.poster_very_unhealthy_recommendation_2,
                R.string.poster_very_unhealthy_recommendation_3,
            ),
            UsAqiCategory.HAZARDOUS to listOf(
                R.string.poster_hazardous_recommendation_1,
                R.string.poster_hazardous_recommendation_2,
                R.string.poster_hazardous_recommendation_3,
            ),
        )

        expected.forEach { (category, recommendationResources) ->
            assertEquals(category.name, recommendationResources, AqiPresentation.forCategory(category).recommendationRes)
        }
        val correctedRows = mapOf(
            UsAqiCategory.GOOD to emptySet(),
            UsAqiCategory.MODERATE to setOf(2),
            UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS to setOf(0, 1, 2),
            UsAqiCategory.UNHEALTHY to setOf(0, 1, 2),
            UsAqiCategory.VERY_UNHEALTHY to setOf(0, 1, 2),
            UsAqiCategory.HAZARDOUS to setOf(0, 1, 2),
        )
        correctedRows.forEach { (category, rows) ->
            assertEquals(category.name, rows, AqiPresentation.forCategory(category).correctedRecommendationTopFractions.keys)
        }
    }
}
