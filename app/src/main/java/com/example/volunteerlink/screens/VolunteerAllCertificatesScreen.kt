package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.ui.theme.*

@Composable
fun VolunteerAllCertificatesScreen(
    onBackSelected: () -> Unit,
    onCertificateSelected: (applicationId: Int) -> Unit
) {
    val certificatedApplications = VolunteerOpportunitySessionStore
        .volunteerApplications
        .filter {
            it.applicationStatus == VolunteerApplicationStatus.COMPLETED &&
                    !it.applicationCertificateId.isNullOrBlank()
        }

    Column(modifier = Modifier.fillMaxSize().background(VolunteerLinkBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VolunteerLinkPrimaryGreen)
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 16.dp),
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
                text = "My Certificates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (certificatedApplications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No certificates yet",
                    color = VolunteerLinkTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = VolunteerLinkScreenHorizontalPadding,
                    vertical = 16.dp
                )
            ) {
                items(
                    items = certificatedApplications,
                    key = { it.applicationId }
                ) { application ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VolunteerLinkSurface)
                            .clickable {
                                onCertificateSelected(application.applicationId)
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = application.applicationEventTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkTextPrimary
                            )
                            Text(
                                text = application.applicationRoleTitle,
                                fontSize = 11.sp,
                                color = VolunteerLinkPrimaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}