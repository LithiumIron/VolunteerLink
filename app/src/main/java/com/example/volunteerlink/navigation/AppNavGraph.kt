package com.example.volunteerlink.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.organisation.navigation.OrganisationNavigationHost
import com.example.volunteerlink.organisation.OrganisationSignInScreen
import com.example.volunteerlink.organisation.OrganisationSignUpScreen
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.screens.UserTypeSelectionScreen
import com.example.volunteerlink.screens.VolunteerSignInScreen
import com.example.volunteerlink.screens.VolunteerSignUpScreen

/**
 * Root navigation graph for the whole application.
 *
 * Both the Volunteer and Organisation branches now route through a
 * Supabase Auth screen before reaching their respective module.
 */
@Composable
// Purpose: Handles app nav graph as one reusable step in the Volunteer flow.
// Usage: Used by the app navigation graph when the volunteer opens, returns from, or switches a destination.
// Navigation effect: Route arguments identify the selected event, role, application or certificate.
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
                },
                onSignUpSelected = {
                    navController.navigate(
                        AppRoutes.VOLUNTEER_SIGNUP
                    )
                }
            )
        }

        composable(AppRoutes.VOLUNTEER_SIGNUP) {
            VolunteerSignUpScreen(
                onBackSelected = {
                    navController.popBackStack()
                },
                onSignedUp = {
                    navController.popBackStack(AppRoutes.VOLUNTEER, inclusive = true)
                    navController.navigate(AppRoutes.VOLUNTEER) {
                        popUpTo(AppRoutes.VOLUNTEER_SIGNUP) {
                            inclusive = true
                        }
                        launchSingleTop = false
                    }
                }
            )
        }

        composable(AppRoutes.VOLUNTEER) {
            // Teammate's existing Volunteer module is used as-is.
            VolunteerOpportunityNavigationHost(
                onLoggedOut = {
                    navController.navigate(AppRoutes.USER_TYPE_SELECTION) {
                        popUpTo(AppRoutes.VOLUNTEER) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
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
                    // A process can survive logout and a second login. Remove the
                    // previous organisation's profile before showing the new Home.
                    OrganisationSessionStore.clearProfileData()
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
                    OrganisationSessionStore.clearProfileData()
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
            OrganisationNavigationHost(
                onLoggedOut = {
                    navController.navigate(AppRoutes.USER_TYPE_SELECTION) {
                        popUpTo(AppRoutes.ORGANISATION) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
