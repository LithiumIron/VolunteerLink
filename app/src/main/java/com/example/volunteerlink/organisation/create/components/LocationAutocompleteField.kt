package com.example.volunteerlink.organisation.create.components

// FILE OVERVIEW:
/*
 * LocationAutocompleteField contains presentation code for the organisation Create/Edit Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.data.location.LocationSuggestion

import com.example.volunteerlink.ui.theme.CreateGreen

@Composable
/**
 * Renders the location autocomplete field input field used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun LocationAutocompleteField(
    query: String,
    selectedLocation: LocationSuggestion?,
    suggestions: List<LocationSuggestion>,
    isSearching: Boolean,
    searchError: String?,
    validationError: String?,
    label: String = "Location",
    placeholder: String = "Search a venue or address",
    onQueryChanged: (String) -> Unit,
    onLocationSelected: (LocationSuggestion) -> Unit,
    onClearLocation: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            isError = validationError != null,
            shape = RoundedCornerShape(14.dp),
            enabled = enabled
        )

        if (isSearching) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = CreateGreen
                )
                Text(
                    text = "Searching locations...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (enabled && suggestions.isNotEmpty() && selectedLocation == null) {
            LocationSuggestionList(
                suggestions = suggestions,
                onLocationSelected = onLocationSelected
            )
        }

        selectedLocation?.let { location ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFFBDD4B5),
                        RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F7EE)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Location Selected",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = CreateGreen
                        )

                        TextButton(onClick = onClearLocation, enabled = enabled) {
                            Text("Change")
                        }
                    }

                    Text(
                        text = location.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (
                        location.address.isNotBlank() &&
                        location.address != location.displayName
                    ) {
                        Text(
                            text = location.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (searchError != null && selectedLocation == null) {
            Text(
                text = searchError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FormError(validationError)
    }
}

@Composable
/**
 * Renders the UI represented by location suggestion list for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun LocationSuggestionList(
    suggestions: List<LocationSuggestion>,
    onLocationSelected: (LocationSuggestion) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color(0xFFDCE5D8),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Text(
                text = "Location Suggestions",
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CreateGreen
            )

            suggestions.forEachIndexed { index, location ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLocationSelected(location) }
                        .padding(
                            horizontal = 14.dp,
                            vertical = 11.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = location.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (
                            location.address.isNotBlank() &&
                            location.address != location.displayName
                        ) {
                            Text(
                                text = location.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF426A34)
                    )
                }

                if (index < suggestions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFFE8E8E8)
                    )
                }
            }
        }
    }
}
