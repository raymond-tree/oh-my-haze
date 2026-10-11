package com.ohmyhaze.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    var showSourceDetails by remember { mutableStateOf(false) }
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

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(location?.label ?: "Monitoring location", style = MaterialTheme.typography.headlineMedium)
            Text("Saved monitoring location", style = MaterialTheme.typography.labelMedium)

            if (estimate == null) {
                Text("No US AQI estimate yet", style = MaterialTheme.typography.titleLarge)
                Text("The first successful air-quality check will appear here.")
            } else {
                Text("US AQI", style = MaterialTheme.typography.titleMedium)
                Text(estimate.displayValue, style = MaterialTheme.typography.displayLarge)
                Text(
                    estimate.category.displayName,
                    color = estimate.category.severityColor(),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    when (displayFreshness) {
                        EstimateDisplayFreshness.FRESH -> "Estimate is within the last 12 hours"
                        EstimateDisplayFreshness.DISPLAY_ONLY -> "Older estimate; too old to trigger alerts"
                        EstimateDisplayFreshness.STALE -> "Stale estimate; model time is over 18 hours old"
                        null -> "Estimate freshness unavailable"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Model-valid time: ${estimate.modelValidAt.formatForUser(timeZone)}")
                Text("Source: Open-Meteo · CAMS Global")
            }

            Text(
                "Last successful API check: ${state.lastSuccessfulCheckAt?.formatForUser(timeZone) ?: "Not yet checked"}",
            )
            Text(
                when {
                    !state.monitoringEnabled -> "Monitoring is paused"
                    delayed -> "Monitoring is delayed; no successful check in over two hours"
                    state.lastSuccessfulCheckAt == null -> "Monitoring is waiting for its first successful check"
                    else -> "Monitoring is on; checks are best-effort"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                when {
                    !state.notificationsEnabled -> "App alerts are off"
                    notificationsAvailable -> "Android notifications are available"
                    else -> "Android notifications are unavailable; checks continue without alerts"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.checkStatus != CheckStatus.AVAILABLE && state.lastCheckError != null) {
                Text(state.lastCheckError, color = MaterialTheme.colorScheme.error)
            }
            if (state.lastSuccessfulCheckAt == null && state.checkStatus == CheckStatus.NEVER_CHECKED) {
                Text("No successful check has been recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Checking…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text("Refresh now")
                }
            }
            TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Settings")
            }
            Text("The model usually updates about every 12 hours. Hourly checks may show the same estimate.")
            TextButton(onClick = { showSourceDetails = !showSourceDetails }) {
                Text(if (showSourceDetails) "Hide estimate details" else "About this estimate and source")
            }
            if (showSourceDetails) SourceDetails()
            Spacer(Modifier.height(4.dp))
            Button(onClick = onChangeLocation, modifier = Modifier.fillMaxWidth()) {
                Text("Change monitoring location")
            }
        }
    }
}

private fun String.toZoneIdOrUtc(): ZoneId = runCatching { ZoneId.of(this) }.getOrDefault(ZoneOffset.UTC)

private fun Instant.formatForUser(zoneId: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(zoneId)
        .format(this)

private fun UsAqiCategory.severityColor(): Color = when (this) {
    UsAqiCategory.GOOD -> Color(0xFF1B5E20)
    UsAqiCategory.MODERATE -> Color(0xFF795500)
    UsAqiCategory.UNHEALTHY_FOR_SENSITIVE_GROUPS -> Color(0xFF9A3412)
    UsAqiCategory.UNHEALTHY -> Color(0xFFB71C1C)
    UsAqiCategory.VERY_UNHEALTHY -> Color(0xFF6A1B9A)
    UsAqiCategory.HAZARDOUS -> Color(0xFF4E342E)
}

private val MONITORING_DELAY_LIMIT: Duration = Duration.ofHours(2)
