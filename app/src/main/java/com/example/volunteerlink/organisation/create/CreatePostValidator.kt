package com.example.volunteerlink.organisation.create

import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostErrors
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
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

}
