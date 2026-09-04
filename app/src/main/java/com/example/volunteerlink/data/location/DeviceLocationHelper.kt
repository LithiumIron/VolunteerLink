package com.example.volunteerlink.data.location

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Contains the Android-specific one-time location lookup used to obtain an approximate device coordinate.
//
// The helper checks permission/provider availability and returns location data to higher-level resolvers rather
// than letting Compose call LocationManager directly.
//
// Location is used for search bias/current-location convenience; VolunteerLink still lets the organisation search
// and choose locations elsewhere.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer

/**
 * Gets a one-time approximate device location only to improve autocomplete
 * ranking. Geoapify still works globally if permission is denied.
 */
/**
 * DETAILED DECLARATION — DeviceLocationHelper
 *
 * Single shared instance for Device Location Helper so related rules/state are defined once for the application
 * process.
 */
object DeviceLocationHelper {

    @SuppressLint("MissingPermission")
    /**
     * DETAILED BEHAVIOUR — getApproximateCurrentLocation
     *
     * Implements the current VolunteerLink responsibility for get approximate current location in this
     * support/model layer.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
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
            val providers = buildList {
                if (LocationManagerCompat.hasProvider(
                    locationManager,
                    LocationManager.NETWORK_PROVIDER
                ) && locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )) add(LocationManager.NETWORK_PROVIDER)

                if (LocationManagerCompat.hasProvider(
                    locationManager,
                    LocationManager.GPS_PROVIDER
                ) && locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )) add(LocationManager.GPS_PROVIDER)
            }

            if (providers.isEmpty()) {
                onResult(null)
                return
            }

            val handler = Handler(Looper.getMainLooper())
            var completed = false
            /**
             * DETAILED BEHAVIOUR — finish
             *
             * Implements the current VolunteerLink responsibility for finish in this support/model layer.
             */
            fun finish(location: Location?) {
                if (!completed) {
                    completed = true
                    handler.removeCallbacksAndMessages(null)
                    onResult(location)
                }
            }
            /**
             * DETAILED BEHAVIOUR — request
             *
             * Implements the current VolunteerLink responsibility for request in this support/model layer.
             *
             * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up
             * without leaving the UI in an assumed-success state.
             */
            fun request(index: Int) {
                if (index >= providers.size) {
                    val lastKnown = providers.asSequence()
                        .mapNotNull { provider ->
                            runCatching {
                                locationManager.getLastKnownLocation(provider)
                            }.getOrNull()
                        }
                        .maxByOrNull { it.time }
                    finish(lastKnown)
                    return
                }
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    providers[index],
                    null as CancellationSignal?,
                    ContextCompat.getMainExecutor(context),
                    Consumer { location ->
                        if (location != null) finish(location)
                        else request(index + 1)
                    }
                )
            }
            handler.postDelayed({
                if (!completed) request(1)
            }, 4_000L)
            handler.postDelayed({ finish(null) }, 10_000L)
            request(0)
        } catch (_: SecurityException) {
            onResult(null)
        } catch (_: IllegalArgumentException) {
            onResult(null)
        }
    }
}
