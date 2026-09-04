package com.example.volunteerlink.organisation.create

// FILE OVERVIEW:
/*
 * CreatePostValidator contains business rules used by the organisation Create/Edit Post flow.
 * Keeping these checks separate from the UI makes validation and edit restrictions easier to
 * reuse from different wizard steps and management actions.
 */


import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostErrors
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.RoleSelectionErrors
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Validation and date/time rules for the Create Post flow.
 *
 * Keeping these rules outside the ViewModel prevents the ViewModel from
 * becoming a giant class as Steps 2-5 are added later.
 */
object CreatePostValidator {

    const val MINIMUM_LEAD_DAYS = 7

    /**
     * Starts the of day millis for the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun startOfDayMillis(
        timeMillis: Long = AppClock.nowMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Returns the minimum start date millis value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun minimumStartDateMillis(
        todayMillis: Long = AppClock.nowMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(todayMillis)
            add(Calendar.DAY_OF_YEAR, MINIMUM_LEAD_DAYS)
        }.timeInMillis
    }

    /**
     * Returns the next day millis value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun nextDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    /**
     * Returns a live publication error only when an already-selected start
     * date has become too close to the current AppClock date. Null/missing
     * dates are handled by the normal Step 1 required-field validation.
     */
    fun minimumLeadTimeError(dateMillis: Long?): String? {
        if (dateMillis == null) return null

        val minimum = minimumStartDateMillis()
        return if (startOfDayMillis(dateMillis) < minimum) {
            "This date is too soon to publish. Choose ${formatDate(minimum)} or later."
        } else {
            null
        }
    }

    /**
     * Existing drafts can become outdated while they sit unpublished. Edit Post
     * uses this message immediately so the organiser can see the exact date that
     * needs changing instead of discovering it only after pressing Continue.
     */
    fun draftStartDateAttention(dateMillis: Long?): String? {
        if (dateMillis == null) return null

        val selected = startOfDayMillis(dateMillis)
        val today = startOfDayMillis()
        val minimum = minimumStartDateMillis()

        return when {
            selected < today ->
                "Start date has passed. Choose ${formatDate(minimum)} or later before publishing."
            selected < minimum ->
                "Start date is less than 7 days away. Choose ${formatDate(minimum)} or later before publishing."
            else -> null
        }
    }

    /**
     * Message used by Review when Save Draft / Publish discovers that time has
     * moved forward since Step 1 was first completed.
     */
    fun minimumLeadTimeIssueMessage(draft: CreatePostDraft): String? {
        val minimum = minimumStartDateMillis()
        val affected = mutableListOf<String>()

        val needsPhysical =
            draft.postType == VolunteerPostType.PHYSICAL ||
                draft.postType == VolunteerPostType.HYBRID
        val needsRemote =
            draft.postType == VolunteerPostType.REMOTE ||
                draft.postType == VolunteerPostType.HYBRID

        if (
            needsPhysical &&
            draft.physicalStartDateMillis != null &&
            startOfDayMillis(draft.physicalStartDateMillis) < minimum
        ) {
            affected += "Physical start date (${formatDate(draft.physicalStartDateMillis)})"
        }

        if (
            needsRemote &&
            draft.remoteStartDateMillis != null &&
            startOfDayMillis(draft.remoteStartDateMillis) < minimum
        ) {
            affected += "Remote start date (${formatDate(draft.remoteStartDateMillis)})"
        }

        if (affected.isEmpty()) return null

        val dateText = affected.joinToString(separator = " and ")
        val verb = if (affected.size == 1) "is" else "are"
        return "$dateText $verb now less than 7 days from today. " +
            "Choose ${formatDate(minimum)} or later before publishing."
    }

    /**
     * Draft saving is allowed even when the current start date does not yet satisfy
     * the 7-day publishing lead time. This helper removes only that timing error while
     * preserving every other Step 1 validation problem.
     */
    fun withoutMinimumLeadTimeErrors(
        draft: CreatePostDraft,
        errors: CreatePostErrors,
        ignorePhysical: Boolean = true,
        ignoreRemote: Boolean = true
    ): CreatePostErrors {
        val physicalTooSoon =
            draft.physicalStartDateMillis != null &&
                minimumLeadTimeError(draft.physicalStartDateMillis) != null
        val remoteTooSoon =
            draft.remoteStartDateMillis != null &&
                minimumLeadTimeError(draft.remoteStartDateMillis) != null

        return errors.copy(
            physicalStartDate = if (ignorePhysical && physicalTooSoon) null else errors.physicalStartDate,
            remoteStartDate = if (ignoreRemote && remoteTooSoon) null else errors.remoteStartDate
        )
    }

    /**
     * Formats the date used by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun formatDate(dateMillis: Long): String {
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(dateMillis)
    }

    /**
     * Returns the end time error used by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun endTimeError(
        startTimeMinutes: Int?,
        endTimeMinutes: Int
    ): String? {
        return when {
            startTimeMinutes == null -> "Select the start time first."
            endTimeMinutes <= startTimeMinutes ->
                "End time must be later than the start time."
            else -> null
        }
    }

    /**
     * Validates the step one used by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun validateStepOne(
        draft: CreatePostDraft
    ): CreatePostErrors {
        val needsPhysical =
            draft.postType == VolunteerPostType.PHYSICAL ||
                    draft.postType == VolunteerPostType.HYBRID

        val needsRemote =
            draft.postType == VolunteerPostType.REMOTE ||
                    draft.postType == VolunteerPostType.HYBRID

        val physicalStartDateError = if (needsPhysical) {
            when {
                draft.physicalStartDateMillis == null ->
                    "Select a start date."

                else -> minimumLeadTimeError(draft.physicalStartDateMillis)
            }
        } else {
            null
        }

        val physicalEndDateError = if (
            needsPhysical && draft.isMultiDayPhysicalEvent
        ) {
            when {
                draft.physicalEndDateMillis == null ->
                    "Select an end date."

                draft.physicalStartDateMillis == null ->
                    "Select a start date first."

                startOfDayMillis(draft.physicalEndDateMillis) <=
                        startOfDayMillis(draft.physicalStartDateMillis) ->
                    "End date must be after the start date."

                else -> null
            }
        } else {
            null
        }

        val physicalTimeError = if (needsPhysical) {
            when {
                draft.physicalStartTimeMinutes == null ->
                    "Select a start time."

                draft.physicalEndTimeMinutes == null ->
                    "Select an end time."

                draft.physicalEndTimeMinutes <= draft.physicalStartTimeMinutes ->
                    "End time must be later than the start time."

                else -> null
            }
        } else {
            null
        }

        val remoteStartDateError = if (needsRemote) {
            when {
                draft.remoteStartDateMillis == null ->
                    "Select a start date."

                else -> minimumLeadTimeError(draft.remoteStartDateMillis)
            }
        } else {
            null
        }

        val remoteDueDateError = if (needsRemote) {
            when {
                draft.remoteDueDateMillis == null ->
                    "Select a due date."

                draft.remoteStartDateMillis == null ->
                    "Select a start date first."

                startOfDayMillis(draft.remoteDueDateMillis) <=
                        startOfDayMillis(draft.remoteStartDateMillis) ->
                    "Due date must be after the start date."

                else -> null
            }
        } else {
            null
        }

        return CreatePostErrors(
            postType = if (draft.postType == null) {
                "Select a post type."
            } else null,

            category = if (draft.category == null) {
                "Select a category."
            } else null,

            title = if (draft.title.isBlank()) {
                "Enter a post title."
            } else null,

            description = if (draft.description.isBlank()) {
                "Enter a description."
            } else null,

            physicalStartDate = physicalStartDateError,
            physicalEndDate = physicalEndDateError,
            physicalTime = physicalTimeError,

            physicalLocation = if (
                needsPhysical && draft.physicalLocation == null
            ) {
                "Select a location from the suggestions."
            } else null,

            physicalCapacity = if (
                draft.postType == VolunteerPostType.PHYSICAL &&
                (draft.physicalVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the number of volunteers needed."
            } else null,

            remoteStartDate = remoteStartDateError,
            remoteDueDate = remoteDueDateError,

            remoteCapacity = if (
                draft.postType == VolunteerPostType.REMOTE &&
                (draft.remoteVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the number of volunteers needed."
            } else null,

            remoteSubmissionMode = if (
                needsRemote && draft.remoteSubmissionMode == null
            ) {
                "Choose a submission setup."
            } else null,

            sharedDeliverable = if (
                needsRemote &&
                draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM &&
                draft.sharedDeliverable.isBlank()
            ) {
                "Describe the shared team deliverable."
            } else null,

            hybridPhysicalCapacity = if (
                draft.postType == VolunteerPostType.HYBRID &&
                (draft.hybridPhysicalVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the Physical volunteer requirement."
            } else null,

            hybridRemoteCapacity = if (
                draft.postType == VolunteerPostType.HYBRID &&
                (draft.hybridRemoteVolunteerCapacity ?: 0) <= 0
            ) {
                "Enter the Remote volunteer requirement."
            } else null
        )
    }

    /**
     * Step 2 is complete only when the selected role capacities exactly match
     * the volunteer requirement entered in Step 1.
     *
     * Hybrid posts are checked separately so Physical and Remote capacities
     * cannot accidentally be mixed together.
     */
    fun validateStepTwo(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): RoleSelectionErrors {
        if (roleCatalogue.isEmpty()) {
            return RoleSelectionErrors(
                general = "Role catalogue is not available yet."
            )
        }

        val templatesById = roleCatalogue.associateBy { it.roleTemplateId }

        if (draft.selectedRoles.any { it.capacity <= 0 }) {
            return RoleSelectionErrors(
                general = "Each selected role needs at least 1 volunteer."
            )
        }

        if (draft.selectedRoles.any { it.roleTemplateId !in templatesById }) {
            return RoleSelectionErrors(
                general = "One selected role is no longer available. Remove it and select another role."
            )
        }

        val physicalSelections = draft.selectedRoles.filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.PHYSICAL
        }

        val remoteSelections = draft.selectedRoles.filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.REMOTE
        }

        /**
         * Returns the capacity error used by the organisation Create/Edit Post flow.
         * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
         */
        fun capacityError(
            label: String,
            required: Int?,
            assigned: Int,
            selectedCount: Int
        ): String? {
            if (required == null || required <= 0) {
                return "Return to Step 1 and set the $label volunteer requirement."
            }

            if (selectedCount == 0) {
                return "Select at least one $label role."
            }

            return if (assigned != required) {
                "Assign exactly $required $label volunteers. Currently assigned $assigned."
            } else {
                null
            }
        }

        val physicalAssigned = physicalSelections.sumOf { it.capacity }
        val remoteAssigned = remoteSelections.sumOf { it.capacity }

        return when (draft.postType) {
            VolunteerPostType.PHYSICAL -> RoleSelectionErrors(
                general = if (remoteSelections.isNotEmpty()) {
                    "Remote roles cannot be added to a Physical post."
                } else {
                    null
                },
                physical = capacityError(
                    label = "Physical",
                    required = draft.requiredPhysicalVolunteerTotal,
                    assigned = physicalAssigned,
                    selectedCount = physicalSelections.size
                )
            )

            VolunteerPostType.REMOTE -> RoleSelectionErrors(
                general = if (physicalSelections.isNotEmpty()) {
                    "Physical roles cannot be added to a Remote post."
                } else {
                    null
                },
                remote = capacityError(
                    label = "Remote",
                    required = draft.requiredRemoteVolunteerTotal,
                    assigned = remoteAssigned,
                    selectedCount = remoteSelections.size
                )
            )

            VolunteerPostType.HYBRID -> RoleSelectionErrors(
                physical = capacityError(
                    label = "Physical",
                    required = draft.requiredPhysicalVolunteerTotal,
                    assigned = physicalAssigned,
                    selectedCount = physicalSelections.size
                ),
                remote = capacityError(
                    label = "Remote",
                    required = draft.requiredRemoteVolunteerTotal,
                    assigned = remoteAssigned,
                    selectedCount = remoteSelections.size
                )
            )

            null -> RoleSelectionErrors(
                general = "Return to Step 1 and select a post type."
            )
        }
    }


    /**
     * Returns VolunteerLink's recommended application method for a role level.
     *
     * This is only the initial recommendation shown in Step 3. Organisations
     * can still choose either Instant Join or Review Applicants for any level.
     */
    fun recommendedApplicationMethodForLevel(
        level: VolunteerRoleLevel
    ): RoleApplicationMethod {
        return when (level) {
            VolunteerRoleLevel.BEGINNER ->
                RoleApplicationMethod.INSTANT_JOIN

            VolunteerRoleLevel.INTERMEDIATE,
            VolunteerRoleLevel.ADVANCED ->
                RoleApplicationMethod.REVIEW_APPLICANTS
        }
    }

    /**
     * Returns every Step 3 problem for one role configuration.
     * Empty means the role is valid and can be marked Ready.
     */
    fun validateRoleConfiguration(
        draft: CreatePostDraft,
        selectedRole: SelectedRoleDraft,
        template: CreateRoleTemplate
    ): List<String> {
        val problems = mutableListOf<String>()
        val allowedSkillIds = template.skillsPractised
            .map { skill -> skill.skillId }
            .toSet()
        val selectedSkillIds = selectedRole.practisedSkillIds.distinct()

        if (selectedSkillIds.size !in 2..4) {
            problems += "Select between 2 and 4 Skills Practised."
        }

        if (selectedSkillIds.any { skillId -> skillId !in allowedSkillIds }) {
            problems += "One selected skill is not available for this role."
        }

        val requiredSkillsValid =
            selectedRole.requiredSkillExperience.all { (skillId, minimum) ->
                skillId in selectedSkillIds &&
                        skillId in allowedSkillIds &&
                        minimum in 1..5
            }

        if (
            template.defaultLevel == VolunteerRoleLevel.BEGINNER &&
            selectedRole.requiredSkillExperience.isNotEmpty()
        ) {
            problems += "Beginner roles cannot require previous skill experience."
        } else if (!requiredSkillsValid) {
            problems += "Required skills must also be practised and use 1 to 5 verified experiences."
        }

        if (
            selectedRole.responsibilities
                .map { responsibility -> responsibility.trim() }
                .none { responsibility -> responsibility.isNotEmpty() }
        ) {
            problems += "Add at least one responsibility."
        }

        val selectedMethod = selectedRole.applicationMethod
        if (selectedMethod == null) {
            problems += "Choose an application method."
        }

        val cleanedQuestions = selectedRole.screeningQuestions
            .map { question -> question.trim() }
            .filter { question -> question.isNotEmpty() }

        if (cleanedQuestions.size > 3) {
            problems += "Use no more than 3 screening questions."
        }

        if (
            selectedMethod == RoleApplicationMethod.INSTANT_JOIN &&
            cleanedQuestions.isNotEmpty()
        ) {
            problems += "Instant Join roles cannot use screening questions."
        }

        if (template.roleMode == VolunteerRoleMode.REMOTE) {
            when (draft.remoteSubmissionMode) {
                RemoteSubmissionMode.SHARED_TEAM -> Unit

                RemoteSubmissionMode.INDIVIDUAL -> {
                    if (selectedRole.individualSubmissionRequirement.isBlank()) {
                        problems += "Add the Individual Deliverable for this Remote role."
                    }
                }

                null -> {
                    problems += "Return to Step 1 and choose a Remote submission setup."
                }
            }
        }

        return problems
    }

    /** Returns true when Step 3 can move forward. */
    fun validateStepThree(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): String? {
        if (draft.selectedRoles.isEmpty()) {
            return "Return to Step 2 and select at least one role."
        }

        val templatesById = roleCatalogue.associateBy { template ->
            template.roleTemplateId
        }

        draft.selectedRoles.forEach { selectedRole ->
            val template = templatesById[selectedRole.roleTemplateId]
                ?: return "One selected role is no longer available. Return to Step 2 and review your roles."

            if (!selectedRole.isConfigured) {
                return "Review and save every selected role before continuing."
            }

            if (
                validateRoleConfiguration(
                    draft = draft,
                    selectedRole = selectedRole,
                    template = template
                ).isNotEmpty()
            ) {
                return "One saved role has changed. Open it and save the role again."
            }
        }

        val needsRemote =
            draft.postType == VolunteerPostType.REMOTE ||
                    draft.postType == VolunteerPostType.HYBRID

        if (
            needsRemote &&
            draft.remoteSubmissionMode == RemoteSubmissionMode.SHARED_TEAM
        ) {
            if (draft.sharedDeliverable.isBlank()) {
                return "Return to Step 1 and describe the shared team deliverable."
            }

            val responsibleRoleId =
                draft.sharedSubmissionResponsibleRoleTemplateId
                    ?: return "Choose which Remote role will submit the shared team deliverable."

            val isSelectedRemoteRole = draft.selectedRoles.any { selectedRole ->
                selectedRole.roleTemplateId == responsibleRoleId &&
                        templatesById[responsibleRoleId]?.roleMode ==
                        VolunteerRoleMode.REMOTE
            }

            if (!isSelectedRemoteRole) {
                return "Choose a selected Remote role to submit the shared team deliverable."
            }
        }

        return null
    }


    // ---------------------------------------------------------------------
    // Step 4: optional Physical / Remote schedule
    // ---------------------------------------------------------------------

    /** Every Physical event date that can appear in the Step 4 timetable. */
    fun physicalScheduleDates(
        draft: CreatePostDraft
    ): List<Long> {
        val start = draft.physicalStartDateMillis
            ?.let(::startOfDayMillis)
            ?: return emptyList()

        val end = if (draft.isMultiDayPhysicalEvent) {
            draft.physicalEndDateMillis
                ?.let(::startOfDayMillis)
                ?: return emptyList()
        } else {
            start
        }

        if (end < start) return emptyList()

        val dates = mutableListOf<Long>()
        var current = start
        while (current <= end && dates.size < 366) {
            dates += current
            current = nextDayMillis(current)
        }
        return dates
    }

    /** Returns selected ROLE... IDs that match the schedule side. */
    fun applicableScheduleRoleIds(
        draft: CreatePostDraft,
        scheduleType: ScheduleType,
        roleCatalogue: List<CreateRoleTemplate>
    ): List<String> {
        val templatesById = roleCatalogue.associateBy { it.roleTemplateId }

        return draft.selectedRoles.mapNotNull { selectedRole ->
            val template = templatesById[selectedRole.roleTemplateId]
                ?: return@mapNotNull null

            val allowed = when (scheduleType) {
                ScheduleType.PHYSICAL ->
                    template.roleMode == VolunteerRoleMode.PHYSICAL
                ScheduleType.REMOTE ->
                    template.roleMode == VolunteerRoleMode.REMOTE
            }

            selectedRole.roleTemplateId.takeIf { allowed }
        }
    }

    /** Returns one concise error for a Step 4 item, or null when it is valid. */
    fun validateScheduleItem(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        nowMillis: Long = AppClock.nowMillis()
    ): String? {
        if (item.title.isBlank()) {
            return when (item.scheduleType) {
                ScheduleType.PHYSICAL -> "Enter an activity name."
                ScheduleType.REMOTE -> "Enter a milestone or checkpoint title."
            }
        }

        val date = item.scheduleDateMillis
            ?.let(::startOfDayMillis)
            ?: return "Select a date."

        val applicableRoleIds = applicableScheduleRoleIds(
            draft = draft,
            scheduleType = item.scheduleType,
            roleCatalogue = roleCatalogue
        )

        if (applicableRoleIds.isEmpty()) {
            return when (item.scheduleType) {
                ScheduleType.PHYSICAL ->
                    "Return to Step 2 and select at least one Physical role."
                ScheduleType.REMOTE ->
                    "Return to Step 2 and select at least one Remote role."
            }
        }

        if (!scheduleRoleTargetsAreValid(item, applicableRoleIds)) {
            return "Choose All Roles or at least one currently selected role."
        }

        when (item.scheduleType) {
            ScheduleType.PHYSICAL -> {
                if (date !in physicalScheduleDates(draft)) {
                    return "This activity date is outside the Physical event dates."
                }

                val eventStart = draft.physicalStartTimeMinutes
                    ?: return "Return to Step 1 and set the Physical event start time."
                val eventEnd = draft.physicalEndTimeMinutes
                    ?: return "Return to Step 1 and set the Physical event end time."
                val start = item.startTimeMinutes
                    ?: return "Select a start time."
                val end = item.endTimeMinutes
                    ?: return "Select an end time."

                if (end <= start) {
                    return "End time must be later than start time."
                }

                if (start < eventStart || end > eventEnd) {
                    return "Keep this activity within the event time set in Step 1."
                }
            }

            ScheduleType.REMOTE -> {
                val remoteStart = draft.remoteStartDateMillis
                    ?.let(::startOfDayMillis)
                    ?: return "Return to Step 1 and set the Remote start date."
                val remoteDue = draft.remoteDueDateMillis
                    ?.let(::startOfDayMillis)
                    ?: return "Return to Step 1 and set the Remote due date."

                if (date !in remoteStart..remoteDue) {
                    return "Keep this milestone within the Remote project dates."
                }

                if (
                    item.startTimeMinutes != null ||
                    item.endTimeMinutes != null ||
                    item.location.isNotBlank()
                ) {
                    return "Remote milestones use a date only, without time or location."
                }
            }
        }

        if (
            item.scheduleType == ScheduleType.PHYSICAL &&
            hasTimedScheduleConflict(
                draft = draft,
                item = item,
                roleCatalogue = roleCatalogue
            )
        ) {
            return "This time overlaps another Physical item for the same role(s)."
        }

        return null
    }

    /**
     * Validates the step four used by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    fun validateStepFour(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        nowMillis: Long = AppClock.nowMillis()
    ): String? {
        draft.scheduleItems.forEach { item ->
            val error = validateScheduleItem(
                draft = draft,
                item = item,
                roleCatalogue = roleCatalogue,
                nowMillis = nowMillis
            )
            if (error != null) return error
        }
        return null
    }

    /** Step 4 stays optional; warn only about empty relevant schedule sections. */
    fun scheduleProceedWarning(
        draft: CreatePostDraft,
        nowMillis: Long = AppClock.nowMillis()
    ): String? {
        val warnings = mutableListOf<String>()
        val hasPhysicalPart =
            draft.postType == VolunteerPostType.PHYSICAL ||
                draft.postType == VolunteerPostType.HYBRID
        val hasRemotePart =
            draft.postType == VolunteerPostType.REMOTE ||
                draft.postType == VolunteerPostType.HYBRID

        if (hasPhysicalPart) {
            val physicalItems = draft.scheduleItems.filter {
                it.scheduleType == ScheduleType.PHYSICAL
            }
            if (physicalItems.isEmpty()) {
                warnings += "Physical Schedule is empty."
            } else {
                val physicalDates = physicalScheduleDates(draft)
                if (physicalDates.size > 1) {
                    val scheduledDates = physicalItems
                        .mapNotNull { it.scheduleDateMillis?.let(::startOfDayMillis) }
                        .toSet()
                    val missingDays = physicalDates.count { it !in scheduledDates }
                    if (missingDays > 0) {
                        warnings += "$missingDays Physical event day(s) have no timetable."
                    }
                }
            }
        }

        if (
            hasRemotePart &&
            draft.scheduleItems.none { it.scheduleType == ScheduleType.REMOTE }
        ) {
            warnings += "Remote Schedule is empty."
        }

        return if (warnings.isEmpty()) {
            null
        } else {
            warnings.joinToString(separator = " ") +
                " Step 4 is optional, so you can still continue if this is intentional."
        }
    }

    /**
     * Checks whether the schedule role targets are valid for the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun scheduleRoleTargetsAreValid(
        item: ScheduleItemDraft,
        applicableRoleIds: List<String>
    ): Boolean {
        if (item.appliesToAllRoles) return applicableRoleIds.isNotEmpty()
        val selectedTargets = item.targetRoleTemplateIds.distinct()
        if (selectedTargets.isEmpty()) return false
        val validIds = applicableRoleIds.toSet()
        return selectedTargets.all { it in validIds }
    }

    /**
     * Returns the effective schedule role ids value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun effectiveScheduleRoleIds(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): Set<String> {
        val applicable = applicableScheduleRoleIds(
            draft = draft,
            scheduleType = item.scheduleType,
            roleCatalogue = roleCatalogue
        )
        return if (item.appliesToAllRoles) {
            applicable.toSet()
        } else {
            item.targetRoleTemplateIds
                .filter { it in applicable }
                .toSet()
        }
    }

    /**
     * Checks whether the current Create/Edit Post state has timed schedule conflict.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun hasTimedScheduleConflict(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): Boolean {
        if (item.scheduleType != ScheduleType.PHYSICAL) return false
        val itemStart = timedStartEpochMillis(item) ?: return false
        val itemEnd = timedEndEpochMillis(item) ?: return false
        val itemRoles = effectiveScheduleRoleIds(draft, item, roleCatalogue)
        if (itemRoles.isEmpty()) return false

        return draft.scheduleItems.any { other ->
            if (
                other.draftId == item.draftId ||
                other.scheduleType != ScheduleType.PHYSICAL
            ) return@any false

            val otherRoles = effectiveScheduleRoleIds(draft, other, roleCatalogue)
            if (itemRoles.intersect(otherRoles).isEmpty()) return@any false

            val otherStart = timedStartEpochMillis(other) ?: return@any false
            val otherEnd = timedEndEpochMillis(other) ?: return@any false
            itemStart < otherEnd && otherStart < itemEnd
        }
    }

    /**
     * Returns the timed start epoch millis value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun timedStartEpochMillis(item: ScheduleItemDraft): Long? {
        val time = item.startTimeMinutes ?: return null
        return timedEpochMillis(item, time)
    }

    /**
     * Returns the timed end epoch millis value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun timedEndEpochMillis(item: ScheduleItemDraft): Long? {
        val time = item.endTimeMinutes ?: return null
        return timedEpochMillis(item, time)
    }

    /**
     * Returns the timed epoch millis value required by the organisation Create/Edit Post flow.
     * Centralising the rule ensures every wizard step evaluates the same requirement consistently.
     */
    private fun timedEpochMillis(
        item: ScheduleItemDraft,
        timeMinutes: Int
    ): Long? {
        val dateMillis = item.scheduleDateMillis ?: return null
        val sourceDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

        return Calendar.getInstance().apply {
            clear()
            set(
                sourceDate.get(Calendar.YEAR),
                sourceDate.get(Calendar.MONTH),
                sourceDate.get(Calendar.DAY_OF_MONTH),
                timeMinutes / 60,
                timeMinutes % 60,
                0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
