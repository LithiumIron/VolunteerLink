package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
fun VolunteerSignUpScreen(
    onBackSelected: () -> Unit,
    onSignedUp: () -> Unit,
    volunteerAuthViewModel: VolunteerAuthViewModel = viewModel()
) {
    val uiState by volunteerAuthViewModel.uiState
        .collectAsStateWithLifecycle()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onSignedUp()
    }

    val isBusy = uiState.isSubmitting || uiState.isCheckingSession

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create a volunteer account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "Find opportunities and track your verified volunteering hours.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    volunteerAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                },
                singleLine = true,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                colors = volunteerFieldColours()
            )

            Spacer(Modifier.height(13.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    volunteerAuthViewModel.clearError()
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
                colors = volunteerFieldColours()
            )

            Spacer(Modifier.height(13.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    volunteerAuthViewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contact phone") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
                },
                placeholder = { Text("e.g. 0123456789") },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = volunteerFieldColours()
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
                colors = volunteerFieldColours()
            )

            if (uiState.needsEmailConfirmation) {
                Spacer(Modifier.height(11.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Account created. Check your email to confirm it, then sign in.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                    volunteerAuthViewModel.signUp(
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        password = password
                    )
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
                    Text(text = "Create account", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun volunteerFieldColours() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary
    )