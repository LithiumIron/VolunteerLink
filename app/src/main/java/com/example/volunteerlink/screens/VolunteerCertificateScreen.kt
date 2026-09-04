package com.example.volunteerlink.screens

// Renders a certificate only after the linked role has been verified as completed.

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerOpportunityApplication
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
// Purpose: Creates the certificate screen only for a verified completed participation and supports PDF export.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
fun VolunteerCertificateScreen(
    volunteerApplicationId: Int,
    onBackSelected: () -> Unit
) {
    // Use the current Android context for permissions, files, resources or external intents.
    val context = LocalContext.current
    // Resolve or prepare the application data used by the next status, cancellation or navigation decision.
    val volunteerApplication =
        VolunteerOpportunitySessionStore.findApplicationById(
            volunteerApplicationId
        )

    // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
    if (
        volunteerApplication == null ||
        volunteerApplication.applicationStatus !=
        VolunteerApplicationStatus.COMPLETED
    ) {
        VolunteerCertificateUnavailableScreen(
            onBackSelected = onBackSelected
        )
        // Keep this Compose block separate so its visual state follows the value prepared above.
        return
    }

    // Prepare certificate evidence that is available only after verified completion.
    val certificateId =
        volunteerApplication.applicationCertificateId
            ?: "VL-${volunteerApplication.applicationDatabaseId}"

    // Register an Android activity-result launcher so the system response returns safely to this screen.
    val downloadLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { destinationUri ->
            // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
            if (destinationUri != null) {
                // Name the calculated was saved value because later UI branches reuse it during this Compose pass.
                val wasSaved = saveVolunteerCertificatePdf(
                    context = context,
                    destinationUri = destinationUri,
                    volunteerApplication = volunteerApplication,
                    certificateId = certificateId
                )
                Toast.makeText(
                    context,
                    // Check this condition before showing the action, preventing an invalid Volunteer flow from continuing.
                    if (wasSaved) {
                        "Certificate PDF saved successfully."
                    } else {
                        "Unable to save the certificate PDF."
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
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
                text = "Volunteer Certificate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
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
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
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
                colors = CardDefaults.cardColors(
                    containerColor = VolunteerLinkSurface
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = VolunteerLinkPrimaryGreen.copy(alpha = 0.65f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 26.dp
                    ),
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
                        text = volunteerApplication.applicationVolunteerName,
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
                        text = volunteerApplication.applicationRoleTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkPrimaryGreen
                    )
                    Text(
                        text = volunteerApplication.applicationEventTitle,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = volunteerApplication.applicationOrganisationName,
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
                        CertificateMetric(
                            label = "COMPLETED",
                            value =
                                volunteerApplication.applicationCompletedDate
                                    ?: "Verified"
                        )
                        CertificateMetric(
                            label = "SERVICE",
                            value = formatCertificateMinutes(
                                volunteerApplication
                                    .applicationVerifiedMinutes
                                    ?: 0
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(17.dp))

                    Text(
                        text = "Credential ID: $certificateId",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkTextSecondary
                    )
                    Text(
                        text =
                            "Issued from an organisation-verified " +
                                "VolunteerLink completion record.",
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            volunteerApplication.applicationOrganisationFeedback
                ?.takeIf(String::isNotBlank)
                ?.let { feedback ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = VolunteerLinkSoftGreenSurface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Organisation feedback",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkPrimaryGreen
                            )
                            Text(
                                text = feedback,
                                modifier = Modifier.padding(top = 5.dp),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = VolunteerLinkTextPrimary
                            )
                        }
                    }
                }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VolunteerLinkSurface,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = {
                    downloadLauncher.launch(
                        "VolunteerLink-$certificateId.pdf"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = VolunteerLinkScreenHorizontalPadding,
                        vertical = 12.dp
                    )
                    .height(50.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerLinkPrimaryGreen
                )
            ) {
                Text(
                    text = "Download Certificate (PDF)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
// Purpose: Handles certificate metric as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun CertificateMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

@Composable
// Purpose: Renders the volunteer certificate unavailable screen and connects user actions to navigation or its ViewModel.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun VolunteerCertificateUnavailableScreen(
    onBackSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Certificate unavailable",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text =
                "A certificate is issued only after the organisation " +
                    "marks an accepted role as Completed.",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            color = VolunteerLinkTextSecondary
        )
        Button(
            onClick = onBackSelected,
            modifier = Modifier.padding(top = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VolunteerLinkPrimaryGreen
            )
        ) {
            Text("Return")
        }
    }
}

// Purpose: Converts certificate minutes into the display text expected by the Volunteer UI.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun formatCertificateMinutes(minutes: Int): String {
    // Name the calculated hours value because later UI branches reuse it during this Compose pass.
    val hours = minutes / 60
    // Name the calculated remainder value because later UI branches reuse it during this Compose pass.
    val remainder = minutes % 60
    return when {
        minutes <= 0 -> "Verified"
        hours == 0 -> "$remainder min"
        remainder == 0 -> "$hours hr"
        else -> "$hours hr $remainder min"
    }
}

// Purpose: Draws the visible certificate data into a PDF and writes it to the user-selected document.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
private fun saveVolunteerCertificatePdf(
    context: Context,
    destinationUri: Uri,
    volunteerApplication: VolunteerOpportunityApplication,
    certificateId: String
): Boolean {
    // Name the calculated document value because later UI branches reuse it during this Compose pass.
    val document = PdfDocument()
    return try {
        // Name the calculated page info value because later UI branches reuse it during this Compose pass.
        val pageInfo =
            PdfDocument.PageInfo.Builder(1120, 792, 1).create()
        // Name the calculated page value because later UI branches reuse it during this Compose pass.
        val page = document.startPage(pageInfo)
        // Calculate whether the following UI or action is allowed before it is rendered or executed.
        val canvas = page.canvas
        // Name the calculated paint value because later UI branches reuse it during this Compose pass.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(android.graphics.Color.rgb(250, 252, 248))

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.color = android.graphics.Color.rgb(48, 92, 38)
        canvas.drawRect(32f, 32f, 1088f, 760f, paint)
        paint.strokeWidth = 2f
        canvas.drawRect(48f, 48f, 1072f, 744f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(48, 92, 38)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 26f
        canvas.drawText("VOLUNTEERLINK", 560f, 105f, paint)

        paint.textSize = 43f
        canvas.drawText(
            "CERTIFICATE OF VOLUNTEER SERVICE",
            560f,
            178f,
            paint
        )

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(95, 105, 92)
        paint.textSize = 20f
        canvas.drawText("This certifies that", 560f, 235f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.color = android.graphics.Color.rgb(28, 34, 27)
        paint.textSize = 39f
        canvas.drawText(
            volunteerApplication.applicationVolunteerName,
            560f,
            290f,
            paint
        )

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(95, 105, 92)
        paint.textSize = 20f
        canvas.drawText(
            "successfully completed the volunteer role",
            560f,
            335f,
            paint
        )

        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.color = android.graphics.Color.rgb(48, 92, 38)
        paint.textSize = 32f
        canvas.drawText(
            volunteerApplication.applicationRoleTitle,
            560f,
            388f,
            paint
        )

        paint.color = android.graphics.Color.rgb(28, 34, 27)
        paint.textSize = 25f
        canvas.drawText(
            volunteerApplication.applicationEventTitle,
            560f,
            434f,
            paint
        )

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(95, 105, 92)
        paint.textSize = 19f
        canvas.drawText(
            volunteerApplication.applicationOrganisationName,
            560f,
            470f,
            paint
        )

        paint.color = android.graphics.Color.rgb(48, 92, 38)
        canvas.drawRect(250f, 515f, 870f, 518f, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.color = android.graphics.Color.rgb(28, 34, 27)
        paint.textSize = 20f
        canvas.drawText(
            "Completed: " +
                (volunteerApplication.applicationCompletedDate
                    ?: "Verified"),
            380f,
            565f,
            paint
        )
        canvas.drawText(
            "Service: " +
                formatCertificateMinutes(
                    volunteerApplication.applicationVerifiedMinutes ?: 0
                ),
            740f,
            565f,
            paint
        )

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = android.graphics.Color.rgb(95, 105, 92)
        paint.textSize = 17f
        canvas.drawText(
            "Credential ID: $certificateId",
            560f,
            635f,
            paint
        )
        paint.textSize = 15f
        canvas.drawText(
            "Issued from an organisation-verified VolunteerLink completion record",
            560f,
            675f,
            paint
        )

        document.finishPage(page)
        context.contentResolver
            .openOutputStream(destinationUri)
            ?.use { outputStream ->
                document.writeTo(outputStream)
            }
            ?: return false
        true
    } catch (_: Exception) {
        false
    } finally {
        document.close()
    }
}
