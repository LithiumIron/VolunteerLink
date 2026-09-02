package com.example.volunteerlink.organisation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OrganisationSignInScreen(
    onBackSelected: () -> Unit,
    onSignUpSelected: () -> Unit,
    onSignedIn: () -> Unit,
    organisationAuthViewModel: OrganisationAuthViewModel = viewModel()
) {
    val uiState by organisationAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onSignedIn()
    }

    val isBusy = uiState.isSubmitting || uiState.isCheckingSession

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
                    onClick = { organisationAuthViewModel.continueWithRestoredSession() }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { organisationAuthViewModel.useDifferentAccount() }
                ) {
                    Text("Use a different account")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        IconButton(onClick = onBackSelected, enabled = !isBusy) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Organisation sign in",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "Sign in to manage your posts and volunteer applicants.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    organisationAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Mail, contentDescription = null)
                },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = organisationFieldColours()
            )

            Spacer(Modifier.height(13.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    organisationAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                },
                singleLine = true,
                enabled = !isBusy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp),
                colors = organisationFieldColours()
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(11.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    organisationAuthViewModel.signIn(email = email, password = password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Sign in as Organisation", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New organisation?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onSignUpSelected, enabled = !isBusy) {
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

@Composable
internal fun organisationFieldColours() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary
    )