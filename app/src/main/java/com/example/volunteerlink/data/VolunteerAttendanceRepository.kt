package com.example.volunteerlink.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

@Serializable
data class VolunteerAttendanceRecord(
    @SerialName("event_date") val date: String,
    @SerialName("attendance_status") val status: String,
    @SerialName("checked_in_at") val recordedAt: String? = null,
    @SerialName("verified_minutes") val minutes: Int = 0
)

@Serializable
data class VolunteerAttendanceDay(
    @SerialName("event_date") val date: String,
    @SerialName("is_active") val active: Boolean
)

data class VolunteerAttendanceData(val records: List<VolunteerAttendanceRecord>, val days: List<VolunteerAttendanceDay>)

object VolunteerAttendanceRepository {
    suspend fun load(post: String, role: String, expectedAccount: String): VolunteerAttendanceData {
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

    suspend fun checkIn(post: String, role: String, pin: String, expectedAccount: String) {
        require(pin.matches(Regex("[0-9]{6}"))) { "Enter the six-digit code provided by the organisation." }
        check(VolunteerRemoteSubmissionRepository.readyAccountId() == expectedAccount) { "Account changed. Reopen this application." }
        supabase.postgrest.rpc("check_in_with_attendance_pin", buildJsonObject {
            put("p_post_id", post); put("p_role_template_id", role); put("p_pin_code", pin)
        })
        check(supabase.auth.currentUserOrNull()?.id == expectedAccount) { "Account changed. Reopen this application." }
    }

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
