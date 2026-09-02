package com.example.volunteerlink.data.location

import com.example.volunteerlink.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import java.util.Locale

/** Handles Geoapify location autocomplete requests. */
class GeoapifyLocationService {

    private val client = HttpClient(Android) {
        expectSuccess = true
    }

    /**
     * Searches worldwide and returns at most five suggestions.
     * Approximate device coordinates only bias ranking; they do not restrict
     * the search to a country.
     */
    suspend fun searchLocations(
        query: String,
        biasLatitude: Double? = null,
        biasLongitude: Double? = null
    ): List<LocationSuggestion> {
        return search(
            query = query,
            type = null,
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )
    }


    /**
     * Searches for an exact venue/address and removes broad administrative
     * results such as whole cities or states.
     */
    suspend fun searchVenues(
        query: String,
        biasLatitude: Double? = null,
        biasLongitude: Double? = null
    ): List<LocationSuggestion> {
        val broadTypes = setOf(
            "suburb",
            "district",
            "postcode",
            "city",
            "county",
            "state",
            "country"
        )

        return search(
            query = query,
            type = null,
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )
            .filter { suggestion ->
                suggestion.resultType == null || suggestion.resultType !in broadTypes
            }
            .take(5)
    }

    /**
     * Searches only broad localities for Impact Weave preferred-area input.
     * This prevents buildings, shops and exact venues from being selected as
     * the activity's general area.
     */
    suspend fun searchAreas(
        query: String,
        biasLatitude: Double? = null,
        biasLongitude: Double? = null
    ): List<LocationSuggestion> {
        val broadTypes = setOf(
            "suburb",
            "district",
            "city",
            "county",
            "state",
            "country"
        )

        return search(
            query = query,
            type = "locality",
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )
            .filter { suggestion ->
                suggestion.resultType == null || suggestion.resultType in broadTypes
            }
            .map { it.asGeneralArea() }
            .distinctBy { it.generalAreaName.lowercase() }
            .take(5)
    }

    private suspend fun search(
        query: String,
        type: String?,
        biasLatitude: Double?,
        biasLongitude: Double?
    ): List<LocationSuggestion> {
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank() || BuildConfig.GEOAPIFY_API_KEY.isBlank()) {
            return emptyList()
        }

        val bias =
            if (biasLatitude != null && biasLongitude != null) {
                // Geoapify proximity order is longitude,latitude.
                "proximity:$biasLongitude,$biasLatitude"
            } else {
                // Keep autocomplete global when device location is unavailable.
                "countrycode:none"
            }

        val language = Locale.getDefault()
            .language
            .takeIf { it.length == 2 }
            ?: "en"

        val response = client.get(
            "https://api.geoapify.com/v1/geocode/autocomplete"
        ) {
            parameter("text", cleanQuery)
            parameter("format", "json")
            parameter("limit", 5)
            parameter("lang", language)
            type?.let { parameter("type", it) }
            parameter("bias", bias)
            parameter("apiKey", BuildConfig.GEOAPIFY_API_KEY)
        }

        return parseLocations(response.bodyAsText())
    }

    private fun parseLocations(json: String): List<LocationSuggestion> {
        val results = JSONObject(json).optJSONArray("results")
            ?: return emptyList()

        return buildList {
            for (index in 0 until results.length()) {
                val item = results.getJSONObject(index)

                val latitude = item.optDouble("lat", Double.NaN)
                val longitude = item.optDouble("lon", Double.NaN)

                if (latitude.isNaN() || longitude.isNaN()) {
                    continue
                }

                val address = item.optString("formatted").ifBlank {
                    listOf(
                        item.optString("address_line1"),
                        item.optString("address_line2")
                    )
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                }

                val name = item.optString("name")
                    .ifBlank { item.optString("address_line1") }
                    .ifBlank { address }

                if (name.isBlank() && address.isBlank()) {
                    continue
                }

                val placeId = item.optString("place_id")
                    .ifBlank { "$latitude,$longitude" }

                add(
                    LocationSuggestion(
                        placeId = placeId,
                        name = name,
                        address = address,
                        city = item.optString("city")
                            .takeIf { it.isNotBlank() },
                        state = item.optString("state")
                            .takeIf { it.isNotBlank() },
                        country = item.optString("country")
                            .takeIf { it.isNotBlank() },
                        latitude = latitude,
                        longitude = longitude,
                        suburb = item.optString("suburb")
                            .takeIf { it.isNotBlank() },
                        district = item.optString("district")
                            .takeIf { it.isNotBlank() },
                        county = item.optString("county")
                            .takeIf { it.isNotBlank() },
                        resultType = item.optString("result_type")
                            .takeIf { it.isNotBlank() }
                    )
                )
            }
        }
            .distinctBy { it.placeId }
            .take(5)
    }
}
