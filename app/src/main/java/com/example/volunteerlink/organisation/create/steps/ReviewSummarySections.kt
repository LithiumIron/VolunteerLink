package com.example.volunteerlink.organisation.create.steps

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides a reusable section used by the Create Post wizard for Review Summary Sections.
//
// The composables read CreatePostUiState/CreatePostDraft values and emit callbacks; they do not call Supabase
// directly.
//
// Validation messages are supplied from CreatePostViewModel/CreatePostValidator so the same business rules apply
// regardless of which UI component displays the field.
//
// Breaking large steps into section files keeps layout code readable while the ViewModel remains the single owner
// of mutable workflow state.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.ui.theme.CreateCardBackground
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.ReviewText
import com.example.volunteerlink.ui.theme.ReviewSecondaryText
import com.example.volunteerlink.ui.theme.ReviewBorder
import com.example.volunteerlink.ui.theme.ReviewSoftSurface
import com.example.volunteerlink.ui.theme.ReviewChipBackground
import com.example.volunteerlink.ui.theme.ReviewWarningBackground
import com.example.volunteerlink.ui.theme.ReviewWarningText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
/**
 * Renders the UI represented by review paused schedule banner for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewPausedScheduleBanner
 *
 * Handles the Compose/UI responsibility for review paused schedule banner.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Renders the review section header header used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewSectionHeader
 *
 * Renders the reusable Review Section Header portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
fun ReviewSectionHeader(
    title: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(Locale.ENGLISH),
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
/**
 * Renders the review meta chip chip used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewMetaChip
 *
 * Renders the reusable Review Meta Chip portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
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
/**
 * Renders the review white card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewWhiteCard
 *
 * Renders the reusable Review White Card portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
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
/**
 * Renders the review post mode summary summary block used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
/**
 * DETAILED BEHAVIOUR — ReviewPostModeSummary
 *
 * Renders the reusable Review Post Mode Summary portion of the Organisation UI.
 *
 * It receives values and event callbacks from its parent, which keeps this component reusable and prevents
 * nested UI elements from owning database state.
 */
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
/**
 * Renders the UI represented by review compact label value for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewCompactLabelValue
 *
 * Handles the Compose/UI responsibility for review compact label value.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Renders the UI represented by review stat for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewStat
 *
 * Handles the Compose/UI responsibility for review stat.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Renders the UI represented by review chevron for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewChevron
 *
 * Handles the Compose/UI responsibility for review chevron.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Renders the UI represented by review detail heading for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewDetailHeading
 *
 * Handles the Compose/UI responsibility for review detail heading.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun ReviewDetailHeading(
    text: String
) {
    Text(
        text = text.uppercase(Locale.ENGLISH),
        style = MaterialTheme.typography.labelMedium,
        color = CreateGreen,
        fontWeight = FontWeight.Bold
    )
}


@Composable
/**
 * Returns the review bullet text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewBulletText
 *
 * Handles the Compose/UI responsibility for review bullet text.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Returns the review numbered text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewNumberedText
 *
 * Handles the Compose/UI responsibility for review numbered text.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
/**
 * Returns the review empty text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — ReviewEmptyText
 *
 * Handles the Compose/UI responsibility for review empty text.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun ReviewEmptyText(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = ReviewSecondaryText
    )
}


/**
 * Returns the review slot text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — reviewSlotText
 *
 * Handles the Compose/UI responsibility for review slot text.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun reviewSlotText(
    capacity: Int?
): String? {
    return capacity?.let { value ->
        "$value ${if (value == 1) "volunteer slot" else "volunteer slots"}"
    }
}


/**
 * Returns the review date value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — reviewDate
 *
 * Handles the Compose/UI responsibility for review date.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun reviewDate(
    millis: Long?
): String {
    if (millis == null) return "Not set"

    return SimpleDateFormat(
        "d MMM yyyy",
        Locale.ENGLISH
    ).format(Date(millis))
}


/**
 * Returns the review date range value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — reviewDateRange
 *
 * Handles the Compose/UI responsibility for review date range.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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


/**
 * Returns the review time range value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — reviewTimeRange
 *
 * Handles the Compose/UI responsibility for review time range.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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


/**
 * Returns the review time value required by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
/**
 * DETAILED BEHAVIOUR — reviewTime
 *
 * Handles the Compose/UI responsibility for review time.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
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
        Locale.ENGLISH,
        "%d:%02d %s",
        hour12,
        minute,
        suffix
    )
}
