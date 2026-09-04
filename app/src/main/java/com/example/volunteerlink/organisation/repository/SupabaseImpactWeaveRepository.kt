package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements Impact Weave persistence through authenticated Supabase RPCs.
//
// The database owns plan status transitions such as MATCHING, WAITING, PARTIAL, READY, DISPOSED and CONVERTED so
// two organisations cannot create contradictory partnership state locally.
//
// The repository converts Compose/ViewModel models to RPC parameters and converts returned JSON back into Impact
// Weave domain models.
//
// Create Post prefill is read from the confirmed Impact Weave plan instead of asking the organisation to re-enter
// agreed activity information.
//
// Conversion completion links accepted partner organisations/support to the real Volunteer Post and marks the plan
// as historical in one server-controlled workflow.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.auth.OrganisationSession
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDatabaseNeed
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchingInput
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPrefill
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePostPartner
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveSupportCandidate
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Serializable
/**
 * Holds the values represented by active impact weave plan row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ActiveImpactWeavePlanRow
 *
 * Domain/UI type for Active Impact Weave Plan Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ActiveImpactWeavePlanRow(
    @SerialName("draft_id") val draftId: String,
    val category: String? = null,
    val title: String,
    val description: String? = null,
    val mode: String,
    @SerialName("has_existing_venue") val hasExistingVenue: Boolean,
    @SerialName("area_name") val areaName: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val status: String,
    @SerialName("needs_count") val needsCount: Int
)

@Serializable
/**
 * Holds the values represented by matching input response as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — MatchingInputResponse
 *
 * Domain/UI type for Matching Input Response used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class MatchingInputResponse(
    val needs: List<MatchingNeedRow> = emptyList(),
    val candidates: List<MatchingCandidateRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by matching need row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — MatchingNeedRow
 *
 * Domain/UI type for Matching Need Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class MatchingNeedRow(
    @SerialName("need_id") val needId: String,
    @SerialName("original_text") val originalText: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("resource_name") val resourceName: String,
    @SerialName("quantity_required") val quantityRequired: Int? = null,
    @SerialName("capacity_required") val capacityRequired: Int? = null,
    @SerialName("confirmed_quantity") val confirmedQuantity: Int = 0,
    @SerialName("is_fulfilled") val isFulfilled: Boolean = false
)

@Serializable
/**
 * Holds the values represented by matching candidate row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — MatchingCandidateRow
 *
 * Domain/UI type for Matching Candidate Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class MatchingCandidateRow(
    // Some candidate responses can include need_id, but matching is performed from the
    // support record itself. Keeping the field optional lets the decoder accept either
    // response shape without using need_id as the compatibility decision.
    @SerialName("need_id") val legacyNeedId: String? = null,
    @SerialName("support_id") val supportId: String,
    @SerialName("organisation_id") val organisationId: String,
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("support_description") val supportDescription: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("resource_name") val resourceName: String,
    val quantity: Int? = null,
    val capacity: Int? = null,
    @SerialName("location_name") val locationName: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("open_to_partnership") val openToPartnership: Boolean = false,
    @SerialName("verification_status") val verificationStatus: String = ""
)

@Serializable
/**
 * Holds the values represented by impact weave post prefill row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePostPrefillRow
 *
 * Domain/UI type for Impact Weave Post Prefill Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePostPrefillRow(
    @SerialName("draft_id") val draftId: String,
    val category: String,
    val title: String,
    val description: String,
    val mode: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("location_name") val locationName: String,
    @SerialName("location_address") val locationAddress: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val partners: List<ImpactWeavePostPartnerRow> = emptyList()
)

@Serializable
/**
 * Holds the values represented by impact weave post partner row as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — ImpactWeavePostPartnerRow
 *
 * Domain/UI type for Impact Weave Post Partner Row used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class ImpactWeavePostPartnerRow(
    @SerialName("organisation_name") val organisationName: String,
    @SerialName("contribution_summary") val contributionSummary: String
)

/**
 * Holds the values represented by prepared impact weave plan as one strongly typed model.
 * It keeps backend-facing work behind the Impact Weave and partnership repository boundary.
 */
/**
 * DETAILED DECLARATION — PreparedImpactWeavePlan
 *
 * Domain/UI type for Prepared Impact Weave Plan used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
private data class PreparedImpactWeavePlan(
    val mode: ImpactWeaveMode,
    val startDate: Long,
    val endDate: Long,
    val startTime: Int,
    val endTime: Int,
    val hasExistingVenue: Boolean,
    val area: LocationSuggestion,
    val venue: LocationSuggestion?,
    val needOriginalTexts: List<String>,
    val needSupportTypes: List<String>,
    val needResourceNames: List<String>,
    val needAmounts: List<Int?>
)

/** Supabase implementation for Impact Weave Find Partners. */
/**
 * DETAILED DECLARATION — SupabaseImpactWeaveRepository
 *
 * Data-access implementation/contract for Supabase Impact Weave Repository, isolating backend details from the
 * screen and ViewModel layers.
 *
 * Protected server state still relies on authenticated Supabase authorization and database rules rather than
 * trusting client-side checks alone.
 *
 * This implementation translates VolunteerLink models to PostgREST/RPC/Storage operations and maps backend
 * responses back into domain models.
 */
class SupabaseImpactWeaveRepository : ImpactWeaveRepository {

    /**
     * Loads the active plans needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadActivePlans
     *
     * Performs the repository/data-layer operation for load active plans.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_list_active_impact_weave_plans`: Returns the authenticated organisation's
     * active and historical Impact Weave plans for the planning/history UI.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    override suspend fun loadActivePlans(): List<ImpactWeaveActivePlan> {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_list_active_impact_weave_plans
        // Returns the authenticated organisation's active and historical Impact Weave plans for the
        // planning/history UI.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_list_active_impact_weave_plans"
        )

        val rows = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<ActiveImpactWeavePlanRow>>(response.data)

        return rows.map { row ->
            val area = LocationSuggestion(
                placeId = "impact-weave-area:${row.draftId}",
                name = row.areaName,
                address = row.areaName,
                city = row.areaName,
                state = null,
                country = null,
                latitude = row.latitude,
                longitude = row.longitude,
                resultType = "impact_weave_area"
            )
            ImpactWeaveActivePlan(
                draftId = row.draftId,
                category = row.category?.let { value ->
                    runCatching { VolunteerPostCategory.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
                },
                title = row.title,
                description = row.description.orEmpty(),
                mode = ImpactWeaveMode.valueOf(row.mode.uppercase(Locale.ROOT)),
                startDateMillis = parseSqlDate(row.startDate),
                endDateMillis = parseSqlDate(row.endDate),
                startTimeMinutes = parseSqlTime(row.startTime),
                endTimeMinutes = parseSqlTime(row.endTime),
                hasExistingVenue = row.hasExistingVenue,
                areaName = row.areaName,
                areaLocation = area,
                status = row.status,
                needsCount = row.needsCount
            )
        }
    }


    /**
     * Starts the matching for the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — startMatching
     *
     * Performs the repository/data-layer operation for start matching.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_start_impact_weave_matching`: Persists the reviewed Impact Weave
     * activity/support requirements and enters server-backed partner matching.
     */
    override suspend fun startMatching(
        draft: ImpactWeaveDraft
    ): StartedImpactWeaveMatchingResult {
        val prepared = preparePlan(draft)
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_start_impact_weave_matching
        // Persists the reviewed Impact Weave activity/support requirements and enters server-backed partner
        // matching.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_start_impact_weave_matching",
            parameters = buildPlanParameters(draft, prepared)
        )

        return StartedImpactWeaveMatchingResult(
            draftId = requireDraftId(
                data = response.data,
                errorMessage = "Impact Weave matching started but no plan ID was returned."
            )
        )
    }

    /**
     * Loads the matching input needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadMatchingInput
     *
     * Performs the repository/data-layer operation for load matching input.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_get_impact_weave_matching_input`: Returns normalized plan needs plus eligible
     * provider support records used to build current partner recommendations.
     */
    override suspend fun loadMatchingInput(draftId: String): ImpactWeaveMatchingInput {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_get_impact_weave_matching_input
        // Returns normalized plan needs plus eligible provider support records used to build current partner
        // recommendations.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_get_impact_weave_matching_input",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
            }
        )

        val decoded = Json { ignoreUnknownKeys = true }
            .decodeFromString<MatchingInputResponse>(response.data)

        return ImpactWeaveMatchingInput(
            needs = decoded.needs.map { row ->
                ImpactWeaveDatabaseNeed(
                    needId = row.needId,
                    originalText = row.originalText,
                    supportType = row.supportType,
                    resourceName = row.resourceName,
                    quantityRequired = row.quantityRequired,
                    capacityRequired = row.capacityRequired,
                    confirmedQuantity = row.confirmedQuantity,
                    isFulfilled = row.isFulfilled
                )
            },
            candidates = decoded.candidates
                .filter { row ->
                    row.openToPartnership &&
                        row.verificationStatus.equals("VERIFIED", ignoreCase = true)
                }
                .map { row ->
                    ImpactWeaveSupportCandidate(
                        supportId = row.supportId,
                        organisationId = row.organisationId,
                        organisationName = row.organisationName,
                        supportDescription = row.supportDescription,
                        supportType = row.supportType,
                        resourceName = row.resourceName,
                        quantity = row.quantity,
                        capacity = row.capacity,
                        locationName = row.locationName,
                        country = row.country,
                        latitude = row.latitude,
                        longitude = row.longitude
                    )
                }
        )
    }

    /**
     * Updates the basic details used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — updateBasicDetails
     *
     * Performs the repository/data-layer operation for update basic details.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_update_impact_weave_basic`: Updates fields that are still editable for an
     * active Impact Weave plan while the database enforces lifecycle restrictions.
     */
    override suspend fun updateBasicDetails(
        draftId: String,
        title: String,
        category: String,
        description: String
    ) {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_update_impact_weave_basic
        // Updates fields that are still editable for an active Impact Weave plan while the database enforces
        // lifecycle restrictions.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_update_impact_weave_basic",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
                put("p_title", title.trim())
                put("p_category", category)
                put("p_description", description.trim())
            }
        )
    }

    /**
     * Derives the reschedule value used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — reschedule
     *
     * Performs the repository/data-layer operation for reschedule.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_reschedule_impact_weave`: Moves an Impact Weave activity schedule through the
     * server workflow so invitation/reconfirmation state can be adjusted consistently.
     */
    override suspend fun reschedule(
        draftId: String,
        startDateMillis: Long,
        endDateMillis: Long,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    ) {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_reschedule_impact_weave
        // Moves an Impact Weave activity schedule through the server workflow so invitation/reconfirmation
        // state can be adjusted consistently.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_reschedule_impact_weave",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
                put("p_start_date", formatDate(startDateMillis))
                put("p_end_date", formatDate(endDateMillis))
                put("p_start_time", formatTime(startTimeMinutes))
                put("p_end_time", formatTime(endTimeMinutes))
            }
        )
    }

    /**
     * Derives the dispose value used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — dispose
     *
     * Performs the repository/data-layer operation for dispose.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_dispose_impact_weave`: Marks an active Impact Weave plan as disposed and
     * closes the associated unresolved partnership work while preserving history.
     */
    override suspend fun dispose(draftId: String) {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_dispose_impact_weave
        // Marks an active Impact Weave plan as disposed and closes the associated unresolved partnership work
        // while preserving history.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_dispose_impact_weave",
            parameters = buildJsonObject { put("p_draft_id", draftId) }
        )
    }

    /**
     * Loads the post prefill needed by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPostPrefill
     *
     * Performs the repository/data-layer operation for load post prefill.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_get_impact_weave_post_prefill`: Returns the confirmed Impact Weave activity
     * details that must prefill Create Post, including the resolved Physical venue where applicable.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    override suspend fun loadPostPrefill(draftId: String): ImpactWeavePostPrefill {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_get_impact_weave_post_prefill
        // Returns the confirmed Impact Weave activity details that must prefill Create Post, including the
        // resolved Physical venue where applicable.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        val response = supabase.postgrest.rpc(
            function = "organisation_get_impact_weave_post_prefill",
            parameters = buildJsonObject { put("p_draft_id", draftId) }
        )
        val row = Json { ignoreUnknownKeys = true }
            .decodeFromString<ImpactWeavePostPrefillRow>(response.data)
        return ImpactWeavePostPrefill(
            draftId = row.draftId,
            category = VolunteerPostCategory.valueOf(row.category.uppercase(Locale.ROOT)),
            title = row.title,
            description = row.description,
            mode = ImpactWeaveMode.valueOf(row.mode.uppercase(Locale.ROOT)),
            startDateMillis = parseSqlDate(row.startDate),
            endDateMillis = parseSqlDate(row.endDate),
            startTimeMinutes = parseSqlTime(row.startTime),
            endTimeMinutes = parseSqlTime(row.endTime),
            location = LocationSuggestion(
                placeId = "impact-weave-post:${row.draftId}",
                name = row.locationName,
                address = row.locationAddress,
                city = row.locationName,
                state = null,
                country = row.country,
                latitude = row.latitude,
                longitude = row.longitude,
                resultType = "impact_weave_location"
            ),
            partners = row.partners.map {
                ImpactWeavePostPartner(it.organisationName, it.contributionSummary)
            }
        )
    }

    /**
     * Confirms the conversion in the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — completeConversion
     *
     * Performs the repository/data-layer operation for complete conversion.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Supabase RPC `organisation_complete_impact_weave_conversion`: Links a successfully-created Volunteer Post
     * to the Impact Weave plan, carries accepted partners into post_partners and marks the plan CONVERTED.
     */
    override suspend fun completeConversion(draftId: String, postId: String) {
        // ------------------------------------------------------------------------
        // SUPABASE RPC: organisation_complete_impact_weave_conversion
        // Links a successfully-created Volunteer Post to the Impact Weave plan, carries accepted partners into
        // post_partners and marks the plan CONVERTED.
        // The client sends parameters and waits for the database result; ownership, lifecycle and multi-row
        // consistency checks belong on the server for this operation.
        // ------------------------------------------------------------------------
        supabase.postgrest.rpc(
            function = "organisation_complete_impact_weave_conversion",
            parameters = buildJsonObject {
                put("p_draft_id", draftId)
                put("p_post_id", postId)
            }
        )
    }

    /**
     * Prepares the plan for the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — preparePlan
     *
     * Performs the repository/data-layer operation for prepare plan.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Uses OrganisationSession so the client operation is associated with the signed-in organisation; server
     * RLS/RPC ownership checks still make the final authorization decision.
     */
    private suspend fun preparePlan(draft: ImpactWeaveDraft): PreparedImpactWeavePlan {
        val organisation = OrganisationSession.requireContext()
        require(organisation.isVerified) {
            "Your organisation must be verified before using Impact Weave."
        }

        require(draft.category != null) { "Select an activity category." }
        require(draft.description.isNotBlank()) { "Enter an activity description." }

        val mode = draft.mode ?: error("Choose Physical or Hybrid first.")
        val startDate = draft.startDateMillis ?: error("Choose the activity start date.")
        val endDate = draft.endDateMillis ?: error("Choose the activity end date.")
        val startTime = draft.startTimeMinutes ?: error("Choose the activity start time.")
        val endTime = draft.endTimeMinutes ?: error("Choose the activity end time.")
        val hasExistingVenue = draft.hasExistingVenue
            ?: error("Choose whether the activity already has a venue.")
        val area = draft.areaLocation ?: error("Select the activity area first.")

        val needOriginalTexts = mutableListOf<String>()
        val needSupportTypes = mutableListOf<String>()
        val needResourceNames = mutableListOf<String>()
        val needAmounts = mutableListOf<Int?>()

        draft.needs.forEach { need ->
            val supportType = need.supportType.trim().uppercase(Locale.ROOT)
            val amount = need.amount

            if (supportType == "VENUE") {
                require(amount == null || amount > 0) {
                    "Venue capacity must be greater than zero when provided."
                }
            } else {
                require(amount != null && amount > 0) {
                    "$supportType requires a positive quantity for ${need.resourceName}."
                }
            }

            needOriginalTexts += need.originalText.trim()
            needSupportTypes += supportType
            needResourceNames += need.resourceName.trim()
            needAmounts += amount
        }

        val venue = if (hasExistingVenue) {
            draft.existingVenueLocation ?: error("Select the existing venue first.")
        } else {
            null
        }

        return PreparedImpactWeavePlan(
            mode = mode,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            hasExistingVenue = hasExistingVenue,
            area = area,
            venue = venue,
            needOriginalTexts = needOriginalTexts,
            needSupportTypes = needSupportTypes,
            needResourceNames = needResourceNames,
            needAmounts = needAmounts
        )
    }

    /**
     * Builds the plan parameters used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — buildPlanParameters
     *
     * Performs the repository/data-layer operation for build plan parameters.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    private fun buildPlanParameters(
        draft: ImpactWeaveDraft,
        prepared: PreparedImpactWeavePlan
    ): JsonObject = buildJsonObject {
        put("p_category", draft.category?.databaseValue ?: error("Select an activity category."))
        put("p_title", draft.title.trim())
        put("p_description", draft.description.trim())
        put("p_mode", prepared.mode.name)
        put("p_has_existing_venue", prepared.hasExistingVenue)
        put(
            "p_area_name",
            draft.areaQuery.trim().ifBlank { prepared.area.generalAreaName }
        )
        prepared.area.state?.takeIf { it.isNotBlank() }
            ?.let { put("p_state_region", it) }
            ?: put("p_state_region", JsonNull)
        put(
            "p_country",
            prepared.area.country?.takeIf { it.isNotBlank() }
                ?: error("The selected activity area has no country.")
        )
        put("p_latitude", prepared.area.latitude)
        put("p_longitude", prepared.area.longitude)

        val venue = prepared.venue
        if (venue != null) {
            put("p_existing_venue_name", venue.displayName)
            venue.address.takeIf { it.isNotBlank() }
                ?.let { put("p_existing_venue_address", it) }
                ?: put("p_existing_venue_address", JsonNull)
            put("p_existing_venue_latitude", venue.latitude)
            put("p_existing_venue_longitude", venue.longitude)
        } else {
            put("p_existing_venue_name", JsonNull)
            put("p_existing_venue_address", JsonNull)
            put("p_existing_venue_latitude", JsonNull)
            put("p_existing_venue_longitude", JsonNull)
        }

        put("p_start_date", formatDate(prepared.startDate))
        put("p_end_date", formatDate(prepared.endDate))
        put("p_start_time", formatTime(prepared.startTime))
        put("p_end_time", formatTime(prepared.endTime))

        put("p_need_original_texts", buildJsonArray {
            prepared.needOriginalTexts.forEach { add(JsonPrimitive(it)) }
        })
        put("p_need_support_types", buildJsonArray {
            prepared.needSupportTypes.forEach { add(JsonPrimitive(it)) }
        })
        put("p_need_resource_names", buildJsonArray {
            prepared.needResourceNames.forEach { add(JsonPrimitive(it)) }
        })
        put("p_need_amounts", buildJsonArray {
            prepared.needAmounts.forEach { amount ->
                if (amount == null) add(JsonNull) else add(JsonPrimitive(amount))
            }
        })
    }

    /**
     * Returns the require draft id value required by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — requireDraftId
     *
     * Performs the repository/data-layer operation for require draft id.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun requireDraftId(data: String, errorMessage: String): String {
        val result = Json.parseToJsonElement(data).jsonObject
        return result["draft_id"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: error(errorMessage)
    }

    /**
     * Parses the sql date used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — parseSqlDate
     *
     * Performs the repository/data-layer operation for parse sql date.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun parseSqlDate(value: String): Long =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }.parse(value)?.time ?: error("Invalid Impact Weave date: $value")

    /**
     * Parses the sql time used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — parseSqlTime
     *
     * Performs the repository/data-layer operation for parse sql time.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun parseSqlTime(value: String): Int {
        val parts = value.take(5).split(":")
        if (parts.size != 2) error("Invalid Impact Weave time: $value")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * Formats the date used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — formatDate
     *
     * Performs the repository/data-layer operation for format date.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun formatDate(timeMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeMillis))

    /**
     * Formats the time used by the organisation Impact Weave and partnership flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — formatTime
     *
     * Performs the repository/data-layer operation for format time.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    private fun formatTime(totalMinutes: Int): String = "%02d:%02d:00".format(
        Locale.US,
        totalMinutes / 60,
        totalMinutes % 60
    )
}
