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

data class PendingVolunteerAction(
    val actionId: Long,
    val actionType: String,
    val targetId: String,
    val payloadJson: String,
    val createdAtEpochMillis: Long
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
        createPendingActionTable(database)
    }

    override fun onUpgrade(
        database: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            createSkillPathCacheTable(database)
        }
        if (oldVersion < 3) {
            createPendingActionTable(database)
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

    suspend fun enqueueAction(
        actionType: String,
        targetId: String,
        payloadJson: String
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COLUMN_USER_SCOPE, targetUserScope())
            put(COLUMN_ACTION_TYPE, actionType)
            put(COLUMN_TARGET_ID, targetId)
            put(COLUMN_PAYLOAD_JSON, payloadJson)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        writableDatabase.insert(
            TABLE_PENDING_ACTIONS,
            null,
            values
        )
    }

    suspend fun readPendingActions(
        userScope: String
    ): List<PendingVolunteerAction> = withContext(Dispatchers.IO) {
        readableDatabase.query(
            TABLE_PENDING_ACTIONS,
            arrayOf(
                COLUMN_ACTION_ID,
                COLUMN_ACTION_TYPE,
                COLUMN_TARGET_ID,
                COLUMN_PAYLOAD_JSON,
                COLUMN_CREATED_AT
            ),
            "$COLUMN_USER_SCOPE = ?",
            arrayOf(userScope),
            null,
            null,
            "$COLUMN_CREATED_AT ASC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PendingVolunteerAction(
                            actionId = cursor.getLong(0),
                            actionType = cursor.getString(1),
                            targetId = cursor.getString(2),
                            payloadJson = cursor.getString(3),
                            createdAtEpochMillis = cursor.getLong(4)
                        )
                    )
                }
            }
        }
    }

    suspend fun deletePendingAction(actionId: Long) =
        withContext(Dispatchers.IO) {
            writableDatabase.delete(
                TABLE_PENDING_ACTIONS,
                "$COLUMN_ACTION_ID = ?",
                arrayOf(actionId.toString())
            )
        }

    private var pendingActionUserScope: String = ""

    fun setPendingActionUserScope(userScope: String) {
        pendingActionUserScope = userScope
    }

    private fun targetUserScope(): String {
        check(pendingActionUserScope.isNotBlank()) {
            "A user scope is required before queueing an offline action."
        }
        return pendingActionUserScope
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

    private fun createPendingActionTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PENDING_ACTIONS (
                action_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_scope TEXT NOT NULL,
                action_type TEXT NOT NULL,
                target_id TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    companion object {
        private const val DATABASE_NAME = "volunteerlink_local.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_DASHBOARD_CACHE = "volunteer_dashboard_cache"
        private const val TABLE_SKILL_PATH_CACHE = "volunteer_skill_path_cache"
        private const val TABLE_PENDING_ACTIONS = "volunteer_pending_actions"
        private const val COLUMN_USER_SCOPE = "user_scope"
        private const val COLUMN_DASHBOARD_JSON = "dashboard_json"
        private const val COLUMN_SKILL_PATH_JSON = "skill_paths_json"
        private const val COLUMN_LAST_SYNCED_AT = "last_synced_at"
        private const val COLUMN_ACTION_ID = "action_id"
        private const val COLUMN_ACTION_TYPE = "action_type"
        private const val COLUMN_TARGET_ID = "target_id"
        private const val COLUMN_PAYLOAD_JSON = "payload_json"
        private const val COLUMN_CREATED_AT = "created_at"

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
