package com.ohmyhaze.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardUsesAQILabelAndOmitsSourceAndModelDetails() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val state = MonitoringState(
            selectedLocation = location,
            estimate = AirQualityEstimate(75.0, now.minusSeconds(600), now, "Asia/Kuala_Lumpur"),
            lastSuccessfulCheckAt = now.minusSeconds(600),
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

        composeRule.onNodeWithText("AQI").assertIsDisplayed()
        composeRule.onNodeWithText("75").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("AQI category gauge: Moderate").assertIsDisplayed()
        composeRule.onNodeWithText("Last checked", substring = true).assertIsDisplayed()
        assertNoDashboardText("US AQI")
        assertNoDashboardText("Model-valid time")
        assertNoDashboardText("Source: Open-Meteo")
        assertNoDashboardText("About this estimate and source")
        assertNoDashboardText("Hourly checks may show the same estimate")
    }

    private fun assertNoDashboardText(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty())
    }

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
        composeRule.onNodeWithText("Could not update AQI. Try again later.").assertIsDisplayed()
        composeRule.onNodeWithText("Alerts unavailable").assertIsDisplayed()
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

        composeRule.onNodeWithText("Recent reading").assertIsDisplayed()
        composeRule.onNodeWithText("Check delayed").assertIsDisplayed()
    }

    @Test
    fun recommendationsAreTextAndProportionateToDisplayedCategory() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val state = MonitoringState(
            selectedLocation = location,
            estimate = AirQualityEstimate(175.0, now.minusSeconds(600), now, "Asia/Kuala_Lumpur"),
            lastSuccessfulCheckAt = now,
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

        composeRule.onNodeWithText("Sensitive groups: avoid long or intense outdoor activity.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Everyone else: reduce prolonged or heavy exertion.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("General guidance only; local health advice may differ.")
            .assertIsDisplayed()
    }

    @Test
    fun recommendationsChangeAcrossAllSixAQICategories() {
        val now = Instant.ofEpochMilli(Instant.now().toEpochMilli())
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = now,
        )
        val samples = listOf(
            25.0 to listOf(
                "Enjoy outdoor activities as usual.",
                "Air quality poses little to no risk for most people.",
            ),
            75.0 to listOf(
                "Most people can continue outdoor activities.",
                "If you are unusually sensitive, choose shorter or lighter activity.",
            ),
            125.0 to listOf(
                "Sensitive groups: shorten outdoor activity and take more breaks.",
                "Everyone else can continue as usual and watch for symptoms.",
            ),
            175.0 to listOf(
                "Sensitive groups: avoid long or intense outdoor activity.",
                "Everyone else: reduce prolonged or heavy exertion.",
            ),
            250.0 to listOf(
                "Sensitive groups: avoid outdoor physical activity.",
                "Others: move long or strenuous activity indoors or reschedule.",
            ),
            350.0 to listOf(
                "Everyone: avoid outdoor physical activity.",
                "If smoke or particles are present, choose a cleaner indoor space.",
            ),
        )
        val state = mutableStateOf(
            MonitoringState(
                selectedLocation = location,
                estimate = AirQualityEstimate(samples.first().first, now.minusSeconds(600), now, "Asia/Kuala_Lumpur"),
                lastSuccessfulCheckAt = now,
                checkStatus = CheckStatus.AVAILABLE,
            ),
        )

        composeRule.setContent {
            OhMyHazeTheme {
                HomeScreen(
                    state = state.value,
                    onChangeLocation = {},
                    onRefresh = {},
                    onOpenSettings = {},
                    isRefreshing = false,
                    notificationsAvailable = true,
                )
            }
        }

        samples.forEachIndexed { index, (aqi, recommendations) ->
            composeRule.runOnIdle {
                state.value = state.value.copy(
                    estimate = AirQualityEstimate(aqi, now.minusSeconds(600), now, "Asia/Kuala_Lumpur"),
                )
            }
            recommendations.forEach { recommendation ->
                composeRule.onNodeWithText(recommendation).assertIsDisplayed()
            }
            if (index < samples.lastIndex) {
                assertNoDashboardText("If smoke or particles are present")
            }
        }
        composeRule.onNodeWithText("General guidance only; local health advice may differ.")
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

        composeRule.onNodeWithText("Monitoring paused").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh now").performClick()
        assertEquals(1, refreshRequests.get())
    }
}
