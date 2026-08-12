package com.example.volunteerlink.organisation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(
            left = 0,
            top = 0,
            right = 0,
            bottom = 0
        ),
        bottomBar = {
            AppBottomNavigationBar(
                items = organisationBottomNavigationItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    if (item.route != currentRoute) {
                        navController.navigate(item.route) {
                            // Keep only one copy of each main destination and
                            // restore its state when returning to it.
                            popUpTo(OrganisationNavigationRoutes.HOME) {
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
                OrganisationCreateScreen()
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
