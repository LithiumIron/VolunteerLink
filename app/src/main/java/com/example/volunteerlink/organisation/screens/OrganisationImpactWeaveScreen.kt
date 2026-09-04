package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Hosts the Organisation Impact Weave planning/matching workflow and binds its screens to ImpactWeaveViewModel.
//
// Early planning input can be autosaved locally, but once a plan enters server-backed partnership matching the
// database status becomes authoritative.
//
// The screen handles presentation and navigation between planning, matching, history and Create Post conversion;
// partnership state changes remain repository/RPC operations.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.impactweave.ImpactWeaveViewModel
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePage

@Composable
/**
 * Renders the organisation impact weave screen screen used in the organisation Impact Weave and partnership flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — OrganisationImpactWeaveScreen
 *
 * Renders the Organisation Impact Weave screen from state supplied by the owning ViewModel/repository-facing
 * coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationImpactWeaveScreen(
    onBack: () -> Unit,
    onCreatePost: (String) -> Unit,
    onViewOrganisationProfile: (String) -> Unit = {},
    viewModel: ImpactWeaveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        viewModel.minimumImpactWeaveStartDateMillis()
    }
    val minimumPostDateMillis = remember(clockState.refreshVersion) {
        CreatePostValidator.minimumStartDateMillis()
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, uiState.page) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.page == ImpactWeavePage.MATCH_RESULTS) {
                viewModel.refreshCurrentMatchState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadActivePlans()
    }

    /**
     * Handles the back in the organisation Impact Weave and partnership flow.
     * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
     */
    /**
     * DETAILED BEHAVIOUR — handleBack
     *
     * Handles the Compose/UI responsibility for handle back.
     *
     * UI-only work stays here; business validation and Supabase persistence remain delegated to the
     * ViewModel/repository layers.
     */
    fun handleBack() {
        if (uiState.isFindingPartners) return
        when (uiState.page) {
            ImpactWeavePage.LIST -> onBack()
            ImpactWeavePage.ACTIVITY_PLAN -> viewModel.returnToList()
            ImpactWeavePage.SUPPORT_NEEDED -> viewModel.goToActivityPlan()
            ImpactWeavePage.REVIEW -> viewModel.goBackFromReview()
            ImpactWeavePage.MATCH_RESULTS -> viewModel.returnToList()
        }
    }

    BackHandler(onBack = ::handleBack)

    when (uiState.page) {
        ImpactWeavePage.LIST -> ImpactWeaveLandingScreen(
            activePlans = uiState.activePlans,
            isLoadingActivePlans = uiState.isLoadingActivePlans,
            activePlansError = uiState.activePlansError,
            onBack = onBack,
            onStart = viewModel::startNewDraft,
            onOpenPlan = viewModel::reopenActivePlan,
            onRetryLoad = viewModel::loadActivePlans
        )

        ImpactWeavePage.ACTIVITY_PLAN -> {
            val draft = uiState.workingDraft ?: return
            ImpactWeaveActivityPlanScreen(
                draft = draft,
                minimumStartDateMillis = minimumStartDateMillis,
                planningDeadlineMillis = viewModel.partnershipPlanningDeadlineMillis(
                    draft.startDateMillis
                ),
                onBack = viewModel::returnToList,
                onCategorySelected = viewModel::updateCategory,
                onTitleChanged = viewModel::updateTitle,
                onDescriptionChanged = viewModel::updateDescription,
                onModeSelected = viewModel::updateMode,
                onDurationSelected = viewModel::updateDuration,
                onStartDateSelected = viewModel::updateStartDate,
                onEndDateSelected = viewModel::updateEndDate,
                onStartTimeSelected = viewModel::updateStartTime,
                onEndTimeSelected = viewModel::updateEndTime,
                onAreaQueryChanged = viewModel::updateAreaQuery,
                onAreaSelected = viewModel::selectArea,
                onAreaCleared = viewModel::clearArea,
                onHasExistingVenueChanged = viewModel::updateHasExistingVenue,
                onVenueQueryChanged = viewModel::updateVenueQuery,
                onVenueSelected = viewModel::selectVenue,
                onVenueCleared = viewModel::clearVenue,
                errorsProvider = viewModel::activityPlanErrors,
                onContinue = viewModel::continueToSupportNeeded
            )
        }

        ImpactWeavePage.SUPPORT_NEEDED -> {
            val draft = uiState.workingDraft ?: return
            ImpactWeaveSupportNeededScreen(
                draft = draft,
                onBack = viewModel::goToActivityPlan,
                onAddNeed = viewModel::addNeed,
                onUpdateNeed = viewModel::updateNeed,
                onRemoveNeed = viewModel::removeNeed,
                onContinue = viewModel::continueToReview
            )
        }

        ImpactWeavePage.REVIEW -> {
            val draft = uiState.workingDraft ?: return
            ImpactWeaveReviewScreen(
                draft = draft,
                planningDeadlineMillis = viewModel.partnershipPlanningDeadlineMillis(
                    draft.startDateMillis
                ),
                onBack = viewModel::goBackFromReview,
                onEditActivity = viewModel::goToActivityPlan,
                onEditNeeds = viewModel::goBackFromReview,
                isFindingPartners = uiState.isFindingPartners,
                findPartnersError = uiState.findPartnersError,
                onFindPartners = viewModel::findPartners
            )
        }

        ImpactWeavePage.MATCH_RESULTS -> {
            val draft = uiState.workingDraft ?: return
            ImpactWeaveMatchResultsScreen(
                draft = draft,
                results = uiState.matchResults,
                isLoading = uiState.isFindingPartners,
                errorMessage = uiState.matchResultsError,
                sentOrganisationIds = uiState.sentPartnershipOrganisationIds,
                partnershipStates = uiState.partnershipStates,
                sendingOrganisationId = uiState.sendingPartnershipOrganisationId,
                requestError = uiState.partnershipRequestError,
                requestSuccess = uiState.partnershipRequestSuccess,
                isSavingPlanChange = uiState.isSavingPlanChange,
                planChangeError = uiState.planChangeError,
                planChangeSuccess = uiState.planChangeSuccess,
                onBack = viewModel::returnToList,
                onRetry = viewModel::retryMatchingResults,
                onSendRequest = viewModel::sendPartnershipRequest,
                onClearRequestFeedback = viewModel::clearPartnershipRequestFeedback,
                onUpdateDetails = viewModel::updateActivePlanDetails,
                onReschedule = viewModel::rescheduleActivePlan,
                onDispose = viewModel::disposeActivePlan,
                onCreatePost = onCreatePost,
                onViewOrganisationProfile = onViewOrganisationProfile,
                onClearPlanFeedback = viewModel::clearPlanChangeFeedback,
                minimumStartDateMillis = minimumStartDateMillis,
                minimumPostDateMillis = minimumPostDateMillis
            )
        }
    }
}
