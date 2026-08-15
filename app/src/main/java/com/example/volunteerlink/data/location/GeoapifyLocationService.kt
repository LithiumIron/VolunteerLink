package com.example.volunteerlink.data.location

import com.example.volunteerlink.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

class GeoapifyLocationService {
    private val client = HttpClient(Android)

    suspend fun searchLocations(
        query: String
    ): List<LocationSuggestion> {

        if (query.isBlank()) {
            return emptyList()
        }

        return try {

            val response = client.get(
                "https://api.geoapify.com/v1/geocode/autocomplete"
            ) {
                parameter("text", query)
                parameter("format", "json")
                parameter("limit", 5)
                parameter("bias", "countrycode:none")
                parameter(
                    "apiKey",
                    BuildConfig.GEOAPIFY_API_KEY
                )
            }

            parseLocations(response.bodyAsText())

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseLocations(
        json: String
    ): List<LocationSuggestion> {

        val root = JSONObject(json)

        val results =
            root.optJSONArray("results")
                ?: return emptyList()

        return buildList {

            for (index in 0 until results.length()) {

                val item =
                    results.getJSONObject(index)

                val latitude =
                    item.optDouble("lat", Double.NaN)

                val longitude =
                    item.optDouble("lon", Double.NaN)

                if (
                    latitude.isNaN() ||
                    longitude.isNaN()
                ) {
                    continue
                }

                add(
                    LocationSuggestion(
                        displayName =
                            item.optString("formatted"),

                        city =
                            item.optString("city")
                                .takeIf { it.isNotBlank() },

                        state =
                            item.optString("state")
                                .takeIf { it.isNotBlank() },

                        country =
                            item.optString("country")
                                .takeIf { it.isNotBlank() },

                        latitude = latitude,
                        longitude = longitude
                    )
                )
            }
        }
    }
}