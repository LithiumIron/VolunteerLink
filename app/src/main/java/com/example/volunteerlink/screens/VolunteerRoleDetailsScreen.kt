package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.VolunteerOpportunitySampleData
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerRoleApplicationFlow
import com.example.volunteerlink.model.VolunteerRoleApplicationMethod
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning

@Composable
fun VolunteerRoleDetailsScreen(
    volunteerEventId: Int,
    volunteerRoleId: Int,
    onBackSelected: () -> Unit,
    onJoinRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit,
    currentVolunteerSkillPathLevel: Int = 2
) {
    val volunteerOpportunityEvent =
        VolunteerOpportunitySampleData.findEventById(
            volunteerEventId
        )

    val volunteerOpportunityRole =
        VolunteerOpportunitySampleData.findRoleById(
            eventId = volunteerEventId,
            roleId = volunteerRoleId
        )

    if (
        volunteerOpportunityEvent == null ||
        volunteerOpportunityRole == null
    ) {
        VolunteerRoleNotFoundScreen(
            onBackSelected = onBackSelected
        )
        return
    }

    val volunteerIsEligible =
        currentVolunteerSkillPathLevel >=
                volunteerOpportunityRole
                    .roleMinimumSkillPathLevel

    val volunteerHasApplied =
        VolunteerOpportunitySampleData
            .hasApplicationForRole(
                eventId = volunteerEventId,
                roleId = volunteerRoleId
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerRoleDetailsTopBar(
            onBackSelected = onBackSelected
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                bottom = 20.dp
            )
        ) {
            item(
                key = "role_header"
            ) {
                VolunteerRoleHeaderSection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent,
                    volunteerOpportunityRole =
                        volunteerOpportunityRole
                )
            }

            item(
                key = "role_assignment"
            ) {
                VolunteerRoleAssignmentSection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent,
                    volunteerOpportunityRole =
                        volunteerOpportunityRole
                )
            }

            item(
                key = "role_requirements"
            ) {
                VolunteerRoleRequirementsSection(
                    volunteerOpportunityRole =
                        volunteerOpportunityRole
                )
            }

            item(
                key = "role_skills"
            ) {
                VolunteerRoleSkillsSection(
                    volunteerOpportunityRole =
                        volunteerOpportunityRole
                )
            }

            if (
                volunteerOpportunityRole
                    .roleResponsibilities
                    .isNotEmpty()
            ) {
                item(
                    key = "role_responsibilities"
                ) {
                    VolunteerRoleResponsibilitiesSection(
                        volunteerOpportunityRole =
                            volunteerOpportunityRole
                    )
                }
            }

            if (
                volunteerOpportunityRole
                    .roleScheduleItems
                    .isNotEmpty()
            ) {
                item(
                    key = "role_schedule"
                ) {
                    VolunteerRoleScheduleSection(
                        volunteerOpportunityRole =
                            volunteerOpportunityRole
                    )
                }
            }

            item(
                key = "role_eligibility"
            ) {
                VolunteerRoleEligibilitySection(
                    volunteerOpportunityRole =
                        volunteerOpportunityRole,
                    currentVolunteerSkillPathLevel =
                        currentVolunteerSkillPathLevel,
                    volunteerIsEligible =
                        volunteerIsEligible
                )
            }
        }

        VolunteerRoleJoinSection(
            volunteerOpportunityRole =
                volunteerOpportunityRole,
            volunteerIsEligible =
                volunteerIsEligible,
            volunteerHasApplied =
                volunteerHasApplied,
            onJoinRoleSelected = {
                onJoinRoleSelected(
                    volunteerOpportunityEvent.eventId,
                    volunteerOpportunityRole.roleId
                )
            }
        )
    }
}

@Composable
private fun VolunteerRoleDetailsTopBar(
    onBackSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .height(56.dp)
            .padding(
                start = 4.dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackSelected
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "Role Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VolunteerRoleHeaderSection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    val roleLevelColour =
        when (volunteerOpportunityRole.roleLevel) {
            "Beginner" -> VolunteerLinkSuccess
            "Intermediate" -> VolunteerLinkWarning
            "Advanced" -> VolunteerLinkError
            else -> VolunteerLinkTextSecondary
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkSurface)
            .padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 18.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            volunteerOpportunityRole
                                .roleTitle
                                .firstOrNull()
                                ?.uppercase()
                                ?: "V",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerOpportunityRole.roleTitle,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        volunteerOpportunityEvent.eventTitle,
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text =
                        volunteerOpportunityEvent
                            .eventOrganisationName,
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            VolunteerRoleLabel(
                labelText =
                    volunteerOpportunityRole.roleLevel,
                labelTextColour = roleLevelColour,
                labelBackgroundColour =
                    roleLevelColour.copy(alpha = 0.12f)
            )

            VolunteerRoleLabel(
                labelText =
                    "${volunteerOpportunityRole.roleVacancies} vacancies",
                labelTextColour =
                    VolunteerLinkPrimaryGreen,
                labelBackgroundColour =
                    VolunteerLinkSoftGreenSurface
            )
        }
    }
}

@Composable
private fun VolunteerRoleAssignmentSection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    VolunteerRoleSectionContainer(
        sectionTitle = "Your Assignment"
    ) {
        Text(
            text =
                volunteerOpportunityRole
                    .roleSpecificAssignment
                    .ifBlank {
                        volunteerOpportunityEvent
                            .eventDescription
                    },
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = VolunteerLinkTextPrimary
        )

        volunteerOpportunityRole
            .roleTrainingDetails
            ?.takeIf { trainingDetails ->
                trainingDetails.isNotBlank()
            }
            ?.let { trainingDetails ->
                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                HorizontalDivider(
                    color = VolunteerLinkBorderColour
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Training provided",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkSuccess
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = trainingDetails,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
    }
}

@Composable
private fun VolunteerRoleRequirementsSection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    VolunteerRoleSectionContainer(
        sectionTitle = "Experience Requirement"
    ) {
        Text(
            text =
                volunteerOpportunityRole
                    .roleExperienceRequirement,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text =
                "Primary skill path: " +
                        volunteerOpportunityRole
                            .rolePrimarySkillPath,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkPrimaryGreen
        )
    }
}

@Composable
private fun VolunteerRoleSkillsSection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 20.dp
            )
    ) {
        Text(
            text = "Skills You Will Practise",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            items(
                items =
                    volunteerOpportunityRole
                        .roleSkillsPractised,
                key = { roleSkill ->
                    roleSkill
                }
            ) { roleSkill ->
                VolunteerRoleLabel(
                    labelText = roleSkill,
                    labelTextColour =
                        VolunteerLinkPrimaryGreen,
                    labelBackgroundColour =
                        VolunteerLinkSoftGreenSurface
                )
            }
        }
    }
}

@Composable
private fun VolunteerRoleResponsibilitiesSection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    VolunteerRoleSectionContainer(
        sectionTitle = "Responsibilities"
    ) {
        volunteerOpportunityRole
            .roleResponsibilities
            .forEachIndexed {
                    responsibilityIndex,
                    responsibility ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp
                        ),
                    verticalAlignment =
                        Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(7.dp),
                        shape = CircleShape,
                        color = VolunteerLinkPrimaryGreen
                    ) {}

                    Spacer(
                        modifier = Modifier.size(9.dp)
                    )

                    Text(
                        text = responsibility,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = VolunteerLinkTextPrimary
                    )
                }

                if (
                    responsibilityIndex <
                    volunteerOpportunityRole
                        .roleResponsibilities
                        .lastIndex
                ) {
                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )
                }
            }
    }
}

@Composable
private fun VolunteerRoleScheduleSection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole
) {
    VolunteerRoleSectionContainer(
        sectionTitle = "Role Schedule"
    ) {
        volunteerOpportunityRole
            .roleScheduleItems
            .forEachIndexed {
                    scheduleIndex,
                    scheduleItem ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.Top
                ) {
                    Text(
                        text =
                            scheduleItem.scheduleTime,
                        modifier = Modifier
                            .padding(end = 12.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )

                    Text(
                        text =
                            scheduleItem.scheduleActivity,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = VolunteerLinkTextPrimary
                    )
                }

                if (
                    scheduleIndex <
                    volunteerOpportunityRole
                        .roleScheduleItems
                        .lastIndex
                ) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
    }
}

@Composable
private fun VolunteerRoleEligibilitySection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    currentVolunteerSkillPathLevel: Int,
    volunteerIsEligible: Boolean
) {
    val eligibilityColour =
        if (volunteerIsEligible) {
            VolunteerLinkSuccess
        } else {
            VolunteerLinkWarning
        }

    val eligibilityBackgroundColour =
        if (volunteerIsEligible) {
            Color(0xFFE8F5E9)
        } else {
            Color(0xFFFFF3E0)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 20.dp
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                eligibilityBackgroundColour
        ),
        border = BorderStroke(
            width = 1.dp,
            color = eligibilityColour.copy(
                alpha = 0.35f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text =
                    if (volunteerIsEligible) {
                        "You are eligible for this role"
                    } else {
                        "Skill Path level required"
                    },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = eligibilityColour
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Your level: $currentVolunteerSkillPathLevel  •  " +
                            "Required level: " +
                            volunteerOpportunityRole
                                .roleMinimumSkillPathLevel,
                fontSize = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerRoleJoinSection(
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    volunteerIsEligible: Boolean,
    volunteerHasApplied: Boolean,
    onJoinRoleSelected: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VolunteerLinkSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 12.dp
            )
        ) {
            Button(
                onClick = onJoinRoleSelected,
                enabled =
                    volunteerIsEligible &&
                            !volunteerHasApplied,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        VolunteerLinkPrimaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor =
                        VolunteerLinkBorderColour,
                    disabledContentColor =
                        VolunteerLinkTextSecondary
                )
            ) {
                Text(
                    text =
                        if (volunteerHasApplied) {
                            "Already Registered"
                        } else if (!volunteerIsEligible) {
                            "Skill Path Level Required"
                        } else {
                            when (
                                volunteerOpportunityRole
                                    .roleApplicationMethod
                            ) {
                                VolunteerRoleApplicationMethod
                                    .INSTANT_JOIN ->
                                    "Join Event"

                                VolunteerRoleApplicationMethod
                                    .REVIEW_APPLICANTS ->
                                    if (
                                        volunteerOpportunityRole
                                            .roleApplicationFlow ==
                                        VolunteerRoleApplicationFlow
                                            .ADDITIONAL_FORM
                                    ) {
                                        "Continue to Application"
                                    } else {
                                        "Apply for Role"
                                    }
                            }
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    if (volunteerHasApplied) {
                        "Already registered — track status in My Applications."
                    } else when (
                        volunteerOpportunityRole
                            .roleApplicationMethod
                    ) {
                        VolunteerRoleApplicationMethod
                            .INSTANT_JOIN ->
                            "No organisation review is required."

                        VolunteerRoleApplicationMethod
                            .REVIEW_APPLICANTS ->
                            if (
                                volunteerOpportunityRole
                                    .roleApplicationFlow ==
                                VolunteerRoleApplicationFlow
                                    .ADDITIONAL_FORM
                            ) {
                                "Additional questions are required."
                            } else {
                                "The organisation will review your application."
                            }
                    },
                modifier = Modifier.fillMaxWidth(),
                fontSize =
                    if (volunteerHasApplied) {
                        12.sp
                    } else {
                        10.sp
                    },
                fontWeight =
                    if (volunteerHasApplied) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (volunteerHasApplied) {
                        VolunteerLinkPrimaryGreen
                    } else {
                        VolunteerLinkTextSecondary
                    }
            )
        }
    }
}

@Composable
private fun VolunteerRoleSectionContainer(
    sectionTitle: String,
    sectionContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 20.dp
            )
    ) {
        Text(
            text = sectionTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = VolunteerLinkSurface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = VolunteerLinkBorderColour
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                sectionContent()
            }
        }
    }
}

@Composable
private fun VolunteerRoleLabel(
    labelText: String,
    labelTextColour: Color,
    labelBackgroundColour: Color
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = labelBackgroundColour
    ) {
        Text(
            text = labelText,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelTextColour
        )
    }
}

@Composable
private fun VolunteerRoleNotFoundScreen(
    onBackSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Role not found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "This volunteer role may no longer be available.",
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = onBackSelected,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    VolunteerLinkPrimaryGreen
            )
        ) {
            Text("Return")
        }
    }
}
