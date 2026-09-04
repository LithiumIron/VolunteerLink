package com.example.volunteerlink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerlink.data.VolunteerApplicationWindow
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.ui.theme.*

/** A direct entry to existing account-scoped favourites, including past events. */
@Composable
fun VolunteerFavouritesScreen(
    onBack: () -> Unit,
    onEvent: (Int) -> Unit,
    opportunityViewModel: VolunteerOpportunityViewModel
) {
    val state by opportunityViewModel.uiState.collectAsStateWithLifecycle()
    // Name the calculated favourites value because later UI branches reuse it during this Compose pass.
    val favourites = VolunteerOpportunitySessionStore.volunteerOpportunityEvents
        .filter { it.eventIsSaved }
    // Arrange the following screen content vertically inside the available space.
    Column(Modifier.fillMaxSize().background(VolunteerLinkBackground)) {
        // Arrange the following controls horizontally and keep their alignment consistent.
        Row(
            Modifier.fillMaxWidth().background(VolunteerLinkPrimaryGreen).statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text("Favourites", color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = opportunityViewModel::refresh, enabled = !state.isRefreshing) {
                Text(if (state.isRefreshing) "Syncing…" else "Sync", color = Color.White)
            }
        }
        if (state.isShowingCachedData) {
            Text("Showing your last synced favourites. Reconnect and sync for the latest availability.",
                modifier = Modifier.padding(16.dp), color = VolunteerLinkTextSecondary)
        }
        state.errorMessage?.let {
            Text(it, color = VolunteerLinkError, modifier = Modifier.padding(16.dp))
        }
        if (state.isLoading && favourites.isEmpty()) {
            CircularProgressIndicator(Modifier.padding(24.dp), color = VolunteerLinkPrimaryGreen)
        } else if (favourites.isEmpty()) {
            Text("No favourites yet", modifier = Modifier.padding(20.dp),
                fontWeight = FontWeight.Bold, color = VolunteerLinkTextPrimary)
            Text("Tap the heart in Opportunity Details to add an event here.",
                modifier = Modifier.padding(horizontal = 20.dp), color = VolunteerLinkTextSecondary)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = VolunteerLinkScreenHorizontalPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favourites, key = { it.eventId }) { event ->
                    VolunteerHomeCompactCard(
                        volunteerOpportunityEvent = event,
                        availabilityNotice = if (VolunteerApplicationWindow.canApply(event)) null
                            else "Not open for applications · kept in your favourites",
                        onVolunteerOpportunitySelected = { onEvent(event.eventId) }
                    )
                }
            }
        }
    }
}
