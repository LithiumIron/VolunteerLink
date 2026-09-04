package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

/** Simple title+message pair shown in the generic info dialog below. */
private data class SettingsInfoDialogContent(
    val title: String,
    val message: String
)

@Composable
// Purpose: Renders the volunteer settings screen and connects user actions to navigation or its ViewModel.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerSettingsScreen(
    onBackSelected: () -> Unit,
    onEditProfileSelected: () -> Unit,
    onLoggedOut: () -> Unit
) {
    // Create a lifecycle-aware coroutine scope for asynchronous work started by this Compose screen.
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }

    // Placeholder content for rows that don't have a real destination yet
    // (Help & Support, Privacy Policy, About). Tapping one just shows a
    // short dialog instead of doing nothing.
    var infoDialogContent by remember {
        mutableStateOf<SettingsInfoDialogContent?>(null)
    }

    // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to access your volunteer profile.") },
            confirmButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = {
                        showLogoutConfirmation = false
                        scope.launch {
                            isLoggingOut = true
                            try {
                                supabase.auth.signOut()
                                VolunteerOpportunitySessionStore.clearProfileData()
                                onLoggedOut()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isLoggingOut = false
                            }
                        }
                    }
                ) { Text("Log out", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = VolunteerLinkPrimaryGreen)
                }
            }
        )
    }

    infoDialogContent?.let { content ->
        AlertDialog(
            onDismissRequest = { infoDialogContent = null },
            title = { Text(content.title) },
            text = { Text(content.message) },
            confirmButton = {
                TextButton(onClick = { infoDialogContent = null }) {
                    Text("OK", color = VolunteerLinkPrimaryGreen)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .height(70.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackSelected) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "SETTINGS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(modifier = Modifier.padding(top = 12.dp)) {

            SettingsSectionLabel("Account")
            SettingsRow(
                icon = Icons.Filled.Person,
                title = "Edit Profile",
                subtitle = "Name, phone, bio, email",
                onClick = onEditProfileSelected
            )

            SettingsSectionLabel("Support")
            SettingsRow(
                icon = Icons.Filled.SupportAgent,
                title = "Help & Support",
                subtitle = "Contact us or view FAQs",
                onClick = {
                    infoDialogContent = SettingsInfoDialogContent(
                        title = "Help & Support",
                        message = "Need help? Reach us at support@volunteerlink.com. " +
                                "A full FAQ section is coming soon."
                    )
                }
            )
            SettingsRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                subtitle = null,
                onClick = {
                    infoDialogContent = SettingsInfoDialogContent(
                        title = "Privacy Policy",
                        message = "Our full privacy policy is coming soon. In the meantime, " +
                                "your data is only used to power your VolunteerLink account " +
                                "and volunteering activity."
                    )
                }
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About VolunteerLink",
                subtitle = "Version 1.0.0",
                onClick = {
                    infoDialogContent = SettingsInfoDialogContent(
                        title = "About VolunteerLink",
                        message = "VolunteerLink v1.0.0 — connecting volunteers with " +
                                "organisations making a difference."
                    )
                }
            )

            SettingsSectionLabel("")
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = if (isLoggingOut) "Logging out..." else "Log Out",
                subtitle = null,
                titleColor = Color(0xFFC62828),
                iconColor = Color(0xFFC62828),
                showChevron = false,
                onClick = {
                    if (!isLoggingOut) showLogoutConfirmation = true
                }
            )
        }
    }
}

@Composable
// Purpose: Handles settings section label as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun SettingsSectionLabel(label: String) {
    if (label.isNotBlank()) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextSecondary
        )
    } else {
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
// Purpose: Handles settings row as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    titleColor: Color = VolunteerLinkTextPrimary,
    iconColor: Color = VolunteerLinkPrimaryGreen,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VolunteerLinkBorderColour,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
