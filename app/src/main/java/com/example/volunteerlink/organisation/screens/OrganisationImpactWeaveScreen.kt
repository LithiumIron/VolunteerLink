package com.example.volunteerlink.organisation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.impactweave.ImpactWeaveViewModel
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePage

@Composable
fun OrganisationImpactWeaveScreen(
    onBack: () -> Unit,
    viewModel: ImpactWeaveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val minimumStartDateMillis = remember(clockState.refreshVersion) {
        viewModel.minimumImpactWeaveStartDateMillis()
    }

    fun handleBack() {
        when (uiState.page) {
            ImpactWeavePage.LIST -> onBack()
            ImpactWeavePage.ACTIVITY_PLAN -> viewModel.returnToList()
            ImpactWeavePage.SUPPORT_NEEDED -> viewModel.goToActivityPlan()
            ImpactWeavePage.REVIEW -> viewModel.goBackFromReview()
        }
    }

    BackHandler(onBack = ::handleBack)

    when (uiState.page) {
        ImpactWeavePage.LIST -> ImpactWeaveDraftListScreen(
            drafts = uiState.drafts,
            onBack = onBack,
            onStart = viewModel::startNewDraft,
            onDraftClick = viewModel::openDraft,
            planningDeadlineFor = viewModel::partnershipPlanningDeadlineMillis
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
                onTitleChanged = viewModel::updateTitle,
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
                onSaveDraft = viewModel::saveDraftAndReturnToList
            )
        }
    }
}
