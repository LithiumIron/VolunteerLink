package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.components.CreateCardBackground
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateLightGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.SelectedRoleDraft
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleLevel
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel

private val RoleBorder = Color(0xFFD7DDD3)
private val MutedText = Color(0xFF667064)
private val DangerRed = Color(0xFFB3261E)
private val BeginnerBackground = Color(0xFFE5F3E2)
private val BeginnerText = Color(0xFF2A5A2B)
private val IntermediateBackground = Color(0xFFEDE7F6)
private val IntermediateText = Color(0xFF644B87)
private val AdvancedBackground = Color(0xFFFFE9D6)
private val AdvancedText = Color(0xFF9A4E11)

/**
 * Step 2 of Create Post.
 *
 * The role cards follow the AssignmentTest prototype design, while all role
 * catalogue information now comes from Supabase through CreatePostViewModel.
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
    val selectedById = draft.selectedRoles.associateBy { it.roleTemplateId }
    val templatesById = catalogue.associateBy { it.roleTemplateId }

    val physicalAssigned = draft.selectedRoles
        .filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.PHYSICAL
        }
        .sumOf { it.capacity }

    val remoteAssigned = draft.selectedRoles
        .filter { selected ->
            templatesById[selected.roleTemplateId]?.roleMode ==
                    VolunteerRoleMode.REMOTE
        }
        .sumOf { it.capacity }

    val query = uiState.roleSearchQuery.trim()

    val visibleRoles = catalogue.filter { role ->
        val matchesPostType = when (draft.postType) {
            VolunteerPostType.PHYSICAL ->
                role.roleMode == VolunteerRoleMode.PHYSICAL

            VolunteerPostType.REMOTE ->
                role.roleMode == VolunteerRoleMode.REMOTE

            VolunteerPostType.HYBRID -> true
            null -> false
        }

        val matchesFilter =
            uiState.roleModeFilter == null ||
                    role.roleMode == uiState.roleModeFilter

        val searchableText = buildString {
            append(role.roleName)
            append(' ')
            append(role.roleArea)
            append(' ')
            append(role.skillPathName)
            append(' ')
            append(role.skillsPractised.joinToString(" ") { it.name })
        }

        matchesPostType &&
                matchesFilter &&
                (
                    query.isBlank() ||
                            searchableText.contains(query, ignoreCase = true)
                    )
    }

    val rolesByArea = visibleRoles.groupBy { it.roleArea }
    val roleErrors = uiState.visibleRoleSelectionErrors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            CreateStepTwoHeader(onBack = onBack)
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Select Volunteer Roles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CreateGreen
                )
                Text(
                    text = "Choose the roles needed for this post and distribute the volunteer requirement from Step 1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
        }

        item {
            AllocationSummary(
                postType = draft.postType,
                physicalAssigned = physicalAssigned,
                physicalRequired = draft.requiredPhysicalVolunteerTotal,
                remoteAssigned = remoteAssigned,
                remoteRequired = draft.requiredRemoteVolunteerTotal,
                physicalError = roleErrors.physical,
                remoteError = roleErrors.remote,
                generalError = roleErrors.general
            )
        }

        item {
            OutlinedTextField(
                value = uiState.roleSearchQuery,
                onValueChange = viewModel::updateRoleSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search roles") },
                placeholder = {
                    Text("Search by role, area, skill path or skill")
                },
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_volunteer_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp)
            )
        }

        if (draft.postType == VolunteerPostType.HYBRID) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleFilterChip(
                        text = "All",
                        selected = uiState.roleModeFilter == null,
                        onClick = {
                            viewModel.updateRoleModeFilter(null)
                        }
                    )
                    RoleFilterChip(
                        text = "Physical",
                        selected =
                            uiState.roleModeFilter == VolunteerRoleMode.PHYSICAL,
                        onClick = {
                            viewModel.updateRoleModeFilter(
                                VolunteerRoleMode.PHYSICAL
                            )
                        }
                    )
                    RoleFilterChip(
                        text = "Remote",
                        selected =
                            uiState.roleModeFilter == VolunteerRoleMode.REMOTE,
                        onClick = {
                            viewModel.updateRoleModeFilter(
                                VolunteerRoleMode.REMOTE
                            )
                        }
                    )
                }
            }
        }

        when {
            uiState.isRoleCatalogueLoading -> {
                item {
                    LoadingCatalogueCard()
                }
            }

            uiState.roleCatalogueError != null -> {
                item {
                    CatalogueErrorCard(
                        message = uiState.roleCatalogueError,
                        onRetry = viewModel::retryRoleCatalogue
                    )
                }
            }

            catalogue.isEmpty() -> {
                item {
                    CatalogueErrorCard(
                        message = "No volunteer roles are available.",
                        onRetry = viewModel::retryRoleCatalogue
                    )
                }
            }

            visibleRoles.isEmpty() -> {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CreateCardBackground,
                        border = BorderStroke(1.dp, RoleBorder)
                    ) {
                        Text(
                            text = "No roles match your search.",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText
                        )
                    }
                }
            }

            else -> {
                rolesByArea.forEach { (area, roles) ->
                    item(key = "area_$area") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = area,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CreateGreen
                            )
                            Text(
                                text = "${roles.size} role${if (roles.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText
                            )
                        }
                    }

                    roles.forEach { role ->
                        item(key = role.roleTemplateId) {
                            val selected = selectedById[role.roleTemplateId]

                            RoleTemplateCard(
                                role = role,
                                selectedRole = selected,
                                totalAssignedForMode = if (
                                    role.roleMode == VolunteerRoleMode.PHYSICAL
                                ) {
                                    physicalAssigned
                                } else {
                                    remoteAssigned
                                },
                                requiredForMode = if (
                                    role.roleMode == VolunteerRoleMode.PHYSICAL
                                ) {
                                    draft.requiredPhysicalVolunteerTotal ?: 0
                                } else {
                                    draft.requiredRemoteVolunteerTotal ?: 0
                                },
                                onAdd = {
                                    viewModel.addRole(role.roleTemplateId)
                                },
                                onRemove = {
                                    viewModel.removeRole(role.roleTemplateId)
                                },
                                onDecrease = {
                                    viewModel.decreaseRoleCapacity(
                                        role.roleTemplateId
                                    )
                                },
                                onIncrease = {
                                    viewModel.increaseRoleCapacity(
                                        role.roleTemplateId
                                    )
                                },
                                onCapacityChanged = { value ->
                                    viewModel.updateRoleCapacity(
                                        roleTemplateId = role.roleTemplateId,
                                        text = value
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (viewModel.continueFromStepTwo()) {
                            onStepTwoComplete()
                        }
                    },
                    enabled = !uiState.isRoleCatalogueLoading &&
                            uiState.roleCatalogueError == null &&
                            catalogue.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Continue to Role Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (uiState.isStepTwoReady) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F7EE)
                    ) {
                        Text(
                            text = "Step 2 is complete and ready for role settings.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CreateGreen
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CreateStepTwoHeader(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onBack) {
            Image(
                painter = painterResource(R.drawable.back),
                contentDescription = "Back to Post Details",
                modifier = Modifier.size(30.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Create Volunteer Post",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CreateGreen
            )

            Text(
                text = "Step 2 of 5 · Select Roles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AllocationSummary(
    postType: VolunteerPostType?,
    physicalAssigned: Int,
    physicalRequired: Int?,
    remoteAssigned: Int,
    remoteRequired: Int?,
    physicalError: String?,
    remoteError: String?,
    generalError: String?
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = CreateCardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Volunteer Allocation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreateGreen
                )
                Text(
                    text = "Role capacities must match the requirement from Step 1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }

            when (postType) {
                VolunteerPostType.PHYSICAL -> {
                    AllocationRow(
                        label = "Physical volunteers",
                        assigned = physicalAssigned,
                        required = physicalRequired ?: 0,
                        error = physicalError
                    )
                }

                VolunteerPostType.REMOTE -> {
                    AllocationRow(
                        label = "Remote volunteers",
                        assigned = remoteAssigned,
                        required = remoteRequired ?: 0,
                        error = remoteError
                    )
                }

                VolunteerPostType.HYBRID -> {
                    AllocationRow(
                        label = "Physical volunteers",
                        assigned = physicalAssigned,
                        required = physicalRequired ?: 0,
                        error = physicalError
                    )

                    HorizontalDivider()

                    AllocationRow(
                        label = "Remote volunteers",
                        assigned = remoteAssigned,
                        required = remoteRequired ?: 0,
                        error = remoteError
                    )
                }

                null -> Unit
            }

            if (!generalError.isNullOrBlank()) {
                Text(
                    text = generalError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AllocationRow(
    label: String,
    assigned: Int,
    required: Int,
    error: String?
) {
    val safeProgress = if (required > 0) {
        (assigned.toFloat() / required.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$assigned / $required assigned",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (assigned == required && required > 0) {
                    CreateGreen
                } else {
                    MutedText
                }
            )
        }

        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = CreateGreen,
            trackColor = CreateLightGreen
        )

        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun RoleFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                textAlign = TextAlign.Center
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CreateLightGreen,
            selectedLabelColor = CreateGreen
        )
    )
}

@Composable
private fun RoleTemplateCard(
    role: CreateRoleTemplate,
    selectedRole: SelectedRoleDraft?,
    totalAssignedForMode: Int,
    requiredForMode: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onCapacityChanged: (String) -> Unit
) {
    val isSelected = selectedRole != null
    val otherSkills = role.skillsPractised.filterNot { skill ->
        role.recommendedSkills.any {
            it.skillId == skill.skillId
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 1.4.dp else 1.dp,
            color = if (isSelected) CreateGreen else RoleBorder
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                Color(0xFFFCFEFB)
            } else {
                CreateCardBackground
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CreateLightGreen
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                roleAreaIcon(role.roleArea)
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = role.roleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LevelBadge(level = role.defaultLevel)

                        Text(
                            text = "${role.roleMode.displayName} role",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedText
                        )
                    }
                }

                if (isSelected) {
                    TextButton(
                        onClick = onRemove,
                        contentPadding = PaddingValues(
                            horizontal = 4.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = "Remove",
                            color = DangerRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            RoleInformationLine(
                label = "Primary Skill Path",
                value = role.skillPathName,
                valueColor = MutedText
            )

            RoleInformationLine(
                label = "Recommended",
                value = role.recommendedSkills.joinToString(", ") { it.name }
                    .ifBlank { "None" },
                valueColor = CreateGreen
            )

            if (otherSkills.isNotEmpty()) {
                RoleInformationLine(
                    label = "Other available",
                    value = otherSkills.joinToString(", ") { it.name },
                    valueColor = MutedText
                )
            }

            if (selectedRole == null) {
                Button(
                    onClick = onAdd,
                    enabled = totalAssignedForMode < requiredForMode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("Add Role")
                }
            } else {
                HorizontalDivider(color = RoleBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Volunteers needed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Adjust this role's capacity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }

                    CapacityControl(
                        capacity = selectedRole.capacity,
                        canDecrease = selectedRole.capacity > 1,
                        canIncrease = totalAssignedForMode < requiredForMode,
                        onDecrease = onDecrease,
                        onIncrease = onIncrease,
                        onCapacityChanged = onCapacityChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleInformationLine(
    label: String,
    value: String,
    valueColor: Color
) {
    Text(
        text = "$label · $value",
        style = MaterialTheme.typography.bodySmall,
        color = valueColor
    )
}

@Composable
private fun LevelBadge(
    level: VolunteerRoleLevel
) {
    val background: Color
    val foreground: Color

    when (level) {
        VolunteerRoleLevel.BEGINNER -> {
            background = BeginnerBackground
            foreground = BeginnerText
        }

        VolunteerRoleLevel.INTERMEDIATE -> {
            background = IntermediateBackground
            foreground = IntermediateText
        }

        VolunteerRoleLevel.ADVANCED -> {
            background = AdvancedBackground
            foreground = AdvancedText
        }
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Text(
            text = level.displayName,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 3.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = foreground
        )
    }
}

@Composable
private fun CapacityControl(
    capacity: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onCapacityChanged: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(42.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(11.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.remove),
                contentDescription = "Decrease capacity",
                modifier = Modifier.size(18.dp)
            )
        }

        OutlinedTextField(
            value = if (capacity > 0) capacity.toString() else "",
            onValueChange = onCapacityChanged,
            modifier = Modifier.width(68.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            shape = RoundedCornerShape(11.dp)
        )

        OutlinedButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.size(42.dp),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(11.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.add),
                contentDescription = "Increase capacity",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LoadingCatalogueCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreateCardBackground,
        border = BorderStroke(1.dp, RoleBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Loading volunteer roles...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = CreateGreen,
                trackColor = CreateLightGreen
            )
        }
    }
}

@Composable
private fun CatalogueErrorCard(
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

private fun roleAreaIcon(roleArea: String): Int {
    return when (roleArea) {
        "General Event Support" ->
            R.drawable.role_general_support

        "Registration & Guest Support" ->
            R.drawable.role_registration

        "Logistics & Distribution" ->
            R.drawable.role_logistics

        "Crowd & Safety Support" ->
            R.drawable.role_safety

        "Community Engagement & Activity Support" ->
            R.drawable.role_community

        "Media & Event Documentation" ->
            R.drawable.role_media

        "Graphic & Visual Design" ->
            R.drawable.role_graphic_design

        "Writing & Content" ->
            R.drawable.role_writing

        "Social Media & Digital Campaigns" ->
            R.drawable.role_social_media

        "Research & Data" ->
            R.drawable.role_research_data

        "Administration & Documentation" ->
            R.drawable.role_administration

        "Digital & Technical Support" ->
            R.drawable.role_technical_support

        else ->
            R.drawable.role_general_support
    }
}
