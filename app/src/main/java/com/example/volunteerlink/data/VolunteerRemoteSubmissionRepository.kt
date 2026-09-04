package com.example.volunteerlink.data

// Validates one remote-project file, uploads it, and saves its submission metadata.

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.UploadData
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

@Serializable
// Purpose: Handles the volunteer remote submission rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerRemoteSubmission(
    @SerialName("submission_id") val id: String,
    @SerialName("file_path") val filePath: String? = null,
    val status: String,
    val feedback: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null
)

@Serializable
// Purpose: Handles the volunteer remote context rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerRemoteContext(
    @SerialName("can_submit") val canSubmit: Boolean,
    val reason: String,
    @SerialName("effective_deadline") val deadline: String,
    @SerialName("submission_mode") val mode: String,
    val requirement: String,
    @SerialName("completion_status") val completionStatus: String,
    val history: List<VolunteerRemoteSubmission> = emptyList()
)

// Purpose: Handles the volunteer remote file rules rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
object VolunteerRemoteFileRules {
    // Keep supported file types and the size limit in one place for the picker and upload checks.
    const val MAX_BYTES = 20_000_000L
    val mimeTypes = linkedMapOf(
        "pdf" to "application/pdf", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
        "png" to "image/png", "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "txt" to "text/plain", "zip" to "application/zip"
    )

    // Purpose: Handles the mime type rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun mimeType(name: String): String = mimeTypes[name.substringAfterLast('.', "").lowercase(Locale.ROOT)]
        ?: throw IllegalArgumentException("Choose a PDF, image, Word, Excel, PowerPoint, TXT or ZIP file.")

    // Purpose: Handles the check size rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun checkSize(bytes: Long) {
        require(bytes > 0) { "The selected file is empty." }
        require(bytes <= MAX_BYTES) { "The selected file exceeds the 20 MB limit." }
    }

    // Purpose: Handles the safe name rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun safeName(name: String): String {
        mimeType(name)
        val extension = name.substringAfterLast('.').lowercase(Locale.ROOT)
        val base = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_-]"), "_")
            .trim('_', '-').take(100).ifBlank { "project" }
        return "$base.$extension"
    }
}

// Purpose: Handles the volunteer remote selected file rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
data class VolunteerRemoteSelectedFile(
    val file: File,
    val displayName: String,
    val storageName: String,
    val mimeType: String,
    val requestId: String = UUID.randomUUID().toString()
)

// Purpose: Handles the volunteer remote submission repository rule in the data layer so screens do not duplicate this business logic.
// Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
// Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
object VolunteerRemoteSubmissionRepository {
    private const val BUCKET = "remote-submissions"

    // Purpose: Handles the signed in id rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    fun signedInId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun readyAccountId(): String {
        supabase.auth.awaitInitialization()
        return signedInId() ?: if (supabase.auth.currentSessionOrNull() != null) {
            supabase.auth.retrieveUserForCurrentSession(updateSession = true).id
        } else {
            error("Sign in before submitting.")
        }
    }

    // Purpose: Reads  from the data source and returns models that the ViewModel can expose to Compose.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun load(postId: String, roleId: String): VolunteerRemoteContext =
        supabase.postgrest.rpc("volunteer_remote_context_v1", buildJsonObject {
            put("p_post_id", postId)
            put("p_role_id", roleId)
        }).decodeAs<VolunteerRemoteContext>()

    // Copy through a bounded buffer into private app cache. Never readBytes() a
    // whole document and never trust the provider's reported size alone.
    // Purpose: Handles the prepare rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun prepare(context: Context, uri: Uri): VolunteerRemoteSelectedFile = withContext(Dispatchers.IO) {
        var name = ""
        var reportedSize: Long? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = c.getString(nameIndex).orEmpty()
                if (sizeIndex >= 0 && !c.isNull(sizeIndex)) reportedSize = c.getLong(sizeIndex)
            }
        }
        // Validate the filename before copying the selected document into app storage.
        val mime = VolunteerRemoteFileRules.mimeType(name)
        reportedSize?.takeIf { it >= 0 }?.let(VolunteerRemoteFileRules::checkSize)
        val local = File.createTempFile("volunteer_remote_", ".tmp", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("The selected file could not be opened. Choose it again.")
            input.use { source ->
                local.outputStream().use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    // Copy in small chunks so a large file does not fill memory.
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= VolunteerRemoteFileRules.MAX_BYTES) { "The selected file exceeds the 20 MB limit." }
                        target.write(buffer, 0, count)
                    }
                    VolunteerRemoteFileRules.checkSize(total)
                }
            }
            VolunteerRemoteSelectedFile(local, name, VolunteerRemoteFileRules.safeName(name), mime)
        } catch (e: Throwable) {
            local.delete() // Only the temporary file created above, never user data.
            throw e
        }
    }

    // Purpose: Handles the submit rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun submit(postId: String, roleId: String, selected: VolunteerRemoteSelectedFile,
                       expectedAccountId: String,
                       onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val uid = readyAccountId()
        check(uid == expectedAccountId) { "Your account changed. Reopen this application." }
        VolunteerRemoteFileRules.checkSize(selected.file.length())
        val path = "$postId/$uid/$roleId/${selected.requestId}/${selected.storageName}"
        val bucket = supabase.storage.from(BUCKET)
        // Stable request ID makes an uncertain upload/finalization retry safe.
        // Never overwrite an object. Already-uploaded files proceed to the RPC.
        if (!bucket.exists(path)) {
            val latest = load(postId, roleId)
            check(latest.canSubmit) { latest.reason }
            selected.file.inputStream().use { input ->
                val channel = input.toByteReadChannel()
                try {
                    bucket.upload(path, UploadData(channel, selected.file.length())) {
                        upsert = false
                        contentType = ContentType.parse(selected.mimeType)
                        httpOverride {
                            timeout { requestTimeoutMillis = 300_000; socketTimeoutMillis = 60_000 }
                            onUpload { sent, length ->
                                onProgress((sent.toDouble() / (length ?: selected.file.length()).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f))
                            }
                        }
                    }
                } finally { channel.cancel(null) }
            }
        }
        onProgress(1f)
        check(readyAccountId() == uid) { "Your account changed. Reopen this application." }
        supabase.postgrest.rpc("volunteer_remote_submit_v1", buildJsonObject {
            put("p_post_id", postId)
            put("p_role_id", roleId)
            put("p_request_id", selected.requestId)
            put("p_file_name", selected.storageName)
        })
        Unit // Success only after the submission record is committed.
    }

    // Purpose: Handles the file url rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun fileUrl(path: String): String = supabase.storage.from(BUCKET).createSignedUrl(path, 1.minutes)
}
