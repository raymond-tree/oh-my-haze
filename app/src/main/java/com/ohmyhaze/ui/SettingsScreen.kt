package com.ohmyhaze.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.ohmyhaze.data.MonitoringState

@Composable
fun SettingsScreen(
    state: MonitoringState,
    notificationsAvailable: Boolean,
    runtimePermissionCanBeRequested: Boolean,
    modifier: Modifier = Modifier,
    onMonitoringChanged: (Boolean) -> Unit,
    onAppAlertsChanged: (Boolean) -> Unit,
    onSensitivityChanged: (Int) -> Unit,
    onChangeLocation: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
) {
    var showDataAttribution by remember { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Saved location: ${state.selectedLocation?.label ?: "Not set"}")
            PreferenceSwitch("Automatic monitoring", state.monitoringEnabled, onMonitoringChanged)
            PreferenceSwitch("App alerts", state.notificationsEnabled, onAppAlertsChanged)

            Text("Alert sensitivity", style = MaterialTheme.typography.titleMedium)
            Text("Notify when a newer AQI estimate first reaches this level.")
            SensitivityChoice(
                label = "AQI 101 — Unhealthy for Sensitive Groups",
                floor = 101,
                selected = state.sensitivityFloor == 101,
                onSelect = onSensitivityChanged,
            )
            SensitivityChoice(
                label = "AQI 151 — Unhealthy",
                floor = 151,
                selected = state.sensitivityFloor == 151,
                onSelect = onSensitivityChanged,
            )
            SensitivityChoice(
                label = "AQI 201 — Very Unhealthy",
                floor = 201,
                selected = state.sensitivityFloor == 201,
                onSelect = onSensitivityChanged,
            )
            SensitivityChoice(
                label = "AQI 301 — Hazardous",
                floor = 301,
                selected = state.sensitivityFloor == 301,
                onSelect = onSensitivityChanged,
            )

            Text(
                if (notificationsAvailable) {
                    "Android notification availability: Available"
                } else {
                    "Android notification availability: Unavailable. Android permission or settings may be blocking alerts."
                },
                color = if (notificationsAvailable) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error,
            )
            if (!state.notificationsEnabled) {
                Text("Air quality alerts are off in this app.")
            } else if (!notificationsAvailable && runtimePermissionCanBeRequested) {
                Button(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow Android notifications")
                }
            }

            Button(onClick = onChangeLocation, modifier = Modifier.fillMaxWidth()) {
                Text("Change monitoring location")
            }
            TextButton(onClick = { showDataAttribution = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Data attribution")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to air quality")
            }
        }
    }
    if (showDataAttribution) {
        DataAttributionDialog(onDismiss = { showDataAttribution = false })
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                onClick { onCheckedChange(!checked); true }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SensitivityChoice(label: String, floor: Int, selected: Boolean, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton) { onSelect(floor) }
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
                onClick { onSelect(floor); true }
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
