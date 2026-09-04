package com.example.volunteerlink.organisation.navigation

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines Organisation navigation metadata/behaviour associated with Organisation Bottom Navigation Items.
//
// Routes and bottom-nav items are kept outside individual screens so navigation destinations, labels and argument
// formats remain consistent.
//
// Navigation passes identifiers and UI intent only; backend repositories still verify ownership/permissions when a
// destination loads protected data.
//
// Architectural layer: Navigation layer.
// ============================================================================


import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.navigation.BottomNavItem

/**
 * Organisation-specific destinations shown inside the shared bottom bar.
 *
 * The bar design itself lives in AppBottomNavigationBar.kt so visual changes
 * do not need to be repeated across every navigation item.
 */
val organisationBottomNavigationItems = listOf(
    BottomNavItem(
        route = OrganisationNavigationRoutes.HOME,
        label = "Home",
        iconRes = R.drawable.ic_volunteer_home,
        iconSize = 27.dp
    ),
    BottomNavItem(
        route = OrganisationNavigationRoutes.MANAGE,
        label = "Manage",
        iconRes = R.drawable.manage,
        iconSize = 30.dp
    ),
    BottomNavItem(
        route = OrganisationNavigationRoutes.CREATE,
        label = "Create",
        iconRes = R.drawable.create,
        iconSize = 30.dp
    ),
    BottomNavItem(
        route = OrganisationNavigationRoutes.CHATS,
        label = "Chats",
        iconRes = R.drawable.chat,
        iconSize = 28.dp
    ),
    BottomNavItem(
        route = OrganisationNavigationRoutes.PROFILE,
        label = "Profile",
        iconRes = R.drawable.profile,
        iconSize = 29.dp
    )
)
