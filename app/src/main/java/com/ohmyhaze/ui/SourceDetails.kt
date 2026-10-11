package com.ohmyhaze.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SourceDetails(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "About this estimate",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Open-Meteo provides model-based air-quality estimates for a regional grid. " +
                "This is not a measurement taken at your exact location or an official Malaysian API/IPU reading.",
        )
        Text("The underlying CAMS Global model usually updates about every 12 hours. Hourly app checks may return the same estimate.")
        Text("Air-quality model data: Copernicus Atmosphere Monitoring Service (CAMS), delivered by Open-Meteo.com.")
        Text("US AQI category names are shown by Oh My Haze. Data is attributed under CC BY 4.0.")
        OutlinedButton(
            onClick = { openLink(context, OPEN_METEO_URL) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open-Meteo Air Quality API")
        }
        OutlinedButton(
            onClick = { openLink(context, CAMS_URL) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("CAMS data provider")
        }
        OutlinedButton(
            onClick = { openLink(context, CC_BY_URL) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Creative Commons Attribution 4.0 license")
        }
    }
}

private fun openLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private const val OPEN_METEO_URL = "https://open-meteo.com/en/docs/air-quality-api"
private const val CAMS_URL = "https://atmosphere.copernicus.eu/"
private const val CC_BY_URL = "https://creativecommons.org/licenses/by/4.0/"
