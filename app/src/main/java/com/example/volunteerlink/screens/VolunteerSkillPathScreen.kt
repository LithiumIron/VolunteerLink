
package com.example.volunteerlink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.data.VolunteerOpportunitySessionStore
import com.example.volunteerlink.model.VolunteerApplicationStatus
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkScreenHorizontalPadding
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import com.example.volunteerlink.ui.theme.VolunteerLinkWarning

private data class VolunteerSkillPathBadge(
    val title: String,
    val description: String,
    val isEarned: Boolean,
    val progressText: String,
    val evidenceLabel: String
)

@Composable
fun VolunteerSkillPathScreen(
    onSkillPathSelected: (
        skillPathId: String
    ) -> Unit,
    skillPathViewModel:
        VolunteerSkillPathViewModel = viewModel()
) {
    val skillPathUiState by
        skillPathViewModel.uiState
            .collectAsStateWithLifecycle()

    var selectedModeFilter by
        rememberSaveable {
            mutableStateOf("All")
        }

    val modeFilters =
        listOf(
            "All",
            "Physical",
            "Remote"
        )

    val visibleSkillPaths =
        skillPathUiState.skillPaths
            .filter { volunteerSkillPath ->
                when (selectedModeFilter) {
                    "Physical" ->
                        volunteerSkillPath.pathMode ==
                                "PHYSICAL"

                    "Remote" ->
                        volunteerSkillPath.pathMode ==
                                "REMOTE"

                    else -> true
                }
            }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerSkillPathHeader(
            onRefreshSelected =
                skillPathViewModel::retry
        )

        when {
            skillPathUiState.isLoading ->
                VolunteerSkillPathLoadingContent()

            skillPathUiState.errorMessage != null ->
                VolunteerSkillPathErrorContent(
                    errorMessage =
                        skillPathUiState.errorMessage
                            ?: "Unable to load Skill Path data.",
                    onRetrySelected =
                        skillPathViewModel::retry
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 110.dp
                    )
                ) {
                    item(
                        key = "skill_path_summary"
                    ) {
                        VolunteerSkillPathSummaryCard(
                            skillPaths =
                                skillPathUiState.skillPaths
                        )
                    }

                    item(
                        key = "skill_path_badges"
                    ) {
                        VolunteerSkillPathBadgesSection(
                            skillPaths =
                                skillPathUiState.skillPaths
                        )
                    }

                    item(
                        key = "skill_path_guide"
                    ) {
                        VolunteerSkillPathGuideCard()
                    }

                    item(
                        key = "skill_path_filters"
                    ) {
                        Column {
                            Text(
                                text = "Explore Skill Paths",
                                modifier = Modifier.padding(
                                    start =
                                        VolunteerLinkScreenHorizontalPadding,
                                    end =
                                        VolunteerLinkScreenHorizontalPadding,
                                    top = 20.dp,
                                    bottom = 8.dp
                                ),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkTextPrimary
                            )

                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal =
                                        VolunteerLinkScreenHorizontalPadding
                                ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = modeFilters,
                                    key = { modeFilter ->
                                        modeFilter
                                    }
                                ) { modeFilter ->
                                    FilterChip(
                                        selected =
                                            selectedModeFilter ==
                                                    modeFilter,
                                        onClick = {
                                            selectedModeFilter =
                                                modeFilter
                                        },
                                        label = {
                                            Text(
                                                text = modeFilter,
                                                fontSize = 11.sp
                                            )
                                        },
                                        shape =
                                            RoundedCornerShape(18.dp),
                                        colors =
                                            FilterChipDefaults
                                                .filterChipColors(
                                                    containerColor =
                                                        VolunteerLinkSurface,
                                                    labelColor =
                                                        VolunteerLinkTextSecondary,
                                                    selectedContainerColor =
                                                        VolunteerLinkPrimaryGreen,
                                                    selectedLabelColor =
                                                        Color.White
                                                )
                                    )
                                }
                            }
                        }
                    }

                    if (visibleSkillPaths.isEmpty()) {
                        item(
                            key = "skill_path_empty"
                        ) {
                            Text(
                                text =
                                    "No Skill Paths match this filter.",
                                modifier = Modifier.padding(
                                    VolunteerLinkScreenHorizontalPadding
                                ),
                                fontSize = 12.sp,
                                color =
                                    VolunteerLinkTextSecondary
                            )
                        }
                    } else {
                        items(
                            items = visibleSkillPaths,
                            key = { volunteerSkillPath ->
                                volunteerSkillPath.skillPathId
                            }
                        ) { volunteerSkillPath ->
                            VolunteerSkillPathCard(
                                volunteerSkillPath =
                                    volunteerSkillPath,
                                onSelected = {
                                    onSkillPathSelected(
                                        volunteerSkillPath
                                            .skillPathId
                                    )
                                }
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun VolunteerSkillPathDetailsScreen(
    skillPathId: String,
    onBackSelected: () -> Unit,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit,
    skillPathViewModel:
        VolunteerSkillPathViewModel = viewModel()
) {
    val skillPathUiState by
        skillPathViewModel.uiState
            .collectAsStateWithLifecycle()

    val volunteerSkillPath =
        skillPathUiState.skillPaths
            .firstOrNull { skillPath ->
                skillPath.skillPathId ==
                        skillPathId
            }

    var selectedDetailTab by
        rememberSaveable(skillPathId) {
            mutableStateOf("Progress")
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
    ) {
        VolunteerSkillPathDetailsTopBar(
            onBackSelected = onBackSelected
        )

        when {
            skillPathUiState.isLoading ->
                VolunteerSkillPathLoadingContent()

            skillPathUiState.errorMessage != null ->
                VolunteerSkillPathErrorContent(
                    errorMessage =
                        skillPathUiState.errorMessage
                            ?: "Unable to load Skill Path data.",
                    onRetrySelected =
                        skillPathViewModel::retry
                )

            volunteerSkillPath == null ->
                VolunteerSkillPathErrorContent(
                    errorMessage =
                        "This Skill Path could not be found.",
                    onRetrySelected = onBackSelected,
                    retryButtonText = "Return"
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 28.dp
                    )
                ) {
                    item(
                        key = "skill_path_detail_header"
                    ) {
                        VolunteerSkillPathDetailHeader(
                            volunteerSkillPath =
                                volunteerSkillPath
                        )
                    }

                    item(
                        key = "skill_path_detail_progress"
                    ) {
                        VolunteerSkillPathProgressSection(
                            volunteerSkillPath =
                                volunteerSkillPath
                        )
                    }

                    item(
                        key = "skill_path_detail_tabs"
                    ) {
                        VolunteerSkillPathDetailTabs(
                            selectedTab = selectedDetailTab,
                            onTabSelected = {
                                    selectedTab ->
                                selectedDetailTab = selectedTab
                            }
                        )
                    }

                    when (selectedDetailTab) {
                        "Skills" -> {
                            item(
                                key = "skill_path_detail_skills"
                            ) {
                                VolunteerSkillPathSkillsSection(
                                    volunteerSkillPath =
                                        volunteerSkillPath
                                )
                            }
                        }

                        "Roles" -> {
                            item(
                                key = "skill_path_detail_roles"
                            ) {
                                VolunteerSkillPathRolesSection(
                                    volunteerSkillPath =
                                        volunteerSkillPath,
                                    onVolunteerRoleSelected =
                                        onVolunteerRoleSelected
                                )
                            }
                        }

                        else -> {
                            item(
                                key = "skill_path_detail_evidence"
                            ) {
                                VolunteerSkillPathEvidenceSection(
                                    volunteerSkillPath =
                                        volunteerSkillPath
                                )
                            }

                        }
                    }
                }
        }
    }
}

@Composable
private fun VolunteerSkillPathDetailTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        "Progress",
        "Skills",
        "Roles"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkSurface)
            .padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 10.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onTabSelected(tab)
                    },
                shape = RoundedCornerShape(9.dp),
                color =
                    if (selectedTab == tab) {
                        VolunteerLinkPrimaryGreen
                    } else {
                        VolunteerLinkSoftGreenSurface
                    }
            ) {
                Text(
                    text = tab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (selectedTab == tab) {
                            Color.White
                        } else {
                            VolunteerLinkPrimaryGreen
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathHeader(
    onRefreshSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 13.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "SKILL PATH",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Verified growth from completed volunteer roles.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.84f)
            )
        }

        Text(
            text = "SYNC",
            modifier = Modifier
                .clickable(onClick = onRefreshSelected)
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VolunteerSkillPathDetailsTopBar(
    onBackSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkPrimaryGreen)
            .statusBarsPadding()
            .height(56.dp)
            .padding(
                start = 4.dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackSelected
        ) {
            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = "Skill Path Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VolunteerSkillPathSummaryCard(
    skillPaths: List<VolunteerSkillPath>
) {
    // Overall impact must use the same verified completion records as Home.
    // Per-path values below the summary remain path-specific evidence.
    val completedApplications =
        VolunteerOpportunitySessionStore.volunteerApplications
            .filter {
                it.applicationStatus ==
                    VolunteerApplicationStatus.COMPLETED
            }
    val verifiedRoles =
        completedApplications.size
    val verifiedMinutes =
        completedApplications.sumOf { application ->
            application.applicationVerifiedMinutes ?: 0
        }
    val activePaths =
        skillPaths.count { skillPath ->
            skillPath.hasVerifiedEvidence
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 16.dp
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkPrimaryGreen.copy(
                alpha = 0.25f
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                            tint = VolunteerLinkPrimaryGreen
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.size(11.dp)
                )

                Column {
                    Text(
                        text = "Your Verified Record",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )

                    Text(
                        text =
                            "Progress is added only after organisation verification.",
                        fontSize = 10.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                VolunteerSkillPathStat(
                    value = verifiedRoles.toString(),
                    label = "Completed\nroles",
                    modifier = Modifier.weight(1f)
                )

                VolunteerSkillPathStat(
                    value =
                        if (verifiedMinutes >= 60) {
                            "${verifiedMinutes / 60}h"
                        } else {
                            "${verifiedMinutes}m"
                        },
                    label = "Verified\ntime",
                    modifier = Modifier.weight(1f)
                )

                VolunteerSkillPathStat(
                    value = "$activePaths/${skillPaths.size}",
                    label = "Active\npaths",
                    modifier = Modifier.weight(1f)
                )
            }

            if (verifiedRoles == 0) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFFFFF7E8)
                ) {
                    Text(
                        text =
                            "You have no verified role evidence yet. " +
                                "Accepted means your place is confirmed; " +
                                "it does not mean the role is completed.",
                        modifier = Modifier.padding(11.dp),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = VolunteerLinkWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = VolunteerLinkSoftGreenSurface
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = 10.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )

            Text(
                text = label,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathBadgesSection(
    skillPaths: List<VolunteerSkillPath>
) {
    val completedApplications =
        VolunteerOpportunitySessionStore.volunteerApplications
            .filter {
                it.applicationStatus == VolunteerApplicationStatus.COMPLETED
            }
    val verifiedRoles =
        completedApplications.size
    val verifiedMinutes =
        completedApplications.sumOf {
            it.applicationVerifiedMinutes ?: 0
        }
    val activePaths =
        skillPaths.count { it.hasVerifiedEvidence }
    val intermediatePaths =
        skillPaths.count { it.currentLevel >= 2 }
    val advancedPaths =
        skillPaths.count { it.currentLevel >= 3 }
    val verifiedOrganisations =
        completedApplications
            .map { it.applicationOrganisationName }
            .filter { it.isNotBlank() }
            .distinct()
            .size
    val verifiedSkills =
        completedApplications
            .flatMap { it.applicationPractisedSkills }
            .filter { it.isNotBlank() }
            .distinct()
            .size
    val certificates =
        completedApplications.count {
            !it.applicationCertificateId.isNullOrBlank()
        }

    val badges = listOf(
        VolunteerSkillPathBadge(
            title = "First Verified Role",
            description =
                "Complete one organisation-verified volunteer role.",
            isEarned = verifiedRoles >= 1,
            progressText = "${verifiedRoles.coerceAtMost(1)}/1 role",
            evidenceLabel = "Verified completion"
        ),
        VolunteerSkillPathBadge(
            title = "Five-Hour Contributor",
            description =
                "Build at least five hours of verified service.",
            isEarned = verifiedMinutes >= 300,
            progressText =
                "${verifiedMinutes.coerceAtMost(300)}/300 min",
            evidenceLabel = "Verified service time"
        ),
        VolunteerSkillPathBadge(
            title = "Multi-Path Explorer",
            description =
                "Earn verified evidence in two different Skill Paths.",
            isEarned = activePaths >= 2,
            progressText = "${activePaths.coerceAtMost(2)}/2 paths",
            evidenceLabel = "Cross-functional evidence"
        ),
        VolunteerSkillPathBadge(
            title = "Intermediate Ready",
            description =
                "Reach Intermediate in any one Skill Path.",
            isEarned = intermediatePaths >= 1,
            progressText =
                "${intermediatePaths.coerceAtMost(1)}/1 path",
            evidenceLabel = "Skill progression"
        ),
        VolunteerSkillPathBadge(
            title = "Trusted Contributor",
            description =
                "Complete three organisation-verified volunteer roles.",
            isEarned = verifiedRoles >= 3,
            progressText = "${verifiedRoles.coerceAtMost(3)}/3 roles",
            evidenceLabel = "Repeat contribution"
        ),
        VolunteerSkillPathBadge(
            title = "Community Connector",
            description =
                "Contribute successfully with three organisations.",
            isEarned = verifiedOrganisations >= 3,
            progressText =
                "${verifiedOrganisations.coerceAtMost(3)}/3 organisations",
            evidenceLabel = "Organisation diversity"
        ),
        VolunteerSkillPathBadge(
            title = "Verified Skill Builder",
            description =
                "Build evidence for five distinct practical skills.",
            isEarned = verifiedSkills >= 5,
            progressText = "${verifiedSkills.coerceAtMost(5)}/5 skills",
            evidenceLabel = "Verified skill portfolio"
        ),
        VolunteerSkillPathBadge(
            title = "Certified Volunteer",
            description =
                "Receive your first organisation-backed certificate.",
            isEarned = certificates >= 1,
            progressText = "${certificates.coerceAtMost(1)}/1 certificate",
            evidenceLabel = "Certificate issued"
        ),
        VolunteerSkillPathBadge(
            title = "Service Champion",
            description =
                "Accumulate twenty hours of verified community service.",
            isEarned = verifiedMinutes >= 1_200,
            progressText =
                "${verifiedMinutes.coerceAtMost(1_200)}/1200 min",
            evidenceLabel = "Sustained service"
        ),
        VolunteerSkillPathBadge(
            title = "Advanced Practitioner",
            description =
                "Reach Advanced in any evidence-backed Skill Path.",
            isEarned = advancedPaths >= 1,
            progressText = "${advancedPaths.coerceAtMost(1)}/1 path",
            evidenceLabel = "Advanced capability"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        VolunteerLinkScreenHorizontalPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Milestone Badges",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text =
                        "Automatically earned from verified evidence.",
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 10.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
            Text(
                text = "${badges.count { it.isEarned }}/${badges.size} EARNED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkSuccess
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        LazyRow(
            contentPadding = PaddingValues(
                horizontal = VolunteerLinkScreenHorizontalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(
                items = badges,
                key = { it.title }
            ) { badge ->
                Card(
                    modifier = Modifier.size(
                        width = 186.dp,
                        height = 142.dp
                    ),
                    shape = RoundedCornerShape(13.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (badge.isEarned) {
                                VolunteerLinkSoftGreenSurface
                            } else {
                                VolunteerLinkSurface
                            }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            if (badge.isEarned) {
                                VolunteerLinkPrimaryGreen.copy(
                                    alpha = 0.38f
                                )
                            } else {
                                VolunteerLinkBorderColour
                            }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color =
                                    if (badge.isEarned) {
                                        VolunteerLinkPrimaryGreen
                                    } else {
                                        VolunteerLinkBorderColour
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            if (badge.isEarned) {
                                                Icons.Filled.CheckCircle
                                            } else {
                                                Icons.Filled.Lock
                                            },
                                        contentDescription =
                                            if (badge.isEarned) {
                                                "Badge earned"
                                            } else {
                                                "Badge locked"
                                            },
                                        modifier = Modifier.size(17.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.size(8.dp))

                            Text(
                                text =
                                    if (badge.isEarned) "EARNED"
                                    else badge.progressText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (badge.isEarned) {
                                        VolunteerLinkSuccess
                                    } else {
                                        VolunteerLinkTextSecondary
                                    }
                            )
                        }

                        Text(
                            text = badge.title,
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = badge.description,
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            color = VolunteerLinkTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (badge.isEarned) {
                            Text(
                                text = badge.evidenceLabel,
                                modifier = Modifier.padding(top = 3.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerLinkSuccess,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathGuideCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VolunteerLinkScreenHorizontalPadding,
                end = VolunteerLinkScreenHorizontalPadding,
                top = 14.dp
            )
    ) {
        Text(
            text = "How Skill Path Works",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = VolunteerLinkSurface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = VolunteerLinkBorderColour
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                VolunteerSkillPathGuideStep(
                    stepNumber = "1",
                    title = "Join a matching role",
                    description =
                        "Each role names one Primary Skill Path and a required level."
                )
                VolunteerSkillPathGuideStep(
                    stepNumber = "2",
                    title = "Complete the volunteer work",
                    description =
                        "Pending or Accepted applications do not increase progress."
                )
                VolunteerSkillPathGuideStep(
                    stepNumber = "3",
                    title = "Organisation verifies completion",
                    description =
                        "The completed role and verified time become evidence in that path."
                )
                VolunteerSkillPathGuideStep(
                    stepNumber = "4",
                    title = "Unlock higher-level roles",
                    description =
                        "Meet every requirement to move from Beginner to Intermediate and Advanced.",
                    showConnector = false
                )
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathGuideStep(
    stepNumber: String,
    title: String,
    description: String,
    showConnector: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = VolunteerLinkPrimaryGreen
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (showConnector) {
                Box(
                    modifier = Modifier
                        .size(
                            width = 2.dp,
                            height = 32.dp
                        )
                        .background(
                            VolunteerLinkPrimaryGreen.copy(
                                alpha = 0.22f
                            )
                        )
                )
            }
        }

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 11.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathCard(
    volunteerSkillPath: VolunteerSkillPath,
    onSelected: () -> Unit
) {
    val openEventRoleCount =
        VolunteerOpportunitySessionStore
            .volunteerOpportunityEvents
            .sumOf { event ->
                event.eventVolunteerRoles.count { role ->
                    role.rolePrimarySkillPath ==
                        volunteerSkillPath.name &&
                        role.roleVacancies > 0
                }
            }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 10.dp
            )
            .clickable(onClick = onSelected),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = VolunteerLinkSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkBorderColour
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = volunteerSkillPath.name,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        VolunteerSkillPathLabel(
                            labelText =
                                volunteerSkillPath.pathMode
                                    .lowercase()
                                    .replaceFirstChar {
                                            character ->
                                        character.uppercase()
                                    },
                            labelColour =
                                if (
                                    volunteerSkillPath.pathMode ==
                                    "REMOTE"
                                ) {
                                    VolunteerLinkInformation
                                } else {
                                    VolunteerLinkPrimaryGreen
                                }
                        )

                        VolunteerSkillPathLabel(
                            labelText =
                                volunteerLevelDisplayName(
                                    volunteerSkillPath.currentLevel
                                ) +
                                    " • Level ${volunteerSkillPath.currentLevel}",
                            labelColour =
                                VolunteerLinkSuccess
                        )
                    }
                }

                Text(
                    text = "›",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            val nextLevel = volunteerSkillPath.nextLevel

            if (nextLevel == null) {
                Text(
                    text = "Maximum level achieved",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkSuccess
                )
            } else {
                Text(
                    text =
                        "Next unlock: " +
                            volunteerLevelDisplayName(
                                nextLevel.levelNumber
                            ) +
                            " • Level ${nextLevel.levelNumber}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                VolunteerSkillPathRequirementProgress(
                    label = "Completed roles",
                    currentValue =
                        volunteerSkillPath
                            .verifiedAssignments,
                    requiredValue =
                        nextLevel.requiredAssignments
                )

                nextLevel.requiredMinutes
                    ?.let { requiredMinutes ->
                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        VolunteerSkillPathRequirementProgress(
                            label = "Verified minutes",
                            currentValue =
                                volunteerSkillPath
                                    .verifiedMinutes
                                    ?: 0,
                            requiredValue = requiredMinutes
                        )
                    }

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text =
                        if (nextLevel.requiredMinutes == null) {
                            "Remote assignments are verified by the organisation. " +
                                "They build completed-role progress but do not add service hours."
                        } else {
                            "Both completed roles and verified time are required."
                        },
                    fontSize = 9.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    "${volunteerSkillPath.skills.size} skills  •  " +
                            "$openEventRoleCount open event roles",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathRequirementProgress(
    label: String,
    currentValue: Int,
    requiredValue: Int
) {
    val progress =
        if (requiredValue <= 0) {
            1f
        } else {
            currentValue.toFloat()
                .div(requiredValue.toFloat())
                .coerceIn(0f, 1f)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 10.sp,
            color = VolunteerLinkTextSecondary
        )

        Text(
            text = "$currentValue/$requiredValue",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )
    }

    Spacer(
        modifier = Modifier.height(4.dp)
    )

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        color = VolunteerLinkPrimaryGreen,
        trackColor = VolunteerLinkBorderColour
    )
}

@Composable
private fun VolunteerSkillPathDetailHeader(
    volunteerSkillPath: VolunteerSkillPath
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VolunteerLinkSurface)
            .padding(
                horizontal =
                    VolunteerLinkScreenHorizontalPadding,
                vertical = 18.dp
            )
    ) {
        Text(
            text = volunteerSkillPath.name,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            VolunteerSkillPathLabel(
                labelText =
                    volunteerSkillPath.pathMode,
                labelColour =
                    VolunteerLinkPrimaryGreen
            )

            VolunteerSkillPathLabel(
                labelText =
                    if (
                        volunteerSkillPath.progressionType ==
                        "ASSIGNMENTS_ONLY"
                    ) {
                        "Assignments"
                    } else {
                        "Assignments + Time"
                    },
                labelColour = VolunteerLinkInformation
            )

            VolunteerSkillPathLabel(
                labelText =
                    volunteerLevelDisplayName(
                        volunteerSkillPath.currentLevel
                    ) +
                        " • Level ${volunteerSkillPath.currentLevel}",
                labelColour = VolunteerLinkSuccess
            )
        }

        if (
            !volunteerSkillPath.description.isNullOrBlank()
        ) {
            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    volunteerSkillPath.description
                        ?: "",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathProgressSection(
    volunteerSkillPath: VolunteerSkillPath
) {
    val nextLevel = volunteerSkillPath.nextLevel

    VolunteerSkillPathSectionContainer(
        sectionTitle = "Your Progress"
    ) {
        Text(
            text =
                volunteerLevelDisplayName(
                    volunteerSkillPath.currentLevel
                ) +
                    " • Level ${volunteerSkillPath.currentLevel}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkPrimaryGreen
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text =
                if (volunteerSkillPath.hasVerifiedEvidence) {
                    "Based on organisation-verified completed roles."
                } else {
                    "Starting level — no completed role evidence yet."
                },
            fontSize = 11.sp,
            color = VolunteerLinkTextSecondary
        )

        if (nextLevel != null) {
            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text =
                    "Requirements for " +
                        volunteerLevelDisplayName(
                            nextLevel.levelNumber
                        ) +
                        " • Level ${nextLevel.levelNumber}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            VolunteerSkillPathRequirementProgress(
                label = "Completed roles",
                currentValue =
                    volunteerSkillPath.verifiedAssignments,
                requiredValue =
                    nextLevel.requiredAssignments
            )

            nextLevel.requiredMinutes
                ?.let { requiredMinutes ->
                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    VolunteerSkillPathRequirementProgress(
                        label = "Verified minutes",
                        currentValue =
                            volunteerSkillPath
                                .verifiedMinutes
                                ?: 0,
                        requiredValue = requiredMinutes
                    )
                }

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Text(
                text =
                    if (nextLevel.requiredMinutes == null) {
                        "Reach the completed-role requirement to level up. " +
                            "Remote assignment time is not counted as verified service hours."
                    } else {
                        "You must reach BOTH requirements. One requirement alone does not level up."
                    },
                fontSize = 10.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkInformation
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathEvidenceSection(
    volunteerSkillPath: VolunteerSkillPath
) {
    val completedEvidence =
        VolunteerOpportunitySessionStore
            .volunteerApplications
            .filter { application ->
                application.applicationStatus ==
                    VolunteerApplicationStatus.COMPLETED &&
                    application.applicationPrimarySkillPath ==
                        volunteerSkillPath.name
            }

    VolunteerSkillPathSectionContainer(
        sectionTitle = "Completed Evidence"
    ) {
        if (completedEvidence.isEmpty()) {
            Text(
                text = "No verified evidence yet",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "Your current applications may be Pending or Accepted. " +
                        "Evidence appears here only after the organisation marks " +
                        "the role Completed.",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                VolunteerSkillPathStat(
                    value =
                        volunteerSkillPath
                            .verifiedAssignments
                            .toString(),
                    label = "Verified\nroles",
                    modifier = Modifier.weight(1f)
                )

                VolunteerSkillPathStat(
                    value =
                        if (
                            volunteerSkillPath.progressionType ==
                                "ASSIGNMENTS_ONLY"
                        ) {
                            volunteerSkillPath
                                .verifiedAssignments
                                .toString()
                        } else {
                            (volunteerSkillPath
                                .verifiedMinutes
                                ?: 0)
                                .toString()
                        },
                    label =
                        if (
                            volunteerSkillPath.progressionType ==
                                "ASSIGNMENTS_ONLY"
                        ) {
                            "Verified\nassignments"
                        } else {
                            "Verified\nminutes"
                        },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            completedEvidence.forEachIndexed {
                    evidenceIndex,
                    application ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = VolunteerLinkSoftGreenSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = VolunteerLinkSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = application.applicationEventTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = application.applicationRoleTitle,
                            fontSize = 10.sp,
                            color = VolunteerLinkPrimaryGreen
                        )
                        Text(
                            text = buildString {
                                append(
                                    application.applicationCompletedDate
                                        ?: "Completed"
                                )
                                application.applicationVerifiedMinutes
                                    ?.let { minutes ->
                                        append(" • $minutes verified minutes")
                                    }
                            },
                            fontSize = 9.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }

                    application.applicationCertificateId
                        ?.let {
                            Text(
                                text = "CERTIFIED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkSuccess
                            )
                        }
                }

                if (evidenceIndex < completedEvidence.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 5.dp),
                        color = VolunteerLinkBorderColour
                    )
                }
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathSkillsSection(
    volunteerSkillPath: VolunteerSkillPath
) {
    val completedEvidence =
        VolunteerOpportunitySessionStore.volunteerApplications
            .filter { application ->
                application.applicationStatus ==
                    VolunteerApplicationStatus.COMPLETED &&
                    application.applicationPrimarySkillPath ==
                    volunteerSkillPath.name
            }

    VolunteerSkillPathSectionContainer(
        sectionTitle = "Verified Skill Evidence"
    ) {
        volunteerSkillPath.skills
            .forEachIndexed { skillIndex, skill ->
                val evidenceCount = completedEvidence.count { application ->
                    application.applicationPractisedSkills.any { practisedSkill ->
                        practisedSkill.equals(skill.name, ignoreCase = true)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(25.dp),
                        shape = CircleShape,
                        color =
                            if (evidenceCount > 0) {
                                VolunteerLinkSoftGreenSurface
                            } else {
                                VolunteerLinkBorderColour.copy(alpha = 0.55f)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector =
                                    if (evidenceCount > 0) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.Lock
                                    },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint =
                                    if (evidenceCount > 0) {
                                        VolunteerLinkSuccess
                                    } else {
                                        VolunteerLinkTextSecondary
                                    }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.size(9.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = skill.name,
                            fontSize = 12.sp,
                            fontWeight =
                                if (evidenceCount > 0) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text =
                                if (evidenceCount > 0) {
                                    "$evidenceCount completed role" +
                                        if (evidenceCount == 1) "" else "s"
                                } else {
                                    "Not yet evidenced"
                                },
                            fontSize = 9.sp,
                            color =
                                if (evidenceCount > 0) {
                                    VolunteerLinkSuccess
                                } else {
                                    VolunteerLinkTextSecondary
                                }
                        )
                    }
                }

                if (
                    skillIndex <
                    volunteerSkillPath.skills.lastIndex
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = 16.dp
                        ),
                        color = VolunteerLinkBorderColour
                    )
                }
            }
    }
}

@Composable
private fun VolunteerSkillPathRolesSection(
    volunteerSkillPath: VolunteerSkillPath,
    onVolunteerRoleSelected: (
        eventId: Int,
        roleId: Int
    ) -> Unit
) {
    val matchingEventRoles =
        VolunteerOpportunitySessionStore
            .volunteerOpportunityEvents
            .flatMap { event ->
                event.eventVolunteerRoles
                    .filter { role ->
                        role.rolePrimarySkillPath ==
                            volunteerSkillPath.name &&
                            role.roleVacancies > 0 &&
                            !VolunteerOpportunitySessionStore
                                .hasApplicationForRole(
                                    eventId = event.eventId,
                                    roleId = role.roleId
                                )
                    }
                    .map { role -> event to role }
            }
            .sortedWith(
                compareBy(
                    { pair ->
                        pair.second.roleMinimumSkillPathLevel >
                            volunteerSkillPath.currentLevel
                    },
                    { pair -> pair.first.eventDate }
                )
            )
            .take(6)

    VolunteerSkillPathSectionContainer(
        sectionTitle = "Build This Skill Path"
    ) {
        Text(
            text =
                "These are real, currently published opportunities. " +
                    "Complete one of these roles to add verified evidence " +
                    "to ${volunteerSkillPath.name}.",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(modifier = Modifier.height(11.dp))

        if (matchingEventRoles.isEmpty()) {
            Text(
                text =
                    "No open roles for this path right now. Try another path or check again after organisations publish new opportunities.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            matchingEventRoles
                .forEachIndexed { roleIndex, pair ->
                    val event = pair.first
                    val role = pair.second
                    val isEligible =
                        volunteerSkillPath.currentLevel >=
                            role.roleMinimumSkillPathLevel

                    Card(
                        onClick = {
                            onVolunteerRoleSelected(
                                event.eventId,
                                role.roleId
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isEligible) {
                                    VolunteerLinkSoftGreenSurface
                                } else {
                                    VolunteerLinkSurface
                                }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isEligible) {
                                VolunteerLinkPrimaryGreen.copy(
                                    alpha = 0.35f
                                )
                            } else {
                                VolunteerLinkBorderColour
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.eventTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VolunteerLinkTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = role.roleTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VolunteerLinkPrimaryGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text =
                                        if (isEligible) "MATCH"
                                        else "LEVEL ${role.roleMinimumSkillPathLevel}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (isEligible) {
                                            VolunteerLinkSuccess
                                        } else {
                                            VolunteerLinkWarning
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text =
                                    "${event.eventDate} • " +
                                        "${event.eventLocation} • " +
                                        "${role.roleVacancies} spots",
                                fontSize = 9.sp,
                                color = VolunteerLinkTextSecondary
                            )
                            Text(
                                text =
                                    role.roleSkillsPractised
                                        .take(3)
                                        .joinToString(" • "),
                                modifier = Modifier.padding(top = 3.dp),
                                fontSize = 9.sp,
                                color = VolunteerLinkInformation,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (roleIndex < matchingEventRoles.lastIndex) {
                        Spacer(modifier = Modifier.height(9.dp))
                    }
                }
        }
    }
}

@Composable
private fun VolunteerSkillPathSectionContainer(
    sectionTitle: String,
    sectionContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start =
                    VolunteerLinkScreenHorizontalPadding,
                end =
                    VolunteerLinkScreenHorizontalPadding,
                top = 20.dp
            )
    ) {
        Text(
            text = sectionTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = VolunteerLinkSurface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = VolunteerLinkBorderColour
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                sectionContent()
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathLabel(
    labelText: String,
    labelColour: Color
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = labelColour.copy(alpha = 0.12f)
    ) {
        Text(
            text = labelText,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColour
        )
    }
}

@Composable
private fun VolunteerSkillPathLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = VolunteerLinkPrimaryGreen
        )
    }
}

@Composable
private fun VolunteerSkillPathErrorContent(
    errorMessage: String,
    onRetrySelected: () -> Unit,
    retryButtonText: String = "Retry"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Skill Path unavailable",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = errorMessage,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = VolunteerLinkTextSecondary
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = onRetrySelected,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    VolunteerLinkPrimaryGreen
            )
        ) {
            Text(retryButtonText)
        }
    }
}

private fun volunteerLevelDisplayName(
    level: Int
): String {
    return when (level) {
        1 -> "Beginner"
        2 -> "Intermediate"
        3 -> "Advanced"
        else -> "Level $level"
    }
}
