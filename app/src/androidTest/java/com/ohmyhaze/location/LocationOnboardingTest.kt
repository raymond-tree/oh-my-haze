package com.ohmyhaze.location

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.watcher.PermissionDialog
import androidx.test.uiautomator.uiAutomator
import com.ohmyhaze.MainActivity
import com.ohmyhaze.OhMyHazeApplication
import com.ohmyhaze.data.AirQualityEstimate
import com.ohmyhaze.data.ApiCheckResult
import com.ohmyhaze.data.RequestSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.TimeUnit

class LocationOnboardingTest {
    @Test
    fun deniedCurrentLocationInPickerKeepsManualFallbackAndShowsAQIDashboard() = uiAutomator {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm revoke $APP_PACKAGE android.permission.ACCESS_COARSE_LOCATION")
        device.executeShellCommand(
            "pm clear-permission-flags $APP_PACKAGE android.permission.ACCESS_COARSE_LOCATION user-set user-fixed",
        )

        startActivity(MainActivity::class.java)
        waitForAppToBeVisible(APP_PACKAGE)

        val app = instrumentation.targetContext.applicationContext as OhMyHazeApplication
        val onboarding = onElementOrNull { textAsString() == "Choose your monitoring location" }
        if (onboarding != null) {
            onElement { textAsString() == HOURLY_EXPLANATION }
            onElement {
                textAsString()?.contains("Your saved coordinates are sent directly to Open-Meteo") == true
            }
            onElement { textAsString()?.contains("does not track your movements") == true }
            onElement { textAsString() == "Choose a place manually" }.click()
        } else {
            onElement { textAsString() == "Change" }.click()
        }

        val savedBeforeRequest = runBlocking { app.monitoringStateStore.state.first().selectedLocation }
        watchFor(PermissionDialog) { clickDeny() }
        onElement { textAsString() == "Use my current location" }.click()
        onElement { textAsString()?.contains("Location permission was denied") == true }
        val savedAfterDenial = runBlocking { app.monitoringStateStore.state.first().selectedLocation }
        assertEquals("Permission denial must preserve the previous saved location", savedBeforeRequest, savedAfterDenial)

        onElement { textAsString() == "Kuala Lumpur" }.click()
        onElement { textAsString() == "Save this location" }.click()
        onElement {
            textAsString() == "No AQI estimate yet" || textAsString() == "AQI"
        }
        val saved = runBlocking { app.monitoringStateStore.state.first().selectedLocation }
        assertEquals("Kuala Lumpur", saved?.label)
        assertEquals(3.139, saved?.latitude ?: -1.0, 0.000001)
        assertEquals(101.6869, saved?.longitude ?: -1.0, 0.000001)

        val checkedAt = Instant.now()
        val snapshot = runBlocking { app.monitoringStateStore.snapshotForRequest(RequestSource.MANUAL)!! }
        val estimate = AirQualityEstimate(
            sourceUsAqi = 100.5,
            modelValidAt = checkedAt.minusSeconds(1_800),
            retrievedAt = checkedAt,
            providerTimezone = "Asia/Kuala_Lumpur",
        )
        runBlocking {
            app.monitoringStateStore.applyResponse(snapshot, ApiCheckResult.Success(estimate, checkedAt))
        }
        onElement { textAsString() == "AQI" }
        onElement { textAsString() == "101" }
        assertFalse(onElementOrNull { textAsString() == "US AQI" } != null)
        assertFalse(onElementOrNull { textAsString()?.startsWith("Model-valid time:") == true } != null)
        assertFalse(onElementOrNull { textAsString()?.contains("Source: Open-Meteo") == true } != null)

        onElement { textAsString() == "Settings" }.click()
        onElement { textAsString() == "Data attribution" }.click()
        onElement {
            textAsString()?.contains("Copernicus Atmosphere Monitoring Service (CAMS)") == true
        }
        onElement { textAsString() == "CC BY 4.0 license" }
        onElement { textAsString() == "Close" }.click()

        runBlocking { app.monitoringStateStore.setMonitoringEnabled(false) }
        app.monitoringScheduler.setMonitoringEnabled(false).result.get(10, TimeUnit.SECONDS)
    }

    private companion object {
        const val APP_PACKAGE = "com.ohmyhaze"
        const val HOURLY_EXPLANATION =
            "Air quality is checked approximately every hour. Android battery settings may delay or occasionally prevent checks and alerts."
    }
}
