package com.example.volunteerlink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalConfiguration
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
internal fun VolunteerPromotionSection(events: List<VolunteerOpportunityEvent>, onSelected: (Int) -> Unit) {
    // Keep the existing event-card visual language; allow space for a swipe hint on small phones.
    val cardWidth = (LocalConfiguration.current.screenWidthDp - 48).coerceIn(240, 380).dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Featured opportunities", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = VolunteerLinkTextPrimary)
            Text("Paid promotion · Swipe to explore", fontSize = 12.sp, color = VolunteerLinkTextSecondary)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(events, key = { it.eventDatabaseId }) { event ->
                Box(Modifier.width(cardWidth)) {
                    VolunteerHomeCompactCard(event, isPromoted = true, onVolunteerOpportunitySelected = { onSelected(event.eventId) })
                }
            }
        }
    }
}

@Composable
internal fun VolunteerPromotionLoadNotice(onRetry: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Promoted opportunities could not load. You can still browse the other opportunities.", fontSize = 12.sp, color = VolunteerLinkTextSecondary)
        TextButton(onClick = onRetry) { Text("Retry promotions") }
    }
}
