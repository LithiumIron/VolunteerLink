package com.example.volunteerlink.organisation.create.steps

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.RemoteSubmissionMode
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ReviewText = Color(0xFF263824)
val ReviewSecondaryText = Color(0xFF687266)
val ReviewBorder = Color(0xFFDDE5DA)
val ReviewSoftSurface = Color(0xFFF5F8F3)
val ReviewChipBackground = Color(0xFFE8F1E4)
val ReviewWarningBackground = Color(0xFFFFF5E8)
val ReviewWarningText = Color(0xFF85500C)

@Composable
fun ReviewPausedScheduleBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ReviewWarningBackground,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Unfinished schedule input",
                color = ReviewWarningText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "A Step 4 editor is still paused. Only saved schedule items are shown in this review.",
                style = MaterialTheme.typography.bodySmall,
                color = ReviewSecondaryText
            )
        }
    }
}


@Composable
fun ReviewSectionHeader(
    title: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onEdit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(CreateGreen)
                )
                Text(
                    text = "Edit",
                    color = CreateGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
fun ReviewMetaChip(
    text: String
) {
    Surface(
        color = ReviewChipBackground,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            color = CreateGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
fun ReviewWhiteCard(
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ReviewBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}


@Composable
fun ReviewPostModeSummary(
    title: String,
    firstLine: String,
    secondLine: String? = null,
    thirdLine: String? = null,
    fourthLine: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = firstLine,
            style = MaterialTheme.typography.bodyMedium,
            color = ReviewText,
            fontWeight = FontWeight.Medium
        )
        listOfNotNull(secondLine, thirdLine, fourthLine).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = ReviewSecondaryText
            )
        }
    }
}


@Composable
fun ReviewCompactLabelValue(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ReviewSecondaryText,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ReviewText
        )
    }
}


@Composable
fun ReviewStat(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ReviewSecondaryText
        )
    }
}


@Composable
fun ReviewChevron(
    isExpanded: Boolean
) {
    Image(
        painter = painterResource(
            if (isExpanded) {
                R.drawable.review_chevron_up
            } else {
                R.drawable.review_chevron_down
            }
        ),
        contentDescription = if (isExpanded) {
            "Collapse details"
        } else {
            "Expand details"
        },
        modifier = Modifier.size(22.dp),
        colorFilter = ColorFilter.tint(CreateGreen)
    )
}


@Composable
fun ReviewDetailHeading(
    text: String
) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium,
        color = CreateGreen,
        fontWeight = FontWeight.Bold
    )
}


@Composable
fun ReviewBulletText(
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = CreateGreen
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = ReviewText
        )
    }
}


@Composable
fun ReviewNumberedText(
    number: Int,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            color = CreateGreen,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = ReviewText
        )
    }
}


@Composable
fun ReviewEmptyText(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = ReviewSecondaryText
    )
}


fun reviewSlotText(
    capacity: Int?
): String? {
    return capacity?.let { value ->
        "$value ${if (value == 1) "volunteer slot" else "volunteer slots"}"
    }
}


fun reviewDate(
    millis: Long?
): String {
    if (millis == null) return "Not set"

    return SimpleDateFormat(
        "d MMM yyyy",
        Locale.getDefault()
    ).format(Date(millis))
}


fun reviewDateRange(
    startMillis: Long?,
    endMillis: Long?
): String {
    if (startMillis == null && endMillis == null) return "Date not set"
    if (startMillis == endMillis && startMillis != null) {
        return reviewDate(startMillis)
    }

    return "${reviewDate(startMillis)} – ${reviewDate(endMillis)}"
}


fun reviewTimeRange(
    startMinutes: Int?,
    endMinutes: Int?
): String {
    if (startMinutes == null && endMinutes == null) return "Time not set"

    return when {
        startMinutes != null && endMinutes != null ->
            "${reviewTime(startMinutes)} – ${reviewTime(endMinutes)}"
        startMinutes != null -> reviewTime(startMinutes)
        else -> reviewTime(endMinutes)
    }
}


fun reviewTime(
    minutes: Int?
): String {
    if (minutes == null) return "Not set"

    val hour24 = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val value = hour24 % 12) {
        0 -> 12
        else -> value
    }

    return String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        hour12,
        minute,
        suffix
    )
}
