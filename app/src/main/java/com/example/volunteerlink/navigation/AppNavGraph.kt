package com.example.volunteerlink.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.organisation.navigation.OrganisationNavigationHost
import com.example.volunteerlink.organisation.OrganisationSignInScreen
import com.example.volunteerlink.organisation.OrganisationSignUpScreen
import com.example.volunteerlink.screens.UserTypeSelectionScreen
import com.example.volunteerlink.screens.VolunteerSignInScreen

/**
 * Root navigation graph for the whole application.
 *
 * Both the Volunteer and Organisation branches now route through a
 * Supabase Auth screen before reaching their respective module.
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
                    navController.navigate(AppRoutes.VOLUNTEER_LOGIN)
                },
                onOrganisationClick = {
                    navController.navigate(AppRoutes.ORGANISATION_LOGIN)
                }
            )
        }

        composable(AppRoutes.VOLUNTEER_LOGIN) {
            VolunteerSignInScreen(
                onBackSelected = {
                    navController.popBackStack()
                },
                onSignedIn = {
                    navController.popBackStack(AppRoutes.VOLUNTEER, inclusive = true)
                    navController.navigate(AppRoutes.VOLUNTEER) {
                        popUpTo(AppRoutes.VOLUNTEER_LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = false
                    }
                }
            )
        }

        composable(AppRoutes.VOLUNTEER) {
            // Teammate's existing Volunteer module is used as-is.
            VolunteerOpportunityNavigationHost()
        }

        composable(AppRoutes.ORGANISATION_LOGIN) {
            OrganisationSignInScreen(
                onBackSelected = {
                    navController.popBackStack()
                },
                onSignUpSelected = {
                    navController.navigate(AppRoutes.ORGANISATION_SIGNUP)
                },
                onSignedIn = {
                    // Clear out any stale Organisation entry from a
                    // PREVIOUS session first — otherwise switching accounts
                    // reuses its ViewModelStore and Home keeps showing the
                    // previous org's data. No-op if there isn't one yet.
                    navController.popBackStack(AppRoutes.ORGANISATION, inclusive = true)
                    navController.navigate(AppRoutes.ORGANISATION) {
                        popUpTo(AppRoutes.ORGANISATION_LOGIN) {
                            inclusive = true
                        }
                        launchSingleTop = false
                    }
                }
            )
        }

        composable(AppRoutes.ORGANISATION_SIGNUP) {
            OrganisationSignUpScreen(
                onBackSelected = {
                    navController.popBackStack()
                },
                onSignedUp = {
                    navController.popBackStack(AppRoutes.ORGANISATION, inclusive = true)
                    navController.navigate(AppRoutes.ORGANISATION) {
                        popUpTo(AppRoutes.ORGANISATION_SIGNUP) {
                            inclusive = true
                        }
                        launchSingleTop = false
                    }
                }
            )
        }

        composable(AppRoutes.ORGANISATION) {
            OrganisationNavigationHost()
        }
    }
}