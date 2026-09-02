package com.example.volunteerlink.data.ai

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

data class OrganisationSupportAnalysis(
    val isValid: Boolean,
    val supportType: String?,
    val resourceName: String,
    val quantity: Int?,
    val capacity: Int?,
    val reason: String?
)

class GroqService {

    private val client = HttpClient(Android) {
        expectSuccess = true
    }

    suspend fun analyseOrganisationSupport(text: String): OrganisationSupportAnalysis {
        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            throw IllegalStateException("GROQ_API_KEY is missing from local.properties.")
        }

        val systemPrompt = """
            You classify ONE physical partnership resource for VolunteerLink Impact Weave.

            Allowed support types are exactly:
            VENUE, EQUIPMENT, FURNITURE, TRANSPORT, SUPPLIES, REFRESHMENTS, SPECIALIST.

            Rules:
            - The user must describe only one kind of support at a time.
            - VENUE uses capacity and quantity must be null.
            - Every other allowed type uses quantity and capacity must be null.
            - Quantity/capacity must be a positive whole number stated or clearly implied by the text.
            - SPECIALIST means a person provided because of specific professional, technical, academic, certified, or specialist expertise.
              Valid examples include lecturers, teachers, doctors, nurses, first aid officers, trainers, technicians, engineers, interpreters, counsellors, photographers, and legal advisers.
            - A stated number of specialists is valid, for example "2 English lecturers" -> SPECIALIST, resource_name "English lecturer", quantity 2.
            - Reject only GENERAL volunteer manpower or ordinary helpers without a distinct expertise, for example "10 volunteers", "5 helpers", "3 event crew", or "4 registration volunteers".
            - Also reject event promotion/social-media advertising, money/funding, vague promises, remote-only digital promotion, or anything outside the seven support types.
            - If multiple different resources are included, reject it and ask the user to add one support item at a time.
            - Keep resource_name short and singular where natural, for example "Passenger van", "Folding chair", "PA speaker", or "First aid officer".
            - For VENUE, resource_name must describe the GENERAL TYPE of place, not its specific proper name, organisation name, campus name, branch name, or building name.
            - Do not limit VENUE to a fixed list. Infer the most useful broad venue type from the description, usually in 1 to 3 words.
              Examples of possible venue types include Hall, Community Hall, Multipurpose Hall, Conference Hall, Banquet Hall, Ballroom, Auditorium, Lecture Hall, Meeting Room, Function Room, Classroom, Training Room, Workshop Space, Exhibition Hall, Event Space, Sports Hall, Stadium, Court, Field, Pavilion, Park, Garden, Outdoor Space, Library, or similar physical places. These are examples only, not an exhaustive list.
            - Strip proper names from a VENUE resource_name. Examples:
              "TAR UMT Main Hall for 300 people" -> VENUE, resource_name "Hall", capacity 300.
              "Dewan Sivik MBPJ for 200 people" -> VENUE, resource_name "Hall", capacity 200.
              "ABC Hotel Grand Ballroom for 500 people" -> VENUE, resource_name "Ballroom", capacity 500.
              "Penang Digital Library for 80 people" -> VENUE, resource_name "Library", capacity 80.
              "School football field for 150 people" -> VENUE, resource_name "Football field", capacity 150.

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

        if (supportType == "VENUE") {
            if (capacity == null) {
                return invalidAnalysis("Include how many people the venue can hold.")
            }
        } else if (quantity == null) {
            return invalidAnalysis("Include how many of this resource your organisation can provide.")
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

    private fun invalidAnalysis(reason: String) = OrganisationSupportAnalysis(
        isValid = false,
        supportType = null,
        resourceName = "",
        quantity = null,
        capacity = null,
        reason = reason
    )

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
