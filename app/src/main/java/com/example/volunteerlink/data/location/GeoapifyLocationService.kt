package com.example.volunteerlink.data.location

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Wraps Geoapify autocomplete/search for Create Post and other location-entry workflows.
//
// It turns a free-text query into structured LocationSuggestion objects containing display text, locality/address
// metadata and coordinates.
//
// A device location can be supplied as a ranking bias so nearby results appear earlier, but Create Post search is
// intentionally broad and is not restricted to venues or the current city.
//
// Network and API response parsing stay in this data-layer service so Compose only renders suggestions and
// selection state.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import com.example.volunteerlink.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
import java.util.Locale

/** Handles Geoapify location autocomplete requests. */
/**
 * DETAILED DECLARATION — GeoapifyLocationService
 *
 * Domain/UI type for Geoapify Location Service used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
class GeoapifyLocationService {

    private val client = HttpClient(Android) {
        expectSuccess = true
    }

    /**
     * Searches worldwide and returns at most five suggestions.
     * Approximate device coordinates only bias ranking; they do not restrict
     * the search to a country.
     */
    /**
     * DETAILED BEHAVIOUR — searchLocations
     *
     * Implements the current VolunteerLink responsibility for search locations in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
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
     * Searches for named places that an organisation can provide as a venue.
     * This is intentionally stricter than an event-location search: broad
     * administrative areas and plain street results are not returned.
     */
    /**
     * DETAILED BEHAVIOUR — searchVenues
     *
     * Implements the current VolunteerLink responsibility for search venues in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    suspend fun searchVenues(
        query: String,
        biasLatitude: Double? = null,
        biasLongitude: Double? = null
    ): List<LocationSuggestion> {
        val amenityResults = search(
            query = query,
            type = "amenity",
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )

        val exactPlaceResults = search(
            query = query,
            type = null,
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )
            .filter { suggestion ->
                suggestion.resultType == "amenity" ||
                    suggestion.resultType == "building"
            }

        return (amenityResults + exactPlaceResults)
            .distinctBy { it.placeId }
            .take(5)
    }

    /**
     * Searches for a real place where an activity can happen. Unlike the
     * partnership venue search, this also allows outdoor POIs and exact
     * addresses/streets while still rejecting city/state/country-only results.
     * Examples include halls, parks, beaches, campuses, fields and stadiums.
     */
    /**
     * DETAILED BEHAVIOUR — searchEventLocations
     *
     * Implements the current VolunteerLink responsibility for search event locations in this support/model
     * layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    suspend fun searchEventLocations(
        query: String,
        biasLatitude: Double? = null,
        biasLongitude: Double? = null
    ): List<LocationSuggestion> {
        val amenityResults = search(
            query = query,
            type = "amenity",
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )

        val eventResultTypes = setOf(
            "amenity",
            "building",
            "street",
            "unknown"
        )

        val exactLocationResults = search(
            query = query,
            type = null,
            biasLatitude = biasLatitude,
            biasLongitude = biasLongitude
        )
            .filter { suggestion ->
                suggestion.resultType == null || suggestion.resultType in eventResultTypes
            }

        return (amenityResults + exactLocationResults)
            .distinctBy { it.placeId }
            .take(5)
    }

    /**
     * Searches only broad localities for Impact Weave preferred-area input.
     * This prevents buildings, shops and exact venues from being selected as
     * the activity's general area.
     */
    /**
     * DETAILED BEHAVIOUR — searchAreas
     *
     * Implements the current VolunteerLink responsibility for search areas in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
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

    /**
     * DETAILED BEHAVIOUR — search
     *
     * Implements the current VolunteerLink responsibility for search in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
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

    /**
     * DETAILED BEHAVIOUR — parseLocations
     *
     * Implements the current VolunteerLink responsibility for parse locations in this support/model layer.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
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
