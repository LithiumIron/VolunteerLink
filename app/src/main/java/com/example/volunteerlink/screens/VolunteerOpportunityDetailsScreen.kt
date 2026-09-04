
package com.example.volunteerlink.screens

// Displays event-level information and passes the selected role into the role-details flow.

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.R
import com.example.volunteerlink.data.OrganisationPublicProfile
import com.example.volunteerlink.data.OrganisationPublicProfileRepository
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityCategory
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.model.VolunteerOpportunityRole
import com.example.volunteerlink.model.VolunteerOpportunityPartner
import com.example.volunteerlink.model.VolunteerOpportunityPartnershipContribution
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
import kotlinx.coroutines.launch

@Composable
// Purpose: Displays one event and sends the selected role to the role-details route.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerOpportunityDetailsScreen(
    volunteerEventId: Int,
    recommendedRoleId: Int = -1,
    recommendationSource: String = "",
    opportunityViewModel: VolunteerOpportunityViewModel,
    onBackSelected: () -> Unit,
    onLocationSelected: (Int) -> Unit,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit
) {
    // Use the current Android context for permissions, files, resources or external intents.
    val context = LocalContext.current
    // The same ViewModel is shared with application screens so save/apply errors are
    // displayed consistently even when the volunteer navigates back to this page.
    val actionState by opportunityViewModel.uiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(volunteerEventId) {
        opportunityViewModel.clearApplicationActionError()
    }
    var showOrganisationPreview by remember { mutableStateOf(false) }
    var organisationPreviewProfile by remember {
        mutableStateOf<OrganisationPublicProfile?>(null)
    }
    var isLoadingOrganisationPreview by remember { mutableStateOf(false) }
    // Create a lifecycle-aware coroutine scope for asynchronous work started by this Compose screen.
    val previewScope = rememberCoroutineScope()

    // Event objects are loaded once into the Volunteer session store. Navigation passes
    // compact IDs instead of serialising the entire event through the route.
    val volunteerOpportunityEvent =
        VolunteerOpportunitySessionStore.findEventById(
            volunteerEventId
        )

    // A deleted/expired event or stale deep link must show a safe recovery screen.
    if (volunteerOpportunityEvent == null) {
        VolunteerOpportunityNotFoundScreen(
            onBackSelected = onBackSelected
        )
        // Keep this Compose block separate so its visual state follows the value prepared above.
        return
    }

    val eventGroupConversationId: String?

    // Arrange the following screen content vertically inside the available space.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerOpportunityDetailsTopBar(
            onBackSelected = onBackSelected,
            isSaved = volunteerOpportunityEvent.eventIsSaved,
            onSavedSelected = {
                opportunityViewModel.setOpportunitySaved(
                    volunteerEventId,
                    !volunteerOpportunityEvent.eventIsSaved
                )
            },
            onShareSelected = {
                shareVolunteerOpportunity(
                    context = context,
                    volunteerOpportunityEvent =
                        volunteerOpportunityEvent
                )
            }
        )

        // Render this content as a lazy scrolling list so only visible items need to be composed.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 28.dp
            )
        ) {
            actionState.applicationActionError?.let { message ->
                item(key = "action_error") {
                    // Display the prepared label; business rules are calculated before reaching this UI call.
                    Text(message, color = VolunteerLinkError, modifier = Modifier.padding(16.dp))
                }
            }
            item(key = "opportunity_summary") {
                VolunteerOpportunitySummarySection(
                    volunteerOpportunityEvent = volunteerOpportunityEvent,
                    onLocationSelected = {
                        onLocationSelected(volunteerOpportunityEvent.eventId)
                    },
                    onOrganisationSelected = {
                        // Load the small public profile only when requested, rather than
                        // fetching every organisation profile while Home is composing.
                        showOrganisationPreview = true
                        if (organisationPreviewProfile?.organisationId !=
                            volunteerOpportunityEvent.eventOrganisationId
                        ) {
                            previewScope.launch {
                                isLoadingOrganisationPreview = true
                                organisationPreviewProfile =
                                    OrganisationPublicProfileRepository
                                        .getPublicProfile(
                                            volunteerOpportunityEvent
                                                .eventOrganisationId
                                        )
                                isLoadingOrganisationPreview = false
                            }
                        }
                    }
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

            if (volunteerOpportunityEvent.eventIsPartnershipPost) {
                item(key = "opportunity_partnership_support") {
                    VolunteerOpportunityPartnershipSection(
                        volunteerOpportunityEvent = volunteerOpportunityEvent
                    )
                }
            }

            if (recommendedRoleId != -1) {
                item(key = "recommended_role") {
                    // Name the calculated recommended value because later UI branches reuse it during this Compose pass.
                    val recommended = volunteerOpportunityEvent.eventVolunteerRoles
                        .firstOrNull { it.roleId == recommendedRoleId }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            if (recommendationSource == "skill_path") "Build this skill path"
                            else "Recommended role for you",
                            color = VolunteerLinkPrimaryGreen, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        if (recommended != null && recommended.roleVacancies > 0 &&
                            com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(volunteerOpportunityEvent)) {
                            VolunteerOpportunityRoleCard(
                                volunteerEventId = volunteerEventId,
                                volunteerOpportunityRole = recommended,
                                onRoleSelected = { onVolunteerRoleSelected(volunteerEventId, recommended.roleId) }
                            )
                        } else {
                            Text("This recommended role is no longer available. Review the other roles below.",
                                color = VolunteerLinkTextSecondary)
                        }
                    }
                }
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
        if (showOrganisationPreview) {
            OrganisationPreviewSheet(
                profile = organisationPreviewProfile,
                isLoading = isLoadingOrganisationPreview,
                fallbackName = volunteerOpportunityEvent.eventOrganisationName,
                onDismissRequest = { showOrganisationPreview = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Purpose: Handles organisation preview sheet as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun OrganisationPreviewSheet(
    profile: OrganisationPublicProfile?,
    isLoading: Boolean,
    fallbackName: String,
    onDismissRequest: () -> Unit
) {
    // Use the current Android context for permissions, files, resources or external intents.
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = VolunteerLinkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 8.dp
                )
                .padding(bottom = 24.dp)
        ) {
            if (isLoading || profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = VolunteerLinkPrimaryGreen
                        )
                    } else {
                        Text(
                            text = "Couldn't load $fallbackName's profile.",
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VolunteerLinkSoftGreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.profileImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.profileImageUrl,
                            contentDescription = "Organisation logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = profile.organisationName.firstOrNull()
                                ?.uppercase() ?: "O",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.organisationName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    if (profile.organisationType.isNotBlank()) {
                        Text(
                            text = profile.organisationType,
                            fontSize = 11.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }

                if (profile.isVerified) {
                    VerifiedOrganisationBadge()
                }
            }

            if (profile.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = profile.description,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            // Name the calculated location value because later UI branches reuse it during this Compose pass.
            val location = listOf(
                profile.locationName, profile.stateRegion, profile.country
            ).filter { it.isNotBlank() }.joinToString(", ")

            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = location,
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            // Calculate whether the following UI or action is allowed before it is rendered or executed.
            val hasContact = profile.websiteUrl.isNotBlank() ||
                    profile.contactPhone.isNotBlank() ||
                    profile.contactEmail.isNotBlank()

            if (hasContact) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = VolunteerLinkBorderColour)
                Spacer(modifier = Modifier.height(12.dp))

                if (profile.websiteUrl.isNotBlank()) {
                    OrganisationPreviewContactRow(
                        label = "Website",
                        value = profile.websiteUrl,
                        onClick = {
                            // Name the calculated url value because later UI branches reuse it during this Compose pass.
                            val url = if (
                                profile.websiteUrl.startsWith("http://") ||
                                profile.websiteUrl.startsWith("https://")
                            ) profile.websiteUrl else "https://${profile.websiteUrl}"
                            startVolunteerIntent(
                                context = context,
                                intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                unavailableMessage = "No browser is available."
                            )
                        }
                    )
                }

                if (profile.contactPhone.isNotBlank()) {
                    OrganisationPreviewContactRow(
                        label = "Phone",
                        value = profile.contactPhone,
                        onClick = {
                            openVolunteerPhone(context, profile.contactPhone)
                        }
                    )
                }

                if (profile.contactEmail.isNotBlank()) {
                    OrganisationPreviewContactRow(
                        label = "Email",
                        value = profile.contactEmail,
                        onClick = {
                            openVolunteerEmail(context, profile.contactEmail)
                        }
                    )
                }
            }
        }
    }
}

@Composable
// Purpose: Handles organisation preview contact row as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun OrganisationPreviewContactRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "›",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )
    }
}

@Composable
// Purpose: Handles volunteer opportunity details top bar as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunityDetailsTopBar(
    onBackSelected: () -> Unit,
    isSaved: Boolean,
    onSavedSelected: () -> Unit,
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

        IconButton(onClick = onSavedSelected) {
            Icon(
                imageVector = if (isSaved) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                contentDescription =
                    if (isSaved) "Remove from favourites"
                    else "Add to favourites",
                tint = Color.White
            )
        }

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
// Purpose: Renders the volunteer opportunity summary section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunitySummarySection(
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent,
    onLocationSelected: () -> Unit,
    onOrganisationSelected: () -> Unit
) {
    // Name the calculated category icon resource id value because later UI branches reuse it during this Compose pass.
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
            VolunteerOpportunityThumbnail(
                storagePath =
                    volunteerOpportunityEvent.eventThumbnailPath,
                fallbackIconResourceId =
                    categoryIconResourceId,
                modifier = Modifier.size(64.dp),
                contentDescription =
                    "${volunteerOpportunityEvent.eventTitle} thumbnail",
                cornerRadius = 14.dp
            )

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable(onClick = onOrganisationSelected)  // NEW
                ) {
                    Text(
                        text = volunteerOpportunityEvent.eventOrganisationName,
                        modifier = Modifier.weight(weight = 1f, fill = false),
                        fontSize = 13.sp,
                        color = VolunteerLinkPrimaryGreen,   // tinted to signal it's tappable
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (volunteerOpportunityEvent.eventIsVerifiedOrganisation) {
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

        VolunteerEventSchedule(volunteerOpportunityEvent)

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
            onClick =
                if (
                    volunteerOpportunityEvent.eventLatitude != null &&
                    volunteerOpportunityEvent.eventLongitude != null
                ) {
                    onLocationSelected
                } else {
                    null
                },
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
// Purpose: Renders the volunteer opportunity about section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Renders the volunteer opportunity roles section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
            sectionTitle = "Roles",
            sectionSupportingText =
                "You can view every role. Closed or full roles cannot accept applications."
        )

        VolunteerOpportunitySessionStore.activeApplicationForEvent(
            volunteerOpportunityEvent.eventId
        )?.let { application ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (application.applicationDatabaseId.startsWith("offline|")) {
                    "Your selected role: ${application.applicationRoleTitle}. " +
                        "Waiting to sync: no place has been reserved."
                } else {
                    "Your current role: ${application.applicationRoleTitle}. " +
                        if (application.applicationStatus == VolunteerApplicationStatus.ACCEPTED)
                            "Accepted." else "Waiting for organisation review."
                },
                color = VolunteerLinkTextPrimary,
                fontSize = 13.sp
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        volunteerOpportunityEvent
            .eventVolunteerRoles
            .forEach { volunteerOpportunityRole ->

                VolunteerOpportunityRoleCard(
                    volunteerEventId =
                        volunteerOpportunityEvent.eventId,
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
// Purpose: Renders the volunteer opportunity partnership section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunityPartnershipSection(
    volunteerOpportunityEvent: VolunteerOpportunityEvent
) {
    // Name the calculated partners value because later UI branches reuse it during this Compose pass.
    val partners = volunteerOpportunityEvent.eventPartnershipPartners

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 12.dp
            )
    ) {
        VolunteerOpportunitySectionTitle(
            sectionTitle = "Partnership Support",
            sectionSupportingText = "This activity is supported by partner organisations."
        )

        Spacer(modifier = Modifier.height(9.dp))

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
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VolunteerLinkSoftGreenSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Partner-supported activity",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                        Text(
                            text = if (partners.isEmpty()) {
                                "Partnership support details are not available right now."
                            } else {
                                "${partners.size} partner ${if (partners.size == 1) "organisation is" else "organisations are"} supporting this activity."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }

                if (partners.isNotEmpty()) {
                    partners.forEachIndexed { index, partner ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = VolunteerLinkBorderColour)
                        }

                        Column(
                            modifier = Modifier.padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = partner.organisationName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkTextPrimary
                            )
                            partner.contributions.forEach { contribution ->
                                VolunteerOpportunityPartnershipContributionRow(contribution)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
// Purpose: Renders the volunteer opportunity partnership contribution row from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunityPartnershipContributionRow(
    contribution: VolunteerOpportunityPartnershipContribution
) {
    // Name the calculated amount value because later UI branches reuse it during this Compose pass.
    val amount = if (contribution.supportType.equals("VENUE", ignoreCase = true)) {
        contribution.capacityProvided?.let { "Capacity $it" }
    } else {
        contribution.quantityProvided?.let { "×$it" }
    }
    // Name the calculated provider value because later UI branches reuse it during this Compose pass.
    val provider = contribution.providerResourceName
        ?.takeIf { it.isNotBlank() }
        ?: contribution.needResourceName

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp),
            shape = CircleShape,
            color = VolunteerLinkPrimaryGreen
        ) {}

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = contribution.needResourceName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = listOfNotNull(provider, amount)
                    .distinct()
                    .joinToString(" · "),
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
// Purpose: Renders the volunteer opportunity role card from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunityRoleCard(
    volunteerEventId: Int,
    volunteerOpportunityRole:
    VolunteerOpportunityRole,
    onRoleSelected: () -> Unit
) {
    // Resolve or prepare event data from the shared dashboard snapshot for this part of the screen.
    val event = VolunteerOpportunitySessionStore.findEventById(volunteerEventId)
    // Read the shared business clock so Today, deadlines and attendance use the same date.
    val businessNow = volunteerBusinessTime()
    // Name the calculated unavailable reason value because later UI branches reuse it during this Compose pass.
    val unavailableReason = when {
        !com.example.volunteerlink.data.VolunteerApplicationWindow.canApply(event, volunteerOpportunityRole, businessNow) ->
            com.example.volunteerlink.data.VolunteerApplicationWindow.reason(event, volunteerOpportunityRole)
        volunteerOpportunityRole.roleVacancies <= 0 -> "Full: no places are available for this role."
        else -> null
    }
    // Resolve or prepare the application data used by the next status, cancellation or navigation decision.
    val existingApplication =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .firstOrNull { application ->
                application.applicationEventId ==
                    volunteerEventId &&
                    application.applicationRoleId ==
                    volunteerOpportunityRole.roleId
            }
    // Resolve or prepare role data used for eligibility, schedule and application controls.
    val roleLevelColour =
        when (volunteerOpportunityRole.roleLevel) {
            "Beginner" -> VolunteerLinkSuccess
            "Intermediate" -> VolunteerLinkWarning
            "Advanced" -> VolunteerLinkError
            else -> VolunteerLinkTextSecondary
        }

    // Resolve or prepare role data used for eligibility, schedule and application controls.
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
                        volunteerOpportunityRole.roleLevel +
                            " role",
                    badgeTextColour = roleLevelColour,
                    badgeBackgroundColour =
                        roleLevelBackgroundColour
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            // Resolve or prepare role data used for eligibility, schedule and application controls.
            val roleDescription =
                volunteerOpportunityRole
                    .roleSpecificAssignment
                    .ifBlank {
                        volunteerOpportunityRole
                            .roleExperienceRequirement
                    }

            event?.let {
                Text(
                    text = com.example.volunteerlink.data.VolunteerScheduleText.compact(it, volunteerOpportunityRole),
                    color = VolunteerLinkTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            unavailableReason?.let { reason ->
                OpportunityBadge("Applications unavailable", Color(0xFF5C5C5C), Color(0xFFEEEEEE))
                Text(
                    text = reason + " You can still view the role details.",
                    color = VolunteerLinkTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
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
                    "Primary path: " +
                        volunteerOpportunityRole
                            .rolePrimarySkillPath,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text =
                    "Skill Path requirement: " +
                        opportunitySkillPathLevelName(
                            volunteerOpportunityRole
                                .roleMinimumSkillPathLevel
                        ) +
                        " • Level " +
                        volunteerOpportunityRole
                            .roleMinimumSkillPathLevel,
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
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
                        existingApplication
                            ?.let { application ->
                                if (application.applicationDatabaseId.startsWith("offline|")) "Waiting to sync"
                                else when (application.applicationStatus) {
                                    VolunteerApplicationStatus.PENDING ->
                                        "Applied • Pending"
                                    VolunteerApplicationStatus.ACCEPTED ->
                                        "Accepted"
                                    VolunteerApplicationStatus.REJECTED ->
                                        "Not selected"
                                    VolunteerApplicationStatus.COMPLETED ->
                                        "Completed"
                                    VolunteerApplicationStatus.NOT_COMPLETED ->
                                        "Not completed"
                                    VolunteerApplicationStatus.CANCELLED ->
                                        "Cancelled"
                                }
                            }
                            ?: when (
                                volunteerOpportunityRole
                                    .roleApplicationMethod
                            ) {
                                VolunteerRoleApplicationMethod
                                    .INSTANT_JOIN ->
                                    "Instant join"

                                VolunteerRoleApplicationMethod
                                    .REVIEW_APPLICANTS ->
                                    if (
                                        volunteerOpportunityRole
                                            .roleApplicationFlow ==
                                        VolunteerRoleApplicationFlow
                                            .ADDITIONAL_FORM
                                    ) {
                                        "Application form"
                                    } else {
                                        "Review required"
                                    }
                            },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (existingApplication != null) {
                            VolunteerLinkPrimaryGreen
                        } else {
                            VolunteerLinkInformation
                        }
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

// Purpose: Handles opportunity skill path level name as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun opportunitySkillPathLevelName(
    level: Int
): String {
    return when (level) {
        1 -> "Beginner"
        2 -> "Intermediate"
        3 -> "Advanced"
        else -> "Level $level"
    }
}

@Composable
// Purpose: Renders the volunteer opportunity contact section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Renders the volunteer opportunity information row from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerOpportunityInformationRow(
    @DrawableRes
    iconResourceId: Int,
    informationTitle: String,
    informationValue: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
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

            if (onClick != null) {
                Text(
                    text = "View on interactive map  ›",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
// Purpose: Renders the volunteer opportunity section title from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Handles verified organisation badge as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Handles opportunity badge as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
// Purpose: Renders the volunteer opportunity not found screen and connects user actions to navigation or its ViewModel.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

// Purpose: Creates and safely launches the Android action for open volunteer email.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

    // Name the calculated email intent value because later UI branches reuse it during this Compose pass.
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

// Purpose: Creates and safely launches the Android action for open volunteer phone.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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

    // Name the calculated phone intent value because later UI branches reuse it during this Compose pass.
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

// Purpose: Handles share volunteer opportunity as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun shareVolunteerOpportunity(
    context: Context,
    volunteerOpportunityEvent:
    VolunteerOpportunityEvent
) {
    // Name the calculated share text value because later UI branches reuse it during this Compose pass.
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

    // Name the calculated share intent value because later UI branches reuse it during this Compose pass.
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

// Purpose: Creates and safely launches the Android action for start volunteer intent.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
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
