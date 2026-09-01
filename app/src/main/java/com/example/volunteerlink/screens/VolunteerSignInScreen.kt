package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

@Composable
fun VolunteerSignInScreen(
    onBackSelected: () -> Unit,
    onSignedIn: () -> Unit,
    volunteerAuthViewModel: VolunteerAuthViewModel = viewModel()
) {
    val uiState by volunteerAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var email by rememberSaveable {
        mutableStateOf("volunteer.login.2026@example.com")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onSignedIn()
    }

    val isBusy = uiState.isSigningIn || uiState.isCheckingSession

    // A saved session was restored on launch — ask before signing in
    // automatically, so a different account can be used instead without
    // needing to sign out manually first. Same pattern as the
    // organisation sign-in screen.
    uiState.pendingAccountEmail?.let { pendingEmail ->
        AlertDialog(
            // Empty on purpose — force an explicit choice rather than
            // letting a tap-outside or back-press silently sign someone in.
            onDismissRequest = {},
            title = { Text("Continue as $pendingEmail?") },
            text = {
                Text(
                    "You're already signed in with this account on this " +
                            "device. Continue with it, or sign in with a " +
                            "different account instead."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { volunteerAuthViewModel.continueWithRestoredSession() }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { volunteerAuthViewModel.useDifferentAccount() }
                ) {
                    Text("Use a different account")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBackSelected,
            enabled = !isBusy
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VolunteerLinkPrimaryGreen
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Volunteer sign in",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text =
                    "Sign in to load your opportunities, applications " +
                            "and verified Skill Path progress from Supabase.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    volunteerAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Mail,
                        contentDescription = null
                    )
                },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = volunteerSignInFieldColours()
            )

            Spacer(Modifier.height(13.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    volunteerAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null
                    )
                },
                singleLine = true,
                enabled = !isBusy,
                visualTransformation =
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp),
                colors = volunteerSignInFieldColours()
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(11.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkError
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    volunteerAuthViewModel.signIn(
                        email = email,
                        password = password
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerLinkPrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign in as Volunteer",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun volunteerSignInFieldColours() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = VolunteerLinkSurface,
        unfocusedContainerColor = VolunteerLinkSurface,
        focusedBorderColor = VolunteerLinkPrimaryGreen,
        unfocusedBorderColor = VolunteerLinkBorderColour,
        cursorColor = VolunteerLinkPrimaryGreen
    )