package com.ohmyhaze.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ohmyhaze.R
import com.ohmyhaze.monitoring.UsAqiCategory

data class AqiCategoryPresentation(
    val illustrationRes: Int,
    val gaugeSegmentIndex: Int,
    val severityColor: Color,
    val posterCategoryRes: Int,
    val posterRangeRes: Int,
    val posterSummaryRes: Int,
    val recommendationRes: List<Int>,
    val correctedRecommendationTopFractions: Map<Int, Float>,
)

object AqiPresentation {
    fun forCategory(category: UsAqiCategory): AqiCategoryPresentation = when (category) {
        UsAqiCategory.GOOD -> AqiCategoryPresentation(
            R.drawable.aqi_good, 0, Color(0xFF196B3A),
            R.string.poster_good_category,
            R.string.poster_good_range,
            R.string.poster_good_summary,
            listOf(
                R.string.poster_good_recommendation_1,
                R.string.poster_good_recommendation_2,
                R.string.poster_good_recommendation_3,
            ),
            emptyMap(),
        )
        UsAqiCategory.MODERATE -> AqiCategoryPresentation(
            R.drawable.aqi_moderate, 1, Color(0xFF805B00),
            R.string.poster_moderate_category,
            R.string.poster_moderate_range,
            R.string.poster_moderate_summary,
            listOf(
                R.string.poster_moderate_recommendation_1,
                R.string.poster_moderate_recommendation_2,
                R.string.poster_moderate_recommendation_3,
            ),
            mapOf(2 to 0.884f),
        )
        UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS ->
            AqiCategoryPresentation(
                R.drawable.aqi_sensitive, 2, Color(0xFFAA3D18),
                R.string.poster_sensitive_category,
                R.string.poster_sensitive_range,
                R.string.poster_sensitive_summary,
                listOf(
                    R.string.poster_sensitive_recommendation_1,
                    R.string.poster_sensitive_recommendation_2,
                    R.string.poster_sensitive_recommendation_3,
                ),
                mapOf(0 to 0.714f, 1 to 0.813f, 2 to 0.886f),
            )
        UsAqiCategory.UNHEALTHY -> AqiCategoryPresentation(
            R.drawable.aqi_unhealthy, 3, Color(0xFFB3261E),
            R.string.poster_unhealthy_category,
            R.string.poster_unhealthy_range,
            R.string.poster_unhealthy_summary,
            listOf(
                R.string.poster_unhealthy_recommendation_1,
                R.string.poster_unhealthy_recommendation_2,
                R.string.poster_unhealthy_recommendation_3,
            ),
            mapOf(0 to 0.714f, 1 to 0.817f, 2 to 0.897f),
        )
        UsAqiCategory.VERY_UNHEALTHY -> AqiCategoryPresentation(
            R.drawable.aqi_very_unhealthy, 4, Color(0xFF7141A5),
            R.string.poster_very_unhealthy_category,
            R.string.poster_very_unhealthy_range,
            R.string.poster_very_unhealthy_summary,
            listOf(
                R.string.poster_very_unhealthy_recommendation_1,
                R.string.poster_very_unhealthy_recommendation_2,
                R.string.poster_very_unhealthy_recommendation_3,
            ),
            mapOf(0 to 0.714f, 1 to 0.813f, 2 to 0.889f),
        )
        UsAqiCategory.HAZARDOUS -> AqiCategoryPresentation(
            R.drawable.aqi_hazardous, 5, Color(0xFF60463A),
            R.string.poster_hazardous_category,
            R.string.poster_hazardous_range,
            R.string.poster_hazardous_summary,
            listOf(
                R.string.poster_hazardous_recommendation_1,
                R.string.poster_hazardous_recommendation_2,
                R.string.poster_hazardous_recommendation_3,
            ),
            mapOf(0 to 0.714f, 1 to 0.808f, 2 to 0.884f),
        )
    }

    fun severityColor(category: UsAqiCategory, darkTheme: Boolean): Color = when {
        !darkTheme -> forCategory(category).severityColor
        category == UsAqiCategory.GOOD -> Color(0xFF80D99C)
        category == UsAqiCategory.MODERATE -> Color(0xFFFFD166)
        category == UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS -> Color(0xFFFFB08A)
        category == UsAqiCategory.UNHEALTHY -> Color(0xFFFFB4AB)
        category == UsAqiCategory.VERY_UNHEALTHY -> Color(0xFFD4BBFF)
        else -> Color(0xFFE6C3B7)
    }
}

@Composable
fun FullAqiPoster(
    presentation: AqiCategoryPresentation,
    modifier: Modifier = Modifier,
) {
    val category = stringResource(presentation.posterCategoryRes)
    val range = stringResource(presentation.posterRangeRes)
    val summary = stringResource(presentation.posterSummaryRes)
    val recommendations = presentation.recommendationRes.map { stringResource(it) }
    val accessibleCopy = "$category. $range. $summary ${recommendations.joinToString(separator = " ")}"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1122f / 1402f)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        // Keep replacement copy aligned to the raster lettering, independent of system font scale.
        val density = LocalDensity.current
        val posterFontSize = with(density) { (maxWidth * 0.0373f).toSp() }
        val posterLineHeight = with(density) { (maxWidth * 0.0407f).toSp() }
        Image(
            painter = painterResource(presentation.illustrationRes),
            contentDescription = accessibleCopy,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        presentation.correctedRecommendationTopFractions.forEach { (recommendationIndex, topFraction) ->
            val recommendation = recommendations[recommendationIndex]
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.28f, y = maxHeight * topFraction)
                    .width(maxWidth * 0.60f)
                    .height(maxHeight * 0.071f)
                    .background(Color.White)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = recommendation,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF283149),
                    fontSize = posterFontSize,
                    lineHeight = posterLineHeight,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun AqiCategoryGauge(
    category: UsAqiCategory,
    modifier: Modifier = Modifier,
) {
    val categories = UsAqiCategory.entries
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clearAndSetSemantics {
                contentDescription = "AQI category gauge: ${category.displayName}"
            },
    ) {
        val gap = 4.dp
        val segmentWidth = (maxWidth - gap * (categories.size - 1)) / categories.size.toFloat()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            categories.forEach { segment ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AqiPresentation.severityColor(segment, darkTheme)),
                )
            }
        }
        val presentation = AqiPresentation.forCategory(category)
        val markerX = (segmentWidth + gap) * presentation.gaugeSegmentIndex + (segmentWidth - 18.dp) / 2
        Box(
            modifier = Modifier
                .offset(x = markerX)
                .align(Alignment.CenterStart)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
        )
    }
}
