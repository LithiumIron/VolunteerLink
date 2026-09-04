package com.example.volunteerlink.organisation.components

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Provides reusable Organisation design/layout components associated with Organisation Module Page.
//
// These composables standardise VolunteerLink Organisation spacing, cards, headers and module-page structure
// without owning repository or ViewModel state.
//
// Keeping shared presentation here reduces duplicated Material3 configuration across Home, Manage, Create, Impact
// Weave and Profile.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

/**
 * Reusable base content for the temporary Organisation pages.
 *
 * These pages currently contain only fixed text. When a page later displays
 * a growing collection, that page should use LazyColumn or LazyRow as needed.
 */
@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationModulePage
 *
 * Handles the Compose/UI responsibility for organisation module page.
 *
 * UI-only work stays here; business validation and Supabase persistence remain delegated to the
 * ViewModel/repository layers.
 */
fun OrganisationModulePage(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
