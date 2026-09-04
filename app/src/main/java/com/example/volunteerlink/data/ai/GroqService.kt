package com.example.volunteerlink.data.ai

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Contains the semantic-analysis service used internally by Impact Weave to rank compatibility between real
// organisation support records and requested needs.
//
// The service receives only candidate/support data already returned by VolunteerLink's eligible-partner backend
// and produces a compatibility classification; it does not create organisations, quantities, capacities or
// partnership records.
//
// Factual eligibility and persisted partnership state remain controlled by Supabase. The AI result is a
// ranking/interpretation layer, not the source of database truth.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import com.example.volunteerlink.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject

/**
 * DETAILED DECLARATION — OrganisationSupportAnalysis
 *
 * Domain/UI type for Organisation Support Analysis used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class OrganisationSupportAnalysis(
    val isValid: Boolean,
    val supportType: String?,
    val resourceName: String,
    val quantity: Int?,
    val capacity: Int?,
    val reason: String?
)


/**
 * DETAILED DECLARATION — GroqImpactWeaveNeed
 *
 * Domain/UI type for Groq Impact Weave Need used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class GroqImpactWeaveNeed(
    val needId: String,
    val supportType: String,
    val resourceName: String,
    val originalText: String
)

/**
 * DETAILED DECLARATION — GroqImpactWeaveCandidate
 *
 * Domain/UI type for Groq Impact Weave Candidate used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class GroqImpactWeaveCandidate(
    val supportId: String,
    val supportType: String,
    val resourceName: String,
    val supportDescription: String
)

/**
 * DETAILED DECLARATION — ImpactWeaveSemanticMatch
 *
 * Domain/UI type for Impact Weave Semantic Match used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class ImpactWeaveSemanticMatch(
    val needId: String,
    val supportId: String,
    val level: String
)

/**
 * DETAILED DECLARATION — GroqService
 *
 * Domain/UI type for Groq Service used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
class GroqService {

    private val client = HttpClient(Android) {
        expectSuccess = true
    }

    /**
     * DETAILED BEHAVIOUR — analyseOrganisationSupport
     *
     * Implements the current VolunteerLink responsibility for analyse organisation support in this
     * support/model layer.
     */
    suspend fun analyseOrganisationSupport(text: String): OrganisationSupportAnalysis {
        val systemPrompt = """
            You classify ONE physical partnership resource that an organisation CAN PROVIDE for VolunteerLink Impact Weave.

            Allowed support types are exactly:
            VENUE, EQUIPMENT, FURNITURE, TRANSPORT, SUPPLIES, REFRESHMENTS, SPECIALIST.

            Rules:
            - The user must describe only one kind of support at a time.
            - VENUE always uses quantity = null. Capacity is optional: extract it only when the user gives a positive whole-number capacity. Otherwise return capacity = null.
            - Never estimate, assume, or invent a VENUE capacity.
            - Every other allowed type uses quantity and capacity must be null.
            - REFRESHMENTS means edible food or drink for the activity, including meal packs, packed meals, food packs, snacks, bottled/drinking/mineral water, and beverages. A Meal Pack is REFRESHMENTS, not SUPPLIES.
            - SUPPLIES means non-food consumable or distributable materials, for example stationery packs, hygiene kits, printed materials, cleaning supplies, or craft materials.
            - For non-VENUE support, quantity must be a positive whole number stated or clearly implied by the text.
            - SPECIALIST means a person provided because of specific professional, technical, academic, certified, or specialist expertise.
              Valid examples include lecturers, teachers, doctors, nurses, first aid officers, trainers, technicians, engineers, interpreters, counsellors, photographers, and legal advisers.
            - A stated number of specialists is valid, for example "2 English lecturers" -> SPECIALIST, resource_name "English lecturer", quantity 2.
            - Reject only GENERAL volunteer manpower or ordinary helpers without a distinct expertise, for example "10 volunteers", "5 helpers", "3 event crew", or "4 registration volunteers".
            - Also reject event promotion/social-media advertising, money/funding, vague promises, remote-only digital promotion, or anything outside the seven support types.
            - If multiple different resources are included, reject it and ask the user to add one support item at a time.
            - Keep resource_name short and singular where natural, for example "Passenger van", "Folding chair", "PA speaker", or "First aid officer".
            - Preserve the specificity actually stated by the user. Do NOT invent packaging, form, subtype, material, or expertise that was not stated.
              Examples: "drinking water" -> resource_name "Drinking water", NOT "Bottled water"; "food" -> "Food", NOT "Meal pack"; "seating" -> "Seating", NOT "Folding chair".
              A more specific resource name is allowed only when the text actually states it, for example "bottles of water" -> "Bottled water" and "meal packs" -> "Meal pack".
            - For VENUE, resource_name must describe the GENERAL TYPE of place, not its specific proper name, organisation name, campus name, branch name, or building name.
            - Do not limit VENUE to a fixed list. Infer the most useful broad venue type from the description, usually in 1 to 3 words.
              Examples of possible venue types include Hall, Community Hall, Multipurpose Hall, Conference Hall, Banquet Hall, Ballroom, Auditorium, Lecture Hall, Meeting Room, Function Room, Classroom, Training Room, Workshop Space, Exhibition Hall, Event Space, Sports Hall, Stadium, Court, Field, Pavilion, Park, Garden, Outdoor Space, Library, or similar physical places. These are examples only, not an exhaustive list.
            - Strip proper names from a VENUE resource_name. Examples:
              "TAR UMT Main Hall for 300 people" -> VENUE, resource_name "Hall", capacity 300.
              "Dewan Sivik MBPJ for 200 people" -> VENUE, resource_name "Hall", capacity 200.
              "ABC Hotel Grand Ballroom for 500 people" -> VENUE, resource_name "Ballroom", capacity 500.
              "Penang Digital Library for 80 people" -> VENUE, resource_name "Library", capacity 80.
              "School football field for 150 people" -> VENUE, resource_name "Football field", capacity 150.
              "We can provide our community park" -> VENUE, resource_name "Park", capacity null.
              "We can provide an outdoor field" -> VENUE, resource_name "Outdoor field", capacity null.

            Return JSON only in exactly this shape:
            {
              "valid": true,
              "support_type": "TRANSPORT",
              "resource_name": "Passenger van",
              "quantity": 2,
              "capacity": null,
              "reason": null
            }

            For invalid input, return:
            {
              "valid": false,
              "support_type": null,
              "resource_name": "",
              "quantity": null,
              "capacity": null,
              "reason": "Short helpful explanation"
            }
        """.trimIndent()

        return requestAnalysis(text, systemPrompt)
    }

    /**
     * DETAILED BEHAVIOUR — analyseImpactWeaveNeed
     *
     * Implements the current VolunteerLink responsibility for analyse impact weave need in this support/model
     * layer.
     */
    suspend fun analyseImpactWeaveNeed(text: String): OrganisationSupportAnalysis {
        val systemPrompt = """
            You classify ONE physical partnership resource that an organisation NEEDS for VolunteerLink Impact Weave.

            Allowed support types are exactly:
            VENUE, EQUIPMENT, FURNITURE, TRANSPORT, SUPPLIES, REFRESHMENTS, SPECIALIST.

            Rules:
            - The user must describe only one kind of support need at a time.
            - VENUE always uses quantity = null. Capacity is optional: extract it only when the user gives a positive whole-number capacity. Otherwise return capacity = null.
            - Never estimate, assume, or invent a VENUE capacity.
            - Every other allowed type uses quantity and capacity must be null.
            - REFRESHMENTS means edible food or drink for the activity, including meal packs, packed meals, food packs, snacks, bottled/drinking/mineral water, and beverages. A Meal Pack is REFRESHMENTS, not SUPPLIES.
            - SUPPLIES means non-food consumable or distributable materials, for example stationery packs, hygiene kits, printed materials, cleaning supplies, or craft materials.
            - For non-VENUE needs, quantity must be a positive whole number stated or clearly implied by the text.
            - SPECIALIST means a person needed because of specific professional, technical, academic, certified, or specialist expertise.
              Valid examples include lecturers, teachers, doctors, nurses, first aid officers, trainers, technicians, engineers, interpreters, counsellors, photographers, and legal advisers.
            - Reject GENERAL volunteer manpower or ordinary helpers such as "10 volunteers", "5 helpers", "event crew", "registration volunteers", or "people to help". Those belong in Volunteer Post roles, not Impact Weave support.
            - Also reject promotion/social-media advertising, money/funding, vague requests, remote-only digital promotion, or anything outside the seven support types.
            - If multiple different resources are included, reject it and ask the user to add one need at a time.
            - Keep resource_name short and singular where natural, for example "Passenger van", "Folding chair", "PA speaker", or "English lecturer".
            - Preserve the specificity actually stated by the user. Do NOT invent packaging, form, subtype, material, or expertise that was not stated.
              Examples: "drinking water" -> resource_name "Drinking water", NOT "Bottled water"; "food" -> "Food", NOT "Meal pack"; "seating" -> "Seating", NOT "Folding chair".
              A more specific resource name is allowed only when the text actually states it, for example "bottles of water" -> "Bottled water" and "meal packs" -> "Meal pack".
            - For VENUE, resource_name must describe the GENERAL TYPE of place needed, not a specific proper place name, organisation name, campus name, branch name, or building name.
            - Do not limit VENUE to a fixed list. Infer the most useful broad venue type from the description, usually in 1 to 3 words.
              Examples can include Hall, Community Hall, Multipurpose Hall, Conference Hall, Banquet Hall, Ballroom, Auditorium, Lecture Hall, Meeting Room, Function Room, Classroom, Training Room, Workshop Space, Exhibition Hall, Event Space, Sports Hall, Stadium, Court, Field, Pavilion, Park, Garden, Outdoor Space, Library, or another sensible physical venue type.
            - Examples:
              "Need a hall for around 150 people" -> VENUE, resource_name "Hall", capacity 150.
              "Need an outdoor park" -> VENUE, resource_name "Park", capacity null.
              "We need 2 passenger vans" -> TRANSPORT, resource_name "Passenger van", quantity 2.
              "Need 2 English lecturers" -> SPECIALIST, resource_name "English lecturer", quantity 2.
              "Need 100 bottles of water" -> REFRESHMENTS, resource_name "Bottled water", quantity 100.
              "Need 10 meal packs" -> REFRESHMENTS, resource_name "Meal pack", quantity 10.
              "Need 20 stationery packs" -> SUPPLIES, resource_name "Stationery pack", quantity 20.

            Return JSON only in exactly this shape:
            {
              "valid": true,
              "support_type": "TRANSPORT",
              "resource_name": "Passenger van",
              "quantity": 2,
              "capacity": null,
              "reason": null
            }

            For invalid input, return:
            {
              "valid": false,
              "support_type": null,
              "resource_name": "",
              "quantity": null,
              "capacity": null,
              "reason": "Short helpful explanation"
            }
        """.trimIndent()

        return requestAnalysis(text, systemPrompt)
    }

    /**
     * Semantic classification only. Quantity/capacity and geography stay deterministic in
     * the app/database so Groq cannot invent availability or locations.
     */
    /**
     * DETAILED BEHAVIOUR — rankImpactWeaveCandidates
     *
     * Implements the current VolunteerLink responsibility for rank impact weave candidates in this
     * support/model layer.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun rankImpactWeaveCandidates(
        needs: List<GroqImpactWeaveNeed>,
        candidates: List<GroqImpactWeaveCandidate>
    ): List<ImpactWeaveSemanticMatch> {
        if (candidates.isEmpty() || needs.isEmpty()) return emptyList()
        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            throw IllegalStateException("GROQ_API_KEY is missing from local.properties.")
        }

        val firstPass = classifyImpactWeaveCandidatesOnce(
            needs = needs,
            candidates = candidates,
            focusedRetry = false
        )

        // The model is instructed to return EVERY need x support pair, including NONE.
        // Do not only retry a need when it has zero matches: a model can classify one provider
        // for Chair but accidentally omit another provider's Folding Chair. That missing pair
        // would make "Request all" silently incomplete even though the second organisation can
        // also help. Retry every need that has at least one missing pair.
        val firstEvaluatedPairs = firstPass
            .mapTo(mutableSetOf()) { it.needId to it.supportId }
        val retryNeeds = needs.filter { need ->
            candidates.any { candidate ->
                (need.needId to candidate.supportId) !in firstEvaluatedPairs
            }
        }

        val secondPass = if (retryNeeds.isEmpty()) {
            emptyList()
        } else {
            runCatching {
                classifyImpactWeaveCandidatesOnce(
                    needs = retryNeeds,
                    candidates = candidates,
                    focusedRetry = true
                )
            }.getOrDefault(emptyList())
        }

        val aiEvaluations = (firstPass + secondPass)
            .groupBy { it.needId to it.supportId }
            .mapNotNull { (_, entries) ->
                entries.firstOrNull { it.level == "DIRECT" }
                    ?: entries.firstOrNull { it.level == "ALTERNATIVE" }
                    ?: entries.firstOrNull { it.level == "NONE" }
            }

        // Deterministic safety net for obvious lexical containment only. AI still decides real
        // semantic relationships such as Food -> Packed Lunch and Drinking Water -> Bottled Water,
        // but it must never omit an obvious pair such as Chair -> Folding Chair or PA Speaker ->
        // Portable PA Speaker merely because another organisation also matched the same need.
        val clearDirectMatches = needs.flatMap { need ->
            candidates.mapNotNull { candidate ->
                if (isClearDirectResourceMatch(need.resourceName, candidate.resourceName)) {
                    ImpactWeaveSemanticMatch(
                        needId = need.needId,
                        supportId = candidate.supportId,
                        level = "DIRECT"
                    )
                } else {
                    null
                }
            }
        }

        return (aiEvaluations + clearDirectMatches)
            .groupBy { it.needId to it.supportId }
            .mapNotNull { (_, entries) ->
                entries.firstOrNull { it.level == "DIRECT" }
                    ?: entries.firstOrNull { it.level == "ALTERNATIVE" }
            }
    }

    /**
     * DETAILED BEHAVIOUR — classifyImpactWeaveCandidatesOnce
     *
     * Implements the current VolunteerLink responsibility for classify impact weave candidates once in this
     * support/model layer.
     */
    private suspend fun classifyImpactWeaveCandidatesOnce(
        needs: List<GroqImpactWeaveNeed>,
        candidates: List<GroqImpactWeaveCandidate>,
        focusedRetry: Boolean
    ): List<ImpactWeaveSemanticMatch> {
        val retryInstruction = if (focusedRetry) {
            """

            FOCUSED RETRY:
            - Some need x support evaluations were missing from the first pass, or a need had no usable match. Inspect EVERY supplied candidate again carefully and return one row for every pair.
            - Do not require exact wording when a broad need can be fulfilled by a concrete subtype.
            - In particular, broad Food can be DIRECT with Meal Pack, Packed Lunch, Packed Meal, Food Pack, or another clearly edible meal/food offering.
            - Broad Drinking Water can be DIRECT with Bottled Water or Mineral Water.
            - Obvious narrower forms remain DIRECT: Chair -> Folding Chair and PA Speaker -> Portable PA Speaker.
            - Still return NONE when the candidate is genuinely different; do not force a match.
            """.trimIndent()
        } else {
            ""
        }

        val systemPrompt = """
            You CLASSIFY REAL VolunteerLink organisation support records against REAL Impact Weave needs.
            You are not choosing a best organisation and you are not shortlisting.

            Important rules:
            - Evaluate EVERY need x candidate pair independently. If there are 5 needs and 8 candidates, return exactly 40 evaluation rows. Do not skip pairs.
            - Multiple candidates and multiple organisations can all be DIRECT for the same need. NEVER keep only the highest-quantity, nearest, or best-looking provider. VolunteerLink needs every valid option so several organisations can contribute to one requirement.
            - Quantity does NOT change the semantic level. Example: Need 70 Food + candidate 40 Meal Packs is still DIRECT. Need 60 Chairs + candidate 40 Folding Chairs is still DIRECT. VolunteerLink handles the amount and remaining quantity after this classification.
            - Capacity does NOT change the semantic level either. A Hall can be semantically DIRECT for a Hall/Venue need even if its capacity is too small; VolunteerLink separately moves an insufficient-capacity venue out of the suitable list.
            - support_type is a broad catalogue hint, NOT a hard matching rule. A different support_type may still be DIRECT when the real resource can practically fulfil the need, and the same support_type may still be NONE.
            - Matching is directional: ask "Can this candidate support actually fulfil this stated need without changing the need's meaning?"
            - If the NEED is broad/general, a sensible concrete subtype can be DIRECT. Examples: Food -> Meal Pack, Packed Lunch, Packed Meal or Food Pack; Drinking Water -> Bottled Water or Mineral Water; Seating -> Folding Chair; Audio Equipment -> PA Speaker; a broad English-education specialist need -> English Lecturer or English Teacher.
            - Food is deliberately broad. A clearly edible meal/lunch/food offering is a valid DIRECT subtype of Food. Do not mark Packed Lunch or Meal Pack as NONE merely because the words are different.
            - If the NEED is specific but the candidate is broader/vaguer, be more cautious. Meal Pack -> generic Food is normally ALTERNATIVE unless the support description clearly confirms packed meals. Bottled Water -> generic Drinking Water is normally ALTERNATIVE unless bottles are clearly stated.
            - Preserve important distinctions. Drinking Water -> Soft Drink is NONE. Food -> Bottled Water is NONE. PA Speaker -> Microphone is normally ALTERNATIVE or NONE because a microphone cannot replace a speaker. Folding Chair -> Table is NONE. A specific English Lecturer need -> generic volunteer or unrelated specialist is NONE.
            - DIRECT means the support is a confident practical fulfilment of the need. Synonyms, equivalent forms, and valid narrower subtypes of a broad need can be DIRECT.
            - ALTERNATIVE means related and potentially useful, but a human should decide because it does not confidently fulfil the request as written.
            - NONE means it should not be suggested.
            - VENUE remains semantic too: a broad "venue" need can accept a sensible real venue subtype, but a specific "hall" need should not treat an unrelated outdoor field as DIRECT.
            - Do NOT judge quantity, capacity, distance, or geographic suitability. VolunteerLink handles those factual checks separately after semantic matching.
            - Return only need_id values from needs and support_id values from candidates. Never invent an organisation, support, need, quantity, capacity, or location.
            - Return a row for NONE too. This is required so every supplied pair is explicitly classified.
            $retryInstruction

            Return JSON only in exactly this shape:
            {
              "matches": [
                {"need_id":"NEED001","support_id":"SUP001","level":"DIRECT"},
                {"need_id":"NEED001","support_id":"SUP002","level":"NONE"},
                {"need_id":"NEED002","support_id":"SUP001","level":"ALTERNATIVE"}
              ]
            }
        """.trimIndent()

        val input = JSONObject().apply {
            put("needs", JSONArray().apply {
                needs.forEach { need ->
                    put(JSONObject().apply {
                        put("need_id", need.needId)
                        put("support_type", need.supportType)
                        put("resource_name", need.resourceName)
                        put("original_text", need.originalText)
                    })
                }
            })
            put("candidates", JSONArray().apply {
                candidates.forEach { candidate ->
                    put(JSONObject().apply {
                        put("support_id", candidate.supportId)
                        put("support_type", candidate.supportType)
                        put("resource_name", candidate.resourceName)
                        put("support_description", candidate.supportDescription)
                    })
                }
            })
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", input.toString())
            })
        }

        val requestBody = JSONObject().apply {
            put("model", "openai/gpt-oss-20b")
            put("messages", messages)
            put("temperature", 0)
            put("reasoning_effort", "medium")
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseJson = JSONObject(response.bodyAsText())
        val content = responseJson
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val validNeedIds = needs.map { it.needId }.toSet()
        val validSupportIds = candidates.map { it.supportId }.toSet()
        val matches = JSONObject(content).optJSONArray("matches") ?: JSONArray()
        val result = mutableListOf<ImpactWeaveSemanticMatch>()

        for (index in 0 until matches.length()) {
            val item = matches.optJSONObject(index) ?: continue
            val needId = item.optString("need_id").trim()
            val supportId = item.optString("support_id").trim()
            val level = item.optString("level").trim().uppercase()

            if (needId !in validNeedIds || supportId !in validSupportIds) continue
            if (level !in setOf("DIRECT", "ALTERNATIVE", "NONE")) continue

            result += ImpactWeaveSemanticMatch(
                needId = needId,
                supportId = supportId,
                level = level
            )
        }

        return result.distinctBy { Triple(it.needId, it.supportId, it.level) }
    }

    /**
     * DETAILED BEHAVIOUR — isClearDirectResourceMatch
     *
     * Implements the current VolunteerLink responsibility for is clear direct resource match in this
     * support/model layer.
     */
    private fun isClearDirectResourceMatch(
        needName: String,
        candidateName: String
    ): Boolean {
        /**
         * DETAILED BEHAVIOUR — tokens
         *
         * Implements the current VolunteerLink responsibility for tokens in this support/model layer.
         */
        fun tokens(value: String): Set<String> = value
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 }
            .toSet()

        val needTokens = tokens(needName)
        val candidateTokens = tokens(candidateName)
        if (needTokens.isEmpty() || candidateTokens.isEmpty()) return false

        // Directional containment is safe here: a concrete candidate may add modifiers to the
        // requested resource (Chair -> Folding Chair, Speaker -> Portable PA Speaker). Do not use
        // mere token intersection; that would incorrectly turn related resources into DIRECT.
        return needTokens == candidateTokens || candidateTokens.containsAll(needTokens)
    }

    /**
     * DETAILED BEHAVIOUR — requestAnalysis
     *
     * Implements the current VolunteerLink responsibility for request analysis in this support/model layer.
     */
    private suspend fun requestAnalysis(
        text: String,
        systemPrompt: String
    ): OrganisationSupportAnalysis {
        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            throw IllegalStateException("GROQ_API_KEY is missing from local.properties.")
        }

        val messages = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
            )
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", text.trim())
                }
            )
        }

        val requestBody = JSONObject().apply {
            put("model", "openai/gpt-oss-20b")
            put("messages", messages)
            put("temperature", 0)
            put("reasoning_effort", "low")
            put(
                "response_format",
                JSONObject().put("type", "json_object")
            )
        }

        val response = client.post(
            "https://api.groq.com/openai/v1/chat/completions"
        ) {
            header(
                HttpHeaders.Authorization,
                "Bearer ${BuildConfig.GROQ_API_KEY}"
            )
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseJson = JSONObject(response.bodyAsText())
        val content = responseJson
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return validateAnalysis(JSONObject(content))
    }

    /**
     * DETAILED BEHAVIOUR — validateAnalysis
     *
     * Implements the current VolunteerLink responsibility for validate analysis in this support/model layer.
     */
    private fun validateAnalysis(json: JSONObject): OrganisationSupportAnalysis {
        val isValid = json.optBoolean("valid", false)
        val supportType = json.optString("support_type")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.uppercase()
        val resourceName = json.optString("resource_name").trim()
        val quantity = json.positiveIntOrNull("quantity")
        val capacity = json.positiveIntOrNull("capacity")
        val reason = json.optString("reason")
            .takeIf { it.isNotBlank() && it != "null" }

        if (!isValid) {
            return OrganisationSupportAnalysis(
                isValid = false,
                supportType = null,
                resourceName = "",
                quantity = null,
                capacity = null,
                reason = reason ?: "This does not look like a supported partnership resource."
            )
        }

        if (supportType !in allowedSupportTypes || resourceName.isBlank()) {
            return invalidAnalysis("I couldn't classify that support clearly. Try describing one resource and its amount.")
        }

        if (supportType != "VENUE" && quantity == null) {
            return invalidAnalysis("Include a quantity greater than 0 for this resource.")
        }

        return OrganisationSupportAnalysis(
            isValid = true,
            supportType = supportType,
            resourceName = resourceName,
            quantity = if (supportType == "VENUE") null else quantity,
            capacity = if (supportType == "VENUE") capacity else null,
            reason = null
        )
    }

    /**
     * DETAILED BEHAVIOUR — invalidAnalysis
     *
     * Implements the current VolunteerLink responsibility for invalid analysis in this support/model layer.
     */
    private fun invalidAnalysis(reason: String) = OrganisationSupportAnalysis(
        isValid = false,
        supportType = null,
        resourceName = "",
        quantity = null,
        capacity = null,
        reason = reason
    )

    /**
     * DETAILED BEHAVIOUR — positiveIntOrNull
     *
     * Implements the current VolunteerLink responsibility for positive int or null in this support/model layer.
     */
    private fun JSONObject.positiveIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key, -1).takeIf { it > 0 }
    }

    companion object {
        private val allowedSupportTypes = setOf(
            "VENUE",
            "EQUIPMENT",
            "FURNITURE",
            "TRANSPORT",
            "SUPPLIES",
            "REFRESHMENTS",
            "SPECIALIST"
        )
    }
}
