package com.example.volunteerlink.organisation.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.organisation.repository.OrganisationProfileRepository
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

// EMAIL removed — the login email now goes through its own two-step OTP
// flow (see emailChangeStep below), not the generic single-field dialog.
private enum class ContactFieldType { PHONE, EMAIL, DESCRIPTION }


@Composable
fun OrganisationSettingScreen(
    onBackSelected: () -> Unit,
    onEditProfileSelected: () -> Unit,
    onLoggedOut: () -> Unit,
    onRefresh: () -> Unit = {}   // NEW — refetches profile data, same as OrganisationProfileScreen's onRefresh
){
    val scope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    var showLogoutConfirmation by rememberSaveable { mutableStateOf(false) }

    var infoDialogContent by remember {
        mutableStateOf<SettingsInfoDialogContent?>(null)
    }
    val cachedProfile = OrganisationSessionStore.profileData

    var editingContactField by remember { mutableStateOf<ContactFieldType?>(null) }
    var contactFieldValue by remember { mutableStateOf("") }
    var isSavingContactField by remember { mutableStateOf(false) }
    var contactFieldError by remember { mutableStateOf<String?>(null) }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to access your organisation profile.") },
            confirmButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = {
                        showLogoutConfirmation = false
                        scope.launch {
                            isLoggingOut = true
                            try {
                                supabase.auth.signOut()
                                OrganisationSessionStore.clearProfileData()
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

    // Generic single-field dialog — now only PHONE and DESCRIPTION.
    editingContactField?.let { fieldType ->
        AlertDialog(
            onDismissRequest = {
                if (!isSavingContactField) {
                    editingContactField = null
                    contactFieldError = null
                }
            },
            title = {
                Text(
                    when (fieldType) {
                        ContactFieldType.PHONE -> "Contact phone"
                        ContactFieldType.EMAIL -> "Contact email"
                        ContactFieldType.DESCRIPTION -> "About your organisation"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        text = when (fieldType) {
                            ContactFieldType.PHONE -> "Shown to volunteers on your posts. Can differ from your login phone number."
                            ContactFieldType.EMAIL -> "Shown to volunteers on your posts. Can differ from your login email."
                            ContactFieldType.DESCRIPTION -> "Shown to volunteers viewing your organisation's profile."
                        },
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = contactFieldValue,
                        onValueChange = {
                            contactFieldValue = it
                            contactFieldError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (fieldType == ContactFieldType.DESCRIPTION)
                                    Modifier.height(140.dp)
                                else Modifier
                            ),
                        singleLine = fieldType != ContactFieldType.DESCRIPTION,
                        enabled = !isSavingContactField,
                        keyboardOptions = when (fieldType) {
                            ContactFieldType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                            ContactFieldType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email)
                            ContactFieldType.DESCRIPTION -> KeyboardOptions.Default
                        }
                    )
                    contactFieldError?.let { message ->
                        Spacer(Modifier.height(6.dp))
                        Text(message, fontSize = 12.sp, color = Color(0xFFC62828))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSavingContactField,
                    onClick = {
                        scope.launch {
                            isSavingContactField = true
                            contactFieldError = null
                            val success = when (fieldType) {
                                ContactFieldType.PHONE ->
                                    OrganisationProfileRepository.updateContactPhone(contactFieldValue.trim())
                                ContactFieldType.EMAIL ->
                                    OrganisationProfileRepository.updateContactEmail(contactFieldValue.trim())
                                ContactFieldType.DESCRIPTION ->
                                    OrganisationProfileRepository.updateDescription(contactFieldValue.trim())
                            }
                            isSavingContactField = false
                            if (success) {
                                OrganisationSessionStore.clearProfileData()
                                onRefresh()   // NEW — immediately reloads instead of waiting for next screen visit
                                editingContactField = null
                            } else {
                                contactFieldError = "Couldn't save. Try again."
                            }
                        }
                    }
                ) {
                    Text(if (isSavingContactField) "Saving..." else "Save", color = VolunteerLinkPrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSavingContactField,
                    onClick = {
                        editingContactField = null
                        contactFieldError = null
                    }
                ) { Text("Cancel") }
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
                subtitle = "Name, phone, bio, location",
                onClick = onEditProfileSelected
            )

            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Description",
                subtitle = cachedProfile?.description?.ifBlank { "Not set" } ?: "Not set",
                onClick = {
                    contactFieldValue = cachedProfile?.description.orEmpty()
                    contactFieldError = null
                    editingContactField = ContactFieldType.DESCRIPTION
                }
            )

            SettingsRow(
                icon = Icons.Filled.Phone,
                title = "Contact Phone",
                subtitle = cachedProfile?.contactPhone?.ifBlank { "Not set" } ?: "Not set",
                onClick = {
                    contactFieldValue = cachedProfile?.contactPhone.orEmpty()
                    contactFieldError = null
                    editingContactField = ContactFieldType.PHONE
                }
            )

            SettingsRow(
                icon = Icons.Filled.Mail,
                title = "Contact Email",
                subtitle = cachedProfile?.contactEmail?.ifBlank { "Not set" } ?: "Not set",
                onClick = {
                    contactFieldValue = cachedProfile?.contactEmail.orEmpty()
                    contactFieldError = null
                    editingContactField = ContactFieldType.EMAIL
                }
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