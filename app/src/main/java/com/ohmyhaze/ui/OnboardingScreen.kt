package com.ohmyhaze.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohmyhaze.R

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onRequestCurrentLocation: () -> Unit,
    onChooseManual: () -> Unit,
    isRequestingLocation: Boolean = false,
    locationError: String? = null,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.onboarding_intro))
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.hourly_check_explanation))
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.location_privacy_disclosure))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestCurrentLocation,
                enabled = !isRequestingLocation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isRequestingLocation) "Getting location…" else "Use current location")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onChooseManual, modifier = Modifier.fillMaxWidth()) {
                Text("Choose a place manually")
            }
            if (locationError != null) {
                Spacer(Modifier.height(12.dp))
                Text(locationError, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
