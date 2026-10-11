package com.ohmyhaze.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.ohmyhaze.location.BundledLocality
import com.ohmyhaze.location.BundledLocations
import com.ohmyhaze.data.SavedMonitoringLocation

@Composable
fun LocationPicker(
    modifier: Modifier = Modifier,
    locations: List<BundledLocality> = BundledLocations.all,
    onCancel: (() -> Unit)? = null,
    onLocationSelected: (SavedMonitoringLocation) -> Unit,
) {
    var selected by remember(locations) { mutableStateOf<BundledLocality?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Choose a place", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Select a saved monitoring location. The app will not follow your movements.")
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(locations, key = BundledLocality::label) { locality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) { selected = locality }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == locality,
                            onClick = { selected = locality },
                        )
                        Text(locality.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Button(
                onClick = { selected?.let { onLocationSelected(it.toSavedLocation()) } },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save this location")
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}
