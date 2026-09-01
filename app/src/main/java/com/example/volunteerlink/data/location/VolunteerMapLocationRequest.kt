package com.example.volunteerlink.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat

/** Reject stale/invalid fixes; use monotonic age, never the demo business clock. */
internal fun isUsableMapFix(ageMillis: Long, accuracyMeters: Float, latitude: Double, longitude: Double): Boolean =
    ageMillis in 0L..60_000L && accuracyMeters.isFinite() && accuracyMeters > 0f &&
        accuracyMeters <= 3_000f && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0

data class VolunteerMapLocationResult(val location: Location?, val message: String)

/**
 * Foreground, bounded one-shot request for Map only. Uses available platform
 * fused/network/GPS providers together. Removes listeners on success, timeout
 * or explicit cancellation; does not modify the shared Organisation helper.
 */
object VolunteerMapLocationRequest {
    @SuppressLint("MissingPermission")
    fun start(context: Context, onResult: (VolunteerMapLocationResult) -> Unit): () -> Unit {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val handler = Handler(Looper.getMainLooper())
        val listeners = mutableListOf<LocationListener>()
        var finished = false
        var best: Location? = null
        var bestIsCached = false
        var sawRejectedFix = false
        var registered = 0

        fun cleanup() {
            handler.removeCallbacksAndMessages(null)
            listeners.forEach { listener -> runCatching { manager.removeUpdates(listener) } }
            listeners.clear()
        }
        fun finish(result: VolunteerMapLocationResult) {
            if (finished) return
            finished = true
            cleanup()
            onResult(result)
        }
        fun usable(location: Location): Boolean {
            if (!location.hasAccuracy() || location.elapsedRealtimeNanos <= 0L) return false
            val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
            if (ageNanos < 0L) return false
            return isUsableMapFix(ageNanos / 1_000_000L, location.accuracy, location.latitude, location.longitude)
        }
        fun deliver(location: Location, cached: Boolean) {
            val metres = kotlin.math.ceil(location.accuracy.toDouble()).toInt()
            val label = if (cached) "Recent device location" else "Location found"
            finish(VolunteerMapLocationResult(location,
                "$label · estimated accuracy ±$metres m." +
                    if (metres > 500) " This is approximate; enable precise location in app permissions for a closer result." else ""))
        }
        fun consider(location: Location, cached: Boolean) {
            if (finished) return
            if (!usable(location)) {
                sawRejectedFix = true
                return
            }
            if (best == null || !usable(best!!) || location.accuracy <= best!!.accuracy) {
                best = Location(location)
                bestIsCached = cached
            }
            // Coarse results remain candidates while better providers have time to answer.
            if (location.accuracy <= 100f) deliver(location, cached)
        }

        val permitted = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!permitted) {
            finish(VolunteerMapLocationResult(null, "Location permission is unavailable. Tap the location button to request access."))
            return {}
        }
        val enabled = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        val providers = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { it in enabled }
        if (providers.isEmpty()) {
            finish(VolunteerMapLocationResult(null, "No location provider is available. Check device Location settings and retry."))
            return {}
        }

        // Check cached age AND accuracy before accepting it. Never accept arbitrary last-known coordinates.
        providers.mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .sortedBy { it.accuracy }
            .forEach { consider(it, cached = true) }
        if (finished) return {}

        providers.forEach { provider ->
            if (!finished) {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) = consider(location, cached = false)
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Required on older Android versions")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                listeners.add(listener)
                runCatching {
                    manager.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper())
                }.onSuccess { registered++ }
            }
        }
        if (registered == 0 && !finished) {
            val candidate = best
            if (candidate != null && usable(candidate)) deliver(candidate, bestIsCached)
            else finish(VolunteerMapLocationResult(null, "Android could not start a location request. Check VolunteerLink's location permission and retry."))
        }
        if (!finished) handler.postDelayed({
            val candidate = best
            if (candidate != null && usable(candidate)) deliver(candidate, bestIsCached)
            else finish(VolunteerMapLocationResult(null,
                if (sawRejectedFix) "Only old or low-accuracy locations were received. No position was used. Please retry with precise location enabled."
                else "No fresh location was received within 20 seconds. Check VolunteerLink's precise location permission and retry."))
        }, 20_000L)
        return {
            finished = true
            cleanup()
        }
    }
}
