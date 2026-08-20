package com.example.volunteerlink.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer

/**
 * Gets a one-time approximate device location only to improve autocomplete
 * ranking. Geoapify still works globally if permission is denied.
 */
object DeviceLocationHelper {

    @SuppressLint("MissingPermission")
    fun getApproximateCurrentLocation(
        context: Context,
        onResult: (Location?) -> Unit
    ) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onResult(null)
            return
        }

        val locationManager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        try {
            val provider = when {
                LocationManagerCompat.hasProvider(
                    locationManager,
                    LocationManager.NETWORK_PROVIDER
                ) && locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> LocationManager.NETWORK_PROVIDER

                LocationManagerCompat.hasProvider(
                    locationManager,
                    LocationManager.GPS_PROVIDER
                ) && locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) -> LocationManager.GPS_PROVIDER

                else -> null
            }

            if (provider == null) {
                onResult(null)
                return
            }

            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                null as CancellationSignal?,
                ContextCompat.getMainExecutor(context),
                Consumer { location -> onResult(location) }
            )
        } catch (_: SecurityException) {
            onResult(null)
        } catch (_: IllegalArgumentException) {
            onResult(null)
        }
    }
}
