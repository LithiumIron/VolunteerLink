package com.example.volunteerlink.organisation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.navigation.AppBottomNavigationBar
import com.example.volunteerlink.organisation.screens.OrganisationChatsScreen
import com.example.volunteerlink.organisation.screens.OrganisationCreateScreen
import com.example.volunteerlink.organisation.screens.OrganisationHomeScreen
import com.example.volunteerlink.organisation.screens.OrganisationManageScreen
import com.example.volunteerlink.organisation.screens.OrganisationProfileScreen

/**
 * Navigation host for the Organisation side only.
 *
 * Screen files do not own a NavController. Keeping navigation here makes each
 * screen easier to read and avoids mixing UI code with navigation logic.
 */
@Composable
fun OrganisationNavigationHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    /*
     * Match the keyboard behaviour used by AssignmentTest.
     *
     * AssignmentTest runs its content edge-to-edge while the Activity keeps
     * android:windowSoftInputMode="adjustResize". In that setup the IME is
     * delivered as an inset instead of shrinking the Compose root and moving
     * Scaffold's bottom bar above the keyboard.
     *
     * Apply the same window behaviour only while the Organisation branch is
     * active. The bottom bar therefore stays at the real bottom of the window
     * and the keyboard naturally covers it. There is no delayed keyboard
     * observer and no hide/show recomposition, so there is no navigation-bar
     * flash before it disappears behind the IME.
     */
    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        onDispose {
            if (window != null) {
                // The root app also contains the teammate-owned Volunteer
                // branch, so restore its previous non-edge-to-edge behaviour
                // when leaving the Organisation branch.
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Same safe-area strategy as AssignmentTest: individual screens own
        // their top inset, the bottom bar owns navigation-bar padding, and the
        // Scaffold protects horizontal cutouts/system controls in landscape.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal
        ),
        bottomBar = {
            AppBottomNavigationBar(
                items = organisationBottomNavigationItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    if (item.route != currentRoute) {
                        navController.navigate(item.route) {
                            popUpTo(
                                OrganisationNavigationRoutes.HOME
                            ) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OrganisationNavigationRoutes.HOME,
            route = "organisation_root_navigation_graph",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(OrganisationNavigationRoutes.HOME) {
                OrganisationHomeScreen()
            }

            composable(OrganisationNavigationRoutes.MANAGE) {
                OrganisationManageScreen()
            }

            composable(OrganisationNavigationRoutes.CREATE) {
                OrganisationCreateScreen(
                    onExitCreate = {
                        if (!navController.popBackStack()) {
                            navController.navigate(OrganisationNavigationRoutes.HOME) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(OrganisationNavigationRoutes.CHATS) {
                OrganisationChatsScreen()
            }

            composable(OrganisationNavigationRoutes.PROFILE) {
                OrganisationProfileScreen()
            }
        }
    }
}

/** Returns the Activity even when Compose is using a ContextWrapper. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
