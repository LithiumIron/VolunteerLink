package com.example.volunteerlink.data.location

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Coordinates the device-location and reverse-geocoding steps needed when an Organisation chooses to use its
// current location.
//
// It keeps Android location permission/provider details out of the screen and returns a structured location result
// that can be placed into the same Create Post fields used by Geoapify selection.
//
// Current location is a convenience input only; it does not bypass validation or become a database write until the
// organisation saves/publishes the post.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.volunteerlink.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * DETAILED DECLARATION — CurrentLocationOutcome
 *
 * Domain/UI type for Current Location Outcome used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CurrentLocationOutcome(
    val match: GeoapifyLocationMatch?,
    val message: String
)

/**
 * DETAILED DECLARATION — CurrentLocationResolver
 *
 * Single shared instance for Current Location Resolver so related rules/state are defined once for the
 * application process.
 */
object CurrentLocationResolver {

    /**
     * DETAILED BEHAVIOUR — hasLocationPermission
     *
     * Implements the current VolunteerLink responsibility for has location permission in this support/model
     * layer.
     */
    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * DETAILED BEHAVIOUR — isLocationEnabled
     *
     * Implements the current VolunteerLink responsibility for is location enabled in this support/model layer.
     */
    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * Same GPS fetch as MapScreen (VolunteerMapLocationRequest), followed by
     * a Geoapify reverse-geocode + countryStates match. onOutcome is always
     * called exactly once — with a match, or null match + a message
     * explaining why (permission/GPS/no-match), so the caller can fall back
     * to manual selection. Returns a cancel function, same contract as
     * VolunteerMapLocationRequest.start().
     */
    /**
     * DETAILED BEHAVIOUR — resolve
     *
     * Implements the current VolunteerLink responsibility for resolve in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     */
    fun resolve(
        context: Context,
        countryStates: Map<String, Map<String, List<String>>>,
        scope: CoroutineScope,
        onOutcome: (CurrentLocationOutcome) -> Unit
    ): () -> Unit {
        return VolunteerMapLocationRequest.start(context) { result ->
            val location = result.location
            if (location == null) {
                onOutcome(CurrentLocationOutcome(null, result.message))
                return@start
            }

            scope.launch {
                val match = GeoapifyReverseGeocoder.matchToKnownLocation(
                    apiKey = BuildConfig.GEOAPIFY_API_KEY,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    countryStates = countryStates
                )

                onOutcome(
                    if (match != null) {
                        CurrentLocationOutcome(match, "Location detected.")
                    } else {
                        CurrentLocationOutcome(
                            null,
                            "Couldn't match your location to a supported area. Please select it manually."
                        )
                    }
                )
            }
        }
    }
}