package com.example.volunteerlink.organisation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning
import java.text.DateFormat
import java.util.Date

/**
 * Small shared building blocks for the Organisation branch.
 *
 * The design deliberately favours native mobile rows, dividers and focused
 * actions over wrapping every item in a card.  Feature screens still own their
 * business logic and callbacks; these components only standardise presentation.
 */
@Composable
fun OrganisationSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }

        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}


@Composable
fun OrganisationSectionSurface(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun OrganisationListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supportingText: String? = null,
    @DrawableRes iconRes: Int? = null,
    statusText: String? = null,
    statusColor: Color = VolunteerLinkPrimaryGreen,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 2.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) VolunteerLinkTextPrimary else VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!statusText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OrganisationStatusPill(statusText, statusColor)
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailingContent()
        } else if (showChevron && onClick != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                painter = painterResource(R.drawable.back),
                contentDescription = null,
                modifier = Modifier
                    .size(17.dp)
                    .rotate(180f),
                tint = VolunteerLinkTextSecondary.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
fun OrganisationStatusPill(
    text: String,
    color: Color = VolunteerLinkPrimaryGreen,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun OrganisationDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = VolunteerLinkBorderColour.copy(alpha = 0.9f)
    )
}


@Composable
fun OrganisationOfflineStatusCard(
    lastSyncedAtEpochMillis: Long?,
    isSyncing: Boolean,
    onSyncSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSyncText = lastSyncedAtEpochMillis?.let { timestamp ->
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(Date(timestamp))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VolunteerLinkWarning.copy(alpha = 0.13f),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkWarning.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Offline data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = lastSyncText?.let {
                        "Showing your last successful sync from $it."
                    } ?: "Showing your last saved organisation data.",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            TextButton(
                onClick = onSyncSelected,
                enabled = !isSyncing
            ) {
                Text(
                    text = if (isSyncing) "SYNCING…" else "SYNC",
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }
        }
    }
}

@Composable
fun OrganisationInfoStrip(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accent: Color = VolunteerLinkPrimaryGreen,
    @DrawableRes iconRes: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (iconRes != null) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.11f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accent
                    )
                }
            }
            Spacer(modifier = Modifier.width(11.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 3.dp),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
fun OrganisationPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OrganisationMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 12.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}


@Composable
fun OrganisationMessageButton(
    personName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = VolunteerLinkSoftGreenSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.chat),
                contentDescription = "Message $personName",
                modifier = Modifier.size(20.dp),
                tint = VolunteerLinkPrimaryGreen
            )
        }
    }
}
