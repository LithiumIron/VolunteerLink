package com.example.volunteerlink.data

import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerRoleApplicationFlow
import com.example.volunteerlink.model.VolunteerRoleApplicationMethod
import com.example.volunteerlink.model.VolunteerRoleScheduleItem
import androidx.compose.runtime.mutableStateListOf

object VolunteerOpportunitySampleData {

    val volunteerOpportunityEvents =
        listOf(

            VolunteerOpportunityEvent(
                eventId = 1,
                eventTitle = "Charity Fun Run 2026",
                eventOrganisationName =
                    "Green Earth Society",
                eventIsVerifiedOrganisation = true,
                eventOpportunityType = "Physical",
                eventCategory =
                    VolunteerOpportunityCategory.SPORTS,
                eventLocation = "Kuala Lumpur",
                eventDistanceKm = 2.3,
                eventDate = "15 Aug 2026",
                eventTime = "7:00 AM - 12:00 PM",
                eventAvailableSpots = 18,
                eventApplicationCount = 86,
                eventDescription =
                    "Support participants and organisers " +
                            "during a community charity run. " +
                            "Volunteers will help create a safe, " +
                            "welcoming and organised experience.",
                eventIsLongTerm = false,

                eventVolunteerRoles =
                    listOf(

                        VolunteerOpportunityRole(
                            roleId = 101,
                            roleTemplateId = "ROLE004",
                            roleTitle =
                                "Greeter & Check-in Assistant",
                            roleLevel = "Beginner",
                            roleVacancies = 5,
                            rolePrimarySkillPath =
                                "Communication & Guest Services",
                            roleSkillsPractised =
                                listOf(
                                    "Active Listening",
                                    "Customer Service",
                                    "Attendee Guidance"
                                ),
                            roleExperienceRequirement =
                                "No previous experience is required. " +
                                        "Volunteers should be friendly " +
                                        "and comfortable speaking to participants.",
                            roleSpecificAssignment =
                                "Welcome participants, answer event " +
                                        "questions and guide attendees to " +
                                        "registration, activity and refreshment areas.",
                            roleTrainingDetails =
                                "A 30-minute briefing will be provided " +
                                        "before the event begins.",
                            roleResponsibilities =
                                listOf(
                                    "Welcome and guide event participants",
                                    "Provide clear event information",
                                    "Assist the registration team",
                                    "Report participant concerns to the coordinator"
                                ),
                            roleScheduleItems =
                                listOf(
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "7:00 AM",
                                        scheduleActivity =
                                            "Volunteer registration and briefing"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "7:30 AM",
                                        scheduleActivity =
                                            "Participant welcome and guidance"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "11:30 AM",
                                        scheduleActivity =
                                            "Closing assistance and clean-up"
                                    )
                                ),
                            roleMinimumSkillPathLevel = 1,
                            roleApplicationFlow =
                                VolunteerRoleApplicationFlow
                                    .DIRECT_SUBMISSION,
                            roleApplicationMethod =
                                VolunteerRoleApplicationMethod
                                    .INSTANT_JOIN
                        ),

                        VolunteerOpportunityRole(
                            roleId = 102,
                            roleTemplateId = "ROLE007",
                            roleTitle =
                                "Equipment & Supply Assistant",
                            roleLevel = "Intermediate",
                            roleVacancies = 6,
                            rolePrimarySkillPath =
                                "Operations, Logistics & Safety",
                            roleSkillsPractised =
                                listOf(
                                    "Logistics Coordination",
                                    "Inventory Handling",
                                    "Team Collaboration"
                                ),
                            roleExperienceRequirement =
                                "Previous volunteering or event " +
                                        "operations experience is recommended.",
                            roleExtraApplicationQuestions =
                                listOf(
                                    "Describe any relevant volunteering experience.",
                                    "Are you comfortable handling event equipment?"
                                ),
                            roleSpecificAssignment =
                                "Support equipment preparation, inventory " +
                                        "tracking and distribution of event supplies.",
                            roleTrainingDetails =
                                "A logistics and safety briefing will be " +
                                        "conducted before deployment.",
                            roleResponsibilities =
                                listOf(
                                    "Prepare and organise event supplies",
                                    "Track equipment movement",
                                    "Coordinate with logistics volunteers",
                                    "Keep walkways and supply areas safe"
                                ),
                            roleScheduleItems =
                                listOf(
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "6:30 AM",
                                        scheduleActivity =
                                            "Equipment preparation"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "7:00 AM",
                                        scheduleActivity =
                                            "Safety and logistics briefing"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "11:30 AM",
                                        scheduleActivity =
                                            "Equipment collection and inventory check"
                                    )
                                ),
                            roleMinimumSkillPathLevel = 2,
                            roleApplicationFlow =
                                VolunteerRoleApplicationFlow
                                    .ADDITIONAL_FORM,
                            roleApplicationMethod =
                                VolunteerRoleApplicationMethod
                                    .REVIEW_APPLICANTS
                        )
                    ),

                eventIsGovernmentApproved = true,
                eventFullAddress =
                    "Dataran Merdeka, Jalan Raja, " +
                            "50050 Kuala Lumpur",
                eventCauseName =
                    "Community Health and Wellbeing",
                eventContactEmail =
                    "volunteer@greenearth.org.my",
                eventContactPhone =
                    "+60 12-345 6789",
                eventShareLink =
                    "https://volunteerlink.example/events/1"
            ),

            VolunteerOpportunityEvent(
                eventId = 2,
                eventTitle = "Food Bank Distribution",
                eventOrganisationName =
                    "Community Food Support",
                eventIsVerifiedOrganisation = true,
                eventOpportunityType = "Physical",
                eventCategory =
                    VolunteerOpportunityCategory.COMMUNITY,
                eventLocation = "Butterworth",
                eventDistanceKm = 5.1,
                eventDate = "22 Aug 2026",
                eventTime = "9:00 AM - 2:00 PM",
                eventAvailableSpots = 12,
                eventApplicationCount = 124,
                eventDescription =
                    "Help prepare and distribute essential food " +
                            "supplies to families in the local community.",
                eventIsLongTerm = false,

                eventVolunteerRoles =
                    listOf(
                        VolunteerOpportunityRole(
                            roleId = 201,
                            roleTemplateId = "ROLE007",
                            roleTitle =
                                "Equipment & Supply Assistant",
                            roleLevel = "Beginner",
                            roleVacancies = 12,
                            rolePrimarySkillPath =
                                "Operations, Logistics & Safety",
                            roleSkillsPractised =
                                listOf(
                                    "Supply Distribution",
                                    "Inventory Handling",
                                    "Team Collaboration"
                                ),
                            roleExperienceRequirement =
                                "No previous experience is required. " +
                                        "Volunteers must be able to work in a team.",
                            roleSpecificAssignment =
                                "Pack food items, organise distribution " +
                                        "stations and assist recipients during collection.",
                            roleTrainingDetails =
                                "A short food-handling and distribution " +
                                        "briefing will be provided.",
                            roleResponsibilities =
                                listOf(
                                    "Pack essential food supplies",
                                    "Arrange supplies by collection category",
                                    "Guide recipients through collection",
                                    "Maintain a clean distribution area"
                                ),
                            roleScheduleItems =
                                listOf(
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "9:00 AM",
                                        scheduleActivity =
                                            "Registration and safety briefing"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "9:30 AM",
                                        scheduleActivity =
                                            "Food packing and station preparation"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "11:00 AM",
                                        scheduleActivity =
                                            "Community distribution"
                                    )
                                ),
                            roleMinimumSkillPathLevel = 1,
                            roleApplicationFlow =
                                VolunteerRoleApplicationFlow
                                    .DIRECT_SUBMISSION,
                            roleApplicationMethod =
                                VolunteerRoleApplicationMethod
                                    .INSTANT_JOIN
                        )
                    ),

                eventIsGovernmentApproved = false,
                eventFullAddress =
                    "Community Hall, Jalan Raja Uda, " +
                            "12300 Butterworth, Penang",
                eventCauseName =
                    "Food Security and Community Support",
                eventContactEmail =
                    "volunteer@communityfood.org.my",
                eventContactPhone =
                    "+60 4-332 1188",
                eventShareLink =
                    "https://volunteerlink.example/events/2"
            ),

            VolunteerOpportunityEvent(
                eventId = 3,
                eventTitle =
                    "Senior Digital Literacy Project",
                eventOrganisationName =
                    "DigitalCare Foundation",
                eventIsVerifiedOrganisation = true,
                eventOpportunityType = "Remote",
                eventCategory =
                    VolunteerOpportunityCategory.EDUCATION,
                eventLocation = "Online",
                eventDistanceKm = null,
                eventDate = "25 Aug 2026",
                eventTime = "Flexible",
                eventAvailableSpots = 8,
                eventApplicationCount = 42,
                eventDescription =
                    "Support the preparation of accessible digital " +
                            "learning materials for senior citizens.",
                eventIsLongTerm = true,

                eventVolunteerRoles =
                    listOf(
                        VolunteerOpportunityRole(
                            roleId = 301,
                            roleTemplateId = "ROLE022",
                            roleTitle =
                                "Content Writer",
                            roleLevel = "Beginner",
                            roleVacancies = 8,
                            rolePrimarySkillPath =
                                "Writing, Content & Digital Campaigns",
                            roleSkillsPractised =
                                listOf(
                                    "Content Writing",
                                    "Editing & Proofreading",
                                    "Digital Communication"
                                ),
                            roleExperienceRequirement =
                                "Basic writing skills and access to a " +
                                        "computer with an internet connection.",
                            roleExtraApplicationQuestions =
                                listOf(
                                    "Why are you interested in digital literacy?",
                                    "How many hours can you contribute each week?"
                                ),
                            roleSpecificAssignment =
                                "Prepare simple guides that teach senior " +
                                        "citizens how to use common digital services.",
                            roleTrainingDetails =
                                "Online training materials and a content " +
                                        "template will be provided.",
                            roleResponsibilities =
                                listOf(
                                    "Write clear step-by-step instructions",
                                    "Review content for readability",
                                    "Use accessible language and formatting",
                                    "Submit completed materials online"
                                ),
                            roleScheduleItems =
                                listOf(
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "Week 1",
                                        scheduleActivity =
                                            "Online onboarding and topic selection"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "Week 2",
                                        scheduleActivity =
                                            "First content draft"
                                    ),
                                    VolunteerRoleScheduleItem(
                                        scheduleTime =
                                            "Week 3",
                                        scheduleActivity =
                                            "Review and final submission"
                                    )
                                ),
                            roleMinimumSkillPathLevel = 1,
                            roleApplicationFlow =
                                VolunteerRoleApplicationFlow
                                    .ADDITIONAL_FORM,
                            roleApplicationMethod =
                                VolunteerRoleApplicationMethod
                                    .REVIEW_APPLICANTS
                        )
                    ),

                eventIsGovernmentApproved = false,
                eventFullAddress = "Online",
                eventCauseName =
                    "Digital Inclusion and Lifelong Learning",
                eventContactEmail =
                    "projects@digitalcare.org.my",
                eventContactPhone =
                    "+60 3-8899 2233",
                eventShareLink =
                    "https://volunteerlink.example/events/3"
            )
        )

    val volunteerApplications =
    mutableStateListOf(

        VolunteerOpportunityApplication(
            applicationId = 1,
            applicationEventId = 1,
            applicationEventTitle =
                "Charity Fun Run 2026",
            applicationOrganisationName =
                "Green Earth Society",
            applicationRoleTitle =
                "Equipment & Supply Assistant",
            applicationSubmittedDate =
                "2 days ago",
            applicationStatus =
                VolunteerApplicationStatus.PENDING,
            applicationRoleId = 102,
            applicationStatusMessage =
                "Your application is being reviewed."
        ),

            VolunteerOpportunityApplication(
                applicationId = 2,
                applicationEventId = 2,
                applicationEventTitle =
                    "Food Bank Distribution",
                applicationOrganisationName =
                    "Community Food Support",
                applicationRoleTitle =
                    "Equipment & Supply Assistant",
                applicationSubmittedDate =
                    "1 week ago",
                applicationStatus =
                    VolunteerApplicationStatus.ACCEPTED,
                applicationRoleId = 201,
                applicationStatusMessage =
                    "Your application has been accepted."
            )
        )

    fun hasApplicationForRole(
        eventId: Int,
        roleId: Int
    ): Boolean {
        return volunteerApplications
            .any { volunteerApplication ->
                volunteerApplication
                    .applicationEventId ==
                        eventId &&
                        volunteerApplication
                            .applicationRoleId ==
                        roleId
            }
    }

    fun submitApplication(
        eventId: Int,
        roleId: Int
    ): VolunteerOpportunityApplication? {
        val volunteerOpportunityEvent =
            findEventById(eventId)
                ?: return null

        val volunteerOpportunityRole =
            findRoleById(
                eventId = eventId,
                roleId = roleId
            ) ?: return null

        val existingApplication =
            volunteerApplications
                .firstOrNull {
                        volunteerApplication ->

                    volunteerApplication
                        .applicationEventId ==
                            eventId &&
                            volunteerApplication
                                .applicationRoleId ==
                            roleId
                }

        if (existingApplication != null) {
            return existingApplication
        }

        val nextApplicationId =
            (
                    volunteerApplications
                        .maxOfOrNull {
                                volunteerApplication ->

                            volunteerApplication
                                .applicationId
                        } ?: 0
                    ) + 1

        val newVolunteerApplication =
            VolunteerOpportunityApplication(
                applicationId =
                    nextApplicationId,
                applicationEventId =
                    volunteerOpportunityEvent
                        .eventId,
                applicationEventTitle =
                    volunteerOpportunityEvent
                        .eventTitle,
                applicationOrganisationName =
                    volunteerOpportunityEvent
                        .eventOrganisationName,
                applicationRoleTitle =
                    volunteerOpportunityRole
                        .roleTitle,
                applicationSubmittedDate =
                    "Just now",
                applicationStatus =
                    if (
                        volunteerOpportunityRole
                            .roleApplicationMethod ==
                        VolunteerRoleApplicationMethod
                            .INSTANT_JOIN
                    ) {
                        VolunteerApplicationStatus.ACCEPTED
                    } else {
                        VolunteerApplicationStatus.PENDING
                    },
                applicationRoleId =
                    volunteerOpportunityRole
                        .roleId,
                applicationStatusMessage =
                    if (
                        volunteerOpportunityRole
                            .roleApplicationMethod ==
                        VolunteerRoleApplicationMethod
                            .INSTANT_JOIN
                    ) {
                        "You joined this role successfully."
                    } else {
                        "Your application has been submitted " +
                                "and is waiting for organisation review."
                    }
            )

        volunteerApplications.add(
            index = 0,
            element = newVolunteerApplication
        )

        return newVolunteerApplication
    }

    fun findEventById(
        eventId: Int
    ): VolunteerOpportunityEvent? {
        return volunteerOpportunityEvents
            .firstOrNull { volunteerOpportunityEvent ->
                volunteerOpportunityEvent.eventId ==
                        eventId
            }
    }

    fun findRoleById(
        eventId: Int,
        roleId: Int
    ): VolunteerOpportunityRole? {
        return findEventById(eventId)
            ?.eventVolunteerRoles
            ?.firstOrNull { volunteerOpportunityRole ->
                volunteerOpportunityRole.roleId ==
                        roleId
            }
    }

    fun findApplicationById(
        applicationId: Int
    ): VolunteerOpportunityApplication? {
        return volunteerApplications
            .firstOrNull { volunteerOpportunityApplication ->
                volunteerOpportunityApplication
                    .applicationId ==
                        applicationId
            }
    }

    fun cancelApplication(
        applicationId: Int
    ): Boolean {
        val applicationIndex =
            volunteerApplications.indexOfFirst {
                    volunteerOpportunityApplication ->
                volunteerOpportunityApplication.applicationId ==
                        applicationId
            }

        if (applicationIndex == -1) {
            return false
        }

        val existingApplication =
            volunteerApplications[applicationIndex]

        if (
            existingApplication.applicationStatus !=
            VolunteerApplicationStatus.PENDING
        ) {
            return false
        }

        volunteerApplications[applicationIndex] =
            existingApplication.copy(
                applicationStatus =
                    VolunteerApplicationStatus.CANCELLED,
                applicationStatusMessage =
                    "You cancelled this application."
            )

        return true
    }
}
