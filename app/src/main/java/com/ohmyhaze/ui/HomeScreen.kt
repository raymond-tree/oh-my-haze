package com.ohmyhaze.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohmyhaze.R
import com.ohmyhaze.data.CheckStatus
import com.ohmyhaze.data.MonitoringState
import com.ohmyhaze.monitoring.EstimateDisplayFreshness
import com.ohmyhaze.monitoring.EstimateFreshness
import com.ohmyhaze.monitoring.UsAqiCategory
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: MonitoringState,
    modifier: Modifier = Modifier,
    onChangeLocation: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    isRefreshing: Boolean,
    notificationsAvailable: Boolean,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = Instant.now()
        }
    }

    val location = state.selectedLocation
    val estimate = state.estimate
    val displayFreshness = estimate?.let { EstimateFreshness.displayStatus(it.modelValidAt, now) }
    val delayed = state.lastSuccessfulCheckAt?.isBefore(now.minus(MONITORING_DELAY_LIMIT)) == true
    val timeZone = estimate?.providerTimezone?.toZoneIdOrUtc() ?: ZoneId.systemDefault()
    val darkTheme = isSystemInDarkTheme()

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            location?.label ?: "Monitoring location",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text("Monitoring location", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onChangeLocation) { Text("Change") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(monitoringStatus(state.monitoringEnabled, delayed, state.lastSuccessfulCheckAt))
                    StatusPill(alertStatus(state.notificationsEnabled, notificationsAvailable))
                }

                if (estimate == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No AQI estimate yet", style = MaterialTheme.typography.titleLarge)
                            Text("Your first successful air-quality check will appear here.")
                        }
                    }
                } else {
                    val presentation = AqiPresentation.forCategory(estimate.category)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AQI", style = MaterialTheme.typography.labelLarge)
                                Text(estimate.displayValue, style = MaterialTheme.typography.displayMedium)
                                Text(
                                    estimate.category.displayName,
                                    color = AqiPresentation.severityColor(estimate.category, darkTheme),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Image(
                                painter = painterResource(presentation.illustrationRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 132.dp, height = 82.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                        }
                        Text(
                            freshnessLabel(displayFreshness),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AqiCategoryGauge(estimate.category, Modifier.padding(top = 1.dp))
                    }
                    HealthRecommendations(estimate.category, presentation)
                }

                Text(
                    "Last checked: ${state.lastSuccessfulCheckAt?.formatForUser(timeZone) ?: "Not yet checked"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.checkStatus != CheckStatus.AVAILABLE && state.lastCheckError != null) {
                    Text(
                        "Could not update AQI. Try again later.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.lastSuccessfulCheckAt == null && state.checkStatus == CheckStatus.NEVER_CHECKED) {
                    Text(
                        "No successful check has been recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Checking…", modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Text("Refresh now")
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRecommendations(category: UsAqiCategory, presentation: AqiCategoryPresentation) {
    val severityColor = AqiPresentation.severityColor(
        category = category,
        darkTheme = isSystemInDarkTheme(),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = severityColor.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("What you can do", style = MaterialTheme.typography.titleSmall)
            presentation.recommendationRes.forEach { resourceId ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, end = 9.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(severityColor),
                    )
                    Text(stringResource(resourceId), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                stringResource(R.string.recommendation_general_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusPill(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun monitoringStatus(enabled: Boolean, delayed: Boolean, lastChecked: Instant?): String = when {
    !enabled -> "Monitoring paused"
    delayed -> "Check delayed"
    lastChecked == null -> "Waiting for first check"
    else -> "Monitoring on"
}

private fun alertStatus(enabled: Boolean, available: Boolean): String = when {
    !enabled -> "Alerts off"
    !available -> "Alerts unavailable"
    else -> "Alerts on"
}

private fun freshnessLabel(freshness: EstimateDisplayFreshness?): String = when (freshness) {
    EstimateDisplayFreshness.FRESH -> "Recent reading"
    EstimateDisplayFreshness.DISPLAY_ONLY -> "Older reading · alerts paused"
    EstimateDisplayFreshness.STALE -> "Stale reading"
    null -> "Freshness unavailable"
}

private fun String.toZoneIdOrUtc(): ZoneId = runCatching { ZoneId.of(this) }.getOrDefault(ZoneOffset.UTC)

private fun Instant.formatForUser(zoneId: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(zoneId)
        .format(this)

private val MONITORING_DELAY_LIMIT: Duration = Duration.ofHours(2)
