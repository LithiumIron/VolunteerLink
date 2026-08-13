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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.volunteerlink.screens.MapScreen
import com.example.volunteerlink.screens.VolunteerApplicationScreen
import com.example.volunteerlink.screens.VolunteerHomeScreen
import com.example.volunteerlink.screens.VolunteerOpportunityDetailsScreen
import com.example.volunteerlink.screens.VolunteerRoleDetailsScreen
import com.example.volunteerlink.screens.VolunteerSearchScreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary

@Composable
fun VolunteerOpportunityNavigationHost() {
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
                        }
                    )
                }

                // Map
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_MAP_ROUTE
                ) {
                    MapScreen()
                }

                // Skill Path
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_SKILL_PATH_ROUTE
                ) {
                    VolunteerTemporaryModuleScreen(
                        moduleTitle = "Skill Path"
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