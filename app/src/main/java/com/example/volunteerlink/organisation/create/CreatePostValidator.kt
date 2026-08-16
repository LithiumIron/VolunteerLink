package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostErrors
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.RoleApplicationMethod
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.RoleSelectionErrors
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import java.util.Calendar

/**
 * Validation and date/time rules for the Create Post flow.
 *
 * Keeping these rules outside the ViewModel prevents the ViewModel from
 * becoming a giant class as Steps 2-5 are added later.
 */
object CreatePostValidator {

    const val MINIMUM_LEAD_DAYS = 7
    const val SHORT_NOTICE_TRAINING_HOURS = 72

    fun startOfDayMillis(
        timeMillis: Long = System.currentTimeMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun minimumStartDateMillis(
        todayMillis: Long = System.currentTimeMillis()
    ): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(todayMillis)
            add(Calendar.DAY_OF_YEAR, MINIMUM_LEAD_DAYS)
        }.timeInMillis
    }

    fun nextDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

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

                startOfDayMillis(draft.physicalStartDateMillis) <
                        minimumStartDateMillis() ->
                    "Start date must be at least 7 days from today."

                else -> null
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

                startOfDayMillis(draft.remoteStartDateMillis) <
                        minimumStartDateMillis() ->
                    "Start date must be at least 7 days from today."

                else -> null
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
    // Step 4: schedule
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

        // Safety limit prevents a broken draft from producing an endless loop.
        while (current <= end && dates.size < 366) {
            dates += current
            current = nextDayMillis(current)
        }

        return dates
    }

    /**
     * Returns the selected ROLE... IDs that may be targeted by one schedule type.
     * Training can target Physical roles, Remote roles, or a mixture of both.
     */
    fun applicableScheduleRoleIds(
        draft: CreatePostDraft,
        scheduleType: ScheduleType,
        roleCatalogue: List<CreateRoleTemplate>
    ): List<String> {
        val templatesById = roleCatalogue.associateBy { template ->
            template.roleTemplateId
        }

        return draft.selectedRoles.mapNotNull { selectedRole ->
            val template = templatesById[selectedRole.roleTemplateId]
                ?: return@mapNotNull null

            val allowed = when (scheduleType) {
                ScheduleType.PHYSICAL ->
                    template.roleMode == VolunteerRoleMode.PHYSICAL

                ScheduleType.REMOTE ->
                    template.roleMode == VolunteerRoleMode.REMOTE

                ScheduleType.TRAINING -> true
            }

            selectedRole.roleTemplateId.takeIf { allowed }
        }
    }

    /** Returns one concise error for a Step 4 item, or null when it is valid. */
    fun validateScheduleItem(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (item.title.isBlank()) {
            return when (item.scheduleType) {
                ScheduleType.PHYSICAL -> "Enter an activity name."
                ScheduleType.REMOTE -> "Enter a milestone or checkpoint title."
                ScheduleType.TRAINING -> "Enter a training or briefing title."
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

                ScheduleType.TRAINING ->
                    "Return to Step 2 and select at least one role."
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

                if (hasTrainingOnlyData(item)) {
                    return "Physical timetable items cannot contain Training-only details."
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

                if (hasTrainingOnlyData(item)) {
                    return "Remote milestones cannot contain Training-only details."
                }
            }

            ScheduleType.TRAINING -> {
                val trainingMode = item.trainingMode
                    ?: return "Choose Online or On-site training."
                val start = item.startTimeMinutes
                    ?: return "Select a training start time."
                val end = item.endTimeMinutes
                    ?: return "Select a training end time."

                if (end <= start) {
                    return "Training end time must be later than start time."
                }

                if (item.allowApplicationsAfterStart == null) {
                    return "Choose whether new applications remain open after training starts."
                }

                val latestAllowedDate = trainingLatestAllowedDate(
                    draft = draft,
                    item = item,
                    roleCatalogue = roleCatalogue
                ) ?: return "The targeted roles do not have a valid opportunity end date."

                if (date > latestAllowedDate) {
                    return "Training cannot be scheduled after the targeted role opportunity has ended."
                }

                val trainingStartMillis = timedStartEpochMillis(item)
                    ?: return "Complete the training date and start time."

                if (trainingStartMillis <= nowMillis) {
                    return "Training must start in the future."
                }

                when (trainingMode) {
                    TrainingMode.ONLINE -> {
                        if (
                            item.location.isNotBlank() ||
                            item.trainingLocationMode != null ||
                            item.trainingLocation != null ||
                            item.trainingLocationQuery.isNotBlank()
                        ) {
                            return "Online training should not contain an on-site location."
                        }

                        val noticeWindowMillis =
                            SHORT_NOTICE_TRAINING_HOURS * 60L * 60L * 1000L

                        if (
                            trainingStartMillis - nowMillis <= noticeWindowMillis &&
                            item.meetingLink.isBlank()
                        ) {
                            return "Add the online meeting link because the session starts within 3 days."
                        }
                    }

                    TrainingMode.ONSITE -> {
                        if (
                            item.onlinePlatform.isNotBlank() ||
                            item.meetingLink.isNotBlank()
                        ) {
                            return "On-site training should not contain online platform or meeting-link details."
                        }

                        when (item.trainingLocationMode) {
                            TrainingLocationMode.EVENT_LOCATION -> {
                                val hasPhysicalPart =
                                    draft.postType == VolunteerPostType.PHYSICAL ||
                                        draft.postType == VolunteerPostType.HYBRID

                                if (!hasPhysicalPart || draft.physicalLocation == null) {
                                    return "The main event location is not available for this training."
                                }
                            }

                            TrainingLocationMode.CUSTOM -> {
                                if (item.trainingLocation == null) {
                                    return "Select the on-site training location from the Geoapify suggestions."
                                }
                            }

                            TrainingLocationMode.TBA -> {
                                val noticeWindowMillis =
                                    SHORT_NOTICE_TRAINING_HOURS * 60L * 60L * 1000L

                                if (trainingStartMillis - nowMillis <= noticeWindowMillis) {
                                    return "Confirm the on-site training location because the session starts within 3 days."
                                }
                            }

                            null -> return "Choose how the on-site training location will be provided."
                        }
                    }
                }
            }
        }

        if (
            item.scheduleType != ScheduleType.REMOTE &&
            hasTimedScheduleConflict(
                draft = draft,
                item = item,
                roleCatalogue = roleCatalogue
            )
        ) {
            return "This time overlaps another Physical or Training item for the same role(s)."
        }

        return null
    }

    fun trainingStartsWithinShortNotice(
        item: ScheduleItemDraft,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (item.scheduleType != ScheduleType.TRAINING) return false

        val trainingStartMillis = timedStartEpochMillis(item) ?: return false
        val noticeWindowMillis =
            SHORT_NOTICE_TRAINING_HOURS * 60L * 60L * 1000L

        return trainingStartMillis > nowMillis &&
            trainingStartMillis - nowMillis <= noticeWindowMillis
    }

    /**
     * Short-notice training is allowed. It is a warning rather than a blocker
     * when the details needed to attend are complete.
     */
    fun scheduleItemWarning(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (item.scheduleType != ScheduleType.TRAINING) return null

        val shortNotice = trainingStartsWithinShortNotice(
            item = item,
            nowMillis = nowMillis
        )

        if (shortNotice) {
            return "This training starts within 3 days. Some volunteers may have little time to prepare or attend. You can still schedule it if this timing is necessary."
        }

        return when {
            item.trainingMode == TrainingMode.ONSITE &&
                item.trainingLocationMode == TrainingLocationMode.TBA ->
                "Location is still to be confirmed. Add the on-site location at least 3 days before this training."

            item.trainingMode == TrainingMode.ONLINE &&
                item.meetingLink.isBlank() ->
                "Meeting link is still to be confirmed. Add it at least 3 days before this training."

            else -> null
        }
    }

    /** Empty Step 4 is valid; any saved item that exists must be complete. */
    fun validateStepFour(
        draft: CreatePostDraft,
        roleCatalogue: List<CreateRoleTemplate>,
        nowMillis: Long = System.currentTimeMillis()
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

    /**
     * Step 4 stays optional. The final Continue action gives one concise
     * confirmation for intentionally empty schedule sections.
     */
    fun scheduleProceedWarning(
        draft: CreatePostDraft,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        val warnings = mutableListOf<String>()
        val hasPhysicalPart =
            draft.postType == VolunteerPostType.PHYSICAL ||
                draft.postType == VolunteerPostType.HYBRID
        val hasRemotePart =
            draft.postType == VolunteerPostType.REMOTE ||
                draft.postType == VolunteerPostType.HYBRID

        if (hasPhysicalPart) {
            val physicalItems = draft.scheduleItems.filter { item ->
                item.scheduleType == ScheduleType.PHYSICAL
            }

            if (physicalItems.isEmpty()) {
                warnings += "Physical Schedule is empty."
            } else {
                val physicalDates = physicalScheduleDates(draft)
                if (physicalDates.size > 1) {
                    val scheduledDates = physicalItems
                        .mapNotNull { item ->
                            item.scheduleDateMillis?.let(::startOfDayMillis)
                        }
                        .toSet()
                    val missingDays = physicalDates.count { date ->
                        date !in scheduledDates
                    }

                    if (missingDays > 0) {
                        warnings += "$missingDays Physical event day(s) have no timetable."
                    }
                }
            }
        }

        if (
            hasRemotePart &&
            draft.scheduleItems.none { item ->
                item.scheduleType == ScheduleType.REMOTE
            }
        ) {
            warnings += "Remote Schedule is empty."
        }

        if (
            draft.scheduleItems.none { item ->
                item.scheduleType == ScheduleType.TRAINING
            }
        ) {
            warnings += "Training & Briefing is empty."
        }

        if (
            draft.scheduleItems.any { item ->
                trainingStartsWithinShortNotice(
                    item = item,
                    nowMillis = nowMillis
                )
            }
        ) {
            warnings += "At least one Training / Briefing starts within 3 days."
        }

        return if (warnings.isEmpty()) {
            null
        } else {
            warnings.joinToString(separator = " ") +
                " Step 4 is optional, so you can still continue if this is intentional."
        }
    }

    private fun hasTrainingOnlyData(item: ScheduleItemDraft): Boolean {
        return item.trainingMode != null ||
            item.trainingLocationMode != null ||
            item.trainingLocationQuery.isNotBlank() ||
            item.trainingLocation != null ||
            item.onlinePlatform.isNotBlank() ||
            item.meetingLink.isNotBlank() ||
            item.trainingTimeZoneId != null ||
            item.allowApplicationsAfterStart != null
    }

    private fun scheduleRoleTargetsAreValid(
        item: ScheduleItemDraft,
        applicableRoleIds: List<String>
    ): Boolean {
        if (item.appliesToAllRoles) {
            return applicableRoleIds.isNotEmpty()
        }

        val selectedTargets = item.targetRoleTemplateIds.distinct()
        if (selectedTargets.isEmpty()) return false

        val validIds = applicableRoleIds.toSet()
        return selectedTargets.all { roleId -> roleId in validIds }
    }

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
                .filter { roleId -> roleId in applicable }
                .toSet()
        }
    }

    /**
     * Training may be before volunteering begins, but not after any targeted
     * role has already finished. A mixed training uses the earliest end date.
     */
    private fun trainingLatestAllowedDate(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): Long? {
        val templatesById = roleCatalogue.associateBy { template ->
            template.roleTemplateId
        }
        val targetIds = effectiveScheduleRoleIds(
            draft = draft,
            item = item,
            roleCatalogue = roleCatalogue
        )
        val targetModes = targetIds.mapNotNull { roleId ->
            templatesById[roleId]?.roleMode
        }.toSet()

        val candidateDates = mutableListOf<Long>()

        if (VolunteerRoleMode.PHYSICAL in targetModes) {
            val physicalEnd =
                draft.physicalEndDateMillis ?: draft.physicalStartDateMillis
            physicalEnd?.let { candidateDates += startOfDayMillis(it) }
        }

        if (VolunteerRoleMode.REMOTE in targetModes) {
            draft.remoteDueDateMillis?.let {
                candidateDates += startOfDayMillis(it)
            }
        }

        return candidateDates.minOrNull()
    }

    private fun hasTimedScheduleConflict(
        draft: CreatePostDraft,
        item: ScheduleItemDraft,
        roleCatalogue: List<CreateRoleTemplate>
    ): Boolean {
        val itemStart = timedStartEpochMillis(item) ?: return false
        val itemEnd = timedEndEpochMillis(item) ?: return false
        val itemRoles = effectiveScheduleRoleIds(
            draft = draft,
            item = item,
            roleCatalogue = roleCatalogue
        )

        if (itemRoles.isEmpty()) return false

        return draft.scheduleItems.any { other ->
            if (
                other.draftId == item.draftId ||
                other.scheduleType == ScheduleType.REMOTE
            ) {
                return@any false
            }

            val otherRoles = effectiveScheduleRoleIds(
                draft = draft,
                item = other,
                roleCatalogue = roleCatalogue
            )
            if (itemRoles.intersect(otherRoles).isEmpty()) {
                return@any false
            }

            val otherStart = timedStartEpochMillis(other) ?: return@any false
            val otherEnd = timedEndEpochMillis(other) ?: return@any false

            itemStart < otherEnd && otherStart < itemEnd
        }
    }

    private fun timedStartEpochMillis(item: ScheduleItemDraft): Long? {
        val time = item.startTimeMinutes ?: return null
        return timedEpochMillis(item, time)
    }

    private fun timedEndEpochMillis(item: ScheduleItemDraft): Long? {
        val time = item.endTimeMinutes ?: return null
        return timedEpochMillis(item, time)
    }

    /**
     * Time-zone support is intentionally postponed. For now Step 4 compares
     * the selected local date/time using the device calendar and stores no
     * training timezone value. This helper can later be replaced centrally.
     */
    private fun timedEpochMillis(
        item: ScheduleItemDraft,
        timeMinutes: Int
    ): Long? {
        val dateMillis = item.scheduleDateMillis ?: return null
        val sourceDate = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }

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
