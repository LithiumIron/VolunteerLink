package com.example.volunteerlink.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.volunteerlink.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class CurrentLocationOutcome(
    val match: GeoapifyLocationMatch?,
    val message: String
)

object CurrentLocationResolver {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

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