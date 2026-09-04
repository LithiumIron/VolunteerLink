package com.example.volunteerlink.organisation.create.steps

// FILE OVERVIEW:
/*
 * ScheduleSections contains presentation code for the organisation Create/Edit Post flow.
 * It focuses on rendering state and forwarding user actions through callbacks/ViewModels,
 * keeping database access and business rules outside the composables where possible.
 */


import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.ui.theme.ScheduleBorder
import com.example.volunteerlink.ui.theme.ScheduleMuted
import com.example.volunteerlink.ui.theme.ScheduleWarning
import com.example.volunteerlink.ui.theme.ScheduleAttention
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.ui.theme.CreateCardBackground
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.ui.theme.ScheduleBorder
import com.example.volunteerlink.ui.theme.ScheduleMuted
import com.example.volunteerlink.ui.theme.ScheduleWarning
import com.example.volunteerlink.ui.theme.ScheduleAttention
import com.example.volunteerlink.ui.theme.CreateGreen
import com.example.volunteerlink.ui.theme.CreateLightGreen
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
/**
 * Renders the UI represented by schedule section selector for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun ScheduleSectionSelector(
    sections: List<ScheduleType>,
    selectedSection: ScheduleType,
    itemCounts: Map<ScheduleType, Int>,
    onSectionSelected: (ScheduleType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CreateLightGreen
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            sections.forEach { section ->
                val selected = section == selectedSection
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSectionSelected(section) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) CreateGreen else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 10.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = when (section) {
                                ScheduleType.PHYSICAL -> "Physical"
                                ScheduleType.REMOTE -> "Remote"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else CreateGreen,
                            maxLines = 1
                        )
                        Text(
                            text = "${itemCounts[section] ?: 0} saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) {
                                Color.White.copy(alpha = 0.82f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the physical schedule section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun PhysicalScheduleSection(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    physicalDates: List<Long>,
    selectedDate: Long,
    getItemError: (String) -> String?,
    canAddItem: Boolean = true,
    canEditItem: (String) -> Boolean = { true },
    canDeleteItem: (String) -> Boolean = { true },
    getItemLockedReason: (String) -> String? = { null },
    onDateSelected: (Long) -> Unit,
    onAddItem: (Long) -> String?,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onCopyDay: (Long) -> Unit,
    canCopyDay: (Long) -> Boolean
) {
    val items = draft.scheduleItems
        .filter { item ->
            item.scheduleType == ScheduleType.PHYSICAL &&
                item.scheduleDateMillis?.let(
                    CreatePostValidator::startOfDayMillis
                ) == CreatePostValidator.startOfDayMillis(selectedDate)
        }
        .sortedBy { item -> item.startTimeMinutes ?: Int.MAX_VALUE }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScheduleSectionIntro(
            iconRes = R.drawable.physical_event,
            title = "Physical Schedule",
            subtitle = "Build the event timetable one day at a time. Overlaps are checked only when the same role is affected."
        )

        ScheduleContextCard(
            primary = physicalDateRangeText(draft),
            secondary = buildString {
                append(scheduleTimeRangeText(
                    draft.physicalStartTimeMinutes,
                    draft.physicalEndTimeMinutes
                ))
                draft.physicalLocation?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { location -> append("  ·  $location") }
            }
        )

        if (physicalDates.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                physicalDates.forEach { date ->
                    ScheduleDateChip(
                        dateMillis = date,
                        selected = CreatePostValidator.startOfDayMillis(date) ==
                            CreatePostValidator.startOfDayMillis(selectedDate),
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatDayHeading(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreateGreen
                )
                Text(
                    text = "Event time · ${scheduleTimeRangeText(
                        draft.physicalStartTimeMinutes,
                        draft.physicalEndTimeMinutes
                    )}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${items.size} ${if (items.size == 1) "activity" else "activities"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (physicalDates.size > 1 && items.isNotEmpty()) {
                TextButton(
                    onClick = { onCopyDay(selectedDate) },
                    enabled = canAddItem && canCopyDay(selectedDate)
                ) {
                    Text("Copy Day")
                }
            }
        }

        if (items.isEmpty()) {
            EmptyScheduleState(
                text = "No Physical activities have been added for this day yet."
            )
        } else {
            items.forEach { item ->
                ScheduleOverviewItemCard(
                    draft = draft,
                    roleCatalogue = roleCatalogue,
                    item = item,
                    error = getItemError(item.draftId),
                    warning = null,
                    canEdit = canEditItem(item.draftId),
                    canDelete = canDeleteItem(item.draftId),
                    lockedReason = getItemLockedReason(item.draftId),
                    onEdit = { onEditItem(item.draftId) },
                    onDelete = { onDeleteItem(item.draftId) }
                )
            }

            if (!canCopyDay(selectedDate) && physicalDates.size > 1) {
                Text(
                    text = "Fix items marked Needs attention before copying this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        AddScheduleButton(
            text = "Add Physical Activity",
            enabled = canAddItem,
            onClick = { onAddItem(selectedDate) }
        )
    }
}

@Composable
/**
 * Renders the remote schedule section section used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun RemoteScheduleSection(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    getItemError: (String) -> String?,
    canAddItem: Boolean = true,
    canEditItem: (String) -> Boolean = { true },
    canDeleteItem: (String) -> Boolean = { true },
    getItemLockedReason: (String) -> String? = { null },
    onAddItem: () -> String?,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val items = draft.scheduleItems
        .filter { item -> item.scheduleType == ScheduleType.REMOTE }
        .sortedBy { item -> item.scheduleDateMillis ?: Long.MAX_VALUE }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScheduleSectionIntro(
            iconRes = R.drawable.remote_project,
            title = "Remote Schedule",
            subtitle = "Add date-based milestones or checkpoints so Remote volunteers know what should be ready and by when."
        )

        ScheduleContextCard(
            primary = remoteDateRangeText(draft),
            secondary = "Milestones do not need a clock time or physical location."
        )

        if (items.isEmpty()) {
            EmptyScheduleState(
                text = "No Remote milestones have been added yet."
            )
        } else {
            items.forEach { item ->
                ScheduleOverviewItemCard(
                    draft = draft,
                    roleCatalogue = roleCatalogue,
                    item = item,
                    error = getItemError(item.draftId),
                    warning = null,
                    canEdit = canEditItem(item.draftId),
                    canDelete = canDeleteItem(item.draftId),
                    lockedReason = getItemLockedReason(item.draftId),
                    onEdit = { onEditItem(item.draftId) },
                    onDelete = { onDeleteItem(item.draftId) }
                )
            }
        }

        AddScheduleButton(
            text = "Add Remote Milestone",
            enabled = canAddItem,
            onClick = { onAddItem() }
        )
    }
}

@Composable
/**
 * Renders the copy physical day dialog dialog used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
fun CopyPhysicalDayDialog(
    sourceDate: Long,
    targetDates: List<Long>,
    targetHasItems: (Long) -> Boolean,
    onDismiss: () -> Unit,
    onTargetSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Copy ${formatScheduleDate(sourceDate)}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose another Physical event day. Copied activities receive new draft IDs.",
                    style = MaterialTheme.typography.bodyMedium
                )

                targetDates.forEach { date ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTargetSelected(date) }
                            .border(
                                width = 1.dp,
                                color = ScheduleBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatScheduleDate(date),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (targetHasItems(date)) {
                                    "Replace"
                                } else {
                                    "Copy here"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = CreateGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
/**
 * Renders the UI represented by schedule item editor for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun ScheduleItemEditor(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    when (item.scheduleType) {
        ScheduleType.PHYSICAL -> PhysicalScheduleItemEditor(
            uiState = uiState,
            item = item,
            viewModel = viewModel
        )

        ScheduleType.REMOTE -> RemoteScheduleItemEditor(
            uiState = uiState,
            item = item,
            viewModel = viewModel
        )
    }
}

@Composable
/**
 * Renders the UI represented by physical schedule item editor for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun PhysicalScheduleItemEditor(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorGroup(
            title = "Activity Details",
            subtitle = "The activity stays on the selected Physical event day."
        ) {
            ReadOnlyScheduleValue(
                label = "Event Day",
                value = item.scheduleDateMillis?.let(::formatScheduleDate)
                    ?: "Not set"
            )

            OutlinedTextField(
                value = item.title,
                onValueChange = viewModel::updateScheduleEditorTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Activity Name *") },
                placeholder = { Text("Example: Volunteer registration") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeSelectionField(
                    label = "Start Time *",
                    selectedTimeMinutes = item.startTimeMinutes,
                    dialogInitialTimeMinutes = item.startTimeMinutes
                        ?: uiState.draft.physicalStartTimeMinutes,
                    onTimeSelected = viewModel::updateScheduleEditorStartTime,
                    modifier = Modifier.weight(1f)
                )
                TimeSelectionField(
                    label = "End Time *",
                    selectedTimeMinutes = item.endTimeMinutes,
                    dialogInitialTimeMinutes = item.endTimeMinutes
                        ?: uiState.draft.physicalEndTimeMinutes,
                    onTimeSelected = viewModel::updateScheduleEditorEndTime,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = item.location,
                onValueChange = viewModel::updateScheduleEditorLocation,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Activity Location (Optional)") },
                placeholder = { Text("Leave blank to use the main event location") },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp)
            )
        }

        RoleTargetEditor(
            draft = uiState.draft,
            roleCatalogue = uiState.roleCatalogue,
            item = item,
            onAppliesToAllChanged = viewModel::updateScheduleEditorAppliesToAll,
            onRoleToggled = viewModel::toggleScheduleEditorRole
        )

        NotesEditor(
            value = item.notes,
            onValueChanged = viewModel::updateScheduleEditorNotes
        )
    }
}

@Composable
/**
 * Renders the UI represented by remote schedule item editor for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RemoteScheduleItemEditor(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    val minimumDate = uiState.draft.remoteStartDateMillis
        ?.let(CreatePostValidator::startOfDayMillis)
        ?: CreatePostValidator.startOfDayMillis()
    val maximumDate = uiState.draft.remoteDueDateMillis
        ?.let(CreatePostValidator::startOfDayMillis)

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorGroup(
            title = "Milestone Details",
            subtitle = "Remote milestones are date-based checkpoints, not timetable blocks."
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = viewModel::updateScheduleEditorTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Milestone / Checkpoint *") },
                placeholder = { Text("Example: First draft ready") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            DateSelectionField(
                label = "Date *",
                selectedDateMillis = item.scheduleDateMillis,
                minimumDateMillis = minimumDate,
                maximumDateMillis = maximumDate,
                onDateSelected = viewModel::updateScheduleEditorDate,
                modifier = Modifier.fillMaxWidth()
            )
        }

        RoleTargetEditor(
            draft = uiState.draft,
            roleCatalogue = uiState.roleCatalogue,
            item = item,
            onAppliesToAllChanged = viewModel::updateScheduleEditorAppliesToAll,
            onRoleToggled = viewModel::toggleScheduleEditorRole
        )

        NotesEditor(
            value = item.notes,
            onValueChanged = viewModel::updateScheduleEditorNotes
        )
    }
}

@Composable
/**
 * Renders the UI represented by role target editor for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun RoleTargetEditor(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft,
    onAppliesToAllChanged: (Boolean) -> Unit,
    onRoleToggled: (String) -> Unit
) {
    val applicableIds = CreatePostValidator.applicableScheduleRoleIds(
        draft = draft,
        scheduleType = item.scheduleType,
        roleCatalogue = roleCatalogue
    )
    val templatesById = roleCatalogue.associateBy { template ->
        template.roleTemplateId
    }
    val roles = applicableIds.mapNotNull { roleId ->
        templatesById[roleId]
    }

    EditorGroup(
        title = "Applies To",
        subtitle = when (item.scheduleType) {
            ScheduleType.PHYSICAL -> "Choose the Physical roles involved in this activity."
            ScheduleType.REMOTE -> "Choose the Remote roles affected by this milestone."
        }
    ) {
        if (roles.size <= 1) {
            roles.firstOrNull()?.let { role ->
                ReadOnlyScheduleValue(
                    label = "Role",
                    value = role.roleName
                )
            }
        } else {
            ScheduleChoiceOption(
                title = when (item.scheduleType) {
                    ScheduleType.PHYSICAL -> "All Physical Roles"
                    ScheduleType.REMOTE -> "All Remote Roles"
                },
                description = "Apply this schedule item to every applicable selected role.",
                selected = item.appliesToAllRoles,
                onClick = { onAppliesToAllChanged(true) }
            )
            ScheduleChoiceOption(
                title = "Selected Roles",
                description = "Choose only the roles that need this schedule item.",
                selected = !item.appliesToAllRoles,
                onClick = { onAppliesToAllChanged(false) }
            )

            if (!item.appliesToAllRoles) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.forEach { role ->
                        RoleCheckRow(
                            role = role,
                            checked = role.roleTemplateId in item.targetRoleTemplateIds,
                            onCheckedChange = {
                                onRoleToggled(role.roleTemplateId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
/**
 * Renders the role check row row used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun RoleCheckRow(
    role: CreateRoleTemplate,
    checked: Boolean,
    enabled: Boolean = true,
    supportingText: String? = null,
    onCheckedChange: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onCheckedChange
            ),
        shape = RoundedCornerShape(12.dp),
        color = ScheduleMuted
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { onCheckedChange() }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = role.roleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = if (role.roleMode == VolunteerRoleMode.PHYSICAL) {
                        "Physical"
                    } else {
                        "Remote"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                supportingText?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
/**
 * Renders the UI represented by notes editor for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun NotesEditor(
    value: String,
    onValueChanged: (String) -> Unit
) {
    EditorGroup(
        title = "Notes",
        subtitle = "Optional details volunteers should know about this schedule item."
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (Optional)") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
/**
 * Renders the UI represented by schedule choice option for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ScheduleChoiceOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (selected) CreateGreen else ScheduleBorder
    val background = if (selected) CreateLightGreen else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (enabled) borderColor else ScheduleBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(13.dp),
        color = background.copy(alpha = if (enabled) 1f else 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
/**
 * Renders the UI represented by editor group for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun EditorGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                ScheduleBorder,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreateGreen
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
/**
 * Renders the UI represented by read only schedule value for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ReadOnlyScheduleValue(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ScheduleMuted
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by schedule section intro for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun ScheduleSectionIntro(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CreateLightGreen
        ) {
            Box(
                modifier = Modifier.padding(9.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the schedule context card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ScheduleContextCard(
    primary: String,
    secondary: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CreateLightGreen
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = CreateGreen
            )
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
/**
 * Renders the UI represented by empty schedule state for the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun EmptyScheduleState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ScheduleMuted
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
/**
 * Renders the add schedule button button used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun AddScheduleButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.add),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
/**
 * Renders the schedule date chip chip used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ScheduleDateChip(
    dateMillis: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = if (selected) CreateGreen else ScheduleBorder,
                shape = RoundedCornerShape(13.dp)
            ),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) CreateLightGreen else Color.White
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = SimpleDateFormat(
                    "EEE",
                    Locale.ENGLISH
                ).format(Date(dateMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = SimpleDateFormat(
                    "dd MMM",
                    Locale.ENGLISH
                ).format(Date(dateMillis)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
/**
 * Renders the schedule overview item card card used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun ScheduleOverviewItemCard(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft,
    error: String?,
    warning: String?,
    canEdit: Boolean = true,
    canDelete: Boolean = true,
    lockedReason: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val outline = when {
        error != null -> MaterialTheme.colorScheme.error
        warning != null -> Color(0xFFD68A27)
        else -> ScheduleBorder
    }
    val background = when {
        error != null -> ScheduleAttention
        warning != null -> ScheduleWarning
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                outline,
                RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        color = background
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = itemEyebrow(item),
                        style = MaterialTheme.typography.labelMedium,
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (error != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Needs attention",
                            modifier = Modifier.padding(
                                horizontal = 9.dp,
                                vertical = 5.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = roleSummary(
                    draft = draft,
                    roleCatalogue = roleCatalogue,
                    item = item
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            itemLocationOrFormatText(item)
                ?.takeIf { text -> text.isNotBlank() }
                ?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (warning != null) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A4B08),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if ((!canEdit || !canDelete) && !lockedReason.isNullOrBlank()) {
                Text(
                    text = lockedReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    enabled = canEdit
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete,
                    enabled = canDelete
                ) {
                    Text(
                        text = "Delete",
                        color = if (canDelete) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Derives the item eyebrow value used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun itemEyebrow(item: ScheduleItemDraft): String {
    return when (item.scheduleType) {
        ScheduleType.PHYSICAL -> scheduleTimeRangeText(
            item.startTimeMinutes,
            item.endTimeMinutes
        )

        ScheduleType.REMOTE -> item.scheduleDateMillis
            ?.let(::formatScheduleDate)
            ?: "Date not set"

    }
}

/**
 * Renders the role summary summary block used in the organisation Create/Edit Post flow.
 * It receives state and callbacks from its caller so presentation code stays separate from database operations.
 */
private fun roleSummary(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft
): String {
    if (item.appliesToAllRoles) {
        return when (item.scheduleType) {
            ScheduleType.PHYSICAL -> "All Physical Roles"
            ScheduleType.REMOTE -> "All Remote Roles"
        }
    }

    val namesById = roleCatalogue.associate { template ->
        template.roleTemplateId to template.roleName
    }
    val selectedStillInPost = draft.selectedRoles
        .map { role -> role.roleTemplateId }
        .toSet()

    return item.targetRoleTemplateIds
        .filter { roleId -> roleId in selectedStillInPost }
        .mapNotNull { roleId -> namesById[roleId] }
        .joinToString(" · ")
        .ifBlank { "Selected roles need review" }
}

/**
 * Returns the item location or format text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun itemLocationOrFormatText(
    item: ScheduleItemDraft
): String? {
    return when (item.scheduleType) {
        ScheduleType.PHYSICAL -> item.location
            .takeIf { it.isNotBlank() }
            ?: "Main event location"
        ScheduleType.REMOTE -> null
    }
}

/**
 * Returns the physical date range text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun physicalDateRangeText(draft: CreatePostDraft): String {
    val start = draft.physicalStartDateMillis ?: return "Physical dates not set"
    val end = if (draft.isMultiDayPhysicalEvent) {
        draft.physicalEndDateMillis ?: start
    } else {
        start
    }

    return if (
        CreatePostValidator.startOfDayMillis(start) ==
        CreatePostValidator.startOfDayMillis(end)
    ) {
        formatScheduleDate(start)
    } else {
        "${formatScheduleDate(start)} – ${formatScheduleDate(end)}"
    }
}

/**
 * Returns the remote date range text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun remoteDateRangeText(draft: CreatePostDraft): String {
    val start = draft.remoteStartDateMillis ?: return "Remote dates not set"
    val due = draft.remoteDueDateMillis ?: return formatScheduleDate(start)
    return "${formatScheduleDate(start)} – ${formatScheduleDate(due)}"
}

/**
 * Returns the schedule time range text used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
private fun scheduleTimeRangeText(
    startMinutes: Int?,
    endMinutes: Int?
): String {
    return "${formatScheduleTime(startMinutes)} – ${formatScheduleTime(endMinutes)}"
}

/**
 * Formats the schedule time used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatScheduleTime(minutes: Int?): String {
    if (minutes == null) return "Not set"

    val hour24 = minutes / 60
    val minute = minutes % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val adjusted = hour24 % 12) {
        0 -> 12
        else -> adjusted
    }
    return String.format(
        Locale.ENGLISH,
        "%d:%02d %s",
        hour12,
        minute,
        suffix
    )
}

/**
 * Formats the day heading used by the organisation Create/Edit Post flow.
 * Keeping this helper close to the screen makes the presentation logic easier to follow while business rules remain outside Compose.
 */
fun formatDayHeading(dateMillis: Long): String {
    return SimpleDateFormat(
        "EEE, dd MMM",
        Locale.ENGLISH
    ).format(Date(dateMillis)).uppercase(Locale.ENGLISH)
}
