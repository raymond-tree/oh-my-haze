package com.ohmyhaze.location

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ohmyhaze.data.SavedMonitoringLocation
import com.ohmyhaze.ui.LocationPicker
import com.ohmyhaze.ui.theme.OhMyHazeTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val localities = listOf(
        BundledLocality("Kuala Lumpur", 3.139, 101.6869),
        BundledLocality("Petaling Jaya", 3.1073, 101.6067),
    )

    @Test
    fun currentLocationActionPrecedesManualChoicesAndReportsProgressAndFailure() {
        val requesting = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        val requestCount = AtomicInteger()
        var saved: SavedMonitoringLocation? = null

        composeRule.setContent {
            OhMyHazeTheme {
                LocationPicker(
                    locations = localities,
                    onUseCurrentLocation = {
                        requestCount.incrementAndGet()
                        requesting.value = true
                    },
                    isRequestingCurrentLocation = requesting.value,
                    locationError = error.value,
                    onLocationSelected = { saved = it },
                )
            }
        }

        val currentLocation = composeRule.onNodeWithText("Use my current location")
        val firstManualPlace = composeRule.onNodeWithText("Kuala Lumpur")
        currentLocation.assertIsDisplayed()
        firstManualPlace.assertIsDisplayed()
        assertTrue(
            "Current-location action should appear above manual choices",
            currentLocation.fetchSemanticsNode().boundsInRoot.top <
                firstManualPlace.fetchSemanticsNode().boundsInRoot.top,
        )

        firstManualPlace.performClick()
        composeRule.onNodeWithText("Save this location").assertIsDisplayed()
        currentLocation.performClick()
        assertEquals(1, requestCount.get())
        composeRule.onNodeWithText("Getting location…").assertIsDisplayed()
        composeRule.onNodeWithText("Save this location").assertIsNotEnabled()

        composeRule.runOnIdle {
            requesting.value = false
            error.value = "Location permission was denied. Choose a place manually instead."
        }
        composeRule.onNodeWithText(
            "Location permission was denied. Choose a place manually instead.",
        ).assertIsDisplayed()
        firstManualPlace.performClick()
        composeRule.onNodeWithText("Save this location").performClick()
        composeRule.runOnIdle {
            assertEquals("Kuala Lumpur", saved?.label)
            assertEquals(3.139, saved?.latitude ?: -1.0, 0.000001)
            assertEquals(101.6869, saved?.longitude ?: -1.0, 0.000001)
        }
    }

    @Test
    fun manualChoiceCanBeSavedAndPickerCanBeCancelled() {
        var saved: SavedMonitoringLocation? = null
        val cancelCount = AtomicInteger()

        composeRule.setContent {
            OhMyHazeTheme {
                LocationPicker(
                    locations = localities,
                    onCancel = { cancelCount.incrementAndGet() },
                    onLocationSelected = { saved = it },
                )
            }
        }

        composeRule.onNodeWithText("Petaling Jaya").performClick()
        composeRule.onNodeWithText("Save this location").performClick()
        composeRule.runOnIdle { assertEquals("Petaling Jaya", saved?.label) }

        composeRule.onNodeWithText("Back").performClick()
        assertEquals(1, cancelCount.get())
    }
}
