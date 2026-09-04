package com.example.volunteerlink.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.data.VolunteerProfileRepository
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.screens.EditVolunteerProfileScreen
import com.example.volunteerlink.screens.MapScreen
import com.example.volunteerlink.screens.VolunteerAllCertificatesScreen
import com.example.volunteerlink.screens.VolunteerApplicationDetailsScreen
import com.example.volunteerlink.screens.VolunteerApplicationScreen
import com.example.volunteerlink.screens.VolunteerCertificateScreen
import com.example.volunteerlink.screens.VolunteerHomeScreen
import com.example.volunteerlink.screens.VolunteerMyApplicationsScreen
import com.example.volunteerlink.screens.VolunteerOpportunityDetailsScreen
import com.example.volunteerlink.screens.VolunteerNotificationsScreen
import com.example.volunteerlink.screens.VolunteerNotificationViewModel
import com.example.volunteerlink.screens.VolunteerOpportunityViewModel
import com.example.volunteerlink.screens.VolunteerProfileScreen
import com.example.volunteerlink.screens.VolunteerRoleDetailsScreen
import com.example.volunteerlink.screens.VolunteerSearchScreen
import com.example.volunteerlink.screens.VolunteerSettingsScreen
import com.example.volunteerlink.screens.VolunteerSkillPathDetailsScreen
import com.example.volunteerlink.screens.VolunteerSkillPathScreen
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import androidx.compose.runtime.LaunchedEffect
import com.example.volunteerlink.chat.data.ChatData
import com.example.volunteerlink.chat.data.Role
import com.example.volunteerlink.chat.repository.SupabaseChatRepository
import com.example.volunteerlink.screens.chat.VolunteerChatListScreen
import com.example.volunteerlink.screens.chat.VolunteerChatRoomScreen
import com.example.volunteerlink.screens.chat.VolunteerGroupInfoScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun VolunteerOpportunityNavigationHost(
    onLoggedOut: () -> Unit
) {
    val volunteerOpportunityViewModel:
            VolunteerOpportunityViewModel = viewModel()

    val opportunityUiState by
    volunteerOpportunityViewModel.uiState
        .collectAsStateWithLifecycle()

    val volunteerNotificationViewModel:
            VolunteerNotificationViewModel = viewModel()

    val notificationUiState by
    volunteerNotificationViewModel.uiState
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

    // Pressing the system back button while at Home would otherwise pop
    // straight out of this nav graph to the user-type selection screen,
    // silently signing the volunteer out with no confirmation. Intercept
    // it and ask first instead — same confirm-before-logout behaviour as
    // the "Log Out" row in Settings.
    var showBackLogoutConfirmation by remember { mutableStateOf(false) }
    var isLoggingOutFromBack by remember { mutableStateOf(false) }
    val backLogoutScope = rememberCoroutineScope()

    BackHandler(
        enabled =
            currentVolunteerNavigationRoute ==
                    VolunteerOpportunityNavigationRoutes.VOLUNTEER_HOME_ROUTE
    ) {
        showBackLogoutConfirmation = true
    }

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

                        onVolunteerRoleSelected = {
                                volunteerEventId,
                                volunteerRoleId ->

                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerOpportunityDetailsRoute(
                                        volunteerEventId = volunteerEventId,
                                        recommendedRoleId = volunteerRoleId,
                                        source = "for_you"
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
                        },

                        onVolunteerNotificationsSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_NOTIFICATIONS_ROUTE
                            )
                        },

                        onVolunteerFavouritesSelected = {
                            volunteerNavigationController.navigate(VolunteerOpportunityNavigationRoutes.VOLUNTEER_FAVOURITES_ROUTE)
                        },
                        unreadNotificationCount =
                            notificationUiState.unreadCount,

                        isShowingCachedData =
                            opportunityUiState.isShowingCachedData,
                        syncWarning = opportunityUiState.syncWarning,

                        lastSyncedAtEpochMillis =
                            opportunityUiState.lastSyncedAtEpochMillis,

                        onSyncSelected =
                            volunteerOpportunityViewModel::refresh,
                        onVolunteerProfileSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes.VOLUNTEER_PROFILE_ROUTE)
                        }
                    )
                }

                composable(
                    route = VolunteerOpportunityNavigationRoutes
                        .VOLUNTEER_NOTIFICATIONS_ROUTE
                ) {
                    VolunteerNotificationsScreen(
                        onBackSelected = {
                            volunteerNavigationController.popBackStack()
                        },
                        onApplicationsSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_MY_APPLICATIONS_ROUTE
                            )
                        },

                        onOpportunitySelected = { postDatabaseId ->
                            VolunteerOpportunitySessionStore
                                .volunteerOpportunityEvents
                                .firstOrNull {
                                    it.eventDatabaseId == postDatabaseId
                                }
                                ?.let { event ->
                                    volunteerNavigationController.navigate(
                                        VolunteerOpportunityNavigationRoutes
                                            .createVolunteerOpportunityDetailsRoute(
                                                event.eventId
                                            )
                                    )
                                }
                        },

                        onSkillPathSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes.VOLUNTEER_SKILL_PATH_ROUTE
                            ) {
                                popUpTo(VolunteerOpportunityNavigationRoutes.VOLUNTEER_HOME_ROUTE) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },

                        notificationViewModel =
                            volunteerNotificationViewModel
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

                composable(VolunteerOpportunityNavigationRoutes.VOLUNTEER_FAVOURITES_ROUTE) {
                    com.example.volunteerlink.screens.VolunteerFavouritesScreen(
                        onBack = { volunteerNavigationController.popBackStack() },
                        onEvent = { id ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes.createVolunteerOpportunityDetailsRoute(id)
                            )
                        },
                        opportunityViewModel = volunteerOpportunityViewModel
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
                            },
                            navArgument("recommendedRoleId") { type = NavType.IntType; defaultValue = -1 },
                            navArgument("source") { type = NavType.StringType; defaultValue = "" }
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
                        recommendedRoleId = navigationBackStackEntry.arguments?.getInt("recommendedRoleId") ?: -1,
                        recommendationSource = navigationBackStackEntry.arguments?.getString("source").orEmpty(),
                        volunteerEventId =
                            volunteerEventId,
                        opportunityViewModel =
                            volunteerOpportunityViewModel,

                        onBackSelected = {
                            volunteerNavigationController
                                .popBackStack()
                        },

                        onLocationSelected = { selectedEventId ->
                            VolunteerOpportunitySessionStore.mapFocusEventId =
                                selectedEventId
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_MAP_ROUTE
                            )
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
                        initialEventId =
                            VolunteerOpportunitySessionStore
                                .mapFocusEventId,
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
                                    .createVolunteerOpportunityDetailsRoute(
                                        volunteerEventId =
                                            volunteerEventId,
                                        recommendedRoleId = volunteerRoleId,
                                        source = "skill_path"
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
                    LaunchedEffect(Unit) {
                        ChatData.currentRole.value = Role.APPLICANT

                        // Removes VolunteerApp's temporary Alex Tan / Organisation A chats
                        // before the real VolunteerLink chats arrive.
                        ChatData.replaceChats(emptyList())

                        runCatching {
                            SupabaseChatRepository.loadForSignedInUser(
                                viewerRole = Role.APPLICANT
                            )
                        }.onSuccess { loaded ->
                            ChatData.updateSignedInProfile(
                                role = Role.APPLICANT,
                                profile = loaded.profile
                            )
                            ChatData.replaceChats(loaded.chats)
                        }.onFailure { error ->
                            error.printStackTrace()
                        }
                    }

                    VolunteerChatListScreen(
                        role = Role.APPLICANT,
                        onOpenChat = { chatId ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerChatRoomRoute(chatId)
                            )
                        }
                    )
                }

                composable(
                    route = VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ROOM_ROUTE,
                    arguments = listOf(
                        navArgument(
                            VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ID_ARGUMENT
                        ) {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val chatId = entry.arguments
                        ?.getString(
                            VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ID_ARGUMENT
                        )
                        .orEmpty()

                    LaunchedEffect(chatId) {
                        ChatData.currentRole.value = Role.APPLICANT

                        runCatching {
                            SupabaseChatRepository.loadMessagesForChat(chatId)
                        }.onSuccess { messages ->
                            ChatData.replaceMessages(
                                chatId = chatId,
                                messages = messages
                            )
                        }.onFailure { error ->
                            error.printStackTrace()
                        }
                    }

                    VolunteerChatRoomScreen(
                        chatId = chatId,
                        onBack = {
                            volunteerNavigationController.popBackStack()
                        },
                        onOpenGroupInfo = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerGroupInfoRoute(chatId)
                            )
                        }
                    )
                }

                composable(
                    route = VolunteerOpportunityNavigationRoutes.VOLUNTEER_GROUP_INFO_ROUTE,
                    arguments = listOf(
                        navArgument(
                            VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ID_ARGUMENT
                        ) {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val chatId = entry.arguments
                        ?.getString(
                            VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ID_ARGUMENT
                        )
                        .orEmpty()

                    LaunchedEffect(Unit) {
                        ChatData.currentRole.value = Role.APPLICANT
                    }

                    VolunteerGroupInfoScreen(
                        chatId = chatId,
                        onBack = {
                            volunteerNavigationController.popBackStack()
                        },
                        onOpenChat = { openedChatId ->
                            volunteerNavigationController.popBackStack(
                                VolunteerOpportunityNavigationRoutes.VOLUNTEER_CHAT_ROUTE,
                                inclusive = false
                            )

                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerChatRoomRoute(openedChatId)
                            )
                        }
                    )
                }

                // Profile
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_PROFILE_ROUTE
                ) {
                    val profileScope = rememberCoroutineScope()

                    VolunteerProfileScreen(
                        onVolunteerSettingSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_SETTINGS_ROUTE
                            )
                        },
                        onVolunteerNotificationsSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_NOTIFICATIONS_ROUTE
                            )
                        },
                        onEditProfileSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EDIT_PROFILE_ROUTE
                            )
                        },
                        onCompletedEventSelected = { applicationId ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerApplicationDetailsRoute(applicationId)
                            )
                        },
                        onCompletedEventsSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_MY_APPLICATIONS_ROUTE
                            )
                        },
                        onCertificateSelected = { applicationId ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerCertificateRoute(applicationId)
                            )
                        },
                        onCertificatesSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_CERTIFICATES_ROUTE  // new — see below
                            )
                        },
                        onSkillPathItemSelected = { skillPathId ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerSkillPathDetailsRoute(skillPathId)
                            )
                        },
                        onSkillPathSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes.VOLUNTEER_SKILL_PATH_ROUTE
                            ) {
                                popUpTo(VolunteerOpportunityNavigationRoutes.VOLUNTEER_HOME_ROUTE) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },

                        onRefresh = {
                            profileScope.launch {
                                VolunteerOpportunitySessionStore.updateProfileLoading(true)
                                val loadedProfile = VolunteerProfileRepository.loadProfile()
                                if (loadedProfile != null) {
                                    VolunteerOpportunitySessionStore.setProfileData(loadedProfile)
                                } else {
                                    VolunteerOpportunitySessionStore.updateProfileLoading(false)
                                }
                            }
                        }
                    )


                }
                // Edit Profile
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_EDIT_PROFILE_ROUTE
                ) {
                    EditVolunteerProfileScreen(
                        onBack = {
                            volunteerNavigationController.popBackStack()
                        },
                        onSaved = {
                            // Invalidate the cached profile so navigating
                            // back to VolunteerProfileScreen refetches the
                            // updated name/phone/bio/availability instead
                            // of showing what was cached before the edit.
                            VolunteerOpportunitySessionStore.clearProfileData()
                        }
                    )
                }

                // Settings
                composable(
                    route =
                        VolunteerOpportunityNavigationRoutes
                            .VOLUNTEER_SETTINGS_ROUTE
                ) {
                    VolunteerSettingsScreen(
                        onBackSelected = {
                            volunteerNavigationController.popBackStack()
                        },

                        onEditProfileSelected = {
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .VOLUNTEER_EDIT_PROFILE_ROUTE
                            )
                        },

                        onLoggedOut = onLoggedOut
                    )
                }

                composable(
                    route = VolunteerOpportunityNavigationRoutes.VOLUNTEER_CERTIFICATES_ROUTE
                ) {
                    VolunteerAllCertificatesScreen(
                        onBackSelected = { volunteerNavigationController.popBackStack() },
                        onCertificateSelected = { applicationId ->
                            volunteerNavigationController.navigate(
                                VolunteerOpportunityNavigationRoutes
                                    .createVolunteerCertificateRoute(applicationId)
                            )
                        }
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
                            selectedNavigationRoute ==
                            VolunteerOpportunityNavigationRoutes
                                .VOLUNTEER_HOME_ROUTE
                        ) {
                            val returnedToHome =
                                volunteerNavigationController
                                    .popBackStack(
                                        route =
                                            VolunteerOpportunityNavigationRoutes
                                                .VOLUNTEER_HOME_ROUTE,
                                        inclusive = false
                                    )

                            if (!returnedToHome) {
                                volunteerNavigationController
                                    .navigate(
                                        VolunteerOpportunityNavigationRoutes
                                            .VOLUNTEER_HOME_ROUTE
                                    ) {
                                        launchSingleTop = true
                                    }
                            }
                        } else if (
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

        if (showBackLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    // Tapping outside cancels — same as choosing "Cancel" —
                    // never silently logs out. Ignored mid-request so the
                    // dialog can't be dismissed while signOut() is running.
                    if (!isLoggingOutFromBack) showBackLogoutConfirmation = false
                },
                title = { Text("Log out?") },
                text = {
                    Text(
                        "You'll need to sign in again to access your " +
                                "volunteer profile."
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !isLoggingOutFromBack,
                        onClick = {
                            backLogoutScope.launch {
                                isLoggingOutFromBack = true
                                try {
                                    supabase.auth.signOut()
                                    VolunteerOpportunitySessionStore.clearProfileData()
                                    showBackLogoutConfirmation = false
                                    onLoggedOut()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isLoggingOutFromBack = false
                                }
                            }
                        }
                    ) { Text("Log out", color = Color(0xFFC62828)) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isLoggingOutFromBack,
                        onClick = {
                            // User said no — stay signed in, just close the
                            // dialog. No navigation, no sign-out.
                            showBackLogoutConfirmation = false
                        }
                    ) {
                        Text("Cancel", color = VolunteerLinkPrimaryGreen)
                    }
                }
            )
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
