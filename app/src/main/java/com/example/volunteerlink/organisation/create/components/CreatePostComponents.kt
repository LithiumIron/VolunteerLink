package com.example.volunteerlink.organisation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.model.VolunteerPostCategory
import com.example.volunteerlink.organisation.create.model.VolunteerPostType

val CreateGreen = Color(0xFF2A4A1E)
private val CreateLightGreen = Color(0xFFE5EFE1)
private val CreateCardBackground = Color(0xFFFBFCF9)

@Composable
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
fun PostTypeCard(
    type: VolunteerPostType,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) CreateGreen else Color(0xFFD5D8D2)
    val background = if (selected) CreateLightGreen else Color.White

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 14.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) CreateGreen else Color(0xFF263824),
                maxLines = 2
            )

            Text(
                text = type.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun CategoryPicker(
    selectedCategory: VolunteerPostCategory?,
    onCategorySelected: (VolunteerPostCategory) -> Unit,
    errorMessage: String?
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF777A76)
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
                        color = if (selectedCategory == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Image(
                        painter = painterResource(R.drawable.org_create_dropdown),
                        contentDescription = "Open categories",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
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

        FormError(errorMessage)
    }
}

@Composable
fun HelpNeededEditor(
    input: String,
    items: List<String>,
    onInputChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    errorMessage: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Help Needed",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Add short points describing what support the post needs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Example: Registration support") },
                singleLine = true,
                isError = errorMessage != null && items.isEmpty(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onAdd() }
                )
            )

            Button(
                onClick = onAdd,
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreateGreen
                )
            ) {
                Text("Add")
            }
        }

        items.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5EF)
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 6.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TextButton(onClick = { onRemove(item) }) {
                        Text("Remove")
                    }
                }
            }
        }

        FormError(errorMessage)
    }
}

@Composable
fun FormSelectionField(
    label: String,
    value: String,
    placeholder: String,
    iconRes: Int? = null,
    errorMessage: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color(0xFF777A76)
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
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = value.ifBlank { placeholder },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        FormError(errorMessage)
    }
}

@Composable
fun VolunteerCapacityField(
    value: Int?,
    onValueChanged: (String) -> Unit,
    label: String = "Volunteers Needed",
    supportingText: String? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
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
fun FormError(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
