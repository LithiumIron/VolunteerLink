package com.example.volunteerlink.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.screens.VolunteerHomeScreen
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
                .VOLUNTEER_CHAT_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_SKILL_PATH_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_MAP_ROUTE,

            VolunteerOpportunityNavigationRoutes
                .VOLUNTEER_PROFILE_ROUTE
        )


    val shouldShowVolunteerBottomNavigationBar =
        currentVolunteerNavigationRoute in
                volunteerBottomNavigationRoutes


    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        contentWindowInsets =
            WindowInsets(
                left = 0,
                top = 0,
                right = 0,
                bottom = 0
            ),

        bottomBar = {

            if (
                shouldShowVolunteerBottomNavigationBar
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

    ) { volunteerNavigationInnerPadding ->


        NavHost(

            navController =
                volunteerNavigationController,

            startDestination =
                VolunteerOpportunityNavigationRoutes
                    .VOLUNTEER_HOME_ROUTE,

            modifier =
                Modifier.padding(
                    volunteerNavigationInnerPadding
                )
        ) {


            composable(
                route =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_HOME_ROUTE
            ) {

                VolunteerHomeScreen()
            }


            composable(
                route =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_CHAT_ROUTE
            ) {

                VolunteerTemporaryModuleScreen(
                    moduleTitle = "Chat"
                )
            }


            composable(
                route =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_SKILL_PATH_ROUTE
            ) {

                VolunteerTemporaryModuleScreen(
                    moduleTitle = "Skill Path"
                )
            }


            composable(
                route =
                    VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_MAP_ROUTE
            ) {

                VolunteerTemporaryModuleScreen(
                    moduleTitle = "Map"
                )
            }


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
}


@Composable
private fun VolunteerTemporaryModuleScreen(
    moduleTitle: String
) {

    Column(
        modifier =
            Modifier
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