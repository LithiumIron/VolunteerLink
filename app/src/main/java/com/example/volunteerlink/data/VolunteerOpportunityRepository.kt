package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityPartner
import com.example.volunteerlink.model.VolunteerOpportunityPartnershipContribution
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerRoleApplicationFlow
import com.example.volunteerlink.model.VolunteerRoleApplicationMethod
import com.example.volunteerlink.model.VolunteerRoleScheduleItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class VolunteerOpportunityDashboardData(
    val events: List<VolunteerOpportunityEvent>,
    val applications: List<VolunteerOpportunityApplication>
)

/**
 * The single Supabase data source for the volunteer opportunity module.
 * UI models keep Int IDs for Navigation Compose, while database IDs remain
 * available for RPC calls and database relationships.
 */
object VolunteerOpportunityRepository {

    suspend fun loadDashboard(): VolunteerOpportunityDashboardData {
        // Refresh, but never modify, the organisation's shared business clock.
        com.example.volunteerlink.data.time.AppClock.refreshFromDatabase()

        // Resolve stale pending applications before any Volunteer screen reads them.
        // This is what turns role-start/full-capacity PENDING rows into DECLINED rows.
        supabase.postgrest.rpc(
            function = "volunteer_resolve_my_application_lifecycle"
        )

        val organisations =
            supabase.from("organisations")
                .select(
                    columns = Columns.raw(
                        "organisation_id,organisation_name,contact_email," +
                                "contact_phone,verification_status"
                    )
                )
                .decodeList<OrganisationRow>()

        val posts =
            supabase.from("volunteer_posts")
                .select()
                .decodeList<VolunteerPostRow>()

        val partnershipRowsByPostId = posts
            .filter { !it.impactWeaveDraftId.isNullOrBlank() }
            .associate { post ->
                post.postId to runCatching {
                    supabase.postgrest.rpc(
                        function = "volunteer_get_post_partnership_support",
                        parameters = buildJsonObject { put("p_post_id", post.postId) }
                    ).decodeList<ImpactWeavePostContributionRow>()
                }.getOrDefault(emptyList())
            }

        val physicalDetails =
            supabase.from("physical_details")
                .select()
                .decodeList<PhysicalDetailRow>()

        val remoteDetails =
            supabase.from("remote_details")
                .select()
                .decodeList<RemoteDetailRow>()

        val postRoles =
            supabase.from("post_roles")
                .select()
                .decodeList<PostRoleRow>()

        // Organisation Create stores detailed role information in normalized
        // child tables. Volunteer screens must read these same rows instead
        // of expecting the old JSON columns on post_roles.
        val postRoleSkills =
            supabase.from("post_role_skills")
                .select()
                .decodeList<PostRoleSkillRow>()

        val postRoleResponsibilities =
            supabase.from("post_role_responsibilities")
                .select()
                .decodeList<PostRoleResponsibilityRow>()

        val postRoleScreeningQuestions =
            supabase.from("post_role_screening_questions")
                .select()
                .decodeList<PostRoleScreeningQuestionRow>()

        val roleTemplates =
            supabase.from("role_templates")
                .select()
                .decodeList<RoleTemplateRow>()

        val roleTemplateSkills =
            supabase.from("role_template_skills")
                .select()
                .decodeList<RoleTemplateSkillRow>()

        val skillPaths =
            supabase.from("skill_paths")
                .select()
                .decodeList<SkillPathNameRow>()

        val skills =
            supabase.from("skills")
                .select()
                .decodeList<SkillNameRow>()

        val scheduleItems =
            supabase.from("schedule_items")
                .select()
                .decodeList<ScheduleItemRow>()

        val scheduleItemRoles =
            supabase.from("schedule_item_roles")
                .select()
                .decodeList<ScheduleItemRoleRow>()

        val metrics = loadMetricsSafely()
        val roleMetrics = loadRoleMetricsSafely()

        val allMappedEvents = mapEvents(
            posts = posts,
            organisations = organisations,
            physicalDetails = physicalDetails,
            impactWeavePartnerRowsByPostId = partnershipRowsByPostId,
            remoteDetails = remoteDetails,
            postRoles = postRoles,
            postRoleSkills = postRoleSkills,
            postRoleResponsibilities = postRoleResponsibilities,
            postRoleScreeningQuestions = postRoleScreeningQuestions,
            roleTemplates = roleTemplates,
            roleTemplateSkills = roleTemplateSkills,
            skillPaths = skillPaths,
            skills = skills,
            scheduleItems = scheduleItems,
            scheduleItemRoles = scheduleItemRoles,
            metrics = metrics,
            roleMetrics = roleMetrics
        )

        val participationRows =
            supabase.from("role_participations")
                .select()
                .decodeList<RoleParticipationRow>()

        val achievements = loadAchievementsSafely()

        val rpcApplications =
            loadApplicationsFromRpcSafely()

        val savedPostIds = loadSavedOpportunityIdsSafely()

        return VolunteerOpportunityDashboardData(
            events = allMappedEvents.map { event ->
                event.copy(
                    eventIsSaved = event.eventDatabaseId in savedPostIds
                )
            }.filter { event ->
                event.eventStatus in setOf("PUBLISHED", "CLOSED", "COMPLETED", "CANCELLED") || event.eventIsSaved
            },
            applications =
                rpcApplications
                    ?.let { rows ->
                        mapRpcApplications(rows, allMappedEvents)
                    }
                    ?: mapApplications(
                        participationRows = participationRows,
                        events = allMappedEvents,
                        achievements = achievements
                    )
        )
    }

    suspend fun submitApplication(
        roleDatabaseId: String,
        questions: List<String>,
        answers: List<String>
    ) {
        require(roleDatabaseId.isNotBlank()) {
            "This role is missing its Supabase ID."
        }

        supabase.postgrest.rpc(
            function = "submit_role_application",
            parameters = buildJsonObject {
                put("target_post_role_id", roleDatabaseId)
                putJsonArray("provided_screening_answers") {
                    questions.forEachIndexed { index, question ->
                        addJsonObject {
                            put("question", question)
                            put(
                                "answer",
                                answers.getOrElse(index) { "" }
                            )
                        }
                    }
                }
            }
        )
    }

    suspend fun cancelApplication(
        applicationDatabaseId: String,
        reason: String,
        details: String
    ) {
        require(applicationDatabaseId.isNotBlank()) {
            "This application is missing its Supabase ID."
        }

        supabase.postgrest.rpc(
            function = "volunteer_cancel_application_v2",
            parameters = buildJsonObject {
                put(
                    "target_participation_id",
                    applicationDatabaseId
                )
                put("cancellation_reason", reason)
                put("cancellation_details", details)
            }
        )
    }

    suspend fun updatePendingApplication(
        applicationDatabaseId: String,
        answers: List<String>
    ) {
        supabase.postgrest.rpc(
            function = "volunteer_update_pending_application_answers",
            parameters = buildJsonObject {
                put("target_participation_id", applicationDatabaseId)
                putJsonArray("provided_screening_answers") {
                    answers.forEach { answer -> add(JsonPrimitive(answer)) }
                }
            }
        )
    }

    suspend fun deleteApplication(applicationDatabaseId: String) {
        supabase.postgrest.rpc(
            function = "volunteer_delete_application",
            parameters = buildJsonObject {
                put("target_participation_id", applicationDatabaseId)
            }
        )
    }

    suspend fun reapplyForRole(
        roleDatabaseId: String,
        answers: List<String>
    ) {
        supabase.postgrest.rpc(
            function = "volunteer_reapply_for_role",
            parameters = buildJsonObject {
                put("target_post_role_id", roleDatabaseId)
                putJsonArray("provided_screening_answers") {
                    answers.forEach { answer ->
                        addJsonObject { put("answer", answer) }
                    }
                }
            }
        )
    }

    suspend fun setOpportunitySaved(
        postId: String,
        shouldSave: Boolean
    ) {
        require(postId.isNotBlank()) {
            "This opportunity is missing its Supabase ID."
        }
        supabase.postgrest.rpc(
            function = "set_my_saved_opportunity",
            parameters = buildJsonObject {
                put("target_post_id", postId)
                put("should_save", shouldSave)
            }
        )
    }

    suspend fun replayPendingAction(
        actionType: String,
        targetId: String,
        payloadJson: String
    ) {
        when (actionType) {
            "SUBMIT" -> {
                val answers = Json.parseToJsonElement(payloadJson)
                    .jsonObject["answers"]
                    ?.jsonArray
                    .orEmpty()
                supabase.postgrest.rpc(
                    function = "submit_role_application",
                    parameters = buildJsonObject {
                        put("target_post_role_id", targetId)
                        putJsonArray("provided_screening_answers") {
                            answers.forEach { add(it) }
                        }
                    }
                )
            }

            "CANCEL_V2" -> {
                val payload = Json.parseToJsonElement(payloadJson).jsonObject
                cancelApplication(
                    applicationDatabaseId = targetId,
                    reason = payload["reason"]?.jsonPrimitive?.content.orEmpty(),
                    details = payload["details"]?.jsonPrimitive?.content.orEmpty()
                )
            }
            "CANCEL" -> {
                // Backward compatibility for actions queued by V15.1.3.
                supabase.postgrest.rpc(
                    function = "cancel_my_application",
                    parameters = buildJsonObject {
                        put("target_participation_id", targetId)
                    }
                )
            }
            "UPDATE_APPLICATION" -> {
                val answers = Json.parseToJsonElement(payloadJson)
                    .jsonObject["answers"]?.jsonArray.orEmpty()
                    .map { it.jsonPrimitive.content }
                updatePendingApplication(targetId, answers)
            }
            "SET_SAVED" -> {
                val shouldSave = Json.parseToJsonElement(payloadJson)
                    .jsonObject["should_save"]?.jsonPrimitive?.content
                    ?.toBooleanStrictOrNull() ?: false
                setOpportunitySaved(targetId, shouldSave)
            }
            else -> error("Unsupported pending action: $actionType")
        }
    }

    private suspend fun loadSavedOpportunityIdsSafely(): Set<String> =
        runCatching {
            supabase.postgrest
                .rpc("get_my_saved_opportunity_ids")
                .decodeList<SavedOpportunityIdRow>()
                .map { it.postId }
                .toSet()
        }.getOrDefault(emptySet())

    private suspend fun loadMetricsSafely():
            Map<String, OpportunityMetricRow> {
        return try {
            supabase.postgrest
                .rpc("get_published_opportunity_metrics")
                .decodeList<OpportunityMetricRow>()
                .associateBy { it.postId }
        } catch (_: Exception) {
            // The read experience still works before the optional metrics RPC
            // is installed. Capacity is used and private applications stay hidden.
            emptyMap()
        }
    }

    private suspend fun loadRoleMetricsSafely():
            Map<String, RoleMetricRow> {
        return try {
            supabase.postgrest
                .rpc("get_published_role_metrics")
                .decodeList<RoleMetricRow>()
                .associateBy { it.postRoleId }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun loadAchievementsSafely():
            Map<String, VolunteerAchievementRow> {
        return try {
            supabase.postgrest
                .rpc("get_my_volunteer_achievement_records")
                .decodeList<VolunteerAchievementRow>()
                .associateBy { it.participationId }
        } catch (_: Exception) {
            // Application history remains usable before the optional
            // certificate/achievement RPC is installed.
            emptyMap()
        }
    }

    private suspend fun loadApplicationsFromRpcSafely():
            List<MyVolunteerApplicationRow>? {
        return try {
            runCatching {
                supabase.postgrest
                    .rpc("get_my_volunteer_applications_v2")
                    .decodeList<MyVolunteerApplicationRow>()
            }.getOrElse {
                supabase.postgrest
                    .rpc("get_my_volunteer_applications")
                    .decodeList<MyVolunteerApplicationRow>()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapEvents(
        posts: List<VolunteerPostRow>,
        organisations: List<OrganisationRow>,
        physicalDetails: List<PhysicalDetailRow>,
        impactWeavePartnerRowsByPostId: Map<String, List<ImpactWeavePostContributionRow>>,
        remoteDetails: List<RemoteDetailRow>,
        postRoles: List<PostRoleRow>,
        postRoleSkills: List<PostRoleSkillRow>,
        postRoleResponsibilities: List<PostRoleResponsibilityRow>,
        postRoleScreeningQuestions: List<PostRoleScreeningQuestionRow>,
        roleTemplates: List<RoleTemplateRow>,
        roleTemplateSkills: List<RoleTemplateSkillRow>,
        skillPaths: List<SkillPathNameRow>,
        skills: List<SkillNameRow>,
        scheduleItems: List<ScheduleItemRow>,
        scheduleItemRoles: List<ScheduleItemRoleRow>,
        metrics: Map<String, OpportunityMetricRow>,
        roleMetrics: Map<String, RoleMetricRow>
    ): List<VolunteerOpportunityEvent> {
        val organisationsById =
            organisations.associateBy { it.organisationId }
        val physicalByPostId =
            physicalDetails.associateBy { it.postId }
        val remoteByPostId =
            remoteDetails.associateBy { it.postId }
        val roleTemplatesById =
            roleTemplates.associateBy { it.roleTemplateId }
        val templateSkillIdsByRoleId =
            roleTemplateSkills
                .groupBy { it.roleTemplateId }
                .mapValues { (_, links) ->
                    links.map { it.skillId }.distinct()
                }
        val postRoleSkillsByRole =
            postRoleSkills.groupBy { it.databaseRoleId }
        val responsibilitiesByRole =
            postRoleResponsibilities.groupBy { it.databaseRoleId }
        val questionsByRole =
            postRoleScreeningQuestions.groupBy { it.databaseRoleId }
        val targetRoleIdsByScheduleId =
            scheduleItemRoles
                .groupBy { it.scheduleItemId }
                .mapValues { (_, links) ->
                    links.map { it.roleTemplateId }.distinct()
                }
        val skillPathNames =
            skillPaths.associate { it.skillPathId to it.name }
        val skillNames =
            skills.associate { it.skillId to it.name }

        return posts
            .filter { post ->
                post.status in setOf(
                    "PUBLISHED",
                    "COMPLETED",
                    "CLOSED",
                    "CANCELLED"
                )
            }
            .sortedBy { it.postId }
            .map { post ->
                val organisation =
                    organisationsById[post.organisationId]
                val physical = physicalByPostId[post.postId]
                val remote = remoteByPostId[post.postId]
                val metric = metrics[post.postId]

                val roles = postRoles
                    .filter { it.postId == post.postId }
                    .sortedBy { it.databaseId }
                    .map { postRole ->
                        val normalizedSkills =
                            postRoleSkillsByRole[
                                postRole.databaseId
                            ].orEmpty()
                        val normalizedResponsibilities =
                            responsibilitiesByRole[
                                postRole.databaseId
                            ].orEmpty()
                                .sortedBy { it.responsibilityNo }
                                .map { it.responsibilityText }
                        val normalizedQuestions =
                            questionsByRole[
                                postRole.databaseId
                            ].orEmpty()
                                .sortedBy { it.questionNo }
                                .map { it.questionText }
                        val roleMetric =
                            roleMetrics[postRole.databaseId]
                        val template =
                            roleTemplatesById[
                                postRole.roleTemplateId
                            ]

                        val roleScheduleItems =
                            scheduleItems
                                .filter { item ->
                                    val targetRoleIds =
                                        targetRoleIdsByScheduleId[
                                            item.scheduleItemId
                                        ].orEmpty()
                                    item.postId == post.postId &&
                                            item.scheduleType != "TRAINING" &&
                                            (item.scheduleType.isNullOrBlank() || item.scheduleType == template?.roleMode) &&
                                            (
                                                    targetRoleIds.isEmpty() ||
                                                            postRole.roleTemplateId in
                                                            targetRoleIds
                                                    )
                                }
                                .sortedWith(
                                    compareBy<ScheduleItemRow> {
                                        it.scheduleDate
                                    }.thenBy {
                                        it.startTime.orEmpty()
                                    }
                                )
                                .map { item ->
                                    VolunteerRoleScheduleItem(
                                        scheduleDate =
                                            formatDatabaseDate(
                                                item.scheduleDate
                                            ),
                                        scheduleTime =
                                            listOfNotNull(item.startTime?.let(::formatDatabaseTime),
                                                item.endTime?.let(::formatDatabaseTime)).joinToString(" - ")
                                                .ifBlank { "Time not specified" },
                                        scheduleActivity = item.title,
                                        rawDate = item.scheduleDate,
                                        startTime = item.startTime.orEmpty(),
                                        endTime = item.endTime.orEmpty(),
                                        scheduleType = item.scheduleType.orEmpty(),
                                        location = item.location.orEmpty(),
                                        notes = item.notes.orEmpty(),
                                        assignedToRole = postRole.roleTemplateId in targetRoleIdsByScheduleId[item.scheduleItemId].orEmpty()
                                    )
                                }

                        val practisedSkillIds =
                            if (normalizedSkills.isNotEmpty()) {
                                normalizedSkills.map { it.skillId }
                            } else {
                                templateSkillIdsByRoleId[
                                    postRole.roleTemplateId
                                ].orEmpty()
                            }

                        val requiredSkillRequirements =
                            normalizedSkills.mapNotNull { link ->
                                link.requiredExperience?.let { experience ->
                                    RequiredSkillRequirementRow(
                                        skillId = link.skillId,
                                        requiredExperience = experience
                                    )
                                }
                            }

                        VolunteerOpportunityRole(
                            roleId = stableNavigationId(
                                postRole.databaseId
                            ),
                            roleTemplateId = postRole.roleTemplateId,
                            roleTitle =
                                template?.roleName
                                    ?: "Volunteer Role",
                            roleLevel =
                                template?.defaultLevel
                                    .orEmpty()
                                    .ifBlank { "BEGINNER" }
                                    .toDisplayWords(),
                            roleVacancies =
                                roleMetric?.availableSpots
                                    ?: postRole.capacity,
                            rolePrimarySkillPath =
                                template
                                    ?.skillPathId
                                    ?.let(skillPathNames::get)
                                    ?: "Community Volunteering",
                            roleSkillsPractised =
                                practisedSkillIds.map { skillId ->
                                    skillNames[skillId] ?: skillId
                                },
                            roleExperienceRequirement =
                                requiredSkillRequirements
                                    .joinToString("\n") { requirement ->
                                        val skillName =
                                            skillNames[requirement.skillId]
                                                ?: requirement.skillId

                                        "$skillName: " +
                                                "${requirement.requiredExperience} " +
                                                "verified assignment(s)"
                                    }
                                    .ifBlank {
                                        "No previous experience is required."
                                    },
                            roleExtraApplicationQuestions =
                                normalizedQuestions,
                            roleSpecificAssignment = postRole.roleNotes.orEmpty()
                                .ifBlank { template?.description.orEmpty() },
                            roleResponsibilities =
                                normalizedResponsibilities,
                            roleScheduleItems = roleScheduleItems,
                            roleMinimumSkillPathLevel =
                                levelNumber(template?.defaultLevel),
                            roleApplicationFlow =
                                if (
                                    normalizedQuestions
                                        .isEmpty()
                                ) {
                                    VolunteerRoleApplicationFlow
                                        .DIRECT_SUBMISSION
                                } else {
                                    VolunteerRoleApplicationFlow
                                        .ADDITIONAL_FORM
                                },
                            roleApplicationMethod =
                                if (
                                    postRole.applicationMethod ==
                                    "INSTANT_JOIN"
                                ) {
                                    VolunteerRoleApplicationMethod
                                        .INSTANT_JOIN
                                } else {
                                    VolunteerRoleApplicationMethod
                                        .REVIEW_APPLICANTS
                                },
                            roleMode = template?.roleMode.orEmpty(),
                            roleSubmissionRequirement = if (template?.roleMode == "REMOTE" && remote?.submissionMode == "SHARED_TEAM")
                                remote.sharedDeliverable.orEmpty() else postRole.individualSubmissionRequirement.orEmpty(),
                            roleSubmissionInstruction = if (template?.roleMode != "REMOTE") "" else if (remote?.submissionMode == "SHARED_TEAM")
                                "Shared team submission. The ${remote.responsibleRoleId?.let { roleTemplatesById[it]?.roleName } ?: "designated responsible"} role uploads the team's file. " +
                                    (if (remote.responsibleRoleId == postRole.roleTemplateId) "This is the responsible role." else "Other team members do not upload a separate file.")
                                else if (remote?.submissionMode == "INDIVIDUAL") "Individual submission: each accepted volunteer submits their own file."
                                else "Submission arrangements are unavailable. Sync or contact the organisation.",
                            roleDatabaseId = postRole.databaseId
                        )
                    }

                val opportunityType =
                    post.mode.toDisplayWords()
                val partnershipPartners = impactWeavePartnerRowsByPostId[post.postId]
                    .orEmpty()
                    .groupBy { it.partnerOrganisationId }
                    .map { (partnerId, rows) ->
                        VolunteerOpportunityPartner(
                            organisationId = partnerId,
                            organisationName = rows.first().partnerOrganisationName,
                            contributions = rows.map { row ->
                                VolunteerOpportunityPartnershipContribution(
                                    supportType = row.supportType,
                                    needResourceName = row.needResourceName,
                                    providerResourceName = row.providerResourceName,
                                    quantityProvided = row.quantityProvided,
                                    capacityProvided = row.capacityProvided
                                )
                            }
                        )
                    }
                    .sortedBy { it.organisationName.lowercase(Locale.ROOT) }
                val startDate =
                    listOfNotNull(physical?.startDate, remote?.startDate).filter(String::isNotBlank).minOrNull().orEmpty()
                val remainingRoleSpots =
                    roles.sumOf { role ->
                        role.roleVacancies
                    }

                val roleApplicationCount =
                    postRoles
                        .filter { role ->
                            role.postId == post.postId
                        }
                        .sumOf { role ->
                            roleMetrics[role.databaseId]
                                ?.applicationCount
                                ?: 0
                        }

                VolunteerOpportunityEvent(
                    eventId = stableNavigationId(post.postId),
                    eventTitle = post.title,
                    eventOrganisationName =
                        organisation?.organisationName
                            ?: "VolunteerLink Organisation",
                    eventOrganisationId = post.organisationId,
                    eventIsVerifiedOrganisation =
                        organisation?.verificationStatus ==
                                "VERIFIED",
                    eventOpportunityType = opportunityType,
                    eventCategory = parseCategory(post.category),
                    eventLocation =
                        physical?.locationName
                            ?: "Online",
                    // A real distance is calculated by the session store only
                    // after Android provides the volunteer's device location.
                    eventDistanceKm = null,
                    eventDate = formatDatabaseDate(startDate),
                    eventPhysicalEndDate = physical?.endDate.orEmpty(),
                    eventPhysicalStartTime = physical?.startTime.orEmpty(),
                    eventPhysicalEndTime = physical?.endTime.orEmpty(),
                    eventTimeZone = physical?.timeZone?.takeIf { it.isNotBlank() } ?: "Asia/Kuala_Lumpur",
                    eventRemoteEndDate = (remote?.newEndDate ?: remote?.endDate).orEmpty(),
                    eventRemoteOriginalEndDate = remote?.endDate.orEmpty(),
                    eventMeetingPoint = physical?.meetingPoint.orEmpty(),
                    eventIsPartnershipPost = !post.impactWeaveDraftId.isNullOrBlank(),
                    eventPartnershipPartners = partnershipPartners,
                    eventEndDate = formatDatabaseDate(
                        listOfNotNull(physical?.endDate, remote?.newEndDate ?: remote?.endDate).maxOrNull() ?: startDate
                    ),
                    eventTime =
                        if (physical != null) {
                            "${formatDatabaseTime(physical.startTime)} - " +
                                    formatDatabaseTime(physical.endTime)
                        } else {
                            "Flexible"
                        },
                    eventAvailableSpots =
                        remainingRoleSpots,
                    eventApplicationCount =
                        if (roleMetrics.isNotEmpty()) {
                            roleApplicationCount
                        } else {
                            metric?.applicationCount ?: 0
                        },
                    eventDescription = post.description,
                    eventIsLongTerm =
                        listOfNotNull(physical?.endDate, remote?.newEndDate ?: remote?.endDate)
                            .any { it != startDate },
                    eventVolunteerRoles = roles,
                    eventIsGovernmentApproved =
                        false,
                    eventFullAddress =
                        physical?.locationAddress
                            ?.ifBlank { physical.locationName }
                            ?: "Online",
                    eventCauseName =
                        post.category
                            ?.toDisplayWords()
                            .orEmpty(),
                    eventContactEmail =
                        organisation?.contactEmail.orEmpty(),
                    eventContactPhone =
                        organisation?.contactPhone.orEmpty(),
                    eventShareLink =
                        "https://volunteerlink.example/opportunities/" +
                                post.postId,
                    eventLatitude = physical?.latitude,
                    eventLongitude = physical?.longitude,
                    eventThumbnailPath = post.thumbnailPath,
                    eventDatabaseId = post.postId,
                    eventStatus = post.status,
                    eventPhysicalStartDate = physical?.startDate.orEmpty(),
                    eventRemoteStartDate = remote?.startDate.orEmpty(),
                    eventApplicationStartDate = listOfNotNull(
                        physical?.startDate, remote?.startDate
                    ).filter { it.isNotBlank() }.minOrNull().orEmpty()
                )
            }
    }

    private fun mapApplications(
        participationRows: List<RoleParticipationRow>,
        events: List<VolunteerOpportunityEvent>,
        achievements: Map<String, VolunteerAchievementRow>
    ): List<VolunteerOpportunityApplication> {
        val roleAndEventByDatabaseId =
            buildMap {
                events.forEach { event ->
                    event.eventVolunteerRoles.forEach { role ->
                        put(role.roleDatabaseId, event to role)
                    }
                }
            }

        return participationRows
            .sortedByDescending { it.createdAt }
            .mapNotNull { participation ->
                val (event, role) =
                    roleAndEventByDatabaseId[
                        participation.databaseRoleId
                    ] ?: return@mapNotNull null

                val status =
                    when {
                        participation.completionStatus ==
                                "COMPLETED" ->
                            VolunteerApplicationStatus.COMPLETED

                        participation.completionStatus ==
                                "NOT_COMPLETED" ->
                            VolunteerApplicationStatus.NOT_COMPLETED

                        participation.applicationStatus ==
                                "ACCEPTED" ->
                            VolunteerApplicationStatus.ACCEPTED

                        participation.applicationStatus ==
                                "DECLINED" ->
                            VolunteerApplicationStatus.REJECTED

                        participation.applicationStatus ==
                                "CANCELLED" ->
                            VolunteerApplicationStatus.CANCELLED

                        else ->
                            VolunteerApplicationStatus.PENDING
                    }

                val achievement =
                    achievements[participation.participationId]

                val verifiedMinutes =
                    achievement?.verifiedMinutes

                val certificateId =
                    if (status == VolunteerApplicationStatus.COMPLETED) {
                        "VL-${participation.participationId}-" +
                                (achievement?.completedAt
                                    ?: participation.completedAt
                                    ?: participation.createdAt)
                                    .take(4)
                    } else {
                        null
                    }

                VolunteerOpportunityApplication(
                    applicationId = stableNavigationId(
                        participation.participationId
                    ),
                    applicationEventId = event.eventId,
                    applicationEventTitle = event.eventTitle,
                    applicationOrganisationName =
                        event.eventOrganisationName,
                    applicationRoleTitle = role.roleTitle,
                    applicationSubmittedDate =
                        formatSubmittedDate(
                            participation.createdAt
                        ),
                    applicationStatus = status,
                    applicationCreatedAtRaw = participation.createdAt,
                    applicationRoleId = role.roleId,
                    applicationStatusMessage =
                        statusMessage(status, participation.decisionNote),
                    applicationRejectionReason =
                        participation.decisionNote,
                    applicationVerifiedHours =
                        verifiedMinutes?.let { minutes ->
                            (minutes + 59) / 60
                        },
                    applicationVerifiedMinutes =
                        verifiedMinutes,
                    applicationCertificateId =
                        certificateId,
                    applicationCompletedDate =
                        (achievement?.completedAt
                            ?: participation.completedAt)
                            ?.let(::formatSubmittedDate),
                    applicationOrganisationFeedback =
                        achievement?.feedback,
                    applicationVolunteerName =
                        achievement?.volunteerName
                            ?.takeIf(String::isNotBlank)
                            ?: "VolunteerLink Volunteer",
                    applicationEventDate = event.eventDate,
                    applicationEventTime = event.eventTime,
                    applicationEventLocation =
                        event.eventFullAddress.ifBlank {
                            event.eventLocation
                        },
                    applicationPrimarySkillPath =
                        role.rolePrimarySkillPath,
                    applicationPractisedSkills =
                        role.roleSkillsPractised,
                    applicationRoleMode = role.roleMode,
                    applicationDatabaseId =
                        participation.participationId
                )
            }
    }

    private fun mapRpcApplications(
        applicationRows: List<MyVolunteerApplicationRow>,
        events: List<VolunteerOpportunityEvent>
    ): List<VolunteerOpportunityApplication> {
        return applicationRows
            .sortedByDescending { row -> row.createdAt }
            .map { row ->
                val status = applicationStatus(
                    applicationStatus = row.applicationStatus,
                    completionStatus = row.completionStatus
                )
                val certificateId =
                    if (status == VolunteerApplicationStatus.COMPLETED) {
                        "VL-${row.participationId}-" +
                                (row.completedAt ?: row.createdAt).take(4)
                    } else {
                        null
                    }

                VolunteerOpportunityApplication(
                    applicationId =
                        stableNavigationId(row.participationId),
                    applicationEventId =
                        stableNavigationId(row.postId),
                    applicationEventTitle = row.eventTitle,
                    applicationOrganisationName =
                        row.organisationName,
                    applicationRoleTitle = row.roleTitle,
                    applicationSubmittedDate =
                        formatSubmittedDate(row.createdAt),
                    applicationStatus = status,
                    applicationCreatedAtRaw = row.createdAt,
                    applicationRoleId =
                        stableNavigationId(row.postRoleId),
                    applicationStatusMessage = statusMessage(status, row.decisionNote),
                    applicationRejectionReason = row.decisionNote,
                    applicationVerifiedHours =
                        row.verifiedMinutes?.let { minutes ->
                            (minutes + 59) / 60
                        },
                    applicationVerifiedMinutes = row.verifiedMinutes
                        ?: if (status == VolunteerApplicationStatus.NOT_COMPLETED) 0 else null,
                    applicationCertificateId = certificateId,
                    applicationCompletedDate =
                        row.completedAt?.let(::formatSubmittedDate),
                    applicationOrganisationFeedback = row.feedback,
                    applicationCompletionReason = row.completionReason,
                    applicationScreeningQuestions =
                        row.screeningAnswers.map { it.questionText },
                    applicationScreeningAnswers =
                        row.screeningAnswers.map { it.answerText },
                    applicationVolunteerName = row.volunteerName,
                    applicationEventDate =
                        row.eventDate?.let(::formatDatabaseDate),
                    applicationEventTime = row.eventTime,
                    applicationEventLocation = row.eventLocation,
                    applicationPrimarySkillPath = row.primarySkillPath,
                    applicationPractisedSkills =
                        row.practisedSkillNames,
                    applicationRoleMode =
                        events.firstOrNull {
                            it.eventDatabaseId == row.postId
                        }?.eventVolunteerRoles?.firstOrNull {
                            it.roleDatabaseId == row.postRoleId
                        }?.roleMode.orEmpty(),
                    applicationDatabaseId = row.participationId
                )
            }
    }
}

private fun applicationStatus(
    applicationStatus: String,
    completionStatus: String
): VolunteerApplicationStatus {
    return when {
        completionStatus == "COMPLETED" ->
            VolunteerApplicationStatus.COMPLETED
        completionStatus == "NOT_COMPLETED" ->
            VolunteerApplicationStatus.NOT_COMPLETED
        applicationStatus == "ACCEPTED" ->
            VolunteerApplicationStatus.ACCEPTED
        applicationStatus == "DECLINED" ->
            VolunteerApplicationStatus.REJECTED
        applicationStatus == "CANCELLED" ->
            VolunteerApplicationStatus.CANCELLED
        else -> VolunteerApplicationStatus.PENDING
    }
}

private fun stableNavigationId(databaseId: String): Int {
    // Hash the complete database identifier. Extracting only digits made
    // DEMO_POST_003 and POST003 both navigate as event 3, which mixed map
    // markers, details and directions from unrelated opportunities.
    return databaseId.hashCode() and Int.MAX_VALUE
}

private fun String.toDisplayWords(): String =
    lowercase()
        .split('_')
        .joinToString(" ") { word ->
            word.replaceFirstChar(Char::uppercase)
        }

private fun levelNumber(databaseLevel: String?): Int =
    when (databaseLevel?.uppercase()) {
        "INTERMEDIATE" -> 2
        "ADVANCED" -> 3
        else -> 1
    }

private fun parseCategory(
    databaseCategory: String?
): VolunteerOpportunityCategory {
    return runCatching {
        VolunteerOpportunityCategory.valueOf(
            databaseCategory.orEmpty().uppercase()
        )
    }.getOrDefault(VolunteerOpportunityCategory.COMMUNITY)
}

private fun formatDatabaseDate(databaseDate: String): String {
    val parts = databaseDate.split('-')
    if (parts.size != 3) return databaseDate.ifBlank { "To be confirmed" }

    val month = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ).getOrNull(parts[1].toIntOrNull()?.minus(1) ?: -1)
        ?: return databaseDate

    return "${parts[2].toIntOrNull() ?: parts[2]} $month ${parts[0]}"
}

private fun formatDatabaseTime(databaseTime: String): String {
    val parts = databaseTime.split(':')
    val hour24 = parts.firstOrNull()?.toIntOrNull() ?: return databaseTime
    val minute = parts.getOrNull(1) ?: "00"
    val suffix = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when (hour24 % 12) {
        0 -> 12
        else -> hour24 % 12
    }
    return "$hour12:$minute $suffix"
}

private fun formatSubmittedDate(timestamp: String): String {
    val normalizedTimestamp = timestamp
        .trim()
        .replace(' ', 'T')
        .replace(Regex("([+-]\\d{2})$"), "$1:00")
        .replace(Regex("(\\.\\d{3})\\d+(?=[+-]|Z$)"), "$1")

    val parsedDate = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    ).firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalizedTimestamp)
        }.getOrNull()
    } ?: return timestamp.take(10).let(::formatDatabaseDate)

    return SimpleDateFormat("d MMM yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    }.format(parsedDate)
}

private fun statusMessage(
    status: VolunteerApplicationStatus,
    decisionNote: String? = null
): String =
    when (status) {
        VolunteerApplicationStatus.PENDING ->
            "Application received. Awaiting organisation review; your place is not confirmed yet."

        VolunteerApplicationStatus.ACCEPTED ->
            "Your place is confirmed. You are accepted for this role."

        VolunteerApplicationStatus.REJECTED ->
            decisionNote?.takeIf { it.isNotBlank() }
                ?: "This application was not accepted. You may choose another open role, but cannot reapply to this same role."

        VolunteerApplicationStatus.COMPLETED ->
            "This volunteer role has been completed."

        VolunteerApplicationStatus.NOT_COMPLETED ->
            "The organisation did not verify this role as completed."

        VolunteerApplicationStatus.CANCELLED ->
            decisionNote?.takeIf { it.isNotBlank() } ?: "This application was cancelled. No place is reserved."
    }

@Serializable
private data class ImpactWeavePostContributionRow(
    @SerialName("impact_weave_draft_id") val impactWeaveDraftId: String,
    @SerialName("partner_organisation_id") val partnerOrganisationId: String,
    @SerialName("partner_organisation_name") val partnerOrganisationName: String,
    @SerialName("support_type") val supportType: String,
    @SerialName("need_resource_name") val needResourceName: String,
    @SerialName("provider_resource_name") val providerResourceName: String? = null,
    @SerialName("quantity_provided") val quantityProvided: Int? = null,
    @SerialName("capacity_provided") val capacityProvided: Int? = null
)

@Serializable
private data class OrganisationRow(
    @SerialName("organisation_id")
    val organisationId: String,
    @SerialName("organisation_name")
    val organisationName: String,
    @SerialName("contact_email")
    val contactEmail: String? = null,
    @SerialName("contact_phone")
    val contactPhone: String? = null,
    @SerialName("verification_status")
    val verificationStatus: String
)

// Made internal (was private) so other repositories in this package — e.g.
// ProfileRepository.kt — can decode volunteer_posts rows with this same
// class instead of declaring their own duplicate.
@Serializable
internal data class VolunteerPostRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("organisation_id")
    val organisationId: String,
    val title: String,
    val description: String,
    val mode: String,
    val status: String,
    @SerialName("thumbnail_path")
    val thumbnailPath: String? = null,
    val category: String? = null,
    @SerialName("impact_weave_draft_id")
    val impactWeaveDraftId: String? = null
)

@Serializable
private data class PhysicalDetailRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String = "",
    @SerialName("time_zone")
    val timeZone: String? = null,
    @SerialName("meeting_point")
    val meetingPoint: String? = null,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("location_name")
    val locationName: String,
    @SerialName("location_address")
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("volunteer_capacity")
    val volunteerCapacity: Int
)

@Serializable
private data class RemoteDetailRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String,
    @SerialName("new_end_date")
    val newEndDate: String? = null,
    @SerialName("submission_mode") val submissionMode: String? = null,
    @SerialName("shared_deliverable") val sharedDeliverable: String? = null,
    @SerialName("responsible_role_template_id") val responsibleRoleId: String? = null,
    @SerialName("volunteer_capacity")
    val volunteerCapacity: Int
)

@Serializable
private data class RequiredSkillRequirementRow(
    @SerialName("skill_id")
    val skillId: String,
    @SerialName("required_experience")
    val requiredExperience: Int = 0
)

@Serializable
private data class PostRoleRow(
    @SerialName("legacy_post_role_id")
    val legacyPostRoleId: String? = null,
    @SerialName("post_id")
    val postId: String,
    @SerialName("role_template_id")
    val roleTemplateId: String,
    val capacity: Int,
    @SerialName("application_method")
    val applicationMethod: String,
    val responsibilities: List<String> = emptyList(),
    @SerialName("practised_skills")
    val practisedSkills: List<String> = emptyList(),
    @SerialName("required_skill_requirements")
    val requiredSkillRequirements:
    List<RequiredSkillRequirementRow> = emptyList(),
    @SerialName("screening_questions")
    val screeningQuestions: List<String> = emptyList(),
    @SerialName("role_notes")
    val roleNotes: String? = null,
    @SerialName("individual_submission_requirement")
    val individualSubmissionRequirement: String? = null
) {
    val databaseId: String
        get() = "$postId|$roleTemplateId"
}

@Serializable
private data class PostRoleSkillRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("skill_id")
    val skillId: String,
    @SerialName("required_experience")
    val requiredExperience: Int? = null
) {
    val databaseRoleId: String
        get() = "$postId|$roleTemplateId"
}

@Serializable
private data class PostRoleResponsibilityRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("responsibility_no")
    val responsibilityNo: Int,
    @SerialName("responsibility_text")
    val responsibilityText: String
) {
    val databaseRoleId: String
        get() = "$postId|$roleTemplateId"
}

@Serializable
private data class PostRoleScreeningQuestionRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("question_no")
    val questionNo: Int,
    @SerialName("question_text")
    val questionText: String
) {
    val databaseRoleId: String
        get() = "$postId|$roleTemplateId"
}

@Serializable
private data class RoleTemplateRow(
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("role_name")
    val roleName: String,
    @SerialName("skill_path_id")
    val skillPathId: String,
    val description: String? = null,
    @SerialName("role_mode")
    val roleMode: String = "",
    @SerialName("skills_practised")
    val skillsPractised: List<String> = emptyList(),
    @SerialName("default_level")
    val defaultLevel: String = "BEGINNER"
)

@Serializable
private data class RoleTemplateSkillRow(
    @SerialName("role_template_id")
    val roleTemplateId: String,
    @SerialName("skill_id")
    val skillId: String
)

@Serializable
private data class SkillPathNameRow(
    @SerialName("skill_path_id")
    val skillPathId: String,
    val name: String
)

@Serializable
private data class SkillNameRow(
    @SerialName("skill_id")
    val skillId: String,
    val name: String
)

@Serializable
private data class ScheduleItemRow(
    @SerialName("schedule_item_id")
    val scheduleItemId: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("schedule_date")
    val scheduleDate: String,
    @SerialName("schedule_type")
    val scheduleType: String? = null,
    val title: String,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val location: String? = null,
    val notes: String? = null
)

@Serializable
private data class ScheduleItemRoleRow(
    @SerialName("schedule_item_id")
    val scheduleItemId: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("role_template_id")
    val roleTemplateId: String
)

@Serializable
private data class RoleParticipationRow(
    @SerialName("post_id")
    val postId: String,

    @SerialName("role_template_id")
    val roleTemplateId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("application_status")
    val applicationStatus: String,

    @SerialName("completion_status")
    val completionStatus: String,

    @SerialName("auto_completed")
    val autoCompleted: Boolean = false,

    @SerialName("joined_at")
    val joinedAt: String? = null,

    @SerialName("completed_at")
    val completedAt: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("cancelled_at")
    val cancelledAt: String? = null,

    @SerialName("decision_note")
    val decisionNote: String? = null
) {
    val participationId: String
        get() =
            "$postId|$roleTemplateId|$userId"

    val databaseRoleId: String
        get() =
            "$postId|$roleTemplateId"
}

@Serializable
private data class OpportunityMetricRow(
    @SerialName("post_id")
    val postId: String,
    @SerialName("application_count")
    val applicationCount: Int,
    @SerialName("available_spots")
    val availableSpots: Int
)

@Serializable
private data class RoleMetricRow(
    @SerialName("post_role_id")
    val postRoleId: String,
    @SerialName("application_count")
    val applicationCount: Int,
    @SerialName("accepted_count")
    val acceptedCount: Int,
    @SerialName("available_spots")
    val availableSpots: Int
)

@Serializable
private data class VolunteerAchievementRow(
    @SerialName("participation_id")
    val participationId: String,
    @SerialName("verified_minutes")
    val verifiedMinutes: Int,
    @SerialName("completed_at")
    val completedAt: String? = null,
    val rating: Int? = null,
    val feedback: String? = null,
    @SerialName("volunteer_name")
    val volunteerName: String? = null
)

@Serializable
private data class MyVolunteerApplicationRow(
    @SerialName("participation_id")
    val participationId: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("post_role_id")
    val postRoleId: String,
    @SerialName("event_title")
    val eventTitle: String,
    @SerialName("organisation_name")
    val organisationName: String,
    @SerialName("role_title")
    val roleTitle: String,
    @SerialName("volunteer_name")
    val volunteerName: String,
    @SerialName("application_status")
    val applicationStatus: String,
    @SerialName("completion_status")
    val completionStatus: String,
    @SerialName("decision_note")
    val decisionNote: String? = null,
    @SerialName("verified_minutes")
    val verifiedMinutes: Int? = null,
    @SerialName("completed_at")
    val completedAt: String? = null,
    val feedback: String? = null,
    @SerialName("completion_reason")
    val completionReason: String? = null,
    @SerialName("screening_answers")
    val screeningAnswers: List<ApplicationScreeningAnswerRow> = emptyList(),
    @SerialName("primary_skill_path")
    val primarySkillPath: String? = null,
    @SerialName("practised_skill_names")
    val practisedSkillNames: List<String> = emptyList(),
    @SerialName("event_date")
    val eventDate: String? = null,
    @SerialName("event_time")
    val eventTime: String? = null,
    @SerialName("event_location")
    val eventLocation: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
private data class ApplicationScreeningAnswerRow(
    @SerialName("question_no") val questionNo: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("answer_text") val answerText: String
)

@Serializable
private data class SavedOpportunityIdRow(
    @SerialName("post_id") val postId: String
)
