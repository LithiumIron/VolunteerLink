package com.example.volunteerlink.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun VolunteerSettingsScreen(
    onBackSelected: () -> Unit,
    onEditProfileSelected: () -> Unit,
    // Called after Supabase sign-out succeeds. Wire this to whatever
    // resets your root nav graph back to the login/auth flow — this
    // screen doesn't know about that graph, only that logout finished.
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

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
                subtitle = "Name, phone, bio, availability",
                onClick = onEditProfileSelected
            )

            SettingsSectionLabel("Preferences")
            SettingsToggleRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Application updates and reminders",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            SettingsSectionLabel("Support")
            SettingsRow(
                icon = Icons.Filled.SupportAgent,
                title = "Help & Support",
                subtitle = "Contact us or view FAQs",
                onClick = { }
            )
            SettingsRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                subtitle = null,
                onClick = { }
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About VolunteerLink",
                subtitle = "Version 1.0.0",
                onClick = { }
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

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VolunteerLinkPrimaryGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VolunteerLinkPrimaryGreen
            )
        )
    }
}