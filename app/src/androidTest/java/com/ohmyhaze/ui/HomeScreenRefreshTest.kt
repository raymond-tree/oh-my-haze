package com.ohmyhaze.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ohmyhaze.data.AirQualityEstimate
import com.ohmyhaze.data.CheckStatus
import com.ohmyhaze.data.LocationSelectionSource
import com.ohmyhaze.data.MonitoringState
import com.ohmyhaze.data.SavedMonitoringLocation
import com.ohmyhaze.ui.theme.OhMyHazeTheme
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun refreshShowsProgressThenEstimateAndPreservesEstimateOnFailure() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val state = mutableStateOf(MonitoringState(selectedLocation = location))
        val refreshing = mutableStateOf(false)
        val refreshRequests = AtomicInteger()

        composeRule.setContent {
            OhMyHazeTheme {
                HomeScreen(
                    state = state.value,
                    onChangeLocation = {},
                    onRefresh = {
                        refreshRequests.incrementAndGet()
                        refreshing.value = true
                    },
                    onOpenSettings = {},
                    isRefreshing = refreshing.value,
                    notificationsAvailable = false,
                )
            }
        }

        composeRule.onNodeWithText("Refresh now").performClick()
        composeRule.onNodeWithText("Checking…").assertIsDisplayed()
        assertEquals(1, refreshRequests.get())

        val estimate = AirQualityEstimate(75.0, now.minusSeconds(600), now, "Asia/Kuala_Lumpur")
        composeRule.runOnIdle {
            state.value = state.value.copy(
                estimate = estimate,
                lastSuccessfulCheckAt = now,
                checkStatus = CheckStatus.AVAILABLE,
            )
            refreshing.value = false
        }
        composeRule.onNodeWithText("75").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh now").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                checkStatus = CheckStatus.NETWORK_ERROR,
                lastCheckError = "Could not reach Open-Meteo. Try again later.",
            )
        }
        composeRule.onNodeWithText("75").assertIsDisplayed()
        composeRule.onNodeWithText("Could not reach Open-Meteo. Try again later.").assertIsDisplayed()
        composeRule.onNodeWithText("Android notifications are unavailable; checks continue without alerts")
            .assertIsDisplayed()
    }

    @Test
    fun delayedCheckIsShownSeparatelyFromStillDisplayableEstimateFreshness() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val checkedAt = now.minusSeconds(3 * 60 * 60L)
        val state = MonitoringState(
            selectedLocation = location,
            estimate = AirQualityEstimate(
                sourceUsAqi = 75.0,
                modelValidAt = now.minusSeconds(4 * 60 * 60L),
                retrievedAt = checkedAt,
                providerTimezone = "Asia/Kuala_Lumpur",
            ),
            lastSuccessfulCheckAt = checkedAt,
            checkStatus = CheckStatus.AVAILABLE,
        )

        composeRule.setContent {
            OhMyHazeTheme {
                HomeScreen(
                    state = state,
                    onChangeLocation = {},
                    onRefresh = {},
                    onOpenSettings = {},
                    isRefreshing = false,
                    notificationsAvailable = true,
                )
            }
        }

        composeRule.onNodeWithText("Estimate is within the last 12 hours").assertIsDisplayed()
        composeRule.onNodeWithText("Monitoring is delayed; no successful check in over two hours")
            .assertIsDisplayed()
    }

    @Test
    fun manualRefreshRemainsAvailableWhenAutomaticMonitoringIsPaused() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val refreshRequests = AtomicInteger()

        composeRule.setContent {
            OhMyHazeTheme {
                HomeScreen(
                    state = MonitoringState(selectedLocation = location, monitoringEnabled = false),
                    onChangeLocation = {},
                    onRefresh = { refreshRequests.incrementAndGet() },
                    onOpenSettings = {},
                    isRefreshing = false,
                    notificationsAvailable = true,
                )
            }
        }

        composeRule.onNodeWithText("Monitoring is paused").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh now").performClick()
        assertEquals(1, refreshRequests.get())
    }
}
