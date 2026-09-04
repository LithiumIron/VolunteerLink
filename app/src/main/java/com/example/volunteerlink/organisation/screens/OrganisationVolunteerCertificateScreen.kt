package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Volunteer Certificate Screen.
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


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.organisation.repository.OrganisationReadOnlyProfileRepository
import com.example.volunteerlink.organisation.repository.OrganisationViewedVolunteerCertificate
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationVolunteerCertificateScreen
 *
 * Renders the Organisation Volunteer Certificate screen from state supplied by the owning ViewModel/repository-
 * facing coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationVolunteerCertificateScreen(
    userId: String,
    postId: String,
    roleTemplateId: String,
    onBack: () -> Unit
) {
    var certificate by remember(userId, postId, roleTemplateId) {
        mutableStateOf<OrganisationViewedVolunteerCertificate?>(null)
    }
    var loading by remember(userId, postId, roleTemplateId) { mutableStateOf(true) }

    LaunchedEffect(userId, postId, roleTemplateId) {
        loading = true
        certificate = OrganisationReadOnlyProfileRepository.loadVolunteerCertificate(
            userId = userId,
            postId = postId,
            roleTemplateId = roleTemplateId
        )
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VolunteerLinkPrimaryGreen)
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Volunteer Certificate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
            }
            return@Column
        }

        val data = certificate
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This certificate could not be loaded.",
                    color = VolunteerLinkTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verified achievement",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkSuccess
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Text(
                        text = "ORGANISATION VERIFIED",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VolunteerLinkSurface),
                border = BorderStroke(
                    width = 2.dp,
                    color = VolunteerLinkPrimaryGreen.copy(alpha = 0.65f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = VolunteerLinkSoftGreenSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = VolunteerLinkSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "CERTIFICATE OF VOLUNTEER SERVICE",
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkPrimaryGreen
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    Text(
                        text = "This certifies that",
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                    Text(
                        text = data.volunteerName,
                        modifier = Modifier.padding(vertical = 5.dp),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "successfully completed the volunteer role",
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = data.roleName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkPrimaryGreen
                    )
                    Text(
                        text = data.eventTitle,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = data.organisationName,
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = VolunteerLinkBorderColour)
                    Spacer(modifier = Modifier.height(13.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReadOnlyCertificateMetric(
                            label = "COMPLETED",
                            value = data.completedAt?.take(10) ?: "Verified"
                        )
                        ReadOnlyCertificateMetric(
                            label = "SERVICE",
                            value = formatCertificateMinutes(data.verifiedMinutes ?: 0)
                        )
                    }

                    Spacer(modifier = Modifier.height(17.dp))

                    Text(
                        text = "Credential ID: ${data.certificateId}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                    Text(
                        text = "Issued from an organisation-verified VolunteerLink completion record.",
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }


            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
/**
 * DETAILED BEHAVIOUR — ReadOnlyCertificateMetric
 *
 * Handles the Compose/UI responsibility for read only certificate metric.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun ReadOnlyCertificateMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
    }
}

/**
 * DETAILED BEHAVIOUR — formatCertificateMinutes
 *
 * Handles the Compose/UI responsibility for format certificate minutes.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
private fun formatCertificateMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val remainder = safe % 60
    return when {
        safe <= 0 -> "Verified"
        hours == 0 -> "$remainder min"
        remainder == 0 -> "$hours hr"
        else -> "$hours hr $remainder min"
    }
}
