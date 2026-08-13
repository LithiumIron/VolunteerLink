package com.example.volunteerlink.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.organisation.navigation.OrganisationNavigationHost
import com.example.volunteerlink.screens.UserTypeSelectionScreen
import com.example.volunteerlink.screens.VolunteerSignInScreen

/**
 * Root navigation graph for the whole application.
 *
 * The existing Volunteer navigation host is left unchanged. This graph only
 * adds the temporary first-page choice and the new Organisation branch.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.USER_TYPE_SELECTION
    ) {
        composable(AppRoutes.USER_TYPE_SELECTION) {
            UserTypeSelectionScreen(
                onVolunteerClick = {
                    navController.navigate(
                        AppRoutes.VOLUNTEER_LOGIN
                    )
                },
                onOrganisationClick = {
                    navController.navigate(AppRoutes.ORGANISATION)
                }
            )
        }

        composable(AppRoutes.VOLUNTEER_LOGIN) {
            VolunteerSignInScreen(
                onBackSelected = {
                    navController.popBackStack()
                },
                onSignedIn = {
                    navController.navigate(AppRoutes.VOLUNTEER) {
                        popUpTo(AppRoutes.VOLUNTEER_LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoutes.VOLUNTEER) {
            // Teammate's existing Volunteer module is used as-is.
            VolunteerOpportunityNavigationHost()
        }

        composable(AppRoutes.ORGANISATION) {
            OrganisationNavigationHost()
        }
    }
}
