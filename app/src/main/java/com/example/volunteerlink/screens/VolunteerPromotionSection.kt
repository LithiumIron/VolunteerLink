package com.example.volunteerlink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.volunteerlink.data.VolunteerOnline
import com.example.volunteerlink.data.VolunteerPromotion
import com.example.volunteerlink.data.VolunteerPromotionRepository
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.model.VolunteerOpportunityEvent
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal data class VolunteerPromotionFeed(
    val entries: List<VolunteerPromotion> = emptyList(),
    val failed: Boolean = false,
    val retry: () -> Unit = {}
)

/** Promotions are online display data, not an offline promise of an active purchase. */
@Composable
internal fun rememberVolunteerPromotionFeed(usingCachedDashboard: Boolean): VolunteerPromotionFeed {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val account = supabase.auth.currentUserOrNull()?.id.orEmpty()
    var entries by remember(account) { mutableStateOf(emptyList<VolunteerPromotion>()) }
    var failed by remember(account) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    // Promotions are refreshed while Home is visible because a paid placement can end
    // during the session. Cached/offline dashboards deliberately show no promotion.
    LaunchedEffect(account, owner, retry, usingCachedDashboard) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var untilNextRefresh = 0
            while (true) {
                if (usingCachedDashboard || account.isBlank() || supabase.auth.currentUserOrNull()?.id != account || !VolunteerOnline.available(context)) {
                    entries = emptyList()
                    failed = false
                    untilNextRefresh = 0
                } else if (untilNextRefresh <= 0) {
                    try {
                        entries = VolunteerPromotionRepository.load(account)
                        failed = false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Missing migration/network failure must not break the ordinary Home feed.
                        entries = emptyList()
                        failed = true
                    }
                    untilNextRefresh = 60
                }
                delay(5_000)
                untilNextRefresh -= 5
            }
        }
    }
    return VolunteerPromotionFeed(if (usingCachedDashboard) emptyList() else entries, failed && !usingCachedDashboard) { retry++ }
}

@Composable
// Purpose: Renders the volunteer promotion section from values prepared by the parent screen; it does not load Supabase data itself.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerPromotionSection(events: List<VolunteerOpportunityEvent>, onSelected: (Int) -> Unit) {
    // Match the ordinary Home list's actual container width and shared side margins.
    // Do not narrow cards to expose the next card, or cap tablet widths at 380dp.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val cardWidth = (maxWidth - VolunteerLinkScreenHorizontalPadding * 2).coerceAtLeast(0.dp)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(horizontal = VolunteerLinkScreenHorizontalPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Featured opportunities", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
            Text("Paid promotion · Swipe to explore", fontSize = 12.sp, color = VolunteerLinkTextSecondary)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = VolunteerLinkScreenHorizontalPadding), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Only events already passed through normal discovery rules reach this list.
            // Promotion changes placement; it never bypasses availability or eligibility.
            items(events, key = { it.eventDatabaseId }) { event ->
                Box(Modifier.width(cardWidth)) {
                    VolunteerHomeCompactCard(event, isPromoted = true, onVolunteerOpportunitySelected = { onSelected(event.eventId) })
                }
            }
        }
      }
    }
}

@Composable
// Purpose: Handles volunteer promotion load notice as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
internal fun VolunteerPromotionLoadNotice(onRetry: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Promoted opportunities could not load. You can still browse the other opportunities.", fontSize = 12.sp, color = VolunteerLinkTextSecondary)
        TextButton(onClick = onRetry) { Text("Retry promotions") }
    }
}
