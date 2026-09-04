package com.example.volunteerlink.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

/**
 * Represents one destination inside the shared bottom navigation bar.
 *
 * Each section of the app provides its own route, label and drawable icon,
 * while AppBottomNavigationBar controls the visual design.
 */
// Purpose: Handles bottom nav item as one reusable step in the Volunteer flow.
// Usage: Used by the app navigation graph when the volunteer opens, returns from, or switches a destination.
// Navigation effect: Route arguments identify the selected event, role, application or certificate.
data class BottomNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val iconSize: Dp = 28.dp
)

/**
 * Shared VolunteerLink bottom navigation bar.
 *
 * Organisation and other parts of the app can reuse the same visual design
 * without repeating the navigation bar code.
 */
@Composable
// Purpose: Handles app bottom navigation bar as one reusable step in the Volunteer flow.
// Usage: Used by the app navigation graph when the volunteer opens, returns from, or switches a destination.
// Navigation effect: Route arguments identify the selected event, role, application or certificate.
fun AppBottomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
    showChatNotification: Boolean = false
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 10.dp,
                end = 10.dp,
                bottom = 8.dp
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(22.dp),
            color = VolunteerLinkSoftGreenSurface.copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.dp,
                color = VolunteerLinkPrimaryGreen.copy(alpha = 0.55f)
            ),
            shadowElevation = 10.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    BottomNavigationItem(
                        item = item,
                        selected = currentRoute == item.route,
                        showNotification = showChatNotification && item.label == "Chats",
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * Displays one destination inside the navigation bar.
 *
 * This is kept private because it is only used internally by
 * AppBottomNavigationBar.
 */
@Composable
private fun RowScope.BottomNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    showNotification: Boolean,
    onClick: () -> Unit
) {

    val navigationContentColour = if (selected) {
        VolunteerLinkPrimaryGreen
    } else {
        VolunteerLinkTextSecondary.copy(alpha = 0.78f)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(37.dp)
                .background(
                    color = if (selected) {
                        VolunteerLinkPrimaryGreen.copy(alpha = 0.14f)
                    } else {
                        Color.Transparent
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painterResource(
                    id = item.iconRes
                ),
                contentDescription = item.label,
                modifier = Modifier.size(item.iconSize),
                tint = navigationContentColour
            )

            if (showNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .background(Color(0xFFE05B4F), CircleShape)
                )
            }
        }

        Text(
            text = item.label.uppercase(),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
            color = navigationContentColour,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
