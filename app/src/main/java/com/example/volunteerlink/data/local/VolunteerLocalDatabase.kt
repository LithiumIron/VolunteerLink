package com.example.volunteerlink.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.volunteerlink.data.VolunteerOpportunityDashboardData
import com.example.volunteerlink.model.VolunteerSkillPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class CachedVolunteerDashboard(
    val data: VolunteerOpportunityDashboardData,
    val lastSyncedAtEpochMillis: Long
)

/**
 * Small, user-scoped local SQLite cache for the volunteer dashboard.
 *
 * Supabase remains the cloud source of truth. This database makes the last
 * successful dashboard available during weak or unavailable connectivity and
 * satisfies the cloud + local persistence architecture without storing auth
 * tokens or passwords.
 */
class VolunteerLocalDatabase private constructor(
    context: Context
) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_DASHBOARD_CACHE (
                user_scope TEXT PRIMARY KEY NOT NULL,
                dashboard_json TEXT NOT NULL,
                last_synced_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createSkillPathCacheTable(database)
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            createSkillPathCacheTable(database)
        }
    }

    suspend fun readDashboard(
        userScope: String
    ): CachedVolunteerDashboard? = withContext(Dispatchers.IO) {
        readableDatabase.query(
            TABLE_DASHBOARD_CACHE,
            arrayOf(COLUMN_DASHBOARD_JSON, COLUMN_LAST_SYNCED_AT),
            "$COLUMN_USER_SCOPE = ?",
            arrayOf(userScope),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return@withContext null
            }

            val dashboardJson = cursor.getString(0)
            val lastSyncedAt = cursor.getLong(1)

            runCatching {
                CachedVolunteerDashboard(
                    data = json.decodeFromString<VolunteerOpportunityDashboardData>(
                        dashboardJson
                    ),
                    lastSyncedAtEpochMillis = lastSyncedAt
                )
            }.getOrNull()
        }
    }

    suspend fun writeDashboard(
        userScope: String,
        dashboard: VolunteerOpportunityDashboardData,
        syncedAtEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COLUMN_USER_SCOPE, userScope)
            put(
                COLUMN_DASHBOARD_JSON,
                json.encodeToString(
                    VolunteerOpportunityDashboardData.serializer(),
                    dashboard
                )
            )
            put(COLUMN_LAST_SYNCED_AT, syncedAtEpochMillis)
        }

        writableDatabase.insertWithOnConflict(
            TABLE_DASHBOARD_CACHE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun clearDashboard(userScope: String) =
        withContext(Dispatchers.IO) {
            writableDatabase.delete(
                TABLE_DASHBOARD_CACHE,
                "$COLUMN_USER_SCOPE = ?",
                arrayOf(userScope)
            )
        }

    suspend fun readSkillPaths(
        userScope: String
    ): List<VolunteerSkillPath>? = withContext(Dispatchers.IO) {
        readableDatabase.query(
            TABLE_SKILL_PATH_CACHE,
            arrayOf(COLUMN_SKILL_PATH_JSON),
            "$COLUMN_USER_SCOPE = ?",
            arrayOf(userScope),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            runCatching {
                json.decodeFromString<List<VolunteerSkillPath>>(
                    cursor.getString(0)
                )
            }.getOrNull()
        }
    }

    suspend fun writeSkillPaths(
        userScope: String,
        skillPaths: List<VolunteerSkillPath>
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COLUMN_USER_SCOPE, userScope)
            put(
                COLUMN_SKILL_PATH_JSON,
                json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(
                        VolunteerSkillPath.serializer()
                    ),
                    skillPaths
                )
            )
            put(COLUMN_LAST_SYNCED_AT, System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            TABLE_SKILL_PATH_CACHE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun createSkillPathCacheTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SKILL_PATH_CACHE (
                user_scope TEXT PRIMARY KEY NOT NULL,
                skill_paths_json TEXT NOT NULL,
                last_synced_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    companion object {
        private const val DATABASE_NAME = "volunteerlink_local.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_DASHBOARD_CACHE = "volunteer_dashboard_cache"
        private const val TABLE_SKILL_PATH_CACHE = "volunteer_skill_path_cache"
        private const val COLUMN_USER_SCOPE = "user_scope"
        private const val COLUMN_DASHBOARD_JSON = "dashboard_json"
        private const val COLUMN_SKILL_PATH_JSON = "skill_paths_json"
        private const val COLUMN_LAST_SYNCED_AT = "last_synced_at"

        @Volatile
        private var instance: VolunteerLocalDatabase? = null

        fun getInstance(context: Context): VolunteerLocalDatabase =
            instance ?: synchronized(this) {
                instance ?: VolunteerLocalDatabase(context).also {
                    instance = it
                }
            }
    }
}
