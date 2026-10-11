package com.ohmyhaze

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.ohmyhaze.data.LocationSelectionSource
import com.ohmyhaze.data.MonitoringState
import com.ohmyhaze.data.MonitoringStateStore
import com.ohmyhaze.data.SavedMonitoringLocation
import com.ohmyhaze.debug.postDebugTestNotification
import com.ohmyhaze.location.CurrentLocationProvider
import com.ohmyhaze.ui.HomeScreen
import com.ohmyhaze.ui.LocationPicker
import com.ohmyhaze.ui.OnboardingScreen
import com.ohmyhaze.ui.SettingsScreen
import com.ohmyhaze.ui.theme.OhMyHazeTheme
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OhMyHazeApplication
        setContent {
            OhMyHazeTheme {
                OhMyHazeApp(
                    stateStore = app.monitoringStateStore,
                    currentLocationProvider = app.currentLocationProvider,
                    onLocationSaved = app::syncMonitoringSchedule,
                )
            }
        }
    }
}

@Composable
private fun OhMyHazeApp(
    stateStore: MonitoringStateStore,
    currentLocationProvider: CurrentLocationProvider,
    onLocationSaved: () -> Unit,
) {
    val context = LocalContext.current
    val state by stateStore.state.collectAsState(initial = MonitoringState())
    val savedLocation = state.selectedLocation
    val scope = rememberCoroutineScope()
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var locationPickerReturnToSettings by remember { mutableStateOf(false) }
    var isRequestingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var manualRefreshInProgress by remember { mutableStateOf(false) }
    var notificationAvailabilityVersion by remember { mutableStateOf(0) }
    val app = context.applicationContext as OhMyHazeApplication
    val notificationAvailable = remember(notificationAvailabilityVersion) {
        app.airQualityNotifier.isAvailable()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notificationAvailabilityVersion += 1
    }

    LaunchedEffect(manualRefreshInProgress) {
        if (manualRefreshInProgress) {
            delay(15_000)
            manualRefreshInProgress = false
        }
    }
    LaunchedEffect(state.lastSuccessfulCheckAt, state.checkStatus, state.lastCheckError) {
        if (manualRefreshInProgress) manualRefreshInProgress = false
    }

    val saveCurrentLocation: suspend (Location) -> Unit = { location ->
        stateStore.saveLocation(
            SavedMonitoringLocation(
                label = "Current location",
                latitude = location.latitude,
                longitude = location.longitude,
                selectionSource = LocationSelectionSource.CURRENT_LOCATION,
                selectedAt = Instant.now(),
            ),
        )
        onLocationSaved()
        showLocationPicker = false
        showSettings = false
        locationPickerReturnToSettings = false
        isRequestingLocation = false
        locationError = null
    }

    val fetchCurrentLocation: () -> Unit = {
        isRequestingLocation = true
        locationError = null
        scope.launch {
            try {
                saveCurrentLocation(currentLocationProvider.getOneShotLocation())
            } catch (error: Exception) {
                if (error is CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
                isRequestingLocation = false
                locationError = "Location is unavailable. Choose a place manually instead."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchCurrentLocation()
        else {
            isRequestingLocation = false
            locationError = "Location permission was denied. Choose a place manually instead."
        }
    }

    val requestCurrentLocation: () -> Unit = {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        } else {
            isRequestingLocation = true
            locationError = null
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val saveSelectedLocation: (SavedMonitoringLocation) -> Unit = { location ->
        scope.launch {
            stateStore.saveLocation(location)
            onLocationSaved()
            showLocationPicker = false
            locationError = null
            locationPickerReturnToSettings = false
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when {
            showLocationPicker -> {
                LocationPicker(
                    modifier = Modifier.padding(padding),
                    onCancel = {
                        showLocationPicker = false
                        showSettings = locationPickerReturnToSettings
                        locationPickerReturnToSettings = false
                        locationError = null
                    },
                    onUseCurrentLocation = requestCurrentLocation,
                    isRequestingCurrentLocation = isRequestingLocation,
                    locationError = locationError,
                    onLocationSelected = saveSelectedLocation,
                )
            }

            savedLocation == null -> {
                OnboardingScreen(
                    modifier = Modifier.padding(padding),
                    onRequestCurrentLocation = requestCurrentLocation,
                    onChooseManual = { showLocationPicker = true },
                    isRequestingLocation = isRequestingLocation,
                    locationError = locationError,
                )
            }

            showSettings -> {
                SettingsScreen(
                    state = state,
                    notificationsAvailable = notificationAvailable,
                    runtimePermissionCanBeRequested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED,
                    modifier = Modifier.padding(padding),
                    onMonitoringChanged = { enabled ->
                        scope.launch {
                            stateStore.setMonitoringEnabled(enabled)
                            onLocationSaved()
                        }
                    },
                    onAppAlertsChanged = { enabled ->
                        scope.launch { stateStore.setNotificationsEnabled(enabled) }
                    },
                    onSensitivityChanged = { floor ->
                        scope.launch { stateStore.setSensitivityFloor(floor) }
                    },
                    onChangeLocation = {
                        locationPickerReturnToSettings = true
                        showSettings = false
                        showLocationPicker = true
                    },
                    onRequestNotificationPermission = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onBack = { showSettings = false },
                )
            }

            else -> {
                HomeScreen(
                    state = state,
                    modifier = Modifier.padding(padding),
                    onChangeLocation = {
                        locationPickerReturnToSettings = false
                        showSettings = false
                        showLocationPicker = true
                    },
                    onRefresh = {
                        manualRefreshInProgress = true
                        app.monitoringScheduler.enqueueManualCheck()
                    },
                    onOpenSettings = { showSettings = true },
                    onTestAlert = {
                        val posted = state.notificationsEnabled &&
                            postDebugTestNotification(context, savedLocation.label)
                        val message = when {
                            !state.notificationsEnabled -> "Turn on App alerts in Oh My Haze settings first."
                            posted -> "Test alert sent"
                            else -> "Notifications are blocked. Check Oh My Haze in Android settings."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    },
                    isRefreshing = manualRefreshInProgress,
                    notificationsAvailable = notificationAvailable,
                )
            }
        }
    }
}
