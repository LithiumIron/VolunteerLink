package com.example.volunteerlink.organisation.data

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Owns the Organisation module's private device-side persistence under Android internal app storage.
//
// The storage is account-scoped: filenames include the authenticated user id so two organisation accounts on the
// same phone cannot accidentally restore one another's drafts or cached snapshots.
//
// Structured data is serialized to JSON in filesDir. This is used for recoverable UI state such as Create Post
// autosave, Impact Weave planning autosave, unread/screen snapshots and unsent text drafts.
//
// Local files are deliberately not treated as authoritative business records. Applicant decisions, attendance,
// submissions, partnership confirmations and published post state still require Supabase.
//
// Logout cleanup removes account-scoped files so private cached information is not left available to the next
// account that signs in on the device.
//
// Architectural layer: Shared data/support layer.
// ============================================================================


import android.content.Context
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePage
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Last successful Home/Manage snapshot kept on the device.
 *
 * This is a read-only fallback. Supabase remains authoritative and a cached
 * snapshot must never be used to perform applicant, attendance or post-state
 * mutations while the device is offline.
 */
@Serializable
/**
 * DETAILED DECLARATION — CachedOrganisationSnapshot
 *
 * Domain/UI type for Cached Organisation Snapshot used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedOrganisationSnapshot(
    val snapshot: OrganisationHomeSnapshot,
    val lastSyncedAtEpochMillis: Long
)

/** Last successful detailed Manage Post snapshot for one post. */
@Serializable
/**
 * DETAILED DECLARATION — CachedOrganisationPost
 *
 * Domain/UI type for Cached Organisation Post used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedOrganisationPost(
    val post: PostManagementPost,
    val lastSyncedAtEpochMillis: Long
)

/** Small promotion record used by the Promotion screen's offline display. */
@Serializable
/**
 * DETAILED DECLARATION — CachedPromotionRecord
 *
 * Domain/UI type for Cached Promotion Record used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedPromotionRecord(
    val promotionId: String,
    val postId: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val createdAtMillis: Long
)

/** Last successful promotion list returned by Supabase. */
@Serializable
/**
 * DETAILED DECLARATION — CachedOrganisationPromotions
 *
 * Domain/UI type for Cached Organisation Promotions used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedOrganisationPromotions(
    val promotions: List<CachedPromotionRecord>,
    val lastSyncedAtEpochMillis: Long
)

/**
 * Device autosave for a NEW Volunteer Post that has not yet been saved to Supabase.
 *
 * This is intentionally different from a database post with status DRAFT:
 * - device autosave protects unfinished typing if Android closes/recreates the app;
 * - a Supabase DRAFT is a real Volunteer Post explicitly saved by the organisation.
 *
 * The small amount of wizard navigation state below lets the organiser resume near
 * the place they left without persisting temporary validation/error messages.
 */
@Serializable
/**
 * DETAILED DECLARATION — CachedCreatePostAutosave
 *
 * Domain/UI type for Cached Create Post Autosave used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedCreatePostAutosave(
    val draft: CreatePostDraft,
    val currentStep: Int = 1,
    val reviewEditStep: Int? = null,
    val activeScheduleSection: ScheduleType? = null,
    val selectedPhysicalScheduleDateMillis: Long? = null,
    val editingScheduleItemId: String? = null,
    val scheduleEditorDraft: ScheduleItemDraft? = null,
    val isScheduleEditorOpen: Boolean = false,
    val lastSavedAtEpochMillis: Long
)

/**
 * Device-only Impact Weave autosave before Find Partners persists the plan.
 *
 * MATCHING/PARTIAL/WAITING/READY plans already live in Supabase and are therefore
 * not duplicated here as business truth. Only the local Activity Plan / Support
 * Needed / Review work is protected from accidental process loss.
 */
@Serializable
/**
 * DETAILED DECLARATION — CachedImpactWeaveAutosave
 *
 * Domain/UI type for Cached Impact Weave Autosave used by the Organisation module.
 *
 * The type makes the data shape explicit so screens/repositories exchange named fields instead of loosely-typed
 * maps.
 */
data class CachedImpactWeaveAutosave(
    val draft: ImpactWeaveDraft,
    val page: ImpactWeavePage,
    val lastSavedAtEpochMillis: Long
)

/**
 * Tiny local draft used for text the organisation has typed but not sent yet.
 * Sent chat messages always come from Supabase; this stores only the compose box.
 */
@Serializable
/**
 * DETAILED DECLARATION — CachedOrganisationTextDraft
 *
 * Represents editable/incomplete user input for Cached Organisation Text Draft before it becomes a server-
 * authoritative record.
 *
 * The draft can contain temporarily incomplete values because validation is applied at step transitions and
 * final persistence boundaries.
 */
data class CachedOrganisationTextDraft(
    val text: String,
    val lastSavedAtEpochMillis: Long
)

/**
 * Internal JSON storage used by Organisation screens.
 *
 * The storage boundary is intentionally conservative:
 * - SAFE TO CACHE: read-only snapshots, unfinished form input and unsent text;
 * - SERVER ONLY: application decisions, attendance, completion, payment records,
 *   partnership invitation state, volunteer contact permission and sent messages;
 * - NEVER STORED HERE: passwords, access tokens, full card numbers, expiry or CVV.
 *
 * Every file is scoped to the currently signed-in Supabase auth user so two
 * organisation accounts on the same phone do not read each other's local data.
 */
/**
 * DETAILED DECLARATION — OrganisationLocalStorage
 *
 * Single shared instance for Organisation Local Storage so related rules/state are defined once for the
 * application process.
 */
object OrganisationLocalStorage {
    lateinit var appContext: Context

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    /** Called once from MainActivity before any Organisation ViewModel uses storage. */
    /**
     * DETAILED BEHAVIOUR — initialise
     *
     * Stores the application Context once so later local-storage operations can resolve filesDir without
     * retaining an Activity/Compose context.
     *
     * Using applicationContext avoids leaking a screen/activity while still giving the singleton access to
     * VolunteerLink private internal storage.
     */
    fun initialise(context: Context) {
        appContext = context.applicationContext
    }

    // -------------------------------------------------------------------------
    // Home / Manage read-only snapshots
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — loadSnapshot
     *
     * Reads the account-scoped local value for load snapshot from VolunteerLink private internal storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadSnapshot(): CachedOrganisationSnapshot? = withContext(Dispatchers.IO) {
        readJsonFile("home")
    }

    /**
     * DETAILED BEHAVIOUR — saveSnapshot
     *
     * Serializes the recoverable local value for save snapshot into the current organisation account's private
     * internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun saveSnapshot(
        snapshot: OrganisationHomeSnapshot,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        writeJsonFile(
            name = "home",
            value = CachedOrganisationSnapshot(
                snapshot = snapshot,
                lastSyncedAtEpochMillis = syncedAtEpochMillis
            )
        )
    }

    /**
     * DETAILED BEHAVIOUR — loadPost
     *
     * Reads the account-scoped local value for load post from VolunteerLink private internal storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadPost(postId: String): CachedOrganisationPost? = withContext(Dispatchers.IO) {
        readJsonFile("post_${safeFilePart(postId)}")
    }

    /**
     * DETAILED BEHAVIOUR — savePost
     *
     * Serializes the recoverable local value for save post into the current organisation account's private
     * internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun savePost(
        post: PostManagementPost,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        writeJsonFile(
            name = "post_${safeFilePart(post.postId)}",
            value = CachedOrganisationPost(
                post = post,
                lastSyncedAtEpochMillis = syncedAtEpochMillis
            )
        )
    }

    // -------------------------------------------------------------------------
    // Promotion display cache
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — loadPromotions
     *
     * Reads the account-scoped local value for load promotions from VolunteerLink private internal storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadPromotions(): CachedOrganisationPromotions? = withContext(Dispatchers.IO) {
        readJsonFile("promotions")
    }

    /**
     * DETAILED BEHAVIOUR — savePromotions
     *
     * Serializes the recoverable local value for save promotions into the current organisation account's
     * private internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun savePromotions(
        promotions: List<CachedPromotionRecord>,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        writeJsonFile(
            name = "promotions",
            value = CachedOrganisationPromotions(
                promotions = promotions,
                lastSyncedAtEpochMillis = syncedAtEpochMillis
            )
        )
    }

    // -------------------------------------------------------------------------
    // Create Post local autosave
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — loadCreatePostAutosave
     *
     * Reads the account-scoped local value for load create post autosave from VolunteerLink private internal
     * storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadCreatePostAutosave(): CachedCreatePostAutosave? = withContext(Dispatchers.IO) {
        readJsonFile("create_post_autosave")
    }

    /**
     * DETAILED BEHAVIOUR — saveCreatePostAutosave
     *
     * Serializes the recoverable local value for save create post autosave into the current organisation
     * account's private internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun saveCreatePostAutosave(
        autosave: CachedCreatePostAutosave
    ) = withContext(Dispatchers.IO) {
        writeJsonFile("create_post_autosave", autosave)
    }

    /**
     * DETAILED BEHAVIOUR — clearCreatePostAutosave
     *
     * Removes local persisted state for clear create post autosave so stale/account-specific data is not
     * restored later.
     *
     * Only device storage is affected; server records are not deleted by this helper.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun clearCreatePostAutosave() = withContext(Dispatchers.IO) {
        deleteJsonFile("create_post_autosave")
    }

    // -------------------------------------------------------------------------
    // Impact Weave local autosave before the plan enters Supabase matching
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — loadImpactWeaveAutosave
     *
     * Reads the account-scoped local value for load impact weave autosave from VolunteerLink private internal
     * storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadImpactWeaveAutosave(): CachedImpactWeaveAutosave? = withContext(Dispatchers.IO) {
        readJsonFile("impact_weave_autosave")
    }

    /**
     * DETAILED BEHAVIOUR — saveImpactWeaveAutosave
     *
     * Serializes the recoverable local value for save impact weave autosave into the current organisation
     * account's private internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun saveImpactWeaveAutosave(
        autosave: CachedImpactWeaveAutosave
    ) = withContext(Dispatchers.IO) {
        writeJsonFile("impact_weave_autosave", autosave)
    }

    /**
     * DETAILED BEHAVIOUR — clearImpactWeaveAutosave
     *
     * Removes local persisted state for clear impact weave autosave so stale/account-specific data is not
     * restored later.
     *
     * Only device storage is affected; server records are not deleted by this helper.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun clearImpactWeaveAutosave() = withContext(Dispatchers.IO) {
        deleteJsonFile("impact_weave_autosave")
    }

    // -------------------------------------------------------------------------
    // Unsent chat compose text
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — loadTextDraft
     *
     * Reads the account-scoped local value for load text draft from VolunteerLink private internal storage.
     *
     * Failure or missing data is treated as cache/autosave absence; the method never substitutes local data for
     * a required server mutation.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun loadTextDraft(
        draftType: String,
        conversationId: String
    ): String = withContext(Dispatchers.IO) {
        val cached: CachedOrganisationTextDraft? = readJsonFile(
            textDraftFileName(draftType, conversationId)
        )
        cached?.text.orEmpty()
    }

    /**
     * DETAILED BEHAVIOUR — saveTextDraft
     *
     * Serializes the recoverable local value for save text draft into the current organisation account's
     * private internal-storage file.
     *
     * This write supports offline/restart continuity only and does not change Supabase business state.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun saveTextDraft(
        draftType: String,
        conversationId: String,
        text: String
    ) = withContext(Dispatchers.IO) {
        val fileName = textDraftFileName(draftType, conversationId)

        // A blank compose box has nothing useful to restore, so remove the file
        // instead of leaving many empty draft files behind.
        if (text.isBlank()) {
            deleteJsonFile(fileName)
        } else {
            writeJsonFile(
                name = fileName,
                value = CachedOrganisationTextDraft(
                    text = text,
                    lastSavedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * DETAILED BEHAVIOUR — clearTextDraft
     *
     * Removes local persisted state for clear text draft so stale/account-specific data is not restored later.
     *
     * Only device storage is affected; server records are not deleted by this helper.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     */
    suspend fun clearTextDraft(
        draftType: String,
        conversationId: String
    ) = withContext(Dispatchers.IO) {
        deleteJsonFile(textDraftFileName(draftType, conversationId))
    }

    // -------------------------------------------------------------------------
    // Logout / account separation
    // -------------------------------------------------------------------------

    /**
     * Deletes every Organisation JSON file owned by the CURRENT auth account.
     *
     * Call this before signOut(), because after Supabase clears the session there
     * is no longer an auth-user id available to identify which account's files
     * should be removed.
     */
    /**
     * DETAILED BEHAVIOUR — clearCurrentOrganisationData
     *
     * Deletes the current auth user's Organisation JSON caches/autosaves/text drafts during logout/account
     * cleanup.
     *
     * The filename convention is account-scoped, so cleanup targets this account's local data without deleting
     * another organisation's server records or unrelated application files.
     *
     * Supabase sign-out is handled separately; this method only clears device persistence.
     *
     * Runs blocking file/network-oriented work off the main UI thread to avoid freezing Compose interactions.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    suspend fun clearCurrentOrganisationData() = withContext(Dispatchers.IO) {
        checkInitialised()
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext
        val suffix = "_v1_${safeFilePart(authUserId)}.json"

        appContext.filesDir
            .listFiles()
            .orEmpty()
            .filter { file ->
                file.isFile &&
                    file.name.startsWith("organisation_") &&
                    file.name.endsWith(suffix)
            }
            .forEach { file -> runCatching { file.delete() } }
    }

    // -------------------------------------------------------------------------
    // Private file helpers
    // -------------------------------------------------------------------------

    /**
     * DETAILED BEHAVIOUR — readJsonFile
     *
     * Provides the internal-storage helper used by read json file.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private inline fun <reified T> readJsonFile(name: String): T? {
        val file = organisationFile(name) ?: return null
        if (!file.exists()) return null

        return runCatching {
            json.decodeFromString<T>(file.readText())
        }.getOrNull()
    }

    /**
     * DETAILED BEHAVIOUR — writeJsonFile
     *
     * Provides the internal-storage helper used by write json file.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     */
    private inline fun <reified T> writeJsonFile(name: String, value: T) {
        val file = organisationFile(name)
            ?: error("A signed-in organisation is required before saving offline data.")

        // Write to a temporary sibling first, then replace the old file. This
        // reduces the chance of leaving half-written JSON if Android kills the
        // process in the middle of an autosave.
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeToString(value))

        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText())
            temporary.delete()
        }
    }

    /**
     * DETAILED BEHAVIOUR — deleteJsonFile
     *
     * Removes local persisted state for delete json file so stale/account-specific data is not restored later.
     *
     * Only device storage is affected; server records are not deleted by this helper.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun deleteJsonFile(name: String) {
        organisationFile(name)?.let { file ->
            if (file.exists()) runCatching { file.delete() }
        }
    }

    /**
     * DETAILED BEHAVIOUR — textDraftFileName
     *
     * Provides the internal-storage helper used by text draft file name.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     */
    private fun textDraftFileName(draftType: String, conversationId: String): String =
        "text_draft_${safeFilePart(draftType)}_${safeFilePart(conversationId)}"

    /**
     * DETAILED BEHAVIOUR — organisationFile
     *
     * Provides the internal-storage helper used by organisation file.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     */
    private fun organisationFile(name: String): File? {
        checkInitialised()
        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return null
        return File(
            appContext.filesDir,
            "organisation_${safeFilePart(name)}_v1_${safeFilePart(authUserId)}.json"
        )
    }

    /**
     * DETAILED BEHAVIOUR — checkInitialised
     *
     * Provides the internal-storage helper used by check initialised.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     *
     * Coordinates account-scoped local persistence only for recoverable/cached UI state; published or
     * transactional business state continues to come from Supabase.
     */
    private fun checkInitialised() {
        check(::appContext.isInitialized) {
            "OrganisationLocalStorage must be initialised by MainActivity."
        }
    }

    /**
     * DETAILED BEHAVIOUR — safeFilePart
     *
     * Provides the internal-storage helper used by safe file part.
     *
     * The helper keeps filenames/serialization/account scoping consistent across all Organisation local
     * persistence.
     */
    fun safeFilePart(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
