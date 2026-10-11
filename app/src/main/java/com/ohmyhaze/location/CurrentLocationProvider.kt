package com.ohmyhaze.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class LocationPermissionRequiredException : Exception("Foreground approximate location permission is required")

class LocationUnavailableException : Exception("A current location could not be obtained")

/** Performs one bounded foreground fix. Monitoring workers must never call this provider. */
class CurrentLocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun getOneShotLocation(): Location = withTimeout(LOCATION_TIMEOUT_MILLIS) {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw LocationPermissionRequiredException()
        }

        val cancellation = CancellationTokenSource()
        try {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { cancellation.cancel() }
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_LOW_POWER)
                    .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                    .build()
                client.getCurrentLocation(request, cancellation.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            if (location == null) continuation.resumeWithException(LocationUnavailableException())
                            else continuation.resume(location)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }
        } finally {
            cancellation.cancel()
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 20_000L
    }
}
