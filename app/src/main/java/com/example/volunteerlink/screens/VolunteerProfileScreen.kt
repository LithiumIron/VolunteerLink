package com.example.volunteerlink.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.R
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme

@Composable
fun VolunteerProfileScreen(
    onVolunteerSettingSelected: () -> Unit = {},
    onVolunteerNotificationsSelected: () -> Unit = {},
    onEditProfileSelected: () -> Unit = {},
    onCompletedEventSelected: (applicationId: Int) -> Unit = {},
    onCompletedEventsSelected: () -> Unit = {},
    onCertificateSelected: (applicationId: Int) -> Unit = {},
    onCertificatesSelected: () -> Unit = {},
    onSkillPathItemSelected: (skillPathId: String) -> Unit = {},
    onSkillPathSelected: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    // Use shared data from SessionStore
    val profileData = VolunteerOpportunitySessionStore.profileData

    LaunchedEffect(Unit) {
        onRefresh()
    }

    val isLoading = profileData == null

    val name = profileData?.fullName ?: "Loading..."
    val email = profileData?.email ?: "Loading..."
    val bio = profileData?.bio ?: ""
    val city = profileData?.city ?: ""
    val memberSince = profileData?.memberSince ?: "Loading..."
    val profileImageUrl = profileData?.profileImageUrl
    val verifiedHours = profileData?.verifiedHours ?: 0
    val completedEvents = profileData?.completedEvents ?: emptyList()
    val certificates = profileData?.certificates ?: emptyList()
    val skillPaths = profileData?.skillPaths ?: emptyList()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = VolunteerLinkPrimaryGreen
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 102.dp)
    ) {

        // =====================================================
        // TOP BAR
        // =====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .height(70.dp)
        ) {
            Text(
                text = "PROFILE",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onVolunteerNotificationsSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_volunteer_notifications),
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onVolunteerSettingSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.setting),
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // =====================================================
        // PROFILE HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(VolunteerLinkSoftGreenSurface),
                contentAlignment = Alignment.Center
            ) {
                // Keep the normal initials underneath the image. This means a
                // missing avatar and an old/broken avatar URL use the same fallback.
                Text(
                    text = name
                        .trim()
                        .split(Regex("\\s+"))
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifBlank { "V" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )

                profileImageUrl
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                    ?.let { safeImageUrl ->
                        AsyncImage(
                            model = safeImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = email,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onEditProfileSelected,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VolunteerLinkSoftGreenSurface)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = "Edit Profile",
                    tint = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // City shown as a small icon + text row, same full-width treatment
        // as bio/member-since below the header.
        if (city.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = city,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

        // Bio and member-since now sit full-width below the header row,
        // instead of squeezed into the narrow column beside the avatar.
        if (bio.isNotBlank()) {
            Text(
                text = bio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color.DarkGray
            )
        }

        Text(
            text = "Member since $memberSince",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )



        // =====================================================
        // VERIFIED HOURS + COMPLETED EVENTS
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VolunteerLinkSoftGreenSurface)
                    .border(
                        width = 1.dp,
                        color = VolunteerLinkBorderColour,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(15.dp)
            ) {
                Column {
                    Text(
                        text = "Verified Hours",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$verifiedHours hrs",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VolunteerLinkSoftGreenSurface)
                    .border(
                        width = 1.dp,
                        color = VolunteerLinkBorderColour,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onCompletedEventsSelected() }
                    .padding(15.dp)
            ) {
                Column {
                    Text(
                        text = "Completed Events",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${completedEvents.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }
        }

        // =====================================================
        // COMPLETED EVENTS LIST
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed Events",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.clickable {
                        onCompletedEventsSelected()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (completedEvents.isEmpty()) {
                Text(
                    text = "No completed events yet",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    completedEvents.take(3).forEach { application ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = VolunteerLinkBorderColour,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onCompletedEventSelected(
                                        application.applicationId
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = application.applicationEventTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = application.applicationRoleTitle,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================
        // CERTIFICATES
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Certificates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.clickable {
                        onCertificatesSelected()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (certificates.isEmpty()) {
                Text(
                    text = "No certificates yet",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    certificates.take(1).forEach { application ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = VolunteerLinkBorderColour,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onCertificateSelected(
                                        application.applicationId
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = application.applicationEventTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // =====================================================
        // SKILL PATHS
        // =====================================================

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skill Paths",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen,
                    modifier = Modifier.clickable {
                        onSkillPathSelected()
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (skillPaths.isEmpty()) {
                Text(
                    text = "No skill paths yet",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    skillPaths.take(3).forEach { skillPath ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VolunteerLinkSoftGreenSurface)
                                .border(
                                    width = 1.dp,
                                    color = VolunteerLinkBorderColour,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSkillPathItemSelected(
                                        skillPath.skillPathId
                                    )
                                }
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    VolunteerLinkTheme {
        VolunteerProfileScreen()
    }
}