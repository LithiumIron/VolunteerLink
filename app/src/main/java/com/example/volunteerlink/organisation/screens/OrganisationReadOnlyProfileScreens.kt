package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Read Only Profile Screens.
//
// The composable layer is responsible for layout, interaction and displaying loading/error/validation state;
// business rules and persistence are delegated to ViewModels/repositories.
//
// This separation makes it clear during maintenance which code changes appearance versus which code changes real
// server data.
//
// Where the screen displays cached information, server-changing actions remain disabled or routed through a fresh
// authenticated repository operation.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.organisation.repository.OrganisationReadOnlyProfileRepository
import com.example.volunteerlink.organisation.repository.OrganisationViewedPartnerProfile
import com.example.volunteerlink.organisation.repository.OrganisationViewedPartnerSupport
import com.example.volunteerlink.organisation.repository.OrganisationViewedVolunteerCertificate
import com.example.volunteerlink.organisation.repository.OrganisationViewedVolunteerProfile
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import java.util.Locale

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationViewVolunteerProfileScreen
 *
 * Renders the Organisation View Volunteer Profile screen from state supplied by the owning
 * ViewModel/repository-facing coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationViewVolunteerProfileScreen(
    postId: String,
    userId: String,
    onBack: () -> Unit,
    onCertificateSelected: (
        userId: String,
        certificatePostId: String,
        roleTemplateId: String
    ) -> Unit = { _, _, _ -> }
) {
    var profile by remember(postId, userId) {
        mutableStateOf<OrganisationViewedVolunteerProfile?>(null)
    }
    var isLoading by remember(postId, userId) { mutableStateOf(true) }
    var loadFailed by remember(postId, userId) { mutableStateOf(false) }

    LaunchedEffect(postId, userId) {
        isLoading = true
        loadFailed = false
        profile = OrganisationReadOnlyProfileRepository.loadVolunteerProfile(userId, postId)
        loadFailed = profile == null
        isLoading = false
    }

    ReadOnlyProfileScaffold(
        title = "VOLUNTEER PROFILE",
        barColor = DeepGreen,
        onBack = onBack
    ) {
        when {
            isLoading -> ProfileLoading()
            loadFailed -> ProfileError(
                "This volunteer profile could not be loaded. Only volunteers connected to your organisation's posts can be viewed."
            )
            profile != null -> VolunteerProfileTemplate(
                profile = profile!!,
                onCertificateSelected = { certificate ->
                    onCertificateSelected(
                        profile!!.userId,
                        certificate.postId,
                        certificate.roleTemplateId
                    )
                }
            )
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationViewPartnerProfileScreen
 *
 * Renders the Organisation View Partner Profile screen from state supplied by the owning ViewModel/repository-
 * facing coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationViewPartnerProfileScreen(
    organisationId: String,
    onBack: () -> Unit
) {
    var profile by remember(organisationId) {
        mutableStateOf<OrganisationViewedPartnerProfile?>(null)
    }
    var isLoading by remember(organisationId) { mutableStateOf(true) }
    var loadFailed by remember(organisationId) { mutableStateOf(false) }

    LaunchedEffect(organisationId) {
        isLoading = true
        loadFailed = false
        profile = OrganisationReadOnlyProfileRepository.loadPartnerProfile(organisationId)
        loadFailed = profile == null
        isLoading = false
    }

    ReadOnlyProfileScaffold(
        title = "PROFILE",
        barColor = MaterialTheme.colorScheme.primary,
        onBack = onBack
    ) {
        when {
            isLoading -> ProfileLoading()
            loadFailed -> ProfileError("This organisation profile could not be loaded right now.")
            profile != null -> OrganisationProfileTemplate(profile!!)
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ReadOnlyProfileScaffold
 *
 * Handles the Compose/UI responsibility for read only profile scaffold.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ReadOnlyProfileScaffold(
    title: String,
    barColor: Color,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(barColor)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                modifier = Modifier.padding(start = 2.dp, bottom = 14.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            content()
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ProfileLoading
 *
 * Handles the Compose/UI responsibility for profile loading.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ProfileLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ProfileError
 *
 * Handles the Compose/UI responsibility for profile error.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ProfileError(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(12.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}

// -----------------------------------------------------------------------------
// Volunteer read-only profile
// This intentionally follows VolunteerProfileScreen's existing layout instead
// of introducing a second profile design.
// -----------------------------------------------------------------------------

@Composable
/**
 * DETAILED BEHAVIOUR — VolunteerProfileTemplate
 *
 * Handles the Compose/UI responsibility for volunteer profile template.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun VolunteerProfileTemplate(
    profile: OrganisationViewedVolunteerProfile,
    onCertificateSelected: (OrganisationViewedVolunteerCertificate) -> Unit
) {
    val context = LocalContext.current
    var showAllCompleted by remember(profile.userId) { mutableStateOf(false) }
    var showAllCertificates by remember(profile.userId) { mutableStateOf(false) }
    var showAllSkillPaths by remember(profile.userId) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            InitialsProfileAvatar(
                name = profile.fullName,
                imageUrl = profile.avatarPath
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = profile.fullName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2
                )

                if (profile.city.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = profile.city,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreen,
                        maxLines = 1
                    )
                }
            }
        }

        if (profile.bio.isNotBlank()) {
            Text(
                text = profile.bio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color.DarkGray
            )
        }

        Text(
            text = "Member since ${profile.memberSince}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        if (profile.sharedPhone.isNotBlank()) {
            VolunteerEventContactSection(
                phone = profile.sharedPhone,
                untilLabel = profile.phoneContactUntilLabel,
                onCall = { openPhone(context, profile.sharedPhone) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VolunteerStatCard(
                title = "Verified Hours",
                value = "${profile.verifiedMinutes.coerceAtLeast(0) / 60} hrs",
                modifier = Modifier.weight(1f)
            )
            VolunteerStatCard(
                title = "Completed Events",
                value = profile.completedEventCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        VolunteerSectionHeader(
            title = "Completed Events",
            showAction = profile.completedEvents.size > 3,
            actionText = if (showAllCompleted) "Show less" else "See all",
            onAction = { showAllCompleted = !showAllCompleted }
        )

        if (profile.completedEvents.isEmpty()) {
            VolunteerEmptySectionText("No completed events yet")
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val items = if (showAllCompleted) profile.completedEvents else profile.completedEvents.take(3)
                items.forEach { event ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = event.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = event.roleName,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        VolunteerSectionHeader(
            title = "Certificates",
            showAction = profile.certificates.size > 1,
            actionText = if (showAllCertificates) "Show less" else "See all",
            onAction = { showAllCertificates = !showAllCertificates }
        )

        if (profile.certificates.isEmpty()) {
            VolunteerEmptySectionText("No certificates yet")
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val items = if (showAllCertificates) profile.certificates else profile.certificates.take(1)
                items.forEach { certificate ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(10.dp))
                            .clickable { onCertificateSelected(certificate) }
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = certificate.eventTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = certificate.roleName,
                                modifier = Modifier.padding(top = 2.dp),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        VolunteerSectionHeader(
            title = "Skill Paths",
            showAction = profile.skillPaths.size > 3,
            actionText = if (showAllSkillPaths) "Show less" else "See all",
            onAction = { showAllSkillPaths = !showAllSkillPaths }
        )

        if (profile.skillPaths.isEmpty()) {
            VolunteerEmptySectionText("No skill paths yet")
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val items = if (showAllSkillPaths) profile.skillPaths else profile.skillPaths.take(3)
                items.forEach { skillPath ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(VolunteerLinkSoftGreenSurface)
                            .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = skillPath.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreen
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Level ${skillPath.currentLevel}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkPrimaryGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${skillPath.verifiedAssignments} verified assignments",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — VolunteerEventContactSection
 *
 * Renders the reusable Volunteer Event Contact Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun VolunteerEventContactSection(
    phone: String,
    untilLabel: String?,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp)
    ) {
        Text(
            text = "Event Contact",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepGreen
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, VolunteerLinkBorderColour)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = phone,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = buildString {
                        append("Shared for this opportunity")
                        untilLabel?.takeIf { it.isNotBlank() }?.let { append(" until $it") }
                        append(".")
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = VolunteerLinkTextSecondary
                )
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) {
                    Text("Call Volunteer", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — VolunteerStatCard
 *
 * Renders the reusable Volunteer Stat Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun VolunteerStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VolunteerLinkSoftGreenSurface)
            .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(12.dp))
            .padding(15.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — VolunteerSectionHeader
 *
 * Renders the reusable Volunteer Section Header portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun VolunteerSectionHeader(
    title: String,
    showAction: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepGreen,
            modifier = Modifier.weight(1f)
        )
        if (showAction) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — VolunteerEmptySectionText
 *
 * Renders the reusable Volunteer Empty Section Text portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun VolunteerEmptySectionText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp),
        fontSize = 13.sp,
        color = Color.Gray
    )
}

// -----------------------------------------------------------------------------
// Organisation read-only profile
// This mirrors OrganisationProfileScreen but removes owner-only controls.
// -----------------------------------------------------------------------------

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationProfileTemplate
 *
 * Handles the Compose/UI responsibility for organisation profile template.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun OrganisationProfileTemplate(profile: OrganisationViewedPartnerProfile) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            InitialsProfileAvatar(
                name = profile.organisationName,
                imageUrl = profile.profileImagePath
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = profile.organisationName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )

                if (profile.organisationType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.organisationType,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (profile.contactEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.contactEmail,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (profile.description.isNotBlank()) {
            Text(
                text = profile.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val location = listOf(profile.locationName, profile.stateRegion, profile.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (location.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Member since ${profile.memberSince}",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (profile.verificationStatus) {
                            "VERIFIED" -> Color(0xFFACD8A7)
                            "REJECTED" -> Color(0xFFE8A6A6)
                            else -> Color(0xFFE8D9A6)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (profile.verificationStatus) {
                        "VERIFIED" -> "Verified organisation"
                        "REJECTED" -> "Verification rejected"
                        else -> "Verification pending"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        ReadOnlyOrganisationPartnershipSection(profile)

        OrganisationContactSection(
            email = profile.contactEmail,
            phone = profile.contactPhone,
            website = profile.websiteUrl,
            onEmail = { openEmail(context, profile.contactEmail) },
            onCall = { openPhone(context, profile.contactPhone) },
            onWebsite = { openWebsite(context, profile.websiteUrl) }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp)
        ) {
            Text(
                text = "Recently Created Events",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (profile.recentPosts.isEmpty()) {
                Text(
                    text = "No public posts yet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.recentPosts.take(3).forEach { post ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = post.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = post.status,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ReadOnlyOrganisationPartnershipSection
 *
 * Renders the reusable Read Only Organisation Partnership Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun ReadOnlyOrganisationPartnershipSection(profile: OrganisationViewedPartnerProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
    ) {
        Text(
            text = "Partnerships",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, VolunteerLinkBorderColour, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open to partnerships",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (profile.openToPartnership) {
                        "This organisation is currently open to partnership requests."
                    } else {
                        "This organisation is not currently open to new partnership requests."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (profile.openToPartnership) {
                    VolunteerLinkSoftGreenSurface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = if (profile.openToPartnership) "Open" else "Closed",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (profile.openToPartnership) {
                        VolunteerLinkPrimaryGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "What we can provide",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Resources this organisation can contribute to a partnership.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (profile.supports.isEmpty()) {
            Text(
                text = if (profile.openToPartnership) {
                    "No support added yet."
                } else {
                    "Support listings are hidden while this organisation is closed to new partnerships."
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, VolunteerLinkBorderColour)
            ) {
                Column {
                    profile.supports.forEachIndexed { index, support ->
                        ReadOnlyOrganisationSupportRow(support)
                        if (index < profile.supports.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ReadOnlyOrganisationSupportRow
 *
 * Renders the reusable Read Only Organisation Support Row portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun ReadOnlyOrganisationSupportRow(support: OrganisationViewedPartnerSupport) {
    val typeLabel = support.supportType
        .lowercase(Locale.ROOT)
        .replaceFirstChar { it.titlecase(Locale.ROOT) }
    val amountText = if (support.supportType.equals("VENUE", ignoreCase = true)) {
        support.capacity?.let { "Capacity $it" } ?: "Capacity not specified"
    } else {
        support.quantity?.let { "$it available" } ?: "Quantity not specified"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = support.resourceName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$typeLabel · $amountText",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (support.supportDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = support.supportDescription,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        support.locationName?.takeIf { it.isNotBlank() }?.let { location ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationContactSection
 *
 * Renders the reusable Organisation Contact Section portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
private fun OrganisationContactSection(
    email: String,
    phone: String,
    website: String,
    onEmail: () -> Unit,
    onCall: () -> Unit,
    onWebsite: () -> Unit
) {
    if (email.isBlank() && phone.isBlank() && website.isBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
    ) {
        Text(
            text = "Organisation Contact",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, VolunteerLinkBorderColour)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (email.isNotBlank()) ContactValue("Email", email)
                if (phone.isNotBlank()) ContactValue("Phone", phone)
                if (website.isNotBlank()) ContactValue("Website", website)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onEmail,
                        enabled = email.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, VolunteerLinkPrimaryGreen)
                    ) {
                        Text("Email", color = VolunteerLinkPrimaryGreen)
                    }
                    Button(
                        onClick = if (phone.isNotBlank()) onCall else onWebsite,
                        enabled = phone.isNotBlank() || website.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                    ) {
                        Text(if (phone.isNotBlank()) "Call" else "Website")
                    }
                }

                if (phone.isNotBlank() && website.isNotBlank()) {
                    Text(
                        text = "Open website",
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp)
                            .clickable(onClick = onWebsite),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ContactValue
 *
 * Handles the Compose/UI responsibility for contact value.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ContactValue(label: String, value: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = value,
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
/**
 * DETAILED BEHAVIOUR — InitialsProfileAvatar
 *
 * Handles the Compose/UI responsibility for initials profile avatar.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun InitialsProfileAvatar(
    name: String,
    imageUrl: String?,
    size: Dp = 76.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(VolunteerLinkSoftGreenSurface),
        contentAlignment = Alignment.Center
    ) {
        // Initials always exist underneath the network image. If there is no
        // avatar or an old/invalid URL fails, the same initials remain visible.
        Text(
            text = profileInitials(name),
            fontSize = if (size >= 70.dp) 26.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )

        imageUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            ?.let { safeImageUrl ->
                AsyncImage(
                    model = safeImageUrl,
                    contentDescription = "$name profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
    }
}

/**
 * DETAILED BEHAVIOUR — profileInitials
 *
 * Handles the Compose/UI responsibility for profile initials.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun profileInitials(name: String): String = name
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifBlank { "V" }

/**
 * DETAILED BEHAVIOUR — openEmail
 *
 * Handles the Compose/UI responsibility for open email.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
private fun openEmail(context: Context, emailAddress: String) {
    val email = emailAddress.trim()
    if (email.isBlank()) {
        Toast.makeText(context, "Email address is unavailable.", Toast.LENGTH_SHORT).show()
        return
    }

    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("mailto", email, null)
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    }

    try {
        context.startActivity(emailIntent)
        return
    } catch (_: Exception) {
        // Some Android builds have no direct mailto handler. Keep a chooser
        // fallback so the button still works with Gmail/Outlook/share targets.
    }

    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    }

    try {
        context.startActivity(Intent.createChooser(fallbackIntent, "Email organisation"))
    } catch (_: Exception) {
        Toast.makeText(context, "No email application is available.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * DETAILED BEHAVIOUR — openPhone
 *
 * Handles the Compose/UI responsibility for open phone.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
private fun openPhone(context: Context, phoneNumber: String) {
    val phone = phoneNumber.trim()
    if (phone.isBlank()) return

    try {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        )
    } catch (_: Exception) {
        Toast.makeText(context, "No phone application is available.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * DETAILED BEHAVIOUR — openWebsite
 *
 * Handles the Compose/UI responsibility for open website.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 *
 * Handles failure explicitly so network/storage/database errors can be surfaced or cleaned up without leaving
 * the UI in an assumed-success state.
 */
private fun openWebsite(context: Context, websiteUrl: String) {
    var website = websiteUrl.trim()
    if (website.isBlank()) return
    if (!website.startsWith("http://", true) && !website.startsWith("https://", true)) {
        website = "https://$website"
    }

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website)))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open this website.", Toast.LENGTH_SHORT).show()
    }
}
