package com.example.volunteerlink.organisation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.R
import com.example.volunteerlink.data.location.LocationSuggestion
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel


@Composable
fun OrganisationCreateScreen(
    viewModel: CreatePostViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = "Create Volunteer Post",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2A4A1E)
                )

                Text(
                    text = "Create a volunteering opportunity for your organisation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        // ---------------------------------------------------------
        // LOCATION SECTION
        // ---------------------------------------------------------

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_volunteer_location
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Search and select the actual location of your volunteer activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = uiState.locationQuery,
                    onValueChange = viewModel::onLocationQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Search Location")
                    },
                    placeholder = {
                        Text("Example: Sunway University")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }


        // ---------------------------------------------------------
        // SEARCHING
        // ---------------------------------------------------------

        if (uiState.isLocationSearching) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2A4A1E)
                    )

                    Text(
                        text = "Searching locations...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        // ---------------------------------------------------------
        // LOCATION SUGGESTIONS
        // ---------------------------------------------------------

        if (
            uiState.locationSuggestions.isNotEmpty() &&
            uiState.selectedLocation == null
        ) {

            item {

                LocationSuggestionsCard(
                    suggestions = uiState.locationSuggestions,
                    onLocationSelected = { location ->

                        focusManager.clearFocus()

                        viewModel.onLocationSelected(
                            location
                        )
                    }
                )
            }
        }


        // ---------------------------------------------------------
        // SELECTED LOCATION
        // ---------------------------------------------------------

        uiState.selectedLocation?.let { selectedLocation ->

            item {

                SelectedLocationCard(
                    location = selectedLocation,
                    onClear = {
                        viewModel.clearLocation()
                    }
                )
            }
        }


        // ---------------------------------------------------------
        // ERROR
        // ---------------------------------------------------------

        uiState.locationError?.let { errorMessage ->

            item {

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }


        // Bottom spacing
        item {
            Spacer(
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}


// =================================================================
// SUGGESTIONS CARD
// =================================================================

@Composable
private fun LocationSuggestionsCard(
    suggestions: List<LocationSuggestion>,
    onLocationSelected: (LocationSuggestion) -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFDCE5D8),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            // -----------------------------------------------------
            // Suggestions header
            // -----------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F8F3)
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Location Suggestions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2A4A1E)
                )
            }


            // -----------------------------------------------------
            // Individual suggestions
            // -----------------------------------------------------

            suggestions.forEachIndexed { index, location ->

                LocationSuggestionItem(
                    location = location,
                    onClick = {
                        onLocationSelected(location)
                    }
                )

                if (index < suggestions.lastIndex) {

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = 58.dp,
                            end = 14.dp
                        ),
                        color = Color(0xFFE8E8E8)
                    )
                }
            }
        }
    }
}


// =================================================================
// INDIVIDUAL SUGGESTION
// =================================================================

@Composable
private fun LocationSuggestionItem(
    location: LocationSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = location.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E1E1E),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val locationDetails =
                buildLocationDetails(location)

            if (locationDetails.isNotBlank()) {
                Text(
                    text = locationDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF707070),
                    maxLines = 1,
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
}


// =================================================================
// SELECTED LOCATION CARD
// =================================================================

@Composable
private fun SelectedLocationCard(
    location: LocationSuggestion,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFBDD4B5),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F7EE)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Location Selected",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2A4A1E)
                    )

                    Text(
                        text = "This location will be used for the post.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C7768)
                    )
                }

                TextButton(
                    onClick = onClear
                ) {
                    Text(
                        text = "Change",
                        color = Color(0xFF426A34)
                    )
                }
            }

            Text(
                text = location.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF202020)
            )

            val locationDetails =
                buildLocationDetails(location)

            if (locationDetails.isNotBlank()) {
                Text(
                    text = locationDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF667064)
                )
            }
        }
    }
}


// =================================================================
// LOCATION TEXT HELPER
// =================================================================

private fun buildLocationDetails(
    location: LocationSuggestion
): String {

    return listOfNotNull(
        location.city,
        location.state,
        location.country
    )
        .map {
            it.trim()
        }
        .filter {
            it.isNotBlank()
        }
        .distinct()
        .joinToString(", ")
}