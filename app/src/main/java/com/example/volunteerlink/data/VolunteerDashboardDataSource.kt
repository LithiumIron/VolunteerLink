package com.example.volunteerlink.data

import android.content.Context
import com.example.volunteerlink.data.local.CachedVolunteerDashboard
import com.example.volunteerlink.data.local.VolunteerLocalDatabase
import io.github.jan.supabase.auth.auth
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.data.local.PendingVolunteerAction

/** Coordinates Supabase (cloud) and SQLite (local) dashboard data. */
object VolunteerDashboardDataSource {
    private lateinit var localDatabase: VolunteerLocalDatabase

    fun initialise(context: Context) {
        if (!::localDatabase.isInitialized) {
            localDatabase = VolunteerLocalDatabase.getInstance(context)
        }
    }

    suspend fun readCached(): CachedVolunteerDashboard? =
        database().readDashboard(currentUserScope())

    suspend fun refreshFromCloud(): VolunteerOpportunityDashboardData {
        val dashboard = VolunteerOpportunityRepository.loadDashboard()
        database().writeDashboard(
            userScope = currentUserScope(),
            dashboard = dashboard
        )
        return dashboard
    }

    suspend fun cacheCurrentSession() {
        database().writeDashboard(
            userScope = currentUserScope(),
            dashboard = VolunteerOpportunitySessionStore.snapshot()
        )
    }

    suspend fun readCachedSkillPaths(): List<VolunteerSkillPath>? =
        database().readSkillPaths(currentUserScope())

    suspend fun cacheSkillPaths(skillPaths: List<VolunteerSkillPath>) {
        database().writeSkillPaths(
            userScope = currentUserScope(),
            skillPaths = skillPaths
        )
    }

    suspend fun enqueuePendingAction(
        actionType: String,
        targetId: String,
        payloadJson: String
    ) {
        val scope = currentUserScope()
        database().setPendingActionUserScope(scope)
        database().enqueueAction(actionType, targetId, payloadJson)
    }

    suspend fun readPendingActions(): List<PendingVolunteerAction> =
        database().readPendingActions(currentUserScope())

    suspend fun deletePendingAction(actionId: Long) =
        database().deletePendingAction(actionId)

    suspend fun syncPendingActions() {
        readPendingActions().forEach { action ->
            VolunteerOpportunityRepository.replayPendingAction(
                actionType = action.actionType,
                targetId = action.targetId,
                payloadJson = action.payloadJson
            )
            deletePendingAction(action.actionId)
        }
    }

    private fun currentUserScope(): String {
        val authUserId = supabase.auth.currentUserOrNull()?.id
            ?: error("A signed-in user is required to load volunteer data.")

        // A cache key is a contract, not only a user ID. Bumping this value
        // prevents an older public-schema/demo snapshot from being displayed
        // after the app migrates to the normalized v1_erd_test source.
        return "$CACHE_CONTRACT_VERSION:$authUserId"
    }

    private fun database(): VolunteerLocalDatabase {
        check(::localDatabase.isInitialized) {
            "VolunteerDashboardDataSource must be initialised by MainActivity."
        }
        return localDatabase
    }

    private const val CACHE_CONTRACT_VERSION = "v1_erd_test:v13"
}
