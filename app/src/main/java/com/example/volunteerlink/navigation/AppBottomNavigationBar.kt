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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 10.dp,
                end = 10.dp,
                bottom = 8.dp
            )
            .height(82.dp),

        shape = RoundedCornerShape(26.dp),

        // Original prototype-style navigation background.
        color = Color(0xFFF7FAF5),

        shadowElevation = 12.dp,
        tonalElevation = 0.dp,

        border = BorderStroke(
            width = 1.2.dp,
            color = Color(0xFF71836B)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            items.forEach { item ->

                BottomNavigationItem(
                    item = item,
                    selected = currentRoute == item.route,
                    showNotification = showChatNotification && item.label == "Chats",
                    onClick = {
                        onItemClick(item)
                    }
                )
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

    val interactionSource = remember {
        MutableInteractionSource()
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !selected,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Rounded selected indicator behind the icon.
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(38.dp)
                .background(
                    color = if (selected) {
                        Color(0xFFD4E6CC)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(19.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painterResource(
                    id = item.iconRes
                ),
                contentDescription = item.label,
                modifier = Modifier.size(item.iconSize),
                tint = if (selected) {
                    Color(0xFF2A4A1E)
                } else {
                    Color(0xFF263824)
                }
            )

            if (showNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 7.dp)
                        .size(9.dp)
                        .background(Color(0xFFE05B4F), RoundedCornerShape(50))
                )
            }
        }

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = item.label.uppercase(),
            fontSize = 10.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
            letterSpacing = 0.3.sp,
            color = if (selected) {
                Color(0xFF2A4A1E)
            } else {
                Color(0xFF263824)
            },
            maxLines = 1
        )
    }
}
