package com.example.volunteerlink.data

// Reads attendance information from Supabase without changing organiser records.

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

@Serializable
// Purpose: Handles the volunteer attendance record rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerAttendanceRecord(
    @SerialName("event_date") val date: String,
    @SerialName("attendance_status") val status: String,
    @SerialName("checked_in_at") val recordedAt: String? = null,
    @SerialName("verified_minutes") val minutes: Int = 0
)

@Serializable
// Purpose: Handles the volunteer attendance day rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerAttendanceDay(
    @SerialName("event_date") val date: String,
    @SerialName("is_active") val active: Boolean
)

// Purpose: Handles the volunteer attendance data rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerAttendanceData(val records: List<VolunteerAttendanceRecord>, val days: List<VolunteerAttendanceDay>)

object VolunteerAttendanceRepository {
    // Purpose: Reads  from the data source and returns models that the ViewModel can expose to Compose.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun load(post: String, role: String, expectedAccount: String): VolunteerAttendanceData {
        // Keep the account check at both ends of the request so a late response from a
        // previous login cannot populate attendance for the next person using the phone.
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == expectedAccount) { "Account changed. Reopen this application." }
        val profile = supabase.from("user_profiles").select(columns = Columns.raw("user_id")) {
            filter { eq("auth_user_id", expectedAccount); eq("account_type", "VOLUNTEER") }
        }.decodeList<JsonObject>().singleOrNull() ?: error("Volunteer profile is unavailable. Sign in again.")
        val user = profile["user_id"]?.jsonPrimitive?.content ?: error("Volunteer profile is unavailable.")
        val records = supabase.from("attendance_records")
            .select(columns = Columns.raw("event_date,attendance_status,checked_in_at,verified_minutes")) {
                filter { eq("post_id", post); eq("role_template_id", role); eq("user_id", user) }
            }.decodeList<VolunteerAttendanceRecord>()
        // Never fetch, display or cache pin_code on the Volunteer side.
        val days = supabase.from("attendance_days").select(columns = Columns.raw("event_date,is_active")) {
            filter { eq("post_id", post) }
        }.decodeList<VolunteerAttendanceDay>()
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
        return VolunteerAttendanceData(records, days)
    }

    // Purpose: Handles the check in rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun checkIn(post: String, role: String, pin: String, expectedAccount: String) {
        // PIN validation and attendance recording happen in Supabase. The device only
        // sends the volunteer's entered code; it never stores or compares the PIN locally.
        require(pin.matches(Regex("[0-9]{6}"))) { "Enter the six-digit code provided by the organisation." }
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == expectedAccount) { "Account changed. Reopen this application." }
        supabase.postgrest.rpc("check_in_with_attendance_pin", buildJsonObject {
            put("p_post_id", post); put("p_role_template_id", role); put("p_pin_code", pin)
        })
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
    }

    // Purpose: Handles the message rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun message(error: Throwable): String {
        val raw = error.message.orEmpty()
        val messages = listOf(
            "Attendance check-in has not opened yet.", "Attendance check-in has closed for today.",
            "Your Physical role is not scheduled for today.", "This post is not accepting attendance.",
            "Today is not a Physical event date.", "The organisation has not started attendance for today.",
            "You are not an accepted volunteer for this Physical role.",
            "The organisation has marked you absent for today. Contact the organisation if this needs correction.",
            "Invalid attendance PIN.", "Account changed. Reopen this application."
        )
        return messages.firstOrNull { raw.contains(it, ignoreCase = true) }
            ?: if (raw.contains("Internet connection is required"))
                "Internet connection is required. Connect and try again."
            else "Unable to confirm attendance. Check your connection, then refresh attendance before retrying."
    }
}
