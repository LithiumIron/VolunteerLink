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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.volunteerlink.navigation.AppBottomNavigationBar
import com.example.volunteerlink.navigation.AppRoutes
import com.example.volunteerlink.organisation.auth.OrganisationSessionStore
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.organisation.repository.OrganisationProfileRepository
import com.example.volunteerlink.organisation.screens.OrganisationChatsScreen
import com.example.volunteerlink.organisation.screens.OrganisationApplicantReviewScreen
import com.example.volunteerlink.organisation.screens.OrganisationCreateScreen
import com.example.volunteerlink.organisation.screens.EditOrganisationProfileScreen
import com.example.volunteerlink.organisation.screens.OrganisationHomeScreen
import com.example.volunteerlink.organisation.screens.OrganisationManageEmptyModuleScreen
import com.example.volunteerlink.organisation.screens.OrganisationManageScreen
import com.example.volunteerlink.organisation.screens.OrganisationPostManagementScreen
import com.example.volunteerlink.organisation.screens.OrganisationVolunteerPostsScreen
import com.example.volunteerlink.organisation.screens.OrganisationProfileScreen
import com.example.volunteerlink.organisation.screens.OrganisationPromotionScreen
import com.example.volunteerlink.organisation.home.model.HomeAttentionType
import com.example.volunteerlink.organisation.screens.OrganisationSettingScreen
import kotlinx.coroutines.launch

private const val RETURN_TO_PEOPLE_AFTER_APPLICANT_REVIEW =
    "returnToPeopleAfterApplicantReview"
private const val OPEN_PEOPLE_FROM_HOME = "openPeopleFromHome"
private const val OPEN_REVIEW_FROM_HOME = "openReviewFromHome"

/**
 * Navigation host for the Organisation side only.
 *
 * Screen files do not own a NavController. Keeping navigation here makes each
 * screen easier to read and avoids mixing UI code with navigation logic.
 */
@Composable
fun OrganisationNavigationHost(
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var reviewExitProtected by remember { mutableStateOf(false) }
    var discardReviewSession by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingBottomRoute by remember { mutableStateOf<String?>(null) }

    fun navigateBottom(route: String) {
        navController.navigate(route) {
            popUpTo(OrganisationNavigationRoutes.HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Keep the Manage bottom-navigation item selected while a Manage sub-page
    // is open. The sub-pages are part of the same module, not new bottom tabs.
    val bottomBarRoute = when (currentRoute) {
        OrganisationNavigationRoutes.MANAGE_POSTS,
        OrganisationNavigationRoutes.MANAGE_POST_DETAIL,
        OrganisationNavigationRoutes.MANAGE_POST_EDIT,
        OrganisationNavigationRoutes.MANAGE_APPLICANT_REVIEW,
        OrganisationNavigationRoutes.MANAGE_IMPACT_WEAVE,
        OrganisationNavigationRoutes.MANAGE_PROMOTIONS ->
            OrganisationNavigationRoutes.MANAGE

        else -> currentRoute
    }

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
            if (
                currentRoute != OrganisationNavigationRoutes.MANAGE_APPLICANT_REVIEW &&
                currentRoute != OrganisationNavigationRoutes.MANAGE_PROMOTIONS &&
                currentRoute != OrganisationNavigationRoutes.EDIT_PROFILE &&
                currentRoute != OrganisationNavigationRoutes.SETTINGS
            ) {
                AppBottomNavigationBar(
                    items = organisationBottomNavigationItems,
                    currentRoute = bottomBarRoute,
                    onItemClick = { item ->
                        if (item.route != currentRoute) {
                            if (
                                currentRoute == OrganisationNavigationRoutes.MANAGE_POST_DETAIL &&
                                reviewExitProtected
                            ) {
                                pendingBottomRoute = item.route
                            } else {
                                navigateBottom(item.route)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OrganisationNavigationRoutes.HOME,
            route = "organisation_root_navigation_graph",
            modifier = Modifier.padding(innerPadding)

        ) {
            composable(OrganisationNavigationRoutes.HOME) {
                OrganisationHomeScreen(
                    onViewAllPosts = {
                        navController.navigate(OrganisationNavigationRoutes.MANAGE_POSTS) {
                            popUpTo(OrganisationNavigationRoutes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPostClick = { postId ->
                        navController.navigate(
                            OrganisationNavigationRoutes.managePostDetail(postId)
                        )
                    },
                    onAttentionClick = { item ->
                        when (item.type) {
                            HomeAttentionType.DRAFT_START_TOO_SOON,
                            HomeAttentionType.DRAFT_START_DATE_PASSED -> {
                                navController.navigate(
                                    OrganisationNavigationRoutes.managePostEdit(item.postId)
                                )
                            }

                            HomeAttentionType.APPLICATIONS_TO_REVIEW -> {
                                navController.navigate(
                                    OrganisationNavigationRoutes.managePostDetail(item.postId)
                                )
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(OPEN_PEOPLE_FROM_HOME, true)
                            }

                            HomeAttentionType.POST_COMPLETION_REVIEW -> {
                                navController.navigate(
                                    OrganisationNavigationRoutes.managePostDetail(item.postId)
                                )
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(OPEN_REVIEW_FROM_HOME, true)
                            }
                        }
                    }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE) {
                OrganisationManageScreen(
                    onVolunteerPostsClick = {
                        navController.navigate(OrganisationNavigationRoutes.MANAGE_POSTS)
                    },
                    onImpactWeaveClick = {
                        navController.navigate(OrganisationNavigationRoutes.MANAGE_IMPACT_WEAVE)
                    },
                    onPromotionsClick = {
                        navController.navigate(OrganisationNavigationRoutes.MANAGE_PROMOTIONS)
                    }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_POSTS) {
                OrganisationVolunteerPostsScreen(
                    onBack = { navController.popBackStack() },
                    onPostClick = { postId ->
                        navController.navigate(
                            OrganisationNavigationRoutes.managePostDetail(postId)
                        )
                    }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_POST_DETAIL) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                val returnToPeopleAfterApplicantReview =
                    backStackEntry.savedStateHandle.get<Boolean>(
                        RETURN_TO_PEOPLE_AFTER_APPLICANT_REVIEW
                    ) == true
                val openPeopleFromHome =
                    backStackEntry.savedStateHandle.get<Boolean>(OPEN_PEOPLE_FROM_HOME) == true
                val openReviewFromHome =
                    backStackEntry.savedStateHandle.get<Boolean>(OPEN_REVIEW_FROM_HOME) == true

                OrganisationPostManagementScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        navController.navigate(
                            OrganisationNavigationRoutes.managePostEdit(postId)
                        )
                    },
                    onViewApplication = { roleTemplateId, userId ->
                        navController.navigate(
                            OrganisationNavigationRoutes.manageApplicantReview(
                                postId = postId,
                                roleTemplateId = roleTemplateId,
                                userId = userId
                            )
                        )
                    },
                    returnToPeopleAfterApplicantReview =
                        returnToPeopleAfterApplicantReview,
                    openPeopleFromHome = openPeopleFromHome,
                    openReviewFromHome = openReviewFromHome,
                    onReturnToPeopleHandled = {
                        backStackEntry.savedStateHandle.remove<Boolean>(
                            RETURN_TO_PEOPLE_AFTER_APPLICANT_REVIEW
                        )
                    },
                    onHomeTargetHandled = {
                        backStackEntry.savedStateHandle.remove<Boolean>(OPEN_PEOPLE_FROM_HOME)
                        backStackEntry.savedStateHandle.remove<Boolean>(OPEN_REVIEW_FROM_HOME)
                    },
                    onExitProtectionChanged = { protected, discard ->
                        reviewExitProtected = protected
                        discardReviewSession = discard
                    }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_POST_EDIT) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                OrganisationCreateScreen(
                    editPostId = postId,
                    onExitCreate = { navController.popBackStack() }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_APPLICANT_REVIEW) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                val roleTemplateId = backStackEntry.arguments
                    ?.getString("roleTemplateId")
                    .orEmpty()
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()

                OrganisationApplicantReviewScreen(
                    postId = postId,
                    roleTemplateId = roleTemplateId,
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onDecisionSaved = {
                        // Tell the previous Manage Post destination which main tab
                        // must be shown after its applicant data refreshes.
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(RETURN_TO_PEOPLE_AFTER_APPLICANT_REVIEW, true)
                        navController.popBackStack()
                    }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_IMPACT_WEAVE) {
                OrganisationManageEmptyModuleScreen(
                    title = "Impact Weave",
                    message = "No Impact Weave projects to manage yet. This section will use its own collaboration lifecycle when we implement it.",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(OrganisationNavigationRoutes.MANAGE_PROMOTIONS) {
                OrganisationPromotionScreen(
                    onBack = { navController.popBackStack() }
                )
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
                val profileScope = rememberCoroutineScope()

                OrganisationProfileScreen(
                    onSettingsSelected = {
                        navController.navigate(OrganisationNavigationRoutes.SETTINGS)
                    },
                    onEditProfileSelected = {
                        navController.navigate(OrganisationNavigationRoutes.EDIT_PROFILE)
                    },
                    onRecentPostsSelected = {
                        navController.navigate(OrganisationNavigationRoutes.MANAGE_POSTS) {
                            popUpTo(OrganisationNavigationRoutes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPostSelected = { postId ->
                        navController.navigate(
                            OrganisationNavigationRoutes.managePostDetail(postId)
                        )
                    },
                    onRefresh = {
                        profileScope.launch {
                            OrganisationSessionStore.updateProfileLoading(true)
                            val loadedProfile = OrganisationProfileRepository.loadProfile()
                            if (loadedProfile != null) {
                                OrganisationSessionStore.setProfileData(loadedProfile)
                            } else {
                                OrganisationSessionStore.updateProfileLoading(false)
                            }
                        }
                    }
                )
            }

            composable(OrganisationNavigationRoutes.EDIT_PROFILE) {
                EditOrganisationProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        // Invalidate the cached profile so navigating back
                        // to OrganisationProfileScreen refetches the
                        // updated fields instead of showing what was
                        // cached before the edit.
                        OrganisationSessionStore.clearProfileData()
                    }
                )
            }

            composable(OrganisationNavigationRoutes.SETTINGS) {
                OrganisationSettingScreen(
                    onBackSelected = { navController.popBackStack() },
                    onEditProfileSelected = {
                        navController.navigate(OrganisationNavigationRoutes.EDIT_PROFILE)
                    },
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }

    val requestedRoute = pendingBottomRoute
    if (requestedRoute != null) {
        AlertDialog(
            onDismissRequest = { pendingBottomRoute = null },
            title = {
                Text(
                    text = "Leave event review?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            },
            text = {
                Text(
                    text = "This review is not finalized yet. Saved attendance corrections will remain, but temporary completion and feedback choices will be discarded if you leave.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = VolunteerLinkTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        discardReviewSession?.invoke()
                        reviewExitProtected = false
                        discardReviewSession = null
                        pendingBottomRoute = null
                        navigateBottom(requestedRoute)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerLinkPrimaryGreen)
                ) { Text("Discard & Leave", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingBottomRoute = null }) { Text("Stay") }
            },
            containerColor = VolunteerLinkSurface
        )
    }
}

/** Returns the Activity even when Compose is using a ContextWrapper. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}