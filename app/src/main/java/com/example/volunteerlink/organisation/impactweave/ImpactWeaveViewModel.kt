package com.example.volunteerlink.organisation.impactweave

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Owns Impact Weave Compose state from initial plan entry through matching/history and Volunteer Post conversion.
//
// It coordinates local autosave only while the plan is still an unfinished device-side form; persisted
// MATCHING/WAITING/PARTIAL/READY state is always reloaded from the repository.
//
// The ViewModel combines factual candidate data from Supabase with semantic compatibility analysis, while
// quantities/capacities/eligibility and invitation status remain server-controlled.
//
// It exposes explicit loading/error/action state so Compose never assumes partnership work succeeded before the
// backend confirms it.
//
// Architectural layer: ViewModel / workflow state layer.
// ============================================================================


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.ai.GroqImpactWeaveCandidate
import com.example.volunteerlink.data.ai.GroqImpactWeaveNeed
import com.example.volunteerlink.data.ai.GroqService
import com.example.volunteerlink.data.ai.ImpactWeaveSemanticMatch
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveActivePlan
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDatabaseNeed
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchResults
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMatchingInput
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDuration
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedMatchResult
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePage
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePartnershipState
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveSupportCandidate
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveUiState
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.data.CachedImpactWeaveAutosave
import com.example.volunteerlink.organisation.data.OrganisationLocalStorage
import com.example.volunteerlink.organisation.repository.ImpactWeaveRepository
import com.example.volunteerlink.organisation.repository.PartnershipRequestItem
import com.example.volunteerlink.organisation.repository.SupabaseImpactWeaveRepository
import com.example.volunteerlink.organisation.repository.SupabasePartnershipRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Encapsulates the state and behaviour represented by impact weave view model.
 * It supports state coordination and user actions for the Impact Weave and partnership flow.
 */
/**
 * DETAILED DECLARATION — ImpactWeaveViewModel
 *
 * Lifecycle-aware state owner for Impact Weave View Model. It survives ordinary Compose recomposition and
 * coordinates asynchronous repository work.
 *
 * UI callbacks enter through methods on this class so validation, loading/error state and dependent business
 * rules remain centralised.
 */
class ImpactWeaveViewModel : ViewModel() {

    private val impactWeaveRepository: ImpactWeaveRepository =
        SupabaseImpactWeaveRepository()
    private val matchingService = GroqService()
    private val partnershipRepository = SupabasePartnershipRepository

    private val _uiState = MutableStateFlow(ImpactWeaveUiState())
    val uiState = _uiState.asStateFlow()

    private var nextDraftId = 1
    private var nextNeedId = 1

    private var restoredLocalDraft = false
    private var lastLocalDraftSignature: CachedImpactWeaveAutosave? = null

    init {
        // Restore only device-only work that has NOT entered Supabase matching yet.
        // Persisted MATCHING/PARTIAL/WAITING/READY plans continue to be loaded by
        // loadActivePlans(), so local storage never replaces partnership truth.
        viewModelScope.launch {
            val cached = runCatching {
                OrganisationLocalStorage.loadImpactWeaveAutosave()
            }.getOrNull()

            if (cached != null && _uiState.value.workingDraft == null) {
                val safePage = when (cached.page) {
                    ImpactWeavePage.ACTIVITY_PLAN,
                    ImpactWeavePage.SUPPORT_NEEDED,
                    ImpactWeavePage.REVIEW -> cached.page
                    else -> ImpactWeavePage.ACTIVITY_PLAN
                }

                // Local integer IDs are UI-only. Advance the counters past the
                // restored draft/needs so newly added needs do not reuse an id.
                nextDraftId = maxOf(nextDraftId, cached.draft.draftId + 1)
                nextNeedId = maxOf(
                    nextNeedId,
                    (cached.draft.needs.maxOfOrNull { it.needId } ?: 0) + 1
                )

                lastLocalDraftSignature = cached.copy(lastSavedAtEpochMillis = 0L)
                _uiState.value = _uiState.value.copy(
                    page = safePage,
                    workingDraft = cached.draft
                )
            }
            restoredLocalDraft = true
        }

        // Autosave changes after a short quiet period. This protects long Impact
        // Weave forms from process death without making a database row before the
        // organisation actually presses Find Partners.
        viewModelScope.launch {
            _uiState.collectLatest { state ->
                if (!restoredLocalDraft) return@collectLatest

                val draft = state.workingDraft
                val isDeviceOnlyDraft = draft != null && draft.databaseDraftId == null &&
                    state.page in setOf(
                        ImpactWeavePage.ACTIVITY_PLAN,
                        ImpactWeavePage.SUPPORT_NEEDED,
                        ImpactWeavePage.REVIEW
                    )

                if (!isDeviceOnlyDraft) {
                    if (lastLocalDraftSignature != null) {
                        runCatching { OrganisationLocalStorage.clearImpactWeaveAutosave() }
                        lastLocalDraftSignature = null
                    }
                    return@collectLatest
                }

                val signature = CachedImpactWeaveAutosave(
                    draft = draft,
                    page = state.page,
                    lastSavedAtEpochMillis = 0L
                )
                if (signature == lastLocalDraftSignature) return@collectLatest

                delay(450)
                val latest = _uiState.value
                val latestDraft = latest.workingDraft
                if (latestDraft == null || latestDraft.databaseDraftId != null) {
                    return@collectLatest
                }

                val latestSignature = CachedImpactWeaveAutosave(
                    draft = latestDraft,
                    page = latest.page,
                    lastSavedAtEpochMillis = 0L
                )
                if (latestSignature != signature) return@collectLatest

                runCatching {
                    OrganisationLocalStorage.saveImpactWeaveAutosave(
                        latestSignature.copy(
                            lastSavedAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
                lastLocalDraftSignature = latestSignature
            }
        }
    }

    /**
     * Loads the active plans needed by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — loadActivePlans
     *
     * Loads or refreshes the data required by load active plans and writes the result into observable UI state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun loadActivePlans() {
        if (_uiState.value.isLoadingActivePlans) return

        _uiState.value = _uiState.value.copy(
            isLoadingActivePlans = true,
            activePlansError = null
        )

        viewModelScope.launch {
            try {
                val plans = impactWeaveRepository.loadActivePlans()
                _uiState.value = _uiState.value.copy(
                    activePlans = plans,
                    isLoadingActivePlans = false,
                    activePlansError = null
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(
                    isLoadingActivePlans = false,
                    activePlansError = safeDatabaseError(exception.message.orEmpty())
                )
            }
        }
    }

    /**
     * Derives the reopen active plan value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — reopenActivePlan
     *
     * Implements the ViewModel workflow operation for reopen active plan.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun reopenActivePlan(plan: ImpactWeaveActivePlan) {
        if (_uiState.value.isFindingPartners) return

        val now = AppClock.nowMillis()
        val reopenedDraft = ImpactWeaveDraft(
            draftId = nextDraftId++,
            databaseDraftId = plan.draftId,
            persistedStatus = plan.status,
            category = plan.category,
            title = plan.title,
            description = plan.description,
            mode = plan.mode,
            duration = if (plan.startDateMillis == plan.endDateMillis) {
                ImpactWeaveDuration.ONE_DAY
            } else {
                ImpactWeaveDuration.MULTIPLE_DAYS
            },
            startDateMillis = plan.startDateMillis,
            endDateMillis = plan.endDateMillis,
            startTimeMinutes = plan.startTimeMinutes,
            endTimeMinutes = plan.endTimeMinutes,
            areaQuery = plan.areaName,
            areaLocation = plan.areaLocation,
            hasExistingVenue = plan.hasExistingVenue,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.MATCH_RESULTS,
            workingDraft = reopenedDraft,
            isFindingPartners = true,
            matchResults = null,
            matchResultsError = null,
            isSavingPlanChange = false,
            planChangeError = null,
            planChangeSuccess = null
        )

        viewModelScope.launch {
            loadMatchingResults(plan.draftId, reopenedDraft)
        }
    }

    /**
     * Starts the new draft for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — startNewDraft
     *
     * Implements the ViewModel workflow operation for start new draft.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun startNewDraft() {
        val now = AppClock.nowMillis()
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.ACTIVITY_PLAN,
            workingDraft = ImpactWeaveDraft(
                draftId = nextDraftId++,
                createdAtMillis = now,
                updatedAtMillis = now
            ),
            findPartnersError = null,
            matchResults = null,
            matchResultsError = null,
            sentPartnershipOrganisationIds = emptySet(),
            partnershipStates = emptyMap(),
            sendingPartnershipOrganisationId = null,
            partnershipRequestError = null,
            partnershipRequestSuccess = null,
            isSavingPlanChange = false,
            planChangeError = null,
            planChangeSuccess = null
        )
    }

    /**
     * Returns the organisation Impact Weave and partnership flow to the requested step or state.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — returnToList
     *
     * Implements the ViewModel workflow operation for return to list.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun returnToList() {
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.LIST,
            workingDraft = null,
            isFindingPartners = false,
            findPartnersError = null,
            matchResults = null,
            matchResultsError = null,
            sentPartnershipOrganisationIds = emptySet(),
            partnershipStates = emptyMap(),
            sendingPartnershipOrganisationId = null,
            partnershipRequestError = null,
            partnershipRequestSuccess = null,
            isSavingPlanChange = false,
            planChangeError = null,
            planChangeSuccess = null
        )
        loadActivePlans()
    }

    /**
     * Derives the go to activity plan value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — goToActivityPlan
     *
     * Controls workflow/navigation state for go to activity plan while keeping step transitions and
     * confirmation rules in one place.
     *
     * The screen emits the intent, but the ViewModel decides whether the transition is currently valid for the
     * draft/post state.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun goToActivityPlan() {
        if (_uiState.value.workingDraft != null) {
            _uiState.value = _uiState.value.copy(page = ImpactWeavePage.ACTIVITY_PLAN)
        }
    }

    /**
     * Derives the continue to support needed value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — continueToSupportNeeded
     *
     * Controls workflow/navigation state for continue to support needed while keeping step transitions and
     * confirmation rules in one place.
     *
     * The screen emits the intent, but the ViewModel decides whether the transition is currently valid for the
     * draft/post state.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun continueToSupportNeeded(): Boolean {
        if (activityPlanErrors().isNotEmpty()) return false
        _uiState.value = _uiState.value.copy(page = ImpactWeavePage.SUPPORT_NEEDED)
        return true
    }

    /**
     * Derives the continue to review value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — continueToReview
     *
     * Controls workflow/navigation state for continue to review while keeping step transitions and confirmation
     * rules in one place.
     *
     * The screen emits the intent, but the ViewModel decides whether the transition is currently valid for the
     * draft/post state.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun continueToReview(): Boolean {
        val draft = _uiState.value.workingDraft ?: return false
        if (draft.needs.isEmpty()) return false
        if (draft.hasExistingVenue == false && draft.needs.none { it.supportType == "VENUE" }) {
            return false
        }
        if (firstIncompleteNeed(draft) != null) return false

        _uiState.value = _uiState.value.copy(page = ImpactWeavePage.REVIEW)
        return true
    }

    /**
     * Derives the go back from review value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — goBackFromReview
     *
     * Controls workflow/navigation state for go back from review while keeping step transitions and
     * confirmation rules in one place.
     *
     * The screen emits the intent, but the ViewModel decides whether the transition is currently valid for the
     * draft/post state.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun goBackFromReview() {
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.SUPPORT_NEEDED,
            findPartnersError = null
        )
    }

    /**
     * Returns the partners used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — findPartners
     *
     * Implements the ViewModel workflow operation for find partners.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Coordinates account-scoped local persistence only for recoverable/cached UI state; published or
     * transactional business state continues to come from Supabase.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun findPartners() {
        if (_uiState.value.isFindingPartners) return

        val draft = _uiState.value.workingDraft ?: return
        if (activityPlanErrors().isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                findPartnersError = "Review the activity details before finding partners."
            )
            return
        }
        if (draft.needs.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                findPartnersError = "Add at least one support need before finding partners."
            )
            return
        }
        if (draft.hasExistingVenue == false && draft.needs.none { it.supportType == "VENUE" }) {
            _uiState.value = _uiState.value.copy(
                findPartnersError = "Add a venue requirement before finding partners."
            )
            return
        }

        firstIncompleteNeed(draft)?.let { need ->
            _uiState.value = _uiState.value.copy(
                findPartnersError = if (need.supportType == "VENUE") {
                    "Check the venue requirement before finding partners."
                } else {
                    "Add a quantity for ${need.resourceName.ifBlank { supportTypeLabelForError(need.supportType) }} before finding partners."
                }
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isFindingPartners = true,
            findPartnersError = null,
            matchResults = null,
            matchResultsError = null
        )

        viewModelScope.launch {
            try {
                // A plan is persisted only here. It enters MATCHING immediately; there
                // is no user-facing Save Draft stage anymore.
                val started = impactWeaveRepository.startMatching(draft)
                val persistedDraft = draft.copy(
                    databaseDraftId = started.draftId,
                    persistedStatus = "MATCHING",
                    updatedAtMillis = AppClock.nowMillis()
                )

                // The plan now has a real Supabase draft_id and partnership
                // lifecycle. Remove the device-only form to avoid two competing
                // copies of the same Impact Weave plan.
                runCatching { OrganisationLocalStorage.clearImpactWeaveAutosave() }
                lastLocalDraftSignature = null

                _uiState.value = _uiState.value.copy(
                    page = ImpactWeavePage.MATCH_RESULTS,
                    workingDraft = persistedDraft,
                    isFindingPartners = true,
                    findPartnersError = null,
                    matchResults = null,
                    matchResultsError = null
                )

                loadMatchingResults(started.draftId, persistedDraft)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(
                    isFindingPartners = false,
                    findPartnersError = friendlyMatchingError(exception)
                )
            }
        }
    }

    /**
     * Retries the current operation in the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — retryMatchingResults
     *
     * Loads or refreshes the data required by retry matching results and writes the result into observable UI
     * state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun retryMatchingResults() {
        if (_uiState.value.isFindingPartners) return
        val draft = _uiState.value.workingDraft ?: return
        val draftId = draft.databaseDraftId ?: return

        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.MATCH_RESULTS,
            isFindingPartners = true,
            matchResultsError = null
        )

        viewModelScope.launch {
            loadMatchingResults(draftId, draft)
        }
    }

    /**
     * Loads the matching results needed by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — loadMatchingResults
     *
     * Loads or refreshes the data required by load matching results and writes the result into observable UI
     * state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private suspend fun loadMatchingResults(
        draftId: String,
        draft: ImpactWeaveDraft
    ) {
        try {
            val rawInput = impactWeaveRepository.loadMatchingInput(draftId)
            val input = rawInput.copy(
                candidates = rawInput.candidates.map { candidate ->
                    candidate.copy(
                        distanceKm = distanceKmFromActivity(draft, candidate)
                    )
                }
            )

            val semanticMatches = if (input.candidates.isEmpty()) {
                emptyList()
            } else {
                try {
                    matchingService.rankImpactWeaveCandidates(
                        needs = input.needs.map { need ->
                            GroqImpactWeaveNeed(
                                needId = need.needId,
                                supportType = need.supportType,
                                resourceName = need.resourceName,
                                originalText = need.originalText
                            )
                        },
                        candidates = input.candidates.map { candidate ->
                            GroqImpactWeaveCandidate(
                                supportId = candidate.supportId,
                                supportType = candidate.supportType,
                                resourceName = candidate.resourceName,
                                supportDescription = candidate.supportDescription
                            )
                        }
                    )
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    // Matching must remain usable if Groq is temporarily unavailable.
                    // This conservative fallback accepts only clear name equivalence;
                    // it never invents candidates or quantities.
                    fallbackSemanticMatches(input)
                }
            }

            val partnershipStates = loadPartnershipStates(draftId)

            _uiState.value = _uiState.value.copy(
                isFindingPartners = false,
                matchResults = buildMatchResults(draftId, input, semanticMatches),
                matchResultsError = null,
                sentPartnershipOrganisationIds = partnershipStates.keys,
                partnershipStates = partnershipStates
            )
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            _uiState.value = _uiState.value.copy(
                isFindingPartners = false,
                matchResultsError = friendlyMatchingError(exception)
            )
        }
    }

    /**
     * Loads the partnership states needed by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — loadPartnershipStates
     *
     * Loads or refreshes the data required by load partnership states and writes the result into observable UI
     * state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private suspend fun loadPartnershipStates(
        draftId: String
    ): Map<String, ImpactWeavePartnershipState> {
        return runCatching {
            partnershipRepository.loadImpactWeavePartnershipStates(draftId)
                .associateBy { it.organisationId }
        }.getOrElse {
            // Backward-safe fallback: still prevent duplicate requests if the richer
            // read RPC has not been applied yet. Item details appear after the SQL
            // migration is installed.
            runCatching {
                partnershipRepository.loadInvitations()
                    .filter { it.direction == "SENT" && it.draftId == draftId }
                    .associate { invitation ->
                        invitation.otherOrganisationId to ImpactWeavePartnershipState(
                            invitationId = invitation.invitationId,
                            organisationId = invitation.otherOrganisationId,
                            organisationName = invitation.otherOrganisationName,
                            status = invitation.status,
                            revisionNumber = invitation.revisionNumber
                        )
                    }
            }.getOrDefault(emptyMap())
        }
    }

    /**
     * Refreshes confirmed quantities and live invitation states without rerunning Groq.
     * This is used when returning from partnership chat after Accept / Decline.
     */
    /**
     * DETAILED BEHAVIOUR — refreshCurrentMatchState
     *
     * Loads or refreshes the data required by refresh current match state and writes the result into observable
     * UI state.
     *
     * The coroutine/repository boundary is handled here so Compose only reacts to loading, success and error
     * state.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun refreshCurrentMatchState() {
        val snapshot = _uiState.value
        if (snapshot.page != ImpactWeavePage.MATCH_RESULTS || snapshot.isFindingPartners) return

        val draftId = snapshot.workingDraft?.databaseDraftId ?: return
        val currentResults = snapshot.matchResults ?: return

        viewModelScope.launch {
            try {
                val latestInput = impactWeaveRepository.loadMatchingInput(draftId)
                val latestNeeds = latestInput.needs.associateBy { it.needId }
                val partnershipStates = loadPartnershipStates(draftId)

                val refreshedResults = currentResults.copy(
                    needResults = currentResults.needResults.map { result ->
                        val latestNeed = latestNeeds[result.need.needId]
                        if (latestNeed == null) result else result.copy(need = latestNeed)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    matchResults = refreshedResults,
                    sentPartnershipOrganisationIds = partnershipStates.keys,
                    partnershipStates = partnershipStates,
                    matchResultsError = null
                )

                runCatching { impactWeaveRepository.loadActivePlans() }
                    .onSuccess { plans ->
                        val latestStatus = plans.firstOrNull { it.draftId == draftId }?.status
                        _uiState.value = _uiState.value.copy(
                            activePlans = plans,
                            workingDraft = _uiState.value.workingDraft?.copy(
                                persistedStatus = latestStatus
                                    ?: _uiState.value.workingDraft?.persistedStatus
                            )
                        )
                    }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(
                    matchResultsError = friendlyMatchingError(exception)
                )
            }
        }
    }

    /**
     * Builds the match results used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildMatchResults
     *
     * Implements the ViewModel workflow operation for build match results.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun buildMatchResults(
        draftId: String,
        input: ImpactWeaveMatchingInput,
        semanticMatches: List<ImpactWeaveSemanticMatch>
    ): ImpactWeaveMatchResults {
        val levelByPair = mutableMapOf<Pair<String, String>, String>()
        semanticMatches.forEach { match ->
            val key = match.needId to match.supportId
            if (levelByPair[key] != "DIRECT") {
                levelByPair[key] = match.level
            }
        }

        val needResults = input.needs.map { need ->
            val candidates = input.candidates
            val directSemantic = candidates.filter {
                levelByPair[need.needId to it.supportId] == "DIRECT"
            }
            val semanticAlternatives = candidates.filter {
                levelByPair[need.needId to it.supportId] == "ALTERNATIVE"
            }

            if (need.supportType == "VENUE") {
                buildVenueMatchResult(
                    need = need,
                    directSemantic = directSemantic,
                    semanticAlternatives = semanticAlternatives
                )
            } else {
                buildQuantityMatchResult(
                    need = need,
                    directSemantic = directSemantic,
                    semanticAlternatives = semanticAlternatives
                )
            }
        }

        val overall = if (needResults.isEmpty()) {
            0f
        } else {
            needResults.map { it.potentialFraction }.average().toFloat().coerceIn(0f, 1f)
        }

        return ImpactWeaveMatchResults(
            draftId = draftId,
            needResults = needResults,
            overallPotentialFraction = overall
        )
    }

    /**
     * Builds the venue match result used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildVenueMatchResult
     *
     * Implements the ViewModel workflow operation for build venue match result.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun buildVenueMatchResult(
        need: ImpactWeaveDatabaseNeed,
        directSemantic: List<ImpactWeaveSupportCandidate>,
        semanticAlternatives: List<ImpactWeaveSupportCandidate>
    ): ImpactWeaveNeedMatchResult {
        val requiredCapacity = need.capacityRequired
        val suitable = directSemantic.filter { candidate ->
            requiredCapacity == null ||
                (candidate.capacity != null && candidate.capacity >= requiredCapacity)
        }

        val localSuitable = suitable.filter { candidate ->
            candidate.distanceKm?.let { it <= LOCAL_VENUE_RADIUS_KM } == true
        }
        val selectedSuitable = (if (localSuitable.isNotEmpty()) localSuitable else suitable)
            .sortedWith(candidateDistanceComparator())

        // Direct venue types with unknown/insufficient capacity remain useful to show,
        // but do not count toward a known capacity target.
        val capacityAlternatives = directSemantic.filterNot { it in suitable }
        val alternatives = (semanticAlternatives + capacityAlternatives)
            .distinctBy { it.supportId }
            .sortedWith(candidateDistanceComparator())

        return ImpactWeaveNeedMatchResult(
            need = need,
            directMatches = selectedSuitable,
            alternativeMatches = alternatives,
            potentialFraction = if (suitable.isNotEmpty()) 1f else 0f,
            potentialCoveredAmount = requiredCapacity?.takeIf { suitable.isNotEmpty() },
            usesWiderVenueArea = suitable.isNotEmpty() && localSuitable.isEmpty()
        )
    }

    /**
     * Builds the quantity match result used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — buildQuantityMatchResult
     *
     * Implements the ViewModel workflow operation for build quantity match result.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun buildQuantityMatchResult(
        need: ImpactWeaveDatabaseNeed,
        directSemantic: List<ImpactWeaveSupportCandidate>,
        semanticAlternatives: List<ImpactWeaveSupportCandidate>
    ): ImpactWeaveNeedMatchResult {
        val required = need.quantityRequired ?: 0
        val direct = directSemantic
            .filter { (it.quantity ?: 0) > 0 }
            .distinctBy { it.supportId }
            .sortedWith(candidateDistanceComparator())
        val available = direct.sumOf { it.quantity ?: 0 }
        val covered = if (required > 0) available.coerceAtMost(required) else 0
        val fraction = if (required > 0) {
            covered.toFloat() / required.toFloat()
        } else {
            0f
        }

        return ImpactWeaveNeedMatchResult(
            need = need,
            directMatches = direct,
            alternativeMatches = semanticAlternatives
                .distinctBy { it.supportId }
                .sortedWith(candidateDistanceComparator()),
            potentialFraction = fraction.coerceIn(0f, 1f),
            potentialCoveredAmount = covered
        )
    }

    /**
     * Derives the fallback semantic matches value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — fallbackSemanticMatches
     *
     * Implements the ViewModel workflow operation for fallback semantic matches.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun fallbackSemanticMatches(
        input: ImpactWeaveMatchingInput
    ): List<ImpactWeaveSemanticMatch> {
        return input.needs.flatMap { need ->
            input.candidates.mapNotNull { candidate ->
                val level = fallbackMatchLevel(need.resourceName, candidate.resourceName)
                if (level == "NONE") null else ImpactWeaveSemanticMatch(
                    needId = need.needId,
                    supportId = candidate.supportId,
                    level = level
                )
            }
        }
    }

    /**
     * Derives the fallback match level value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — fallbackMatchLevel
     *
     * Implements the ViewModel workflow operation for fallback match level.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun fallbackMatchLevel(needName: String, candidateName: String): String {
        val need = matchingTokens(needName)
        val candidate = matchingTokens(candidateName)
        if (need.isEmpty() || candidate.isEmpty()) return "NONE"
        if (need == candidate || need.containsAll(candidate) || candidate.containsAll(need)) {
            return "DIRECT"
        }
        return if (need.intersect(candidate).isNotEmpty()) "ALTERNATIVE" else "NONE"
    }

    /**
     * Derives the matching tokens value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — matchingTokens
     *
     * Implements the ViewModel workflow operation for matching tokens.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun matchingTokens(value: String): Set<String> = value
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 2 }
        .toSet()

    /**
     * Derives the distance km from activity value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — distanceKmFromActivity
     *
     * Implements the ViewModel workflow operation for distance km from activity.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun distanceKmFromActivity(
        draft: ImpactWeaveDraft,
        candidate: ImpactWeaveSupportCandidate
    ): Double? {
        val area = draft.areaLocation ?: return null
        val candidateLat = candidate.latitude ?: return null
        val candidateLong = candidate.longitude ?: return null
        return haversineKm(
            area.latitude,
            area.longitude,
            candidateLat,
            candidateLong
        )
    }

    /**
     * Derives the haversine km value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — haversineKm
     *
     * Implements the ViewModel workflow operation for haversine km.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun haversineKm(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = Math.toRadians(latitude2 - latitude1)
        val deltaLong = Math.toRadians(longitude2 - longitude1)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) *
            sin(deltaLong / 2) * sin(deltaLong / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Checks whether the organisation Impact Weave and partnership flow allows idate distance comparator.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — candidateDistanceComparator
     *
     * Implements the ViewModel workflow operation for candidate distance comparator.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun candidateDistanceComparator() =
        compareBy<ImpactWeaveSupportCandidate> { it.distanceKm ?: Double.MAX_VALUE }
            .thenBy { it.organisationName.lowercase() }

    /**
     * Updates the category used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateCategory
     *
     * Receives the UI event for changing category and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateCategory(value: VolunteerPostCategory) = updateDraft { copy(category = value) }

    /**
     * Updates the title used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateTitle
     *
     * Receives the UI event for changing title and applies it through the ViewModel instead of mutating Compose
     * state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateTitle(value: String) = updateDraft { copy(title = value) }

    /**
     * Updates the description used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateDescription
     *
     * Receives the UI event for changing description and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateDescription(value: String) = updateDraft { copy(description = value) }

    /**
     * Updates the mode used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateMode
     *
     * Receives the UI event for changing mode and applies it through the ViewModel instead of mutating Compose
     * state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateMode(value: ImpactWeaveMode) = updateDraft { copy(mode = value) }

    /**
     * Updates the duration used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateDuration
     *
     * Receives the UI event for changing duration and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateDuration(value: ImpactWeaveDuration) = updateDraft {
        when (value) {
            ImpactWeaveDuration.ONE_DAY -> copy(
                duration = value,
                endDateMillis = startDateMillis
            )

            ImpactWeaveDuration.MULTIPLE_DAYS -> copy(
                duration = value,
                endDateMillis = endDateMillis?.takeIf { end ->
                    startDateMillis == null || end >= startDateMillis
                }
            )
        }
    }

    /**
     * Updates the start date used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateStartDate
     *
     * Receives the UI event for changing start date and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateStartDate(value: Long) = updateDraft {
        when (duration) {
            ImpactWeaveDuration.ONE_DAY -> copy(
                startDateMillis = value,
                endDateMillis = value
            )

            ImpactWeaveDuration.MULTIPLE_DAYS -> copy(
                startDateMillis = value,
                endDateMillis = endDateMillis?.takeIf { it >= value }
            )
        }
    }

    /**
     * Updates the end date used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateEndDate
     *
     * Receives the UI event for changing end date and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateEndDate(value: Long) = updateDraft {
        if (duration == ImpactWeaveDuration.MULTIPLE_DAYS) {
            copy(endDateMillis = value)
        } else {
            this
        }
    }

    /**
     * Matches Create Post: changing the start time clears an end time that is
     * no longer valid instead of leaving an impossible schedule in the draft.
     */
    /**
     * DETAILED BEHAVIOUR — updateStartTime
     *
     * Receives the UI event for changing start time and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateStartTime(hour24: Int, minute: Int): String? {
        val startMinutes = hour24 * 60 + minute
        updateDraft {
            copy(
                startTimeMinutes = startMinutes,
                endTimeMinutes = endTimeMinutes?.takeIf { it > startMinutes }
            )
        }
        return null
    }

    /**
     * Invalid end times are rejected inside the time dialog, the same way the
     * Create Post Physical schedule behaves.
     */
    /**
     * DETAILED BEHAVIOUR — updateEndTime
     *
     * Receives the UI event for changing end time and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun updateEndTime(hour24: Int, minute: Int): String? {
        val endMinutes = hour24 * 60 + minute
        val startMinutes = _uiState.value.workingDraft?.startTimeMinutes

        if (startMinutes != null && endMinutes <= startMinutes) {
            return "End time must be later than the start time."
        }

        updateDraft { copy(endTimeMinutes = endMinutes) }
        return null
    }

    /**
     * Updates the area query used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateAreaQuery
     *
     * Receives the UI event for changing area query and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateAreaQuery(value: String) = updateDraft {
        copy(
            areaQuery = value,
            areaLocation = null
        )
    }

    /**
     * Selects the area used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — selectArea
     *
     * Implements the ViewModel workflow operation for select area.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    fun selectArea(location: LocationSuggestion) = updateDraft {
        val area = location.asGeneralArea()
        copy(
            areaQuery = area.generalAreaName,
            areaLocation = area
        )
    }

    /**
     * Clears the area for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — clearArea
     *
     * Implements the ViewModel workflow operation for clear area.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun clearArea() = updateDraft {
        copy(
            areaQuery = "",
            areaLocation = null
        )
    }

    /**
     * Updates the has existing venue used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateHasExistingVenue
     *
     * Receives the UI event for changing has existing venue and applies it through the ViewModel instead of
     * mutating Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateHasExistingVenue(value: Boolean) = updateDraft {
        if (value) {
            copy(
                hasExistingVenue = true,
                areaQuery = "",
                areaLocation = null
            )
        } else {
            copy(
                hasExistingVenue = false,
                venueQuery = "",
                existingVenueLocation = null
            )
        }
    }

    /**
     * Updates the venue query used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateVenueQuery
     *
     * Receives the UI event for changing venue query and applies it through the ViewModel instead of mutating
     * Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateVenueQuery(value: String) = updateDraft {
        copy(
            venueQuery = value,
            existingVenueLocation = null
        )
    }

    /**
     * Selects the venue used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — selectVenue
     *
     * Implements the ViewModel workflow operation for select venue.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Works with structured location suggestions/coordinates so free-text search is separated from the final
     * location fields saved with the post/plan.
     */
    fun selectVenue(location: LocationSuggestion) = updateDraft {
        val area = location.asGeneralArea()
        copy(
            venueQuery = location.displayName,
            existingVenueLocation = location,
            areaQuery = area.generalAreaName,
            areaLocation = area
        )
    }

    /**
     * Clears the venue for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — clearVenue
     *
     * Implements the ViewModel workflow operation for clear venue.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun clearVenue() = updateDraft {
        copy(
            venueQuery = "",
            existingVenueLocation = null,
            areaQuery = "",
            areaLocation = null
        )
    }

    /**
     * Adds the need to the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — addNeed
     *
     * Implements the ViewModel workflow operation for add need.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun addNeed(
        originalText: String,
        supportType: String,
        resourceName: String,
        amount: Int?
    ) {
        val normalizedSupportType = supportType.trim().uppercase()
        val need = ImpactWeaveNeedDraft(
            needId = nextNeedId++,
            originalText = originalText.trim(),
            supportType = normalizedSupportType,
            resourceName = resourceName.trim(),
            quantityRequired = if (normalizedSupportType == "VENUE") null else amount,
            capacityRequired = if (normalizedSupportType == "VENUE") amount else null
        )

        updateDraft {
            copy(needs = needs + need)
        }
    }

    /**
     * Updates the need used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateNeed
     *
     * Receives the UI event for changing need and applies it through the ViewModel instead of mutating Compose
     * state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     */
    fun updateNeed(
        needId: Int,
        originalText: String,
        supportType: String,
        resourceName: String,
        amount: Int?
    ) {
        val normalizedSupportType = supportType.trim().uppercase()
        updateDraft {
            copy(
                needs = needs.map { need ->
                    if (need.needId != needId) {
                        need
                    } else {
                        need.copy(
                            originalText = originalText.trim(),
                            supportType = normalizedSupportType,
                            resourceName = resourceName.trim(),
                            quantityRequired = if (normalizedSupportType == "VENUE") null else amount,
                            capacityRequired = if (normalizedSupportType == "VENUE") amount else null
                        )
                    }
                }
            )
        }
    }

    /**
     * Removes the need from the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — removeNeed
     *
     * Implements the ViewModel workflow operation for remove need.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun removeNeed(needId: Int) {
        updateDraft {
            copy(needs = needs.filterNot { it.needId == needId })
        }
    }

    /**
     * Derives the activity plan errors value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — activityPlanErrors
     *
     * Implements the ViewModel workflow operation for activity plan errors.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun activityPlanErrors(): Map<String, String> {
        val draft = _uiState.value.workingDraft ?: return mapOf("draft" to "Draft is unavailable.")
        val errors = linkedMapOf<String, String>()
        val minimumStartDate = minimumImpactWeaveStartDateMillis()

        if (draft.category == null) {
            errors["category"] = "Select an activity category."
        }
        if (draft.title.trim().length < 3) {
            errors["title"] = "Enter an activity title."
        }
        if (draft.description.isBlank()) {
            errors["description"] = "Enter an activity description."
        }
        if (draft.mode == null) {
            errors["mode"] = "Choose Physical or Hybrid."
        }
        if (draft.startDateMillis == null) {
            errors["startDate"] = "Choose the activity date."
        } else if (draft.startDateMillis < minimumStartDate) {
            errors["startDate"] = "Impact Weave activities must start at least 10 days from today."
        }
        if (draft.duration == ImpactWeaveDuration.MULTIPLE_DAYS) {
            if (draft.endDateMillis == null) {
                errors["endDate"] = "Choose the activity end date."
            } else if (
                draft.startDateMillis != null &&
                draft.endDateMillis <= draft.startDateMillis
            ) {
                errors["endDate"] = "Multiple-day activities must end after the start date."
            }
        }
        if (draft.startTimeMinutes == null) {
            errors["startTime"] = "Set the activity start time."
        }
        if (draft.endTimeMinutes == null) {
            errors["endTime"] = "Set the activity end time."
        }
        if (
            draft.duration == ImpactWeaveDuration.ONE_DAY &&
            draft.startTimeMinutes != null &&
            draft.endTimeMinutes != null &&
            draft.endTimeMinutes <= draft.startTimeMinutes
        ) {
            errors["endTime"] = "End time must be after the start time."
        }
        if (draft.hasExistingVenue == null) {
            errors["hasVenue"] = "Choose whether you already have a venue."
        }
        if (draft.hasExistingVenue == true && draft.existingVenueLocation == null) {
            errors["venue"] = "Select the existing venue from the suggestions."
        }
        if (draft.hasExistingVenue == false && draft.areaLocation == null) {
            errors["area"] = "Select a preferred general area from the suggestions."
        }

        return errors
    }

    /**
     * Returns the minimum impact weave start date millis value required by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — minimumImpactWeaveStartDateMillis
     *
     * Implements the ViewModel workflow operation for minimum impact weave start date millis.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     */
    fun minimumImpactWeaveStartDateMillis(): Long {
        return startOfLocalDay(AppClock.nowMillis()).let { today ->
            Calendar.getInstance().apply {
                timeInMillis = today
                add(Calendar.DAY_OF_YEAR, 10)
            }.timeInMillis
        }
    }

    /**
     * Returns the partnership planning deadline millis value required by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — partnershipPlanningDeadlineMillis
     *
     * Implements the ViewModel workflow operation for partnership planning deadline millis.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    fun partnershipPlanningDeadlineMillis(startDateMillis: Long?): Long? {
        if (startDateMillis == null) return null
        return Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
    }

    /**
     * Sends the partnership request for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — sendPartnershipRequest
     *
     * Implements the ViewModel workflow operation for send partnership request.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun sendPartnershipRequest(
        organisationId: String,
        organisationName: String,
        items: List<PartnershipRequestItem>
    ) {
        val draftId = _uiState.value.workingDraft?.databaseDraftId ?: return
        if (_uiState.value.sendingPartnershipOrganisationId != null) return
        if (organisationId in _uiState.value.sentPartnershipOrganisationIds) return

        _uiState.value = _uiState.value.copy(
            sendingPartnershipOrganisationId = organisationId,
            partnershipRequestError = null,
            partnershipRequestSuccess = null
        )

        viewModelScope.launch {
            try {
                partnershipRepository.sendPartnershipRequest(
                    draftId = draftId,
                    receiverOrganisationId = organisationId,
                    items = items
                )

                val partnershipStates = loadPartnershipStates(draftId)

                _uiState.value = _uiState.value.copy(
                    sendingPartnershipOrganisationId = null,
                    sentPartnershipOrganisationIds = partnershipStates.keys,
                    partnershipStates = partnershipStates,
                    partnershipRequestError = null,
                    partnershipRequestSuccess =
                        "Partnership request sent to $organisationName."
                )

                // The first request changes MATCHING -> WAITING in Supabase. Refresh the
                // landing data quietly so the status is correct when the user goes back.
                runCatching { impactWeaveRepository.loadActivePlans() }
                    .onSuccess { plans ->
                        _uiState.value = _uiState.value.copy(activePlans = plans)
                    }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(
                    sendingPartnershipOrganisationId = null,
                    partnershipRequestError = safePartnershipRequestError(exception.message.orEmpty()),
                    partnershipRequestSuccess = null
                )
            }
        }
    }

    /**
     * Clears the partnership request feedback for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — clearPartnershipRequestFeedback
     *
     * Implements the ViewModel workflow operation for clear partnership request feedback.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun clearPartnershipRequestFeedback() {
        _uiState.value = _uiState.value.copy(
            partnershipRequestError = null,
            partnershipRequestSuccess = null
        )
    }

    /**
     * Updates the active plan details used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateActivePlanDetails
     *
     * Receives the UI event for changing active plan details and applies it through the ViewModel instead of
     * mutating Compose state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun updateActivePlanDetails(
        category: VolunteerPostCategory,
        title: String,
        description: String
    ) {
        val draft = _uiState.value.workingDraft ?: return
        val draftId = draft.databaseDraftId ?: return
        if (_uiState.value.isSavingPlanChange) return
        if (title.trim().length < 3 || description.isBlank()) {
            _uiState.value = _uiState.value.copy(
                planChangeError = "Enter a title and activity description before saving."
            )
            return
        }

        runPlanChange {
            impactWeaveRepository.updateBasicDetails(
                draftId = draftId,
                title = title,
                category = category.databaseValue,
                description = description
            )
            _uiState.value = _uiState.value.copy(
                workingDraft = draft.copy(
                    category = category,
                    title = title.trim(),
                    description = description.trim(),
                    updatedAtMillis = AppClock.nowMillis()
                ),
                planChangeSuccess = "Activity details updated. Existing support remains confirmed."
            )
        }
    }

    /**
     * Derives the reschedule active plan value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — rescheduleActivePlan
     *
     * Implements the ViewModel workflow operation for reschedule active plan.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun rescheduleActivePlan(
        startDateMillis: Long,
        endDateMillis: Long,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    ) {
        val draft = _uiState.value.workingDraft ?: return
        val draftId = draft.databaseDraftId ?: return
        if (_uiState.value.isSavingPlanChange) return
        if (draft.startDateMillis == startDateMillis &&
            draft.endDateMillis == endDateMillis &&
            draft.startTimeMinutes == startTimeMinutes &&
            draft.endTimeMinutes == endTimeMinutes
        ) {
            _uiState.value = _uiState.value.copy(
                planChangeError = "Choose a different date or time before updating."
            )
            return
        }
        if (startDateMillis < minimumImpactWeaveStartDateMillis()) {
            _uiState.value = _uiState.value.copy(
                planChangeError = "The new start date must be at least 10 days from today."
            )
            return
        }
        if (endDateMillis < startDateMillis ||
            (endDateMillis == startDateMillis && endTimeMinutes <= startTimeMinutes)
        ) {
            _uiState.value = _uiState.value.copy(
                planChangeError = "The activity must end after it starts."
            )
            return
        }

        runPlanChange {
            impactWeaveRepository.reschedule(
                draftId,
                startDateMillis,
                endDateMillis,
                startTimeMinutes,
                endTimeMinutes
            )
            _uiState.value = _uiState.value.copy(
                workingDraft = draft.copy(
                    duration = if (startDateMillis == endDateMillis) {
                        ImpactWeaveDuration.ONE_DAY
                    } else {
                        ImpactWeaveDuration.MULTIPLE_DAYS
                    },
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes,
                    persistedStatus = "WAITING",
                    updatedAtMillis = AppClock.nowMillis()
                ),
                planChangeSuccess = "Schedule updated. Accepted partners must reconfirm before their support counts again."
            )
            refreshCurrentMatchState()
        }
    }

    /**
     * Derives the dispose active plan value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — disposeActivePlan
     *
     * Implements the ViewModel workflow operation for dispose active plan.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    fun disposeActivePlan() {
        val draft = _uiState.value.workingDraft ?: return
        val draftId = draft.databaseDraftId ?: return
        if (_uiState.value.isSavingPlanChange) return
        runPlanChange {
            impactWeaveRepository.dispose(draftId)
            _uiState.value = _uiState.value.copy(
                workingDraft = draft.copy(
                    persistedStatus = "DISPOSED",
                    updatedAtMillis = AppClock.nowMillis()
                ),
                planChangeSuccess = "Plan disposed. Its details and conversation history remain available as read-only history."
            )
            runCatching { impactWeaveRepository.loadActivePlans() }
                .onSuccess { plans ->
                    _uiState.value = _uiState.value.copy(activePlans = plans)
                }
        }
    }

    /**
     * Clears the plan change feedback for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — clearPlanChangeFeedback
     *
     * Implements the ViewModel workflow operation for clear plan change feedback.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    fun clearPlanChangeFeedback() {
        _uiState.value = _uiState.value.copy(
            planChangeError = null,
            planChangeSuccess = null
        )
    }

    /**
     * Derives the run plan change value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — runPlanChange
     *
     * Implements the ViewModel workflow operation for run plan change.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     *
     * Runs asynchronous work in a lifecycle-aware coroutine and exposes progress/error state rather than
     * blocking the UI thread.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     *
     * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without
     * leaving the UI in an assumed-success state.
     */
    private fun runPlanChange(action: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(
            isSavingPlanChange = true,
            planChangeError = null,
            planChangeSuccess = null
        )
        viewModelScope.launch {
            try {
                action()
                _uiState.value = _uiState.value.copy(isSavingPlanChange = false)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.value = _uiState.value.copy(
                    isSavingPlanChange = false,
                    planChangeError = safeDatabaseError(exception.message.orEmpty())
                )
            }
        }
    }

    /**
     * Derives the first incomplete need value used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — firstIncompleteNeed
     *
     * Implements the ViewModel workflow operation for first incomplete need.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun firstIncompleteNeed(draft: ImpactWeaveDraft): ImpactWeaveNeedDraft? =
        draft.needs.firstOrNull { need ->
            when (need.supportType.trim().uppercase()) {
                "VENUE" -> need.capacityRequired != null && need.capacityRequired <= 0
                else -> need.quantityRequired == null || need.quantityRequired <= 0
            }
        }

    /**
     * Returns the support type label for error used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — supportTypeLabelForError
     *
     * Implements the ViewModel workflow operation for support type label for error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun supportTypeLabelForError(supportType: String): String =
        supportType.lowercase().replaceFirstChar { it.titlecase() }

    /**
     * Returns the friendly matching error used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — friendlyMatchingError
     *
     * Implements the ViewModel workflow operation for friendly matching error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun friendlyMatchingError(exception: Exception): String {
        val message = exception.message.orEmpty()
        return when {
            message.contains("at least 10 days", ignoreCase = true) ->
                "The activity must start at least 10 days from today."
            message.contains("venue requirement", ignoreCase = true) ->
                "Add a venue requirement before finding partners."
            message.contains("must be verified", ignoreCase = true) ->
                "Your organisation must be verified before finding partners."
            message.contains("401", ignoreCase = true) ||
                message.contains("JWT", ignoreCase = true) ||
                message.contains("token", ignoreCase = true) ->
                "Your session could not be verified. Sign in again and try Find Partners."
            else -> safeDatabaseError(message)
        }
    }

    /**
     * Returns the safe partnership request error used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — safePartnershipRequestError
     *
     * Implements the ViewModel workflow operation for safe partnership request error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun safePartnershipRequestError(rawMessage: String): String {
        val safe = safeDatabaseError(rawMessage)
        return if (safe.startsWith("Unable to start Impact Weave", ignoreCase = true)) {
            "Unable to send the partnership request right now. Please try again."
        } else {
            safe
        }
    }

    /**
     * Keep request headers/tokens out of the UI, but preserve the useful first
     * PostgREST/PostgreSQL error line so matching failures are actually diagnosable.
     */
    /**
     * DETAILED BEHAVIOUR — safeDatabaseError
     *
     * Implements the ViewModel workflow operation for safe database error.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun safeDatabaseError(rawMessage: String): String {
        val safeHead = rawMessage
            .substringBefore("\nCode:")
            .substringBefore("\nHint:")
            .substringBefore("\nDetails:")
            .substringBefore("\nURL:")
            .substringBefore("\nHeaders:")
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        val containsSecret = safeHead.contains("Bearer ", ignoreCase = true) ||
            safeHead.contains("apikey", ignoreCase = true) ||
            safeHead.contains("Authorization", ignoreCase = true) ||
            safeHead.contains("https://", ignoreCase = true)

        return if (safeHead.isNotBlank() && !containsSecret) {
            safeHead.take(220)
        } else {
            "Unable to start Impact Weave matching right now. Please try again."
        }
    }

    /**
     * Updates the draft used by the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — updateDraft
     *
     * Receives the UI event for changing draft and applies it through the ViewModel instead of mutating Compose
     * state inside the screen.
     *
     * Centralising the mutation here allows dependent validation, mode-specific cleanup and navigation rules to
     * run together with the value change.
     *
     * Uses AppClock for business-date/time decisions so the same code works with the project test clock and
     * normal device time.
     *
     * Updates observable state immutably so Compose recomposes from one explicit source of truth.
     */
    private fun updateDraft(change: ImpactWeaveDraft.() -> ImpactWeaveDraft) {
        val draft = _uiState.value.workingDraft ?: return
        _uiState.value = _uiState.value.copy(
            workingDraft = draft.change().copy(updatedAtMillis = AppClock.nowMillis()),
            findPartnersError = null
        )
    }

    /**
     * Starts the of local day for the organisation Impact Weave and partnership flow.
     * The ViewModel updates observable UI state so Compose can react without managing repository details directly.
     */
    /**
     * DETAILED BEHAVIOUR — startOfLocalDay
     *
     * Implements the ViewModel workflow operation for start of local day.
     *
     * It translates screen intent into immutable UI-state changes and/or repository work so presentation code
     * stays free of backend/business decisions.
     */
    private fun startOfLocalDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val LOCAL_VENUE_RADIUS_KM = 25.0
    }
}
