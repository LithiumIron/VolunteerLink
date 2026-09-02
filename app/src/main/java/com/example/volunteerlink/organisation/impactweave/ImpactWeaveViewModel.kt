package com.example.volunteerlink.organisation.impactweave

import androidx.lifecycle.ViewModel
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveDuration
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveMode
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveNeedDraft
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeavePage
import com.example.volunteerlink.organisation.impactweave.model.ImpactWeaveUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class ImpactWeaveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImpactWeaveUiState())
    val uiState = _uiState.asStateFlow()

    private var nextDraftId = 1
    private var nextNeedId = 1

    fun startNewDraft() {
        val now = AppClock.nowMillis()
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.ACTIVITY_PLAN,
            workingDraft = ImpactWeaveDraft(
                draftId = nextDraftId++,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    fun openDraft(draftId: Int) {
        val draft = _uiState.value.drafts.firstOrNull { it.draftId == draftId } ?: return
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.REVIEW,
            workingDraft = draft
        )
    }

    fun returnToList() {
        _uiState.value = _uiState.value.copy(
            page = ImpactWeavePage.LIST,
            workingDraft = null
        )
    }

    fun goToActivityPlan() {
        if (_uiState.value.workingDraft != null) {
            _uiState.value = _uiState.value.copy(page = ImpactWeavePage.ACTIVITY_PLAN)
        }
    }

    fun continueToSupportNeeded(): Boolean {
        if (activityPlanErrors().isNotEmpty()) return false
        saveWorkingCopy()
        _uiState.value = _uiState.value.copy(page = ImpactWeavePage.SUPPORT_NEEDED)
        return true
    }

    fun continueToReview(): Boolean {
        val draft = _uiState.value.workingDraft ?: return false
        if (draft.needs.isEmpty()) return false
        if (draft.hasExistingVenue == false && draft.needs.none { it.supportType == "VENUE" }) {
            return false
        }
        saveWorkingCopy()
        _uiState.value = _uiState.value.copy(page = ImpactWeavePage.REVIEW)
        return true
    }

    fun goBackFromReview() {
        _uiState.value = _uiState.value.copy(page = ImpactWeavePage.SUPPORT_NEEDED)
    }

    fun saveDraftAndReturnToList() {
        saveWorkingCopy()
        returnToList()
    }

    fun updateTitle(value: String) = updateDraft { copy(title = value) }

    fun updateMode(value: ImpactWeaveMode) = updateDraft { copy(mode = value) }

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
    fun updateEndTime(hour24: Int, minute: Int): String? {
        val endMinutes = hour24 * 60 + minute
        val startMinutes = _uiState.value.workingDraft?.startTimeMinutes

        if (startMinutes != null && endMinutes <= startMinutes) {
            return "End time must be later than the start time."
        }

        updateDraft { copy(endTimeMinutes = endMinutes) }
        return null
    }

    fun updateAreaQuery(value: String) = updateDraft {
        copy(
            areaQuery = value,
            areaLocation = null
        )
    }

    fun selectArea(location: LocationSuggestion) = updateDraft {
        val area = location.asGeneralArea()
        copy(
            areaQuery = area.generalAreaName,
            areaLocation = area
        )
    }

    fun clearArea() = updateDraft {
        copy(
            areaQuery = "",
            areaLocation = null
        )
    }

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

    fun updateVenueQuery(value: String) = updateDraft {
        copy(
            venueQuery = value,
            existingVenueLocation = null
        )
    }

    fun selectVenue(location: LocationSuggestion) = updateDraft {
        val area = location.asGeneralArea()
        copy(
            venueQuery = location.displayName,
            existingVenueLocation = location,
            areaQuery = area.generalAreaName,
            areaLocation = area
        )
    }

    fun clearVenue() = updateDraft {
        copy(
            venueQuery = "",
            existingVenueLocation = null,
            areaQuery = "",
            areaLocation = null
        )
    }

    fun addNeed(
        originalText: String,
        supportType: String,
        resourceName: String,
        amount: Int
    ) {
        val need = ImpactWeaveNeedDraft(
            needId = nextNeedId++,
            originalText = originalText.trim(),
            supportType = supportType,
            resourceName = resourceName.trim(),
            quantityRequired = if (supportType == "VENUE") null else amount,
            capacityRequired = if (supportType == "VENUE") amount else null
        )

        updateDraft {
            copy(needs = needs + need)
        }
        saveWorkingCopy()
    }

    fun updateNeed(
        needId: Int,
        originalText: String,
        supportType: String,
        resourceName: String,
        amount: Int
    ) {
        updateDraft {
            copy(
                needs = needs.map { need ->
                    if (need.needId != needId) {
                        need
                    } else {
                        need.copy(
                            originalText = originalText.trim(),
                            supportType = supportType,
                            resourceName = resourceName.trim(),
                            quantityRequired = if (supportType == "VENUE") null else amount,
                            capacityRequired = if (supportType == "VENUE") amount else null
                        )
                    }
                }
            )
        }
        saveWorkingCopy()
    }

    fun removeNeed(needId: Int) {
        updateDraft {
            copy(needs = needs.filterNot { it.needId == needId })
        }
        saveWorkingCopy()
    }

    fun activityPlanErrors(): Map<String, String> {
        val draft = _uiState.value.workingDraft ?: return mapOf("draft" to "Draft is unavailable.")
        val errors = linkedMapOf<String, String>()
        val minimumStartDate = minimumImpactWeaveStartDateMillis()

        if (draft.title.trim().length < 3) {
            errors["title"] = "Enter an activity title."
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

    fun minimumImpactWeaveStartDateMillis(): Long {
        return startOfLocalDay(AppClock.nowMillis()).let { today ->
            Calendar.getInstance().apply {
                timeInMillis = today
                add(Calendar.DAY_OF_YEAR, 10)
            }.timeInMillis
        }
    }

    fun partnershipPlanningDeadlineMillis(startDateMillis: Long?): Long? {
        if (startDateMillis == null) return null
        return Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
    }

    private fun saveWorkingCopy() {
        val working = _uiState.value.workingDraft ?: return
        val saved = working.copy(updatedAtMillis = AppClock.nowMillis())
        val existing = _uiState.value.drafts.any { it.draftId == saved.draftId }
        val drafts = if (existing) {
            _uiState.value.drafts.map { draft ->
                if (draft.draftId == saved.draftId) saved else draft
            }
        } else {
            _uiState.value.drafts + saved
        }

        _uiState.value = _uiState.value.copy(
            drafts = drafts,
            workingDraft = saved
        )
    }

    private fun updateDraft(change: ImpactWeaveDraft.() -> ImpactWeaveDraft) {
        val draft = _uiState.value.workingDraft ?: return
        _uiState.value = _uiState.value.copy(
            workingDraft = draft.change().copy(updatedAtMillis = AppClock.nowMillis())
        )
    }

    private fun startOfLocalDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
