package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.components.CreateCardBackground
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateLightGreen
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

val RoleSelectionBorder = Color(0xFFC8D4C4)

/**
 * Step 2 follows the prototype flow:
 * - role categories are collapsed by default;
 * - only one category is expanded when the organiser chooses to view it;
 * - selected roles are removed from the available list and shown below;
 * - capacities must exactly match the requirement entered in Step 1.
 *
 * Catalogue data comes from Supabase through CreatePostViewModel. Step 2 only
 * keeps role_template_id + capacity in the shared draft, which is ready to be
 * converted into post_roles rows later when the full post is published.
 */
@Composable
fun SelectRolesStep(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onBack: () -> Unit,
    onStepTwoComplete: () -> Unit = {}
) {
    val draft = uiState.draft
    val catalogue = uiState.roleCatalogue
    val postType = draft.postType

    val activeMode = when (postType) {
        VolunteerPostType.PHYSICAL -> VolunteerRoleMode.PHYSICAL
        VolunteerPostType.REMOTE -> VolunteerRoleMode.REMOTE
        VolunteerPostType.HYBRID ->
            uiState.roleModeFilter ?: VolunteerRoleMode.PHYSICAL
        null -> VolunteerRoleMode.PHYSICAL
    }

    var expandedRoleAreas by remember(postType, activeMode) {
        mutableStateOf(emptySet<String>())
    }

    val templatesById = catalogue.associateBy { it.roleTemplateId }

    val rolesForActiveMode = catalogue.filter { role ->
        role.roleMode == activeMode && roleSelectionMatchesPostType(
            roleMode = role.roleMode,
            postType = postType
        )
    }

    val roleAreasForActiveMode = rolesForActiveMode
        .map { it.roleArea }
        .distinct()

    val selectedRoleIds = draft.selectedRoles
        .map { it.roleTemplateId }
        .toSet()

    // A selected role disappears from the available list and is shown in the
    // Selected Roles section instead, matching the prototype behaviour.
    val availableRolesForActiveMode = rolesForActiveMode.filterNot { role ->
        role.roleTemplateId in selectedRoleIds
    }

    val normalizedQuery = uiState.roleSearchQuery.trim()

    val searchResults = if (normalizedQuery.isBlank()) {
        emptyList()
    } else {
        availableRolesForActiveMode.filter { role ->
            role.roleName.contains(normalizedQuery, ignoreCase = true) ||
                    role.description.contains(normalizedQuery, ignoreCase = true) ||
                    role.roleArea.contains(normalizedQuery, ignoreCase = true) ||
                    role.defaultLevel.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    role.skillPathName.contains(normalizedQuery, ignoreCase = true) ||
                    role.skillsPractised.any { skill ->
                        skill.name.contains(normalizedQuery, ignoreCase = true)
                    }
        }
    }

    val selectedRolesForActiveMode = draft.selectedRoles.filter { selectedRole ->
        templatesById[selectedRole.roleTemplateId]?.roleMode == activeMode
    }

    val expandableAreas = roleAreasForActiveMode
        .filter { area ->
            availableRolesForActiveMode.any { role -> role.roleArea == area }
        }
        .toSet()

    val allAvailableCategoriesExpanded =
        expandableAreas.isNotEmpty() &&
                expandedRoleAreas.containsAll(expandableAreas)

    val physicalAssigned = assignedRoleCapacityForMode(
        draft = draft,
        mode = VolunteerRoleMode.PHYSICAL,
        templatesById = templatesById
    )
    val remoteAssigned = assignedRoleCapacityForMode(
        draft = draft,
        mode = VolunteerRoleMode.REMOTE,
        templatesById = templatesById
    )

    val roleSelectionIsValid =
        catalogue.isNotEmpty() &&
                !uiState.isRoleCatalogueLoading &&
                uiState.roleCatalogueError == null &&
                !uiState.roleSelectionErrors.hasErrors()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            RoleSelectionHeader(onBack = onBack)
        }

        item {
            RoleAssignmentSummaryCard(
                draft = draft,
                templatesById = templatesById
            )
        }

        if (postType == VolunteerPostType.HYBRID) {
            item {
                RoleSelectionHybridModeSelector(
                    selectedMode = activeMode,
                    physicalAssigned = physicalAssigned,
                    physicalRequired = draft.requiredPhysicalVolunteerTotal ?: 0,
                    remoteAssigned = remoteAssigned,
                    remoteRequired = draft.requiredRemoteVolunteerTotal ?: 0,
                    onModeSelected = { mode ->
                        viewModel.updateRoleModeFilter(mode)
                    }
                )
            }
        }

        item {
            OutlinedTextField(
                value = uiState.roleSearchQuery,
                onValueChange = viewModel::updateRoleSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Search roles")
                },
                placeholder = {
                    Text(text = "Example: registration, design, leader")
                },
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_volunteer_search),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )
        }

        when {
            uiState.isRoleCatalogueLoading -> {
                item {
                    RoleSelectionLoadingCard()
                }
            }

            uiState.roleCatalogueError != null -> {
                item {
                    RoleSelectionCatalogueErrorCard(
                        message = uiState.roleCatalogueError,
                        onRetry = viewModel::retryRoleCatalogue
                    )
                }
            }

            catalogue.isEmpty() -> {
                item {
                    RoleSelectionCatalogueErrorCard(
                        message = "No volunteer roles are available.",
                        onRetry = viewModel::retryRoleCatalogue
                    )
                }
            }

            normalizedQuery.isNotBlank() -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RoleSelectionSectionHeading(
                            title = "Search Results · ${searchResults.size}",
                            subtitle = "Matching available ${activeMode.displayName.lowercase()} roles.",
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                viewModel.updateRoleSearchQuery("")
                            }
                        ) {
                            Text(text = "Clear")
                        }
                    }
                }

                if (searchResults.isEmpty()) {
                    item {
                        RoleSelectionEmptyCard(
                            text = "No available roles match \"$normalizedQuery\". Try another role name or category."
                        )
                    }
                } else {
                    items(
                        items = searchResults,
                        key = { role -> "search_${role.roleTemplateId}" }
                    ) { role ->
                        AvailableRoleSelectionCard(
                            role = role,
                            canAdd = remainingRoleCapacityForMode(
                                draft = draft,
                                mode = role.roleMode,
                                templatesById = templatesById
                            ) > 0,
                            onAdd = {
                                viewModel.addRole(role.roleTemplateId)
                            }
                        )
                    }
                }
            }

            else -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RoleSelectionSectionHeading(
                            title = "Available Roles by Category",
                            subtitle = "Tap a category to view its roles. Selected roles are moved below.",
                            modifier = Modifier.weight(1f)
                        )

                        if (expandableAreas.size > 1) {
                            TextButton(
                                onClick = {
                                    expandedRoleAreas =
                                        if (allAvailableCategoriesExpanded) {
                                            emptySet()
                                        } else {
                                            expandableAreas
                                        }
                                }
                            ) {
                                Text(
                                    text = if (allAvailableCategoriesExpanded) {
                                        "Collapse all"
                                    } else {
                                        "Expand all"
                                    }
                                )
                            }
                        }
                    }
                }

                if (roleAreasForActiveMode.isEmpty()) {
                    item {
                        RoleSelectionEmptyCard(
                            text = "No role categories are available for this post type."
                        )
                    }
                } else {
                    roleAreasForActiveMode.forEach { roleArea ->
                        val allRolesInArea = rolesForActiveMode.filter { role ->
                            role.roleArea == roleArea
                        }

                        val availableRolesInArea = availableRolesForActiveMode.filter { role ->
                            role.roleArea == roleArea
                        }

                        val selectedCountInArea =
                            allRolesInArea.size - availableRolesInArea.size

                        val isExpanded =
                            roleArea in expandedRoleAreas &&
                                    availableRolesInArea.isNotEmpty()

                        item(key = "area_$roleArea") {
                            RoleSelectionAreaCard(
                                roleArea = roleArea,
                                availableCount = availableRolesInArea.size,
                                selectedCount = selectedCountInArea,
                                isExpanded = isExpanded,
                                onToggle = {
                                    if (availableRolesInArea.isNotEmpty()) {
                                        expandedRoleAreas = if (isExpanded) {
                                            expandedRoleAreas - roleArea
                                        } else {
                                            expandedRoleAreas + roleArea
                                        }
                                    }
                                }
                            )
                        }

                        if (isExpanded) {
                            items(
                                items = availableRolesInArea,
                                key = { role -> "available_${role.roleTemplateId}" }
                            ) { role ->
                                Box(
                                    modifier = Modifier.padding(start = 10.dp)
                                ) {
                                    AvailableRoleSelectionCard(
                                        role = role,
                                        canAdd = remainingRoleCapacityForMode(
                                            draft = draft,
                                            mode = role.roleMode,
                                            templatesById = templatesById
                                        ) > 0,
                                        onAdd = {
                                            viewModel.addRole(role.roleTemplateId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (
            !uiState.isRoleCatalogueLoading &&
            uiState.roleCatalogueError == null &&
            catalogue.isNotEmpty()
        ) {
            item {
                RoleSelectionSectionHeading(
                    title = "Selected Roles · ${selectedRolesForActiveMode.size}",
                    subtitle = if (postType == VolunteerPostType.HYBRID) {
                        "Showing ${activeMode.displayName} roles."
                    } else {
                        "Adjust the capacity for each selected role."
                    }
                )
            }

            if (selectedRolesForActiveMode.isEmpty()) {
                item {
                    RoleSelectionEmptyCard(
                        text = "No ${activeMode.displayName.lowercase()} roles selected yet."
                    )
                }
            } else {
                items(
                    items = selectedRolesForActiveMode,
                    key = { selectedRole ->
                        "selected_${selectedRole.roleTemplateId}"
                    }
                ) { selectedRole ->
                    val role = templatesById[selectedRole.roleTemplateId]

                    if (role != null) {
                        val remaining = remainingRoleCapacityForMode(
                            draft = draft,
                            mode = role.roleMode,
                            templatesById = templatesById
                        )

                        SelectedRoleSelectionCard(
                            role = role,
                            selectedRole = selectedRole,
                            canIncrease = remaining > 0,
                            maxCapacity =
                                selectedRole.capacity + remaining.coerceAtLeast(0),
                            onIncrease = {
                                viewModel.increaseRoleCapacity(
                                    role.roleTemplateId
                                )
                            },
                            onDecrease = {
                                viewModel.decreaseRoleCapacity(
                                    role.roleTemplateId
                                )
                            },
                            onSetCapacity = { capacity ->
                                viewModel.updateRoleCapacity(
                                    roleTemplateId = role.roleTemplateId,
                                    text = capacity.toString()
                                )
                            },
                            onRemove = {
                                viewModel.removeRole(role.roleTemplateId)
                            }
                        )
                    }
                }
            }

            item {
                RoleSelectionStatus(
                    uiState = uiState,
                    templatesById = templatesById
                )
            }

            item {
                Button(
                    onClick = {
                        if (viewModel.continueFromStepTwo()) {
                            onStepTwoComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = roleSelectionIsValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    )
                ) {
                    Text(text = "Continue to Role Settings")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RoleSelectionHeader(
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Back to post details",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(CreateGreen)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Select Roles",
                style = MaterialTheme.typography.headlineSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Step 2 of 4 · Choose roles and assign their capacity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoleAssignmentSummaryCard(
    draft: CreatePostDraft,
    templatesById: Map<String, CreateRoleTemplate>
) {
    val requiredTotal = draft.requiredVolunteerTotal ?: 0
    val assignedTotal = draft.selectedRoles.sumOf { it.capacity }
    val remainingTotal = requiredTotal - assignedTotal
    val progress = if (requiredTotal > 0) {
        (assignedTotal.toFloat() / requiredTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CreateLightGreen
                ) {
                    Image(
                        painter = painterResource(R.drawable.group),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp),
                        colorFilter = ColorFilter.tint(CreateGreen)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Role Assignment",
                        style = MaterialTheme.typography.titleLarge,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$assignedTotal of $requiredTotal assigned",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(
                        color = Color(0xFFDDE5DA),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(9.dp)
                        .background(
                            color = if (remainingTotal < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                CreateGreen
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Text(
                text = when {
                    remainingTotal > 0 ->
                        "$remainingTotal volunteer position${if (remainingTotal == 1) "" else "s"} remaining"
                    remainingTotal == 0 ->
                        "All volunteer positions have been assigned."
                    else ->
                        "Reduce the selected role capacity by ${-remainingTotal}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (remainingTotal < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (draft.postType == VolunteerPostType.HYBRID) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleSelectionModeAssignmentLine(
                        label = "Physical",
                        assigned = assignedRoleCapacityForMode(
                            draft = draft,
                            mode = VolunteerRoleMode.PHYSICAL,
                            templatesById = templatesById
                        ),
                        required = draft.requiredPhysicalVolunteerTotal ?: 0
                    )

                    RoleSelectionModeAssignmentLine(
                        label = "Remote",
                        assigned = assignedRoleCapacityForMode(
                            draft = draft,
                            mode = VolunteerRoleMode.REMOTE,
                            templatesById = templatesById
                        ),
                        required = draft.requiredRemoteVolunteerTotal ?: 0
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionModeAssignmentLine(
    label: String,
    assigned: Int,
    required: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label roles",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "$assigned of $required",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (assigned == required) {
                CreateGreen
            } else if (assigned > required) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun RoleSelectionHybridModeSelector(
    selectedMode: VolunteerRoleMode,
    physicalAssigned: Int,
    physicalRequired: Int,
    remoteAssigned: Int,
    remoteRequired: Int,
    onModeSelected: (VolunteerRoleMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Role Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoleSelectionModeButton(
                title = "Physical",
                progressText = "$physicalAssigned/$physicalRequired",
                isSelected = selectedMode == VolunteerRoleMode.PHYSICAL,
                onClick = {
                    onModeSelected(VolunteerRoleMode.PHYSICAL)
                },
                modifier = Modifier.weight(1f)
            )

            RoleSelectionModeButton(
                title = "Remote",
                progressText = "$remoteAssigned/$remoteRequired",
                isSelected = selectedMode == VolunteerRoleMode.REMOTE,
                onClick = {
                    onModeSelected(VolunteerRoleMode.REMOTE)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RoleSelectionModeButton(
    title: String,
    progressText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CreateGreen
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title)
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title)
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun RoleSelectionAreaCard(
    roleArea: String,
    availableCount: Int,
    selectedCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val hasAvailableRoles = availableCount > 0

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = hasAvailableRoles,
                onClick = onToggle
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isExpanded) {
                CreateLightGreen
            } else {
                CreateCardBackground
            }
        ),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) {
                CreateGreen
            } else {
                RoleSelectionBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleSelectionAreaIcon(roleArea = roleArea)

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = roleArea,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasAvailableRoles) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Text(
                    text = when {
                        availableCount == 0 ->
                            "All roles in this category are selected"
                        selectedCount > 0 ->
                            "$availableCount available · $selectedCount selected"
                        availableCount == 1 ->
                            "1 role available"
                        else ->
                            "$availableCount roles available"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (hasAvailableRoles) {
                    if (isExpanded) CreateGreen else CreateLightGreen
                } else {
                    Color(0xFFE9ECE7)
                }
            ) {
                Text(
                    text = when {
                        !hasAvailableRoles -> "Done"
                        isExpanded -> "Hide"
                        else -> "View roles"
                    },
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !hasAvailableRoles ->
                            MaterialTheme.colorScheme.onSurfaceVariant
                        isExpanded -> Color.White
                        else -> CreateGreen
                    }
                )
            }
        }
    }
}

@Composable
fun RoleSelectionSectionHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = CreateGreen,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AvailableRoleSelectionCard(
    role: CreateRoleTemplate,
    canAdd: Boolean,
    onAdd: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleSelectionAreaIcon(roleArea = role.roleArea)

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = role.roleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    RoleSelectionLevelChip(level = role.defaultLevel)

                    Text(
                        text = role.roleArea,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onAdd,
                    enabled = canAdd,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    )
                ) {
                    Text(text = "Add")
                }
            }

            Text(
                text = role.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SelectedRoleSelectionCard(
    role: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft,
    canIncrease: Boolean,
    maxCapacity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onSetCapacity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            color = RoleSelectionBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleSelectionAreaIcon(roleArea = role.roleArea)

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = role.roleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleSelectionLevelChip(level = role.defaultLevel)

                        Text(
                            text = "${role.roleMode.displayName} role",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onRemove
                ) {
                    Image(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = role.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Volunteers needed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Use − / + or type a number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleCapacityIconButton(
                        iconRes = R.drawable.remove,
                        contentDescription = "Decrease capacity",
                        enabled = selectedRole.capacity > 1,
                        onClick = onDecrease
                    )

                    RoleCapacityNumberField(
                        roleTemplateId = role.roleTemplateId,
                        value = selectedRole.capacity,
                        maxValue = maxCapacity,
                        onValueChange = onSetCapacity
                    )

                    RoleCapacityIconButton(
                        iconRes = R.drawable.add,
                        contentDescription = "Increase capacity",
                        enabled = canIncrease,
                        onClick = onIncrease
                    )
                }
            }
        }
    }
}

@Composable
fun RoleCapacityNumberField(
    roleTemplateId: String,
    value: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit
) {
    val initialText = value.toString()

    var input by remember(roleTemplateId) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    var hasFocus by remember(roleTemplateId) {
        mutableStateOf(false)
    }

    // Keep the text synchronized with minus/plus changes without fighting the
    // organiser while a valid value is being typed.
    LaunchedEffect(value) {
        if (input.text.toIntOrNull() != value) {
            val updatedText = value.toString()
            input = TextFieldValue(
                text = updatedText,
                selection = TextRange(updatedText.length)
            )
        }
    }

    Surface(
        modifier = Modifier
            .width(54.dp)
            .height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFB8C8B3)
        ),
        color = Color.White
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = input,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.text
                        .filter { character -> character.isDigit() }
                        .take(4)

                    if (digitsOnly.isEmpty()) {
                        // Allow a temporary blank while replacing the number.
                        input = TextFieldValue(
                            text = "",
                            selection = TextRange.Zero
                        )
                    } else {
                        val requested = digitsOnly.toIntOrNull()
                            ?: return@BasicTextField

                        val accepted = requested.coerceIn(
                            minimumValue = 1,
                            maximumValue = maxValue.coerceAtLeast(1)
                        )

                        val acceptedText = accepted.toString()
                        input = TextFieldValue(
                            text = acceptedText,
                            selection = TextRange(acceptedText.length)
                        )

                        if (accepted != value) {
                            onValueChange(accepted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        val justFocused = focusState.isFocused && !hasFocus
                        hasFocus = focusState.isFocused

                        if (justFocused) {
                            // Select the old value so typing a new number
                            // replaces it instead of turning 1 + 3 into 13.
                            input = input.copy(
                                selection = TextRange(
                                    start = 0,
                                    end = input.text.length
                                )
                            )
                        } else if (
                            !focusState.isFocused &&
                            input.text.isBlank()
                        ) {
                            val restoredText = value.toString()
                            input = TextFieldValue(
                                text = restoredText,
                                selection = TextRange(restoredText.length)
                            )
                        }
                    }
                    .padding(horizontal = 6.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = CreateGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(CreateGreen)
            )
        }
    }
}

@Composable
fun RoleCapacityIconButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                CreateGreen
            } else {
                Color(0xFFB8BDB6)
            }
        ),
        color = Color.Transparent
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    if (enabled) {
                        CreateGreen
                    } else {
                        Color(0xFF9EA39C)
                    }
                )
            )
        }
    }
}

@Composable
fun RoleSelectionLevelChip(
    level: VolunteerRoleLevel
) {
    val containerColor = when (level) {
        VolunteerRoleLevel.BEGINNER -> Color(0xFFE6F3E1)
        VolunteerRoleLevel.INTERMEDIATE -> Color(0xFFE9E8F8)
        VolunteerRoleLevel.ADVANCED -> Color(0xFFFFEAD8)
    }

    val textColor = when (level) {
        VolunteerRoleLevel.BEGINNER -> Color(0xFF2A6A29)
        VolunteerRoleLevel.INTERMEDIATE -> Color(0xFF4F46A5)
        VolunteerRoleLevel.ADVANCED -> Color(0xFFA24A12)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = level.displayName,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 3.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RoleSelectionAreaIcon(
    roleArea: String
) {
    Surface(
        shape = CircleShape,
        color = CreateLightGreen
    ) {
        Image(
            painter = painterResource(roleSelectionAreaIconRes(roleArea)),
            contentDescription = null,
            modifier = Modifier
                .padding(10.dp)
                .size(28.dp),
            colorFilter = ColorFilter.tint(CreateGreen)
        )
    }
}

fun roleSelectionAreaIconRes(
    roleArea: String
): Int {
    return when (roleArea) {
        "General Event Support" -> R.drawable.role_general_support
        "Registration & Guest Support" -> R.drawable.role_registration
        "Logistics & Distribution" -> R.drawable.role_logistics
        "Crowd & Safety Support" -> R.drawable.role_safety
        "Community Engagement & Activity Support" -> R.drawable.role_community
        "Media & Event Documentation" -> R.drawable.role_media
        "Graphic & Visual Design" -> R.drawable.role_graphic_design
        "Writing & Content" -> R.drawable.role_writing
        "Social Media & Digital Campaigns" -> R.drawable.role_social_media
        "Research & Data" -> R.drawable.role_research_data
        "Administration & Documentation" -> R.drawable.role_administration
        "Digital & Technical Support" -> R.drawable.role_technical_support
        else -> R.drawable.group
    }
}

@Composable
fun RoleSelectionEmptyCard(
    text: String
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RoleSelectionStatus(
    uiState: CreatePostUiState,
    templatesById: Map<String, CreateRoleTemplate>
) {
    val draft = uiState.draft
    val isValid = !uiState.roleSelectionErrors.hasErrors()

    val physicalAssigned = assignedRoleCapacityForMode(
        draft = draft,
        mode = VolunteerRoleMode.PHYSICAL,
        templatesById = templatesById
    )
    val remoteAssigned = assignedRoleCapacityForMode(
        draft = draft,
        mode = VolunteerRoleMode.REMOTE,
        templatesById = templatesById
    )

    val message = uiState.roleSelectionErrors.general ?: when (draft.postType) {
        VolunteerPostType.PHYSICAL -> roleCapacityCompletionMessage(
            label = "physical",
            assigned = physicalAssigned,
            required = draft.requiredPhysicalVolunteerTotal ?: 0
        )

        VolunteerPostType.REMOTE -> roleCapacityCompletionMessage(
            label = "remote",
            assigned = remoteAssigned,
            required = draft.requiredRemoteVolunteerTotal ?: 0
        )

        VolunteerPostType.HYBRID -> {
            val physicalMessage = roleCapacityCompletionMessage(
                label = "physical",
                assigned = physicalAssigned,
                required = draft.requiredPhysicalVolunteerTotal ?: 0
            )

            val remoteMessage = roleCapacityCompletionMessage(
                label = "remote",
                assigned = remoteAssigned,
                required = draft.requiredRemoteVolunteerTotal ?: 0
            )

            if (isValid) {
                "All physical and remote positions are assigned."
            } else {
                "$physicalMessage $remoteMessage"
            }
        }

        null -> "Return to Step 1 and select a post type."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isValid) {
            CreateLightGreen
        } else {
            Color(0xFFF4F5F2)
        }
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isValid) {
                CreateGreen
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun RoleSelectionLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreateCardBackground,
        border = BorderStroke(1.dp, RoleSelectionBorder)
    ) {
        Text(
            text = "Loading volunteer roles...",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RoleSelectionCatalogueErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreateCardBackground,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

fun roleSelectionMatchesPostType(
    roleMode: VolunteerRoleMode,
    postType: VolunteerPostType?
): Boolean {
    return when (postType) {
        VolunteerPostType.PHYSICAL ->
            roleMode == VolunteerRoleMode.PHYSICAL
        VolunteerPostType.REMOTE ->
            roleMode == VolunteerRoleMode.REMOTE
        VolunteerPostType.HYBRID -> true
        null -> false
    }
}

fun assignedRoleCapacityForMode(
    draft: CreatePostDraft,
    mode: VolunteerRoleMode,
    templatesById: Map<String, CreateRoleTemplate>
): Int {
    return draft.selectedRoles
        .filter { selectedRole ->
            templatesById[selectedRole.roleTemplateId]?.roleMode == mode
        }
        .sumOf { selectedRole -> selectedRole.capacity }
}

fun remainingRoleCapacityForMode(
    draft: CreatePostDraft,
    mode: VolunteerRoleMode,
    templatesById: Map<String, CreateRoleTemplate>
): Int {
    val required = when (mode) {
        VolunteerRoleMode.PHYSICAL -> draft.requiredPhysicalVolunteerTotal ?: 0
        VolunteerRoleMode.REMOTE -> draft.requiredRemoteVolunteerTotal ?: 0
    }

    return required - assignedRoleCapacityForMode(
        draft = draft,
        mode = mode,
        templatesById = templatesById
    )
}

fun roleCapacityCompletionMessage(
    label: String,
    assigned: Int,
    required: Int
): String {
    val remaining = required - assigned

    return when {
        remaining > 0 ->
            "Assign $remaining more $label position${if (remaining == 1) "" else "s"}."
        remaining < 0 ->
            "Reduce $label capacity by ${-remaining}."
        else ->
            "All $label positions are assigned."
    }
}
