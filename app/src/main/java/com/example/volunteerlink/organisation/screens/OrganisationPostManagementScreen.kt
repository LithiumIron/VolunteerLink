package com.example.volunteerlink.organisation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementRole
import com.example.volunteerlink.organisation.viewmodel.OrganisationPostManagementViewModel
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import java.util.Locale

/**
 * Mobile post-management detail opened from Manage > Volunteer Posts.
 *
 * V1 deliberately keeps one stable screen for Physical, Remote and Hybrid
 * posts. Hybrid simply shows both timelines in the summary instead of creating
 * separate Physical/Remote management dashboards.
 */
@Composable
fun OrganisationPostManagementScreen(
    postId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: OrganisationPostManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    val post = uiState.post

    when {
        uiState.isLoading -> ManageLoadingState()
        uiState.errorMessage != null -> ManageErrorState(
            message = uiState.errorMessage,
            onRetry = viewModel::refresh
        )
        post != null -> OrganisationPostManagementContent(
            post = post,
            onBack = onBack,
            onEdit = onEdit,
            onToggleShortlist = viewModel::toggleApplicantShortlist
        )
    }
}

@Composable
private fun OrganisationPostManagementContent(
    post: com.example.volunteerlink.organisation.manage.model.PostManagementPost,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleShortlist: (PostManagementPerson) -> Unit
) {
    var selectedTabName by rememberSaveable {
        mutableStateOf(PostManagementTab.OVERVIEW.name)
    }
    var selectedPeopleTabName by rememberSaveable {
        mutableStateOf(PostManagementPeopleTab.APPLICANTS.name)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRoleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPerson by remember { mutableStateOf<PostManagementPerson?>(null) }

    val selectedTab = runCatching {
        PostManagementTab.valueOf(selectedTabName)
    }.getOrDefault(PostManagementTab.OVERVIEW)

    val selectedPeopleTab = runCatching {
        PostManagementPeopleTab.valueOf(selectedPeopleTabName)
    }.getOrDefault(PostManagementPeopleTab.APPLICANTS)

    val peopleForTab = when (selectedPeopleTab) {
        PostManagementPeopleTab.APPLICANTS -> post.applicants
        PostManagementPeopleTab.VOLUNTEERS -> post.volunteers
    }

    // Only offer role filters that actually contain people in the selected tab.
    // This keeps Instant Join roles with no pending applications out of Applicants.
    val rolesForSelectedPeopleTab = post.roles.filter { role ->
        peopleForTab.any { person ->
            person.roleTemplateId == role.roleTemplateId
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase(Locale.US)
    val visiblePeople = peopleForTab.filter { person ->
        val matchesRole = selectedRoleId == null ||
                person.roleTemplateId == selectedRoleId
        val matchesQuery = normalizedQuery.isBlank() ||
                person.fullName.lowercase(Locale.US).contains(normalizedQuery) ||
                person.roleName.lowercase(Locale.US).contains(normalizedQuery) ||
                person.city.orEmpty().lowercase(Locale.US).contains(normalizedQuery)
        matchesRole && matchesQuery
    }

    // Keep the role filter, but still organise the visible people by role.
    // This remains readable when one post has several roles and many people.
    val visibleRoleGroups: List<Pair<PostManagementRole, List<PostManagementPerson>>> =
        post.roles.mapNotNull { role ->
            val peopleInRole = visiblePeople.filter {
                it.roleTemplateId == role.roleTemplateId
            }

            if (peopleInRole.isEmpty()) {
                null
            } else {
                val sortedPeople = if (
                    selectedPeopleTab == PostManagementPeopleTab.APPLICANTS
                ) {
                    peopleInRole.sortedWith(
                        compareByDescending<PostManagementPerson> { it.isShortlisted }
                            .thenBy { it.appliedAt.orEmpty() }
                            .thenBy { it.fullName }
                    )
                } else {
                    peopleInRole.sortedBy { it.fullName }
                }

                role to sortedPeople
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        PostManagementTopBar(
            onBack = onBack,
            onEdit = onEdit
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 14.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "post_summary") {
                PostManagementSummaryCard(post)
            }

            item(key = "post_tabs") {
                PostManagementMainTabs(
                    selected = selectedTab,
                    pendingApplicantCount = post.applicants.size,
                    onSelected = { selectedTabName = it.name }
                )
            }

            when (selectedTab) {
                PostManagementTab.OVERVIEW -> {
                    item(key = "overview") {
                        PostManagementOverview(post)
                    }
                }

                PostManagementTab.PEOPLE -> {
                    item(key = "people_controls") {
                        PostManagementPeopleControls(
                            selectedTab = selectedPeopleTab,
                            applicantCount = post.applicants.size,
                            volunteerCount = post.volunteers.size,
                            query = searchQuery,
                            selectedRoleId = selectedRoleId,
                            roles = rolesForSelectedPeopleTab,
                            onTabSelected = {
                                selectedPeopleTabName = it.name
                                selectedRoleId = null
                            },
                            onQueryChange = { searchQuery = it },
                            onRoleSelected = { selectedRoleId = it }
                        )
                    }

                    if (visiblePeople.isEmpty()) {
                        item(key = "people_empty") {
                            PostManagementPeopleEmptyState(
                                selectedTab = selectedPeopleTab,
                                hasFilters = searchQuery.isNotBlank() || selectedRoleId != null
                            )
                        }
                    } else {
                        visibleRoleGroups.forEach { (role, peopleInRole) ->
                            item(
                                key = "people_role_${role.roleTemplateId}_${selectedPeopleTab.name}"
                            ) {
                                PostManagementPeopleRoleHeader(
                                    role = role,
                                    selectedTab = selectedPeopleTab,
                                    applicantCount = post.applicants.count {
                                        it.roleTemplateId == role.roleTemplateId
                                    },
                                    volunteerCount = post.volunteers.count {
                                        it.roleTemplateId == role.roleTemplateId
                                    }
                                )
                            }

                            items(
                                count = peopleInRole.size,
                                key = { index ->
                                    val person = peopleInRole[index]
                                    "${person.userId}_${person.roleTemplateId}_${selectedPeopleTab.name}"
                                }
                            ) { index ->
                                val person = peopleInRole[index]
                                PostManagementPersonCard(
                                    person = person,
                                    isApplicant = selectedPeopleTab == PostManagementPeopleTab.APPLICANTS,
                                    isApplicationOpen = role.isApplicationOpen,
                                    onViewProfile = { selectedPerson = it },
                                    onToggleShortlist = onToggleShortlist
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPerson?.let { person ->
        PostManagementProfilePreviewDialog(
            person = person,
            onDismiss = { selectedPerson = null }
        )
    }

}
