package com.example.volunteerlink.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.volunteerlink.R
import com.example.volunteerlink.data.VolunteerProfileRepository
import com.example.volunteerlink.data.saveProfileImage
import com.example.volunteerlink.ui.theme.DeepGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTheme
import kotlinx.coroutines.launch



@Composable
fun EditVolunteerProfileScreen(
    onBack: () -> Unit = {},
    // Called after a successful save. Pass in whatever refresh mechanism
    // VolunteerProfileScreen's session store uses (e.g. clearing the
    // cached profileData) so the profile view doesn't keep showing stale
    // data after you navigate back — loadProfile() here only updates
    // this screen's own state, not the shared cache.
    onSaved: () -> Unit = {}
) {

    // =========================
    // PROFILE INFORMATION
    // =========================

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }


    // =========================
    // PROFILE IMAGE
    // =========================

    var profileImageUrl by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var isSaving by remember {
        mutableStateOf(false)
    }


    // =========================
    // LOAD EXISTING PROFILE
    // =========================
    // Without this, every field above starts blank and saving would wipe
    // out the volunteer's real name/phone/bio with empty strings unless
    // they happened to retype everything from scratch.

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val existingProfile = VolunteerProfileRepository.loadProfile()

        if (existingProfile != null) {
            name = existingProfile.fullName
            email = existingProfile.email
            phone = existingProfile.phone
            bio = existingProfile.bio
            profileImageUrl = existingProfile.profileImageUrl
        }

        isLoading = false
    }


    // =========================
    // IMAGE PICKER
    // =========================

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                scope.launch {

                    val imageUrl = saveProfileImage(
                        context = context,
                        uri = uri
                    )

                    if (imageUrl != null) {
                        profileImageUrl = imageUrl
                    }
                }
            }
        }


    // =========================
    // SCREEN
    // =========================

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {


        // =========================
        // TOP BAR
        // =========================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .height(70.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_volunteer_back
                        ),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "EDIT PROFILE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
// =========================
            // PROFILE PICTURE
            // =========================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(VolunteerLinkSoftGreenSurface)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },

                    contentAlignment = Alignment.Center
                ) {

                    if (profileImageUrl != null) {

                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Icon(
                            painter = painterResource(
                                id = R.drawable.profile
                            ),
                            contentDescription = "Add Profile Picture",
                            tint = VolunteerLinkPrimaryGreen,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = "Tap to change profile picture",
                color = DeepGreen,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )


            // =========================
            // PROFILE INFORMATION
            // =========================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

                // NAME
                Text(
                    text = "Name",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Enter your name")
                    }
                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                // EMAIL (read-only — login email lives in auth.users, not
                // user_profiles, and changing it requires Supabase's email
                // change flow with re-confirmation. Editing it here would
                // silently do nothing, which is worse than not showing it
                // as editable at all.)
                Text(
                    text = "Email",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                    placeholder = {
                        Text("Your login email")
                    }
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Email can't be changed from here.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                // PHONE
                Text(
                    text = "Phone Number",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Enter your phone number")
                    }
                )


                Spacer(
                    modifier = Modifier.height(20.dp)
                )


                // BIO
                Text(
                    text = "Bio",
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = {
                        bio = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false,
                    placeholder = {
                        Text("Tell organisations a bit about yourself")
                    }
                )


                Spacer(
                    modifier = Modifier.height(30.dp)
                )


                // =========================
                // SAVE BUTTON
                // =========================

                Button(
                    onClick = {

                        scope.launch {
                            isSaving = true

                            try {
                                val success = VolunteerProfileRepository.updateProfile(
                                    name = name,
                                    phone = phone,
                                    bio = bio,
                                    profileImageUrl = profileImageUrl
                                )

                                if (success) {
                                    onSaved()
                                    onBack()
                                } else {
                                    println("Failed to update profile")
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                println("Failed to update profile: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },

                    enabled = !isSaving,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {

                    Text(
                        text = if (isSaving) {
                            "SAVING..."
                        } else {
                            "SAVE"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


    }
}

@Preview(showBackground = true)
@Composable
fun ProfileEditPreview(){
    VolunteerLinkTheme() {
        EditVolunteerProfileScreen()
    }
}