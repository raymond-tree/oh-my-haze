package com.ohmyhaze.location

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.watcher.PermissionDialog
import androidx.test.uiautomator.scrollToElement
import androidx.test.uiautomator.uiAutomator
import com.ohmyhaze.data.AirQualityEstimate
import com.ohmyhaze.data.ApiCheckResult
import com.ohmyhaze.MainActivity
import com.ohmyhaze.OhMyHazeApplication
import com.ohmyhaze.data.RequestSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class LocationOnboardingTest {
    @Test
    fun deniedLocationPermissionStillOffersManualSelectionAndSavesTheChoice() = uiAutomator {
        startActivity(MainActivity::class.java)
        waitForAppToBeVisible(APP_PACKAGE)

        val onboarding = onElementOrNull { textAsString() == "Choose your monitoring location" }
        if (onboarding != null) {
            onElement { textAsString() == HOURLY_EXPLANATION }
            onElement {
                textAsString()?.contains("Your saved coordinates are sent directly to Open-Meteo") == true
            }
            onElement { textAsString()?.contains("does not track your movements") == true }
            onElement { textAsString() == "Choose a place manually" }
            watchFor(PermissionDialog) { clickDeny() }
            onElement { textAsString() == "Use current location" }.click()
            onElement { textAsString()?.contains("Location permission was denied") == true }
            onElement { textAsString() == "Choose a place manually" }.click()
        } else {
            onElement { textAsString() == "Change monitoring location" }.click()
        }
        onElement { textAsString() == "Kuala Lumpur" }.click()
        onElement { textAsString() == "Save this location" }.click()
        onElement { textAsString() == "No US AQI estimate yet" }

        val app = instrumentation.targetContext.applicationContext as OhMyHazeApplication
        val saved = runBlocking { app.monitoringStateStore.state.first().selectedLocation }
        assertEquals("Kuala Lumpur", saved?.label)
        assertEquals(3.139, saved?.latitude ?: -1.0, 0.000001)
        assertEquals(101.6869, saved?.longitude ?: -1.0, 0.000001)

        onElement { textAsString() == "No US AQI estimate yet" }
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
        onElement { textAsString() == "US AQI" }
        onElement { textAsString() == "101" }
        onElement { textAsString() == "Unhealthy for Sensitive Groups" }
        onElement { textAsString()?.startsWith("Model-valid time:") == true }
        onElement { textAsString()?.startsWith("Last successful API check:") == true }
        onElement { textAsString()?.contains("Hourly checks may show the same estimate") == true }
        onElement { textAsString()?.contains("About this estimate and source") == true }.click()
        onElement { textAsString()?.contains("regional grid") == true }
        onElement { textAsString()?.contains("official Malaysian API/IPU reading") == true }
        onElement { isScrollable }.scrollToElement(Direction.DOWN) { textAsString() == "CAMS data provider" }
        onElement { textAsString() == "CAMS data provider" }
        onElement { textAsString()?.contains("Creative Commons Attribution 4.0") == true }

        runBlocking {
            app.monitoringStateStore.setMonitoringEnabled(false)
        }
        app.monitoringScheduler.setMonitoringEnabled(false).result.get(10, TimeUnit.SECONDS)
    }

    private companion object {
        const val APP_PACKAGE = "com.ohmyhaze"
        const val HOURLY_EXPLANATION =
            "Air quality is checked approximately every hour. Android battery settings may delay or occasionally prevent checks and alerts."
    }
}
