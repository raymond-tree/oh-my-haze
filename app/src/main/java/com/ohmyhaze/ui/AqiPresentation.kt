package com.ohmyhaze.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ohmyhaze.R
import com.ohmyhaze.monitoring.UsAqiCategory

data class AqiCategoryPresentation(
    val illustrationRes: Int,
    val gaugeSegmentIndex: Int,
    val severityColor: Color,
    val recommendationRes: List<Int>,
)

object AqiPresentation {
    fun forCategory(category: UsAqiCategory): AqiCategoryPresentation = when (category) {
        UsAqiCategory.GOOD -> AqiCategoryPresentation(
            R.drawable.aqi_good, 0, Color(0xFF196B3A),
            listOf(R.string.recommendation_good_1, R.string.recommendation_good_2),
        )
        UsAqiCategory.MODERATE -> AqiCategoryPresentation(
            R.drawable.aqi_moderate, 1, Color(0xFF805B00),
            listOf(R.string.recommendation_moderate_1, R.string.recommendation_moderate_2),
        )
        UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS ->
            AqiCategoryPresentation(
                R.drawable.aqi_sensitive, 2, Color(0xFFAA3D18),
                listOf(R.string.recommendation_sensitive_1, R.string.recommendation_sensitive_2),
            )
        UsAqiCategory.UNHEALTHY -> AqiCategoryPresentation(
            R.drawable.aqi_unhealthy, 3, Color(0xFFB3261E),
            listOf(R.string.recommendation_unhealthy_1, R.string.recommendation_unhealthy_2),
        )
        UsAqiCategory.VERY_UNHEALTHY -> AqiCategoryPresentation(
            R.drawable.aqi_very_unhealthy, 4, Color(0xFF7141A5),
            listOf(R.string.recommendation_very_unhealthy_1, R.string.recommendation_very_unhealthy_2),
        )
        UsAqiCategory.HAZARDOUS -> AqiCategoryPresentation(
            R.drawable.aqi_hazardous, 5, Color(0xFF60463A),
            listOf(R.string.recommendation_hazardous_1, R.string.recommendation_hazardous_2),
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
