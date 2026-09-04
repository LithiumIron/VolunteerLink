package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.volunteerlink.shared.authFieldColours
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkError
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

@Composable
// Purpose: Renders the volunteer sign in screen and connects user actions to navigation or its ViewModel.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerSignInScreen(
    onBackSelected: () -> Unit,
    onSignUpSelected: () -> Unit,
    onSignedIn: () -> Unit,
    volunteerAuthViewModel: VolunteerAuthViewModel = viewModel()
) {
    val uiState by volunteerAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(false) }

    // Run this side effect only when its Compose keys change, instead of repeating it on every redraw.
    LaunchedEffect(uiState.isAuthenticated) {
        // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
        if (uiState.isAuthenticated) onSignedIn()
    }

    // Calculate whether the following UI or action is allowed before it is rendered or executed.
    val isBusy = uiState.isSigningIn || uiState.isCheckingSession

    // Arrange the following screen content vertically inside the available space.
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

        // Arrange the following screen content vertically inside the available space.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Display the prepared label; business rules are calculated before reaching this UI call.
            Text(
                text = "Volunteer sign in",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(Modifier.height(7.dp))

            // Display the prepared label; business rules are calculated before reaching this UI call.
            Text(
                text =
                    "Sign in to load your opportunities, applications " +
                            "and verified Skill Path progress from Supabase.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(Modifier.height(26.dp))

            // Display an editable Compose field and send each value change back to screen state.
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
                colors = authFieldColours()
            )

            Spacer(Modifier.height(13.dp))

            // Display an editable Compose field and send each value change back to screen state.
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
                colors = authFieldColours()
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(11.dp))
                // Display the prepared label; business rules are calculated before reaching this UI call.
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkError
                )
            }

            Spacer(Modifier.height(11.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBusy) { rememberMe = !rememberMe },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    enabled = !isBusy
                )
                Text(
                    text = "Remember me on this device",
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    volunteerAuthViewModel.signIn(
                        email = email,
                        password = password,
                        rememberMe = rememberMe
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
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New volunteer?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onSignUpSelected,
                    enabled = !isBusy,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Create an account",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
