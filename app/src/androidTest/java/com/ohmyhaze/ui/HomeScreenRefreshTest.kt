package com.ohmyhaze.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
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
                Scaffold { contentPadding ->
                    HomeScreen(
                        state = state,
                        modifier = Modifier.padding(contentPadding),
                        onChangeLocation = {},
                        onRefresh = {},
                        onOpenSettings = {},
                        isRefreshing = false,
                        notificationsAvailable = true,
                    )
                }
            }
        }

        composeRule.onNodeWithText("AQI").assertIsDisplayed()
        composeRule.onNodeWithText("75").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("AQI category gauge: Moderate").assertIsDisplayed()
        composeRule.onNodeWithText("Monitoring on").assertIsDisplayed()
        val posterNode = composeRule.onNodeWithContentDescription(
            "Moderate. AQI 51–100. Air quality is acceptable. Normal outdoor activities are fine.",
            substring = true,
        )
        posterNode.fetchSemanticsNode()
        val posterBounds = posterNode.fetchSemanticsNode().boundsInRoot
        val viewportBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val viewportWidthDp = viewportBounds.width / density
        val viewportHeightDp = viewportBounds.height / density
        val defaultFontScale = context.resources.configuration.fontScale <= 1.01f
        val standardCompactPortrait =
            viewportWidthDp in 350f..370f && viewportHeightDp in 780f..810f && defaultFontScale
        val refreshButton = composeRule.onNodeWithText("Refresh now")
        val refreshBounds = refreshButton.fetchSemanticsNode().boundsInRoot
        if (standardCompactPortrait) {
            assertTrue(
                "The complete poster should fit the standard portrait viewport",
                posterBounds.top >= viewportBounds.top && posterBounds.bottom <= viewportBounds.bottom,
            )
            assertTrue(
                "The refresh action should fit the standard 360 × 800 dp viewport with the full poster",
                refreshBounds.bottom <= viewportBounds.bottom,
            )
        } else {
            posterNode.performScrollTo().assertIsDisplayed()
            refreshButton.performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Last checked", substring = true).fetchSemanticsNode()
        assertNoDashboardText("What you can do")
        assertNoDashboardText("Moderate")
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

        composeRule.onNodeWithText("Refresh now").performScrollTo().performClick()
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
        composeRule.onNodeWithText("Refresh now").performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                checkStatus = CheckStatus.NETWORK_ERROR,
                lastCheckError = "Could not reach Open-Meteo. Try again later.",
            )
        }
        composeRule.onNodeWithText("75").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Could not update AQI. Try again later.").fetchSemanticsNode()
        composeRule.onNodeWithText("Alerts unavailable").fetchSemanticsNode()
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
                Scaffold { contentPadding ->
                    HomeScreen(
                        state = state,
                        modifier = Modifier.padding(contentPadding),
                        onChangeLocation = {},
                        onRefresh = {},
                        onOpenSettings = {},
                        isRefreshing = false,
                        notificationsAvailable = true,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Recent reading").assertIsDisplayed()
        composeRule.onNodeWithText("Check delayed").assertIsDisplayed()
    }

    @Test
    fun fullPosterCopyIsAccessibleWithoutDuplicateVisibleAdvice() {
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
            estimate = AirQualityEstimate(125.0, now.minusSeconds(600), now, "Asia/Kuala_Lumpur"),
            lastSuccessfulCheckAt = now,
            checkStatus = CheckStatus.AVAILABLE,
        )

        composeRule.setContent {
            OhMyHazeTheme {
                Scaffold { contentPadding ->
                    HomeScreen(
                        state = state,
                        modifier = Modifier.padding(contentPadding),
                        onChangeLocation = {},
                        onRefresh = {},
                        onOpenSettings = {},
                        isRefreshing = false,
                        notificationsAvailable = true,
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(
            "Unhealthy for Sensitive Groups. AQI 101–150. Sensitive groups may experience health effects.",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Sensitive groups: reduce long or strenuous outdoor activity.",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "For smoke or particle pollution, use a well-fitting N95 respirator.",
            substring = true,
        ).assertIsDisplayed()
        assertNoDashboardText("What you can do")
        assertNoDashboardText("Everyone else: reduce prolonged")
        composeRule.onNodeWithText("General guidance only; local pollutant-specific advice may differ.")
            .fetchSemanticsNode()
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
                "Good. AQI 0–50.",
                "Open windows for fresh air.",
            ),
            75.0 to listOf(
                "Moderate. AQI 51–100.",
                "In smoke, sensitive people may use a well-fitting N95 respirator.",
            ),
            125.0 to listOf(
                "Unhealthy for Sensitive Groups. AQI 101–150.",
                "Sensitive groups: reduce long or strenuous outdoor activity.",
                "For smoke or particle pollution, use a well-fitting N95 respirator.",
            ),
            175.0 to listOf(
                "Unhealthy. AQI 151–200.",
                "Reduce long or strenuous outdoor activity.",
                "For smoke, use an air cleaner to reduce indoor particles.",
            ),
            250.0 to listOf(
                "Very Unhealthy. AQI 201–300.",
                "Sensitive groups stay indoors.\nOthers limit outdoor activity.",
                "For smoke, use an air cleaner to reduce indoor particles.",
            ),
            350.0 to listOf(
                "Hazardous. AQI 301+.",
                "Everyone: avoid outdoor physical activity.",
                "If outdoors in smoke, wear a well-fitting N95 respirator.",
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
                composeRule.onNodeWithContentDescription(recommendation, substring = true)
                    .performScrollTo()
                    .assertIsDisplayed()
            }
            if (index < samples.lastIndex) {
                assertNoDashboardText("What you can do")
            }
        }
        composeRule.onNodeWithText("General guidance only; local pollutant-specific advice may differ.")
            .fetchSemanticsNode()
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
