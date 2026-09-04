package com.example.volunteerlink.data

// Builds the volunteer dashboard from cached data first, then refreshes it from Supabase.

import android.content.Context
import com.example.volunteerlink.data.local.CachedVolunteerDashboard
import com.example.volunteerlink.data.local.VolunteerLocalDatabase
import io.github.jan.supabase.auth.auth
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.data.local.PendingVolunteerAction

/** Coordinates Supabase (cloud) and SQLite (local) dashboard data. */
object VolunteerDashboardDataSource {
    private lateinit var localDatabase: VolunteerLocalDatabase
    private lateinit var applicationContext: Context

    fun initialise(context: Context) {
        applicationContext = context.applicationContext
        if (!::localDatabase.isInitialized) {
            localDatabase = VolunteerLocalDatabase.getInstance(context)
        }
    }

    // Purpose: Handles the read cached rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun readCached(): CachedVolunteerDashboard? =
        database().readDashboard(currentUserScope())

    // Purpose: Handles the refresh from cloud rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun refreshFromCloud(): VolunteerOpportunityDashboardData {
        val scope = currentUserScope()
        val cloud = VolunteerOpportunityRepository.loadDashboard()
        val dashboard = cloud.copy(applications = cloud.applications + VolunteerApplicationActions.pendingApplications(applicationContext, cloud))
        check(currentUserScope() == scope) { "Account changed. Reopen the Volunteer home screen." }
        database().writeDashboard(
            userScope = scope,
            dashboard = dashboard
        )
        return dashboard
    }

    // Purpose: Handles the cache current session rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun cacheCurrentSession() {
        database().writeDashboard(
            userScope = currentUserScope(),
            dashboard = VolunteerOpportunitySessionStore.snapshot()
        )
    }

    // Purpose: Handles the read cached skill paths rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun readCachedSkillPaths(): List<VolunteerSkillPath>? =
        database().readSkillPaths(currentUserScope())

    // Purpose: Handles the cache skill paths rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun cacheSkillPaths(skillPaths: List<VolunteerSkillPath>) {
        database().writeSkillPaths(
            userScope = currentUserScope(),
            skillPaths = skillPaths
        )
    }

    // Purpose: Handles the enqueue pending action rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun enqueuePendingAction(
        actionType: String,
        targetId: String,
        payloadJson: String
    ) {
        val scope = currentUserScope()
        database().setPendingActionUserScope(scope)
        database().enqueueAction(actionType, targetId, payloadJson)
    }

    // Purpose: Handles the read pending actions rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun readPendingActions(): List<PendingVolunteerAction> =
        database().readPendingActions(currentUserScope())

    // Purpose: Applies the delete pending action data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun deletePendingAction(actionId: Long) =
        database().deletePendingAction(actionId)

    // Purpose: Applies the sync pending actions data operation and returns only after local/shared state can be updated consistently.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    suspend fun syncPendingActions(): List<String> {
        val warnings = mutableListOf<String>()
        val ownerScope = currentUserScope()
        readPendingActions().forEach { action ->
            check(currentUserScope() == ownerScope) { "Account changed. Reopen Volunteer home before syncing." }
            try {
                VolunteerOpportunityRepository.replayPendingAction(
                    actionType = action.actionType,
                    targetId = action.targetId,
                    payloadJson = action.payloadJson
                )
                check(currentUserScope() == ownerScope) { "Account changed. Reopen Volunteer home before syncing." }
                deletePendingAction(action.actionId)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) {
                // Keep the original request for recovery; do not pretend it succeeded
                // and do not stop the unrelated dashboard refresh.
                warnings += "An earlier ${action.actionType.lowercase().replace('_', ' ')} request for ${action.targetId.substringBefore('|')} is still unconfirmed. Check My Applications before retrying."
            }
        }
        return warnings
    }

    // Purpose: Handles the current user scope rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    private fun currentUserScope(): String {
        val authUserId = supabase.auth.currentUserOrNull()?.id
            ?: error("A signed-in user is required to load volunteer data.")

        // A cache key is a contract, not only a user ID. Bumping this value
        // prevents an older public-schema/demo snapshot from being displayed
        // after the app migrates to the normalized v1_erd_test source.
        return "$CACHE_CONTRACT_VERSION:$authUserId"
    }

    // Purpose: Handles the database rule in the data layer so screens do not duplicate this business logic.
    // Usage: Called by a Volunteer ViewModel or another data-layer coordinator, not directly by a UI button.
    // Data effect: The returned value is mapped before Compose reads it, keeping database details outside the UI.
    private fun database(): VolunteerLocalDatabase {
        check(::localDatabase.isInitialized) {
            "VolunteerDashboardDataSource must be initialised by MainActivity."
        }
        return localDatabase
    }

    private const val CACHE_CONTRACT_VERSION = "v1_erd_test:v13"
}
