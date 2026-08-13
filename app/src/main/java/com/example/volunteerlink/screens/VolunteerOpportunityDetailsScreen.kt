package com.example.volunteerlink.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.data.VolunteerOpportunitySampleData
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerRoleApplicationFlow
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
fun VolunteerOpportunityDetailsScreen(
    volunteerEventId: Int,
    onBackSelected: () -> Unit,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit
) {
    val context = LocalContext.current

    val volunteerOpportunityEvent =
        VolunteerOpportunitySampleData.findEventById(
            volunteerEventId
        )

    if (volunteerOpportunityEvent == null) {
        VolunteerOpportunityNotFoundScreen(
            onBackSelected = onBackSelected
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerOpportunityDetailsTopBar(
            onBackSelected = onBackSelected,
            onShareSelected = {
                shareVolunteerOpportunity(
                    context = context,
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 28.dp
            )
        ) {
            item(
                key = "opportunity_summary"
            ) {
                VolunteerOpportunitySummarySection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent
                )
            }

            item(
                key = "opportunity_about"
            ) {
                VolunteerOpportunityAboutSection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent
                )
            }

            item(
                key = "opportunity_roles"
            ) {
                VolunteerOpportunityRolesSection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent,
                    onVolunteerRoleSelected =
                        onVolunteerRoleSelected
                )
            }

            item(
                key = "opportunity_contact"
            ) {
                VolunteerOpportunityContactSection(
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent,
                    onEmailSelected = {
                        openVolunteerEmail(
                            context = context,
                            emailAddress =
                                volunteerOpportunityEvent
                                    .eventContactEmail
                        )
                    },
                    onPhoneSelected = {
                        openVolunteerPhone(
                            context = context,
                            phoneNumber =
                                volunteerOpportunityEvent
                                    .eventContactPhone
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityDetailsTopBar(
    onBackSelected: () -> Unit,
    onShareSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .height(56.dp)
            .padding(
                start = 4.dp,
                end = 8.dp
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
            text = "Opportunity Details",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        TextButton(
            onClick = onShareSelected
        ) {
            Text(
                text = "Share",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VolunteerOpportunitySummarySection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent
) {
    val categoryIconResourceId =
        when (volunteerOpportunityEvent.eventCategory) {
            VolunteerOpportunityCategory.SPORTS ->
                R.drawable.ic_volunteer_category_sports

            VolunteerOpportunityCategory.COMMUNITY ->
                R.drawable.ic_volunteer_category_community

            VolunteerOpportunityCategory.EDUCATION ->
                R.drawable.ic_volunteer_category_education

            else ->
                R.drawable.ic_volunteer_physical_event
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(14.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = categoryIconResourceId
                        ),
                        contentDescription = null,
                        tint = VolunteerLinkPrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        volunteerOpportunityEvent.eventTitle,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventOrganisationName,
                        modifier = Modifier.weight(
                            weight = 1f,
                            fill = false
                        ),
                        fontSize = 13.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (
                        volunteerOpportunityEvent
                            .eventIsVerifiedOrganisation
                    ) {
                        VerifiedOrganisationBadge()
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            OpportunityBadge(
                badgeText =
                    volunteerOpportunityEvent
                        .eventOpportunityType,
                badgeTextColour =
                    VolunteerLinkPrimaryGreen,
                badgeBackgroundColour =
                    VolunteerLinkSoftGreenSurface
            )

            if (
                volunteerOpportunityEvent
                    .eventIsLongTerm
            ) {
                OpportunityBadge(
                    badgeText = "Long Term",
                    badgeTextColour =
                        VolunteerLinkInformation,
                    badgeBackgroundColour =
                        Color(0xFFE3F2FD)
                )
            }

            if (
                volunteerOpportunityEvent
                    .eventIsGovernmentApproved
            ) {
                OpportunityBadge(
                    badgeText = "Approved",
                    badgeTextColour =
                        VolunteerLinkSuccess,
                    badgeBackgroundColour =
                        Color(0xFFE8F5E9)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        VolunteerOpportunityInformationRow(
            iconResourceId =
                R.drawable.ic_volunteer_calendar,
            informationTitle = "Date and time",
            informationValue =
                "${volunteerOpportunityEvent.eventDate}\n" +
                        volunteerOpportunityEvent.eventTime
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        VolunteerOpportunityInformationRow(
            iconResourceId =
                if (
                    volunteerOpportunityEvent
                        .eventOpportunityType == "Remote"
                ) {
                    R.drawable.ic_volunteer_remote_project
                } else {
                    R.drawable.ic_volunteer_location
                },
            informationTitle = "Location",
            informationValue =
                buildString {
                    append(
                        volunteerOpportunityEvent
                            .eventFullAddress
                            .ifBlank {
                                volunteerOpportunityEvent
                                    .eventLocation
                            }
                    )

                    if (
                        volunteerOpportunityEvent
                            .eventOpportunityType != "Remote"
                    ) {
                        volunteerOpportunityEvent
                            .eventDistanceKm
                            ?.let { eventDistanceKm ->
                                append("\n")
                                append(eventDistanceKm)
                                append(" km from you")
                            }
                    }
                }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        VolunteerOpportunityInformationRow(
            iconResourceId =
                R.drawable.ic_volunteer_physical_event,
            informationTitle = "Availability",
            informationValue =
                "${volunteerOpportunityEvent.eventAvailableSpots} " +
                        "volunteer spots available"
        )
    }
}

@Composable
private fun VolunteerOpportunityAboutSection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent
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
        VolunteerOpportunitySectionTitle(
            sectionTitle = "About this opportunity"
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
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text =
                        volunteerOpportunityEvent
                            .eventDescription,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextPrimary
                )

                if (
                    volunteerOpportunityEvent
                        .eventCauseName
                        .isNotBlank()
                ) {
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
                        text = "Cause",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            volunteerOpportunityEvent
                                .eventCauseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }

                Text(
                    text =
                        "${volunteerOpportunityEvent.eventApplicationCount} " +
                                "people have applied",
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityRolesSection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 22.dp
            )
    ) {
        VolunteerOpportunitySectionTitle(
            sectionTitle = "Available Roles",
            sectionSupportingText =
                "Choose a role that matches your interests and experience."
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        volunteerOpportunityEvent
            .eventVolunteerRoles
            .forEach { volunteerOpportunityRole ->

                VolunteerOpportunityRoleCard(
                    volunteerOpportunityRole =
                        volunteerOpportunityRole,
                    onRoleSelected = {
                        onVolunteerRoleSelected(
                            volunteerOpportunityEvent.eventId,
                            volunteerOpportunityRole.roleId
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
    }
}

@Composable
private fun VolunteerOpportunityRoleCard(
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onRoleSelected: () -> Unit
) {
    val roleLevelColour =
        when (volunteerOpportunityRole.roleLevel) {
            "Beginner" -> VolunteerLinkSuccess
            "Intermediate" -> VolunteerLinkWarning
            "Advanced" -> VolunteerLinkError
            else -> VolunteerLinkTextSecondary
        }

    val roleLevelBackgroundColour =
        when (volunteerOpportunityRole.roleLevel) {
            "Beginner" -> Color(0xFFE8F5E9)
            "Intermediate" -> Color(0xFFFFF3E0)
            "Advanced" -> Color(0xFFFFEBEE)
            else -> VolunteerLinkSoftGreenSurface
        }

    Card(
        onClick = onRoleSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        volunteerOpportunityRole.roleTitle,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                OpportunityBadge(
                    badgeText =
                        volunteerOpportunityRole.roleLevel,
                    badgeTextColour = roleLevelColour,
                    badgeBackgroundColour =
                        roleLevelBackgroundColour
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            val roleDescription =
                volunteerOpportunityRole
                    .roleSpecificAssignment
                    .ifBlank {
                        volunteerOpportunityRole
                            .roleExperienceRequirement
                    }

            Text(
                text = roleDescription,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    volunteerOpportunityRole
                        .rolePrimarySkillPath,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "${volunteerOpportunityRole.roleVacancies} vacancies",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )

                Text(
                    text =
                        when (
                            volunteerOpportunityRole
                                .roleApplicationFlow
                        ) {
                            VolunteerRoleApplicationFlow
                                .DIRECT_SUBMISSION ->
                                "Quick apply"

                            VolunteerRoleApplicationFlow
                                .ADDITIONAL_FORM ->
                                "Application form"
                        },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkInformation
                )

                Text(
                    text = "  ›",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityContactSection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    onEmailSelected: () -> Unit,
    onPhoneSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 12.dp
            )
    ) {
        VolunteerOpportunitySectionTitle(
            sectionTitle = "Organisation Contact"
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
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = VolunteerLinkSoftGreenSurface
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text =
                                    volunteerOpportunityEvent
                                        .eventOrganisationName
                                        .firstOrNull()
                                        ?.uppercase()
                                        ?: "V",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    VolunteerLinkPrimaryGreen
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.size(11.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                volunteerOpportunityEvent
                                    .eventOrganisationName,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                VolunteerLinkTextPrimary
                        )

                        Text(
                            text =
                                if (
                                    volunteerOpportunityEvent
                                        .eventIsVerifiedOrganisation
                                ) {
                                    "Verified organisation"
                                } else {
                                    "Organisation"
                                },
                            fontSize = 11.sp,
                            color =
                                VolunteerLinkTextSecondary
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEmailSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                VolunteerLinkPrimaryGreen
                        )
                    ) {
                        Text(
                            text = "Email",
                            color =
                                VolunteerLinkPrimaryGreen
                        )
                    }

                    Button(
                        onClick = onPhoneSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    VolunteerLinkPrimaryGreen
                            )
                    ) {
                        Text(
                            text = "Call",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerOpportunityInformationRow(
    @DrawableRes
    iconResourceId: Int,
    informationTitle: String,
    informationValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(9.dp),
            color = VolunteerLinkSoftGreenSurface
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = iconResourceId
                    ),
                    contentDescription = null,
                    tint = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.size(11.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = informationTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = informationValue,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextPrimary
            )
        }
    }
}

@Composable
private fun VolunteerOpportunitySectionTitle(
    sectionTitle: String,
    sectionSupportingText: String? = null
) {
    Column {
        Text(
            text = sectionTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        if (sectionSupportingText != null) {
            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = sectionSupportingText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VerifiedOrganisationBadge() {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFFE8F5E9)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = R.drawable.ic_volunteer_verified
                ),
                contentDescription =
                    "Verified organisation",
                modifier = Modifier.size(12.dp),
                tint = Color.Unspecified
            )

            Text(
                text = "Verified",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkSuccess
            )
        }
    }
}

@Composable
private fun OpportunityBadge(
    badgeText: String,
    badgeTextColour: Color,
    badgeBackgroundColour: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeBackgroundColour
    ) {
        Text(
            text = badgeText,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeTextColour
        )
    }
}

@Composable
private fun VolunteerOpportunityNotFoundScreen(
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
            text = "Opportunity not found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "This opportunity may no longer be available.",
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

private fun openVolunteerEmail(
    context: Context,
    emailAddress: String
) {
    if (emailAddress.isBlank()) {
        Toast.makeText(
            context,
            "Email address is unavailable.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val emailIntent =
        Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:$emailAddress")
        )

    startVolunteerIntent(
        context = context,
        intent = emailIntent,
        unavailableMessage =
            "No email application is available."
    )
}

private fun openVolunteerPhone(
    context: Context,
    phoneNumber: String
) {
    if (phoneNumber.isBlank()) {
        Toast.makeText(
            context,
            "Phone number is unavailable.",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val phoneIntent =
        Intent(
            Intent.ACTION_DIAL,
            Uri.parse(
                "tel:${Uri.encode(phoneNumber)}"
            )
        )

    startVolunteerIntent(
        context = context,
        intent = phoneIntent,
        unavailableMessage =
            "No phone application is available."
    )
}

private fun shareVolunteerOpportunity(
    context: Context,
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent
) {
    val shareText =
        buildString {
            append(
                volunteerOpportunityEvent.eventTitle
            )
            append("\n")
            append(
                volunteerOpportunityEvent
                    .eventOrganisationName
            )
            append("\n")
            append(
                volunteerOpportunityEvent.eventDate
            )
            append(" • ")
            append(
                volunteerOpportunityEvent.eventLocation
            )

            if (
                volunteerOpportunityEvent
                    .eventShareLink
                    .isNotBlank()
            ) {
                append("\n")
                append(
                    volunteerOpportunityEvent
                        .eventShareLink
                )
            }
        }

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                volunteerOpportunityEvent.eventTitle
            )
            putExtra(
                Intent.EXTRA_TEXT,
                shareText
            )
        }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Share opportunity"
        )
    )
}

private fun startVolunteerIntent(
    context: Context,
    intent: Intent,
    unavailableMessage: String
) {
    try {
        context.startActivity(intent)
    } catch (
        exception: Exception
    ) {
        Toast.makeText(
            context,
            unavailableMessage,
            Toast.LENGTH_SHORT
        ).show()
    }
}