package com.ohmyhaze.location

import com.ohmyhaze.data.LocationSelectionSource
import com.ohmyhaze.data.SavedMonitoringLocation
import java.time.Instant

/** Static locality choices keep denied-permission onboarding independent of geocoding services. */
data class BundledLocality(
    val label: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toSavedLocation(selectedAt: Instant = Instant.now()): SavedMonitoringLocation =
        SavedMonitoringLocation(
            label = label,
            latitude = latitude,
            longitude = longitude,
            selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
            selectedAt = selectedAt,
        )
}

object BundledLocations {
    val all: List<BundledLocality> = listOf(
        BundledLocality("Kuala Lumpur", 3.1390, 101.6869),
        BundledLocality("Petaling Jaya", 3.1073, 101.6067),
        BundledLocality("Shah Alam", 3.0733, 101.5185),
        BundledLocality("Putrajaya", 2.9264, 101.6964),
        BundledLocality("George Town", 5.4141, 100.3288),
        BundledLocality("Ipoh", 4.5975, 101.0901),
        BundledLocality("Johor Bahru", 1.4927, 103.7414),
        BundledLocality("Kuantan", 3.8077, 103.3260),
        BundledLocality("Kuching", 1.5535, 110.3593),
        BundledLocality("Kota Kinabalu", 5.9804, 116.0735),
    )
}
