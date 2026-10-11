package com.ohmyhaze.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ohmyhaze.data.LocationSelectionSource
import com.ohmyhaze.data.MonitoringState
import com.ohmyhaze.data.SavedMonitoringLocation
import com.ohmyhaze.ui.theme.OhMyHazeTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MonitoringSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsControlsUpdateMonitoringAlertsSensitivityAndLocation() {
        val location = SavedMonitoringLocation(
            label = "Kuala Lumpur",
            latitude = 3.139,
            longitude = 101.6869,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = Instant.now(),
        )
        val state = mutableStateOf(
            MonitoringState(
                selectedLocation = location,
                monitoringEnabled = true,
                notificationsEnabled = true,
                sensitivityFloor = 101,
            ),
        )
        var locationChanges = 0

        composeRule.setContent {
            OhMyHazeTheme {
                SettingsScreen(
                    state = state.value,
                    notificationsAvailable = false,
                    runtimePermissionCanBeRequested = false,
                    onMonitoringChanged = { state.value = state.value.copy(monitoringEnabled = it) },
                    onAppAlertsChanged = { state.value = state.value.copy(notificationsEnabled = it) },
                    onSensitivityChanged = { state.value = state.value.copy(sensitivityFloor = it) },
                    onChangeLocation = { locationChanges += 1 },
                    onRequestNotificationPermission = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Saved location: Kuala Lumpur").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("US AQI", substring = true).fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Automatic monitoring").assertIsOn()
        composeRule.onNodeWithContentDescription("Automatic monitoring").performClick()
        composeRule.onNodeWithContentDescription("Automatic monitoring").assertIsOff()

        composeRule.onNodeWithText(
            "Android notification availability: Unavailable. Android permission or settings may be blocking alerts.",
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App alerts").performClick()
        composeRule.onNodeWithContentDescription("App alerts").assertIsOff()
        composeRule.onNodeWithText("Air quality alerts are off in this app.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App alerts").performClick()
        composeRule.onNodeWithContentDescription("App alerts").assertIsOn()

        val choices = listOf(
            101 to "AQI 101 — Unhealthy for Sensitive Groups",
            151 to "AQI 151 — Unhealthy",
            201 to "AQI 201 — Very Unhealthy",
            301 to "AQI 301 — Hazardous",
        )
        composeRule.onNodeWithContentDescription(choices.first().second).assertIsSelected()
        for ((floor, label) in choices.drop(1)) {
            composeRule.onNodeWithContentDescription(label).performScrollTo().performClick()
            composeRule.onNodeWithContentDescription(label).assertIsSelected()
            assertEquals(floor, state.value.sensitivityFloor)
        }
        composeRule.onNodeWithContentDescription(choices.first().second).assertIsNotSelected()

        composeRule.onNodeWithText("Data attribution").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "Air-quality data: Copernicus Atmosphere Monitoring Service (CAMS), delivered by Open-Meteo.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("CC BY 4.0 license").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        composeRule.onNodeWithText("Change monitoring location").performScrollTo().performClick()
        assertEquals(1, locationChanges)
    }
}
