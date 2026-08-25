package com.example.volunteerlink.data.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure Haversine distance calculation, reusable and JVM-testable. */
object VolunteerDistanceCalculator {
    fun kilometres(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Double {
        require(fromLatitude in -90.0..90.0)
        require(toLatitude in -90.0..90.0)
        require(fromLongitude in -180.0..180.0)
        require(toLongitude in -180.0..180.0)

        val latitudeDelta = Math.toRadians(toLatitude - fromLatitude)
        val longitudeDelta = Math.toRadians(toLongitude - fromLongitude)
        val fromLatitudeRadians = Math.toRadians(fromLatitude)
        val toLatitudeRadians = Math.toRadians(toLatitude)

        val haversine =
            sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
                cos(fromLatitudeRadians) * cos(toLatitudeRadians) *
                sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)

        val angularDistance = 2.0 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
        val rawKilometres = EARTH_RADIUS_KM * angularDistance
        return round(rawKilometres * 10.0) / 10.0
    }

    private const val EARTH_RADIUS_KM = 6_371.0088
}
