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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.model.VolunteerSkillPath
import com.example.volunteerlink.model.VolunteerSkillPathLevel
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
        VolunteerSkillPathHeader()

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
                        key = "skill_path_detail_levels"
                    ) {
                        VolunteerSkillPathLevelsSection(
                            volunteerSkillPath =
                                volunteerSkillPath
                        )
                    }

                    item(
                        key = "skill_path_detail_skills"
                    ) {
                        VolunteerSkillPathSkillsSection(
                            volunteerSkillPath =
                                volunteerSkillPath
                        )
                    }

                    item(
                        key = "skill_path_detail_roles"
                    ) {
                        VolunteerSkillPathRolesSection(
                            volunteerSkillPath =
                                volunteerSkillPath
                        )
                    }
                }
        }
    }
}

@Composable
private fun VolunteerSkillPathHeader() {
    Column(
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
            )
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
                "Build verified skills through volunteer roles.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.84f)
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
            containerColor =
                VolunteerLinkSoftGreenSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = VolunteerLinkPrimaryGreen.copy(
                alpha = 0.25f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = VolunteerLinkPrimaryGreen
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(13.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "${skillPaths.size} Skill Paths available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "${skillPaths.count { it.pathMode == "PHYSICAL" }} physical  •  " +
                                "${skillPaths.count { it.pathMode == "REMOTE" }} remote",
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }
    }
}

@Composable
private fun VolunteerSkillPathCard(
    volunteerSkillPath: VolunteerSkillPath,
    onSelected: () -> Unit
) {
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
                                "Level ${volunteerSkillPath.currentLevel}",
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

            LinearProgressIndicator(
                progress = {
                    volunteerSkillPath.progressFraction
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = VolunteerLinkPrimaryGreen,
                trackColor = VolunteerLinkBorderColour
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    volunteerSkillPathProgressText(
                        volunteerSkillPath
                    ),
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    "${volunteerSkillPath.skills.size} skills  •  " +
                            "${volunteerSkillPath.relatedRoles.size} related roles",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkPrimaryGreen
            )
        }
    }
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
    VolunteerSkillPathSectionContainer(
        sectionTitle = "Your Progress"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text =
                        "Current Level ${volunteerSkillPath.currentLevel}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text =
                        volunteerSkillPathProgressText(
                            volunteerSkillPath
                        ),
                    fontSize = 11.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Text(
                text =
                    "${(volunteerSkillPath.progressFraction * 100).toInt()}%",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        LinearProgressIndicator(
            progress = {
                volunteerSkillPath.progressFraction
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = VolunteerLinkPrimaryGreen,
            trackColor = VolunteerLinkBorderColour
        )
    }
}

@Composable
private fun VolunteerSkillPathLevelsSection(
    volunteerSkillPath: VolunteerSkillPath
) {
    VolunteerSkillPathSectionContainer(
        sectionTitle = "Level Requirements"
    ) {
        volunteerSkillPath.levels
            .forEachIndexed {
                    levelIndex,
                    skillPathLevel ->
                VolunteerSkillPathLevelRow(
                    skillPathLevel = skillPathLevel,
                    currentLevel =
                        volunteerSkillPath.currentLevel
                )

                if (
                    levelIndex <
                    volunteerSkillPath.levels.lastIndex
                ) {
                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
    }
}

@Composable
private fun VolunteerSkillPathLevelRow(
    skillPathLevel: VolunteerSkillPathLevel,
    currentLevel: Int
) {
    val levelIsReached =
        currentLevel >= skillPathLevel.levelNumber

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                if (levelIsReached) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Lock
                },
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint =
                if (levelIsReached) {
                    VolunteerLinkSuccess
                } else {
                    VolunteerLinkTextSecondary
                }
        )

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = skillPathLevel.levelName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )

            Text(
                text = buildString {
                    append(
                        "${skillPathLevel.requiredAssignments} verified assignments"
                    )
                    skillPathLevel.requiredMinutes
                        ?.let { requiredMinutes ->
                            append("  •  ")
                            append(requiredMinutes / 60)
                            append(" verified hours")
                        }
                },
                fontSize = 10.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

@Composable
private fun VolunteerSkillPathSkillsSection(
    volunteerSkillPath: VolunteerSkillPath
) {
    VolunteerSkillPathSectionContainer(
        sectionTitle = "Skills in this Path"
    ) {
        volunteerSkillPath.skills
            .forEachIndexed { skillIndex, skill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape = CircleShape,
                        color = VolunteerLinkPrimaryGreen
                    ) {}

                    Spacer(
                        modifier = Modifier.size(9.dp)
                    )

                    Text(
                        text = skill.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextPrimary
                    )
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
    volunteerSkillPath: VolunteerSkillPath
) {
    VolunteerSkillPathSectionContainer(
        sectionTitle = "Related Volunteer Roles"
    ) {
        if (volunteerSkillPath.relatedRoles.isEmpty()) {
            Text(
                text =
                    "No role templates are linked to this path yet.",
                fontSize = 12.sp,
                color = VolunteerLinkTextSecondary
            )
        } else {
            volunteerSkillPath.relatedRoles
                .forEachIndexed { roleIndex, role ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = role.roleName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = role.roleArea,
                            fontSize = 10.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }

                    if (
                        roleIndex <
                        volunteerSkillPath.relatedRoles
                            .lastIndex
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                vertical = 9.dp
                            ),
                            color = VolunteerLinkBorderColour
                        )
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

private fun volunteerSkillPathProgressText(
    volunteerSkillPath: VolunteerSkillPath
): String {
    val nextLevel = volunteerSkillPath.nextLevel
        ?: return "Maximum level achieved"

    return buildString {
        append(
            "${volunteerSkillPath.verifiedAssignments}/${nextLevel.requiredAssignments} assignments"
        )

        nextLevel.requiredMinutes
            ?.let { requiredMinutes ->
                append("  •  ")
                append(
                    "${volunteerSkillPath.verifiedMinutes ?: 0}/$requiredMinutes minutes"
                )
            }

        append(
            " to ${nextLevel.levelName}"
        )
    }
}
