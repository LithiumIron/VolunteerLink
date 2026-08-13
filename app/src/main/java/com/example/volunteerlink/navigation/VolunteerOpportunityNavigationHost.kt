package com.example.volunteerlink.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.screens.MapScreen
import com.example.volunteerlink.screens.VolunteerApplicationDetailsScreen
import com.example.volunteerlink.screens.VolunteerApplicationScreen
import com.example.volunteerlink.screens.VolunteerCertificateScreen
import com.example.volunteerlink.screens.VolunteerHomeScreen
import com.example.volunteerlink.screens.VolunteerMyApplicationsScreen
import com.example.volunteerlink.screens.VolunteerOpportunityDetailsScreen
import com.example.volunteerlink.screens.VolunteerOpportunityViewModel
import com.example.volunteerlink.screens.VolunteerRoleDetailsScreen
import com.example.volunteerlink.screens.VolunteerSearchScreen
import com.example.volunteerlink.screens.VolunteerSkillPathDetailsScreen
import com.example.volunteerlink.screens.VolunteerSkillPathScreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

@Composable
fun VolunteerOpportunityNavigationHost() {
    val volunteerOpportunityViewModel:
        VolunteerOpportunityViewModel = viewModel()

    val opportunityUiState by
        volunteerOpportunityViewModel.uiState
            .collectAsStateWithLifecycle()

    if (opportunityUiState.isLoading) {
        VolunteerOpportunityLoadingScreen()
        return
    }

    opportunityUiState.errorMessage?.let { errorMessage ->
        VolunteerOpportunityLoadErrorScreen(
            errorMessage = errorMessage,
            onRetry = volunteerOpportunityViewModel::retry
        )
        return
    }

    val volunteerNavigationController =
        rememberNavController()

    val currentVolunteerNavigationBackStackEntry by
    volunteerNavigationController
        .currentBackStackEntryAsState()

    val currentVolunteerNavigationRoute =
        currentVolunteerNavigationBackStackEntry
            ?.destination
            ?.route

    val volunteerBottomNavigationRoutes =
        setOf(
            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_HOME_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_MAP_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_SKILL_PATH_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_CHAT_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_PROFILE_ROUTE
        )

    val shouldShowVolunteerBottomNavigationBar =
        currentVolunteerNavigationRoute in
                volunteerBottomNavigationRoutes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor =
                VolunteerLinkBackground,
            contentWindowInsets =
                WindowInsets(
                    left = 0,
                    top = 0,
                    right = 0,
                    bottom = 0
                )
        ) { volunteerNavigationInnerPadding ->

            NavHost(
                navController =
                    volunteerNavigationController,

                startDestination =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_HOME_ROUTE,

                route =
                    "volunteer_root_navigation_graph",

                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        volunteerNavigationInnerPadding
                    )
            ) {
                // Home
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_HOME_ROUTE
                ) {
                    VolunteerHomeScreen(
                        onVolunteerSearchSelected = {
                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .VOLUNTEER_SEARCH_ROUTE
                                )
                        },

                        onVolunteerOpportunitySelected = {
                                volunteerEventId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerOpportunityDetailsRoute(
                                            volunteerEventId
                                        )
                                )
                        },

                        onVolunteerApplicationSelected = {
                                volunteerApplicationId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerApplicationDetailsRoute(
                                            volunteerApplicationId
                                        )
                                )
                        },

                        onViewAllApplicationsSelected = {
                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .VOLUNTEER_MY_APPLICATIONS_ROUTE
                                )
                        }
                    )
                }

                // Search
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_SEARCH_ROUTE
                ) {
                    VolunteerSearchScreen(
                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },

                        onVolunteerOpportunitySelected = {
                                volunteerEventId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerOpportunityDetailsRoute(
                                            volunteerEventId
                                        )
                                )
                        }
                    )
                }

                // My Applications
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_MY_APPLICATIONS_ROUTE
                ) {
                    VolunteerMyApplicationsScreen(
                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },
                        onVolunteerApplicationSelected = {
                                volunteerApplicationId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerApplicationDetailsRoute(
                                            volunteerApplicationId
                                        )
                                )
                        }
                    )
                }

                // Opportunity Details
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_OPPORTUNITY_DETAILS_ROUTE,

                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            }
                        )
                ) { navigationBackStackEntry ->

                    val volunteerEventId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerOpportunityDetailsScreen(
                        volunteerEventId =
                            volunteerEventId,

                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },

                        onVolunteerRoleSelected = {
                                selectedEventId,
                                selectedRoleId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerRoleDetailsRoute(
                                            volunteerEventId =
                                                selectedEventId,
                                            volunteerRoleId =
                                                selectedRoleId
                                        )
                                )
                        }
                    )
                }

                // Role Details
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_ROLE_DETAILS_ROUTE,

                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            },

                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_ROLE_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            }
                        )
                ) { navigationBackStackEntry ->

                    val volunteerEventId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            )
                            ?: return@composable

                    val volunteerRoleId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_ROLE_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerRoleDetailsScreen(
                        volunteerEventId =
                            volunteerEventId,

                        volunteerRoleId =
                            volunteerRoleId,

                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },

                        onJoinRoleSelected = {
                                selectedEventId,
                                selectedRoleId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerRoleApplicationRoute(
                                            volunteerEventId =
                                                selectedEventId,
                                            volunteerRoleId =
                                                selectedRoleId
                                        )
                                )
                        }
                    )
                }

                // Role Application
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_ROLE_APPLICATION_ROUTE,

                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            },

                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_ROLE_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            }
                        )
                ) { navigationBackStackEntry ->

                    val volunteerEventId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EVENT_ID_ARGUMENT
                            )
                            ?: return@composable

                    val volunteerRoleId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_ROLE_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerApplicationScreen(
                        volunteerEventId =
                            volunteerEventId,

                        volunteerRoleId =
                            volunteerRoleId,

                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },

                        onReturnHomeSelected = {
                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .VOLUNTEER_HOME_ROUTE
                                ) {
                                    popUpTo(
                                        VolunteerOpportunityNavigationRoutes
                                            .VOLUNTEER_HOME_ROUTE
                                    ) {
                                        inclusive = false
                                    }

                                    launchSingleTop = true
                                }
                        },
                        volunteerOpportunityViewModel =
                            volunteerOpportunityViewModel
                    )
                }

                // Application Details
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_APPLICATION_DETAILS_ROUTE,
                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_APPLICATION_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            }
                        )
                ) { navigationBackStackEntry ->
                    val volunteerApplicationId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_APPLICATION_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerApplicationDetailsScreen(
                        volunteerApplicationId =
                            volunteerApplicationId,
                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },
                        onVolunteerOpportunitySelected = {
                                volunteerEventId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerOpportunityDetailsRoute(
                                            volunteerEventId
                                        )
                                )
                        },
                        onVolunteerRoleSelected = {
                                volunteerEventId,
                                volunteerRoleId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerRoleDetailsRoute(
                                            volunteerEventId =
                                                volunteerEventId,
                                            volunteerRoleId =
                                                volunteerRoleId
                                        )
                                )
                        },
                        onCertificateSelected = {
                                volunteerApplicationId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerCertificateRoute(
                                            volunteerApplicationId
                                        )
                                )
                        },
                        volunteerOpportunityViewModel =
                            volunteerOpportunityViewModel
                    )
                }

                // Certificate for a completed volunteer role
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_CERTIFICATE_ROUTE,
                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_APPLICATION_ID_ARGUMENT
                            ) {
                                type = NavType.IntType
                            }
                        )
                ) { navigationBackStackEntry ->
                    val volunteerApplicationId =
                        navigationBackStackEntry
                            .arguments
                            ?.getInt(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_APPLICATION_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerCertificateScreen(
                        volunteerApplicationId =
                            volunteerApplicationId,
                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        }
                    )
                }

                // Map
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_MAP_ROUTE
                ) {
                    MapScreen(
                        volunteerOpportunityEvents =
                            VolunteerOpportunitySessionStore
                                .volunteerOpportunityEvents
                                .toList(),
                        onEventSelected = {
                                volunteerEventId ->

                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerOpportunityDetailsRoute(
                                        volunteerEventId
                                    )
                            )
                        }
                    )
                }

                // Skill Path
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_SKILL_PATH_ROUTE
                ) {
                    VolunteerSkillPathScreen(
                        onSkillPathSelected = {
                                volunteerSkillPathId ->

                            volunteerNavigationController
                                .navigate(
                                    VolunteerOpportunityNavigationRoutes
                                        .createVolunteerSkillPathDetailsRoute(
                                            volunteerSkillPathId
                                        )
                                )
                        }
                    )
                }

                // Skill Path Details
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_SKILL_PATH_DETAILS_ROUTE,
                    arguments =
                        listOf(
                            navArgument(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_SKILL_PATH_ID_ARGUMENT
                            ) {
                                type = NavType.StringType
                            }
                        )
                ) { navigationBackStackEntry ->
                    val volunteerSkillPathId =
                        navigationBackStackEntry
                            .arguments
                            ?.getString(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_SKILL_PATH_ID_ARGUMENT
                            )
                            ?: return@composable

                    VolunteerSkillPathDetailsScreen(
                        skillPathId =
                            volunteerSkillPathId,
                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },
                        onVolunteerRoleSelected = {
                                volunteerEventId,
                                volunteerRoleId ->

                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerRoleDetailsRoute(
                                        volunteerEventId =
                                            volunteerEventId,
                                        volunteerRoleId =
                                            volunteerRoleId
                                    )
                            )
                        }
                    )
                }

                // Chats
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_CHAT_ROUTE
                ) {
                    VolunteerTemporaryModuleScreen(
                        moduleTitle = "Chats"
                    )
                }

                // Profile
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_PROFILE_ROUTE
                ) {
                    VolunteerTemporaryModuleScreen(
                        moduleTitle = "Profile"
                    )
                }
            }
        }

        if (
            shouldShowVolunteerBottomNavigationBar
        ) {
            Box(
                modifier = Modifier.align(
                    Alignment.BottomCenter
                )
            ) {
                VolunteerBottomNavigationBar(
                    currentVolunteerNavigationRoute =
                        currentVolunteerNavigationRoute,

                    onVolunteerNavigationItemSelected = {
                            selectedNavigationRoute ->

                        if (
                            selectedNavigationRoute !=
                            currentVolunteerNavigationRoute
                        ) {
                            volunteerNavigationController
                                .navigate(
                                    selectedNavigationRoute
                                ) {
                                    popUpTo(
                                        VolunteerOpportunityNavigationRoutes
                                            .VOLUNTEER_HOME_ROUTE
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
        }
    }
}

@Composable
private fun VolunteerOpportunityLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = VolunteerLinkPrimaryGreen
        )

        Text(
            text = "Loading opportunities from Supabase...",
            modifier = Modifier.padding(top = 14.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}

@Composable
private fun VolunteerOpportunityLoadErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to load volunteer data",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Text(
            text = errorMessage,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VolunteerLinkPrimaryGreen
            )
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun VolunteerTemporaryModuleScreen(
    moduleTitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                VolunteerLinkBackground
            )
            .statusBarsPadding()
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = moduleTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Text(
            text =
                "This module will be connected during group integration.",
            modifier =
                Modifier.padding(top = 8.dp),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
    }
}
