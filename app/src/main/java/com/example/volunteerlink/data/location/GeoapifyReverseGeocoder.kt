package com.example.volunteerlink.data.location

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

// Stores the country, state or region, and city matched from the GPS location.
data class GeoapifyLocationMatch(
    val country: String,
    val stateRegion: String,
    val locationName: String
)

object GeoapifyReverseGeocoder {

    suspend fun matchToKnownLocation(
        apiKey: String,
        latitude: Double,
        longitude: Double,
        countryStates: Map<String, Map<String, List<String>>>
    ): GeoapifyLocationMatch? = withContext(Dispatchers.IO) {
        try {
            // Builds the Geoapify reverse-geocoding API URL using the GPS coordinates.
            val url = URL(
                "https://api.geoapify.com/v1/geocode/reverse" +
                        "?lat=$latitude&lon=$longitude&apiKey=${Uri.encode(apiKey)}"
            )

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            // Stores the API response body after a successful request.
            val body = try {
                if (connection.responseCode !in 200..299) return@withContext null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            // Converts the JSON response into a Geoapify response object.
            val parsed = Json { ignoreUnknownKeys = true }
                .decodeFromString<GeoapifyReverseResponse>(body)

            // Retrieves the first location feature from the API response.
            val properties = parsed.features.firstOrNull()?.properties
                ?: return@withContext null

            matchProperties(properties, countryStates)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun matchProperties(
        properties: GeoapifyProperties,
        countryStates: Map<String, Map<String, List<String>>>
    ): GeoapifyLocationMatch? {
        // Collects possible city or area names returned by Geoapify.
        val candidateCities = listOfNotNull(
            properties.city, properties.county, properties.suburb, properties.district
        )

        countryStates.forEach { (countryName, states) ->
            if (!countryName.equals(properties.country, ignoreCase = true)) return@forEach

            states.forEach { (stateName, locations) ->
                val stateMatches = properties.state?.let {
                    it.equals(stateName, ignoreCase = true) || it.contains(stateName, ignoreCase = true)
                } == true

                if (stateMatches) {
                    val matchedLocation = locations.firstOrNull { location ->
                        candidateCities.any { candidate -> candidate.equals(location, ignoreCase = true) }
                    }
                    if (matchedLocation != null) {
                        return GeoapifyLocationMatch(countryName, stateName, matchedLocation)
                    }
                }
            }
        }
        return null
    }
}

// Stores the list of reverse-geocoding features returned by Geoapify
@Serializable
private data class GeoapifyReverseResponse(val features: List<GeoapifyFeature> = emptyList())

// Stores the location properties returned for a Geoapify result.
@Serializable
private data class GeoapifyFeature(val properties: GeoapifyProperties)

// Stores the country, state and possible city names returned by Geoapify.
@Serializable
private data class GeoapifyProperties(
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val county: String? = null,
    val suburb: String? = null,
    val district: String? = null
)