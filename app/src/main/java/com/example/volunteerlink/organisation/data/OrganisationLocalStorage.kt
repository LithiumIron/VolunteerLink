package com.example.volunteerlink.organisation.data

import android.content.Context
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedOrganisationSnapshot(
    val snapshot: OrganisationHomeSnapshot,
    val lastSyncedAtEpochMillis: Long
)

@Serializable
data class CachedOrganisationPost(
    val post: PostManagementPost,
    val lastSyncedAtEpochMillis: Long
)

@Serializable
data class CachedPromotionRecord(
    val promotionId: String,
    val postId: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val createdAtMillis: Long
)

@Serializable
data class CachedOrganisationPromotions(
    val promotions: List<CachedPromotionRecord>,
    val lastSyncedAtEpochMillis: Long
)

/**
 * Practical 8-style internal JSON storage for Organisation screens.
 *
 * Supabase stays the source of truth. Successful cloud reads are saved locally,
 * then Home/Manage/Post details/Promotions can fall back to the last saved copy
 * when the network or Supabase is temporarily unavailable.
 */
object OrganisationLocalStorage {
    lateinit var appContext: Context

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun initialise(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun loadSnapshot(): CachedOrganisationSnapshot? = withContext(Dispatchers.IO) {
        val file = organisationFile("home") ?: return@withContext null
        if (!file.exists()) return@withContext null

        runCatching {
            json.decodeFromString<CachedOrganisationSnapshot>(file.readText())
        }.getOrNull()
    }

    suspend fun saveSnapshot(
        snapshot: OrganisationHomeSnapshot,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val file = organisationFile("home")
            ?: error("A signed-in organisation is required before saving offline data.")

        file.writeText(
            json.encodeToString(
                CachedOrganisationSnapshot(
                    snapshot = snapshot,
                    lastSyncedAtEpochMillis = syncedAtEpochMillis
                )
            )
        )
    }

    suspend fun loadPost(postId: String): CachedOrganisationPost? =
        withContext(Dispatchers.IO) {
            val file = organisationFile("post_${safeFilePart(postId)}")
                ?: return@withContext null
            if (!file.exists()) return@withContext null

            runCatching {
                json.decodeFromString<CachedOrganisationPost>(file.readText())
            }.getOrNull()
        }

    suspend fun savePost(
        post: PostManagementPost,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val file = organisationFile("post_${safeFilePart(post.postId)}")
            ?: error("A signed-in organisation is required before saving offline data.")

        file.writeText(
            json.encodeToString(
                CachedOrganisationPost(
                    post = post,
                    lastSyncedAtEpochMillis = syncedAtEpochMillis
                )
            )
        )
    }

    suspend fun loadPromotions(): CachedOrganisationPromotions? =
        withContext(Dispatchers.IO) {
            val file = organisationFile("promotions") ?: return@withContext null
            if (!file.exists()) return@withContext null

            runCatching {
                json.decodeFromString<CachedOrganisationPromotions>(file.readText())
            }.getOrNull()
        }

    suspend fun savePromotions(
        promotions: List<CachedPromotionRecord>,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val file = organisationFile("promotions")
            ?: error("A signed-in organisation is required before saving offline data.")

        file.writeText(
            json.encodeToString(
                CachedOrganisationPromotions(
                    promotions = promotions,
                    lastSyncedAtEpochMillis = syncedAtEpochMillis
                )
            )
        )
    }

    fun organisationFile(name: String): File? {
        check(::appContext.isInitialized) {
            "OrganisationLocalStorage must be initialised by MainActivity."
        }

        val authUserId = supabase.auth.currentUserOrNull()?.id ?: return null
        return File(
            appContext.filesDir,
            "organisation_${safeFilePart(name)}_v1_${safeFilePart(authUserId)}.json"
        )
    }

    fun safeFilePart(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
