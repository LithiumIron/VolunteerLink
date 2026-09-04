package com.example.volunteerlink.organisation.create.components

// FILE OVERVIEW:
/*
 * CreatePostComponents contains presentation code for the organisation Create/Edit Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.ui.theme.CreateCardBackground
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType


@Composable
/**
 * Renders the UI represented by edit restriction notice for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun EditRestrictionNotice(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF7E8),
        border = BorderStroke(1.dp, Color(0xFFE3C472))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = Color(0xFFFFE8B5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D5318)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5F4815)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
/**
 * Renders the create section card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun CreateSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CreateGreen
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
/**
 * Renders the post type card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun PostTypeCard(
    type: VolunteerPostType,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val borderColor = when {
        !enabled -> Color(0xFFC7CBC5)
        selected -> CreateGreen
        else -> Color(0xFFD5D8D2)
    }
    val background = when {
        !enabled -> Color(0xFFF1F2F0)
        selected -> CreateLightGreen
        else -> Color.White
    }

    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Icon area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .alpha(if (enabled) 1f else 0.42f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keep title area consistent for all 3 cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = type.displayName,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        selected -> CreateGreen
                        else -> Color(0xFF263824)
                    },
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Keep description centered too
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = type.shortDescription,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
/**
 * Renders the category picker picker used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun CategoryPicker(
    selectedCategory: VolunteerPostCategory?,
    onCategorySelected: (VolunteerPostCategory) -> Unit,
    errorMessage: String?,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleError = errorMessage.takeIf { enabled }

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            }
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { expanded = true },
                shape = RoundedCornerShape(14.dp),
                color = if (enabled) Color.Transparent else Color(0xFFF1F2F0),
                border = BorderStroke(
                    1.dp,
                    when {
                        visibleError != null -> MaterialTheme.colorScheme.error
                        !enabled -> Color(0xFFC7CBC5)
                        else -> Color(0xFF777A76)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 15.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedCategory?.displayName
                            ?: "Select a category",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            selectedCategory == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Image(
                        painter = painterResource(R.drawable.org_create_dropdown),
                        contentDescription = "Open categories",
                        modifier = Modifier
                            .size(18.dp)
                            .alpha(if (enabled) 1f else 0.42f)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                VolunteerPostCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.displayName) },
                        onClick = {
                            expanded = false
                            onCategorySelected(category)
                        }
                    )
                }
            }
        }

        FormError(visibleError)
    }
}

@Composable
/**
 * Renders the form selection field input field used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun FormSelectionField(
    label: String,
    value: String,
    placeholder: String,
    iconRes: Int? = null,
    errorMessage: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val visibleError = errorMessage.takeIf { enabled }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = if (enabled) Color.Transparent else Color(0xFFF1F2F0),
            border = BorderStroke(
                1.dp,
                when {
                    visibleError != null -> MaterialTheme.colorScheme.error
                    !enabled -> Color(0xFFC7CBC5)
                    else -> Color(0xFF777A76)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(if (enabled) 1f else 0.42f)
                    )
                }

                Text(
                    text = value.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        value.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        FormError(visibleError)
    }
}

@Composable
/**
 * Renders the volunteer capacity field input field used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun VolunteerCapacityField(
    value: Int?,
    onValueChanged: (String) -> Unit,
    label: String = "Volunteers Needed",
    supportingText: String? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OutlinedTextField(
            value = value?.toString().orEmpty(),
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            isError = errorMessage != null,
            shape = RoundedCornerShape(14.dp),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        if (!supportingText.isNullOrBlank() && errorMessage == null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FormError(errorMessage)
    }
}

@Composable
/**
 * Returns the form error used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun FormError(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
