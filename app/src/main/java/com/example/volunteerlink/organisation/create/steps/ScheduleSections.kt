package com.example.volunteerlink.organisation.create.steps

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateLightGreen
import com.example.volunteerlink.organisation.create.components.DateSelectionField
import com.example.volunteerlink.organisation.create.components.LocationAutocompleteField
import com.example.volunteerlink.organisation.create.components.TimeSelectionField
import com.example.volunteerlink.organisation.create.model.CreatePostDraft
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.CreateRoleTemplate
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.TrainingLocationMode
import com.example.volunteerlink.organisation.create.model.TrainingMode
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.create.model.VolunteerRoleMode
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScheduleBorder = Color(0xFFDCE5D8)
private val ScheduleMuted = Color(0xFFF7F9F6)
private val ScheduleAttention = Color(0xFFFFE9E7)
private val ScheduleWarning = Color(0xFFFFF4E5)

@Composable
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
                                ScheduleType.TRAINING -> "Training"
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
fun PhysicalScheduleSection(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    physicalDates: List<Long>,
    selectedDate: Long,
    getItemError: (String) -> String?,
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
                    enabled = canCopyDay(selectedDate)
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
            onClick = { onAddItem(selectedDate) }
        )
    }
}

@Composable
fun RemoteScheduleSection(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    getItemError: (String) -> String?,
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
                    onEdit = { onEditItem(item.draftId) },
                    onDelete = { onDeleteItem(item.draftId) }
                )
            }
        }

        AddScheduleButton(
            text = "Add Remote Milestone",
            onClick = { onAddItem() }
        )
    }
}

@Composable
fun TrainingScheduleSection(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    getItemError: (String) -> String?,
    getItemWarning: (String) -> String?,
    onAddItem: () -> String?,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val items = draft.scheduleItems
        .filter { item -> item.scheduleType == ScheduleType.TRAINING }
        .sortedWith(
            compareBy<ScheduleItemDraft> { item ->
                item.scheduleDateMillis ?: Long.MAX_VALUE
            }.thenBy { item -> item.startTimeMinutes ?: Int.MAX_VALUE }
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScheduleSectionIntro(
            iconRes = R.drawable.instructions,
            title = "Training & Briefing",
            subtitle = "Add preparation sessions for any selected roles. Training can be Online or On-site, including for Remote opportunities."
        )

        ScheduleContextCard(
            primary = "Optional preparation",
            secondary = "A location or online meeting link may be TBA while training is more than 3 days away. Confirm the detail volunteers need to attend before the 3-day point."
        )

        if (items.isEmpty()) {
            EmptyScheduleState(
                text = "No Training or Briefing session has been added yet."
            )
        } else {
            items.forEach { item ->
                ScheduleOverviewItemCard(
                    draft = draft,
                    roleCatalogue = roleCatalogue,
                    item = item,
                    error = getItemError(item.draftId),
                    warning = getItemWarning(item.draftId),
                    onEdit = { onEditItem(item.draftId) },
                    onDelete = { onDeleteItem(item.draftId) }
                )
            }
        }

        AddScheduleButton(
            text = "Add Training / Briefing",
            onClick = { onAddItem() }
        )
    }
}

@Composable
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

        ScheduleType.TRAINING -> TrainingScheduleItemEditor(
            uiState = uiState,
            item = item,
            viewModel = viewModel
        )
    }
}

@Composable
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
private fun TrainingScheduleItemEditor(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    val clockState by AppClock.state.collectAsStateWithLifecycle()
    val today = remember(clockState.refreshVersion) {
        CreatePostValidator.startOfDayMillis()
    }
    val maximumDate = listOfNotNull(
        uiState.draft.physicalEndDateMillis
            ?: uiState.draft.physicalStartDateMillis,
        uiState.draft.remoteDueDateMillis
    )
        .map(CreatePostValidator::startOfDayMillis)
        .maxOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorGroup(
            title = "Training Details",
            subtitle = "Training may happen before the volunteering work begins, but it must have a confirmed date and time."
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = viewModel::updateScheduleEditorTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Training / Briefing Title *") },
                placeholder = { Text("Example: Volunteer safety briefing") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            DateSelectionField(
                label = "Date *",
                selectedDateMillis = item.scheduleDateMillis,
                minimumDateMillis = today,
                maximumDateMillis = maximumDate,
                onDateSelected = viewModel::updateScheduleEditorDate,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeSelectionField(
                    label = "Start Time *",
                    selectedTimeMinutes = item.startTimeMinutes,
                    onTimeSelected = viewModel::updateScheduleEditorStartTime,
                    modifier = Modifier.weight(1f)
                )
                TimeSelectionField(
                    label = "End Time *",
                    selectedTimeMinutes = item.endTimeMinutes,
                    onTimeSelected = viewModel::updateScheduleEditorEndTime,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (CreatePostValidator.trainingStartsWithinShortNotice(item)) {
            ScheduleReminderCard(
                message = "Short-notice training: this session starts within 3 days. Confirm the details volunteers need to attend before saving."
            )
        }

        EditorGroup(
            title = "Format",
            subtitle = "Choose how volunteers will attend this preparation session."
        ) {
            ScheduleChoiceOption(
                title = "Online",
                description = "Volunteers join through an online platform.",
                selected = item.trainingMode == TrainingMode.ONLINE,
                onClick = { viewModel.updateTrainingMode(TrainingMode.ONLINE) }
            )
            ScheduleChoiceOption(
                title = "On-site",
                description = "Volunteers attend a physical training location.",
                selected = item.trainingMode == TrainingMode.ONSITE,
                onClick = { viewModel.updateTrainingMode(TrainingMode.ONSITE) }
            )

            when (item.trainingMode) {
                TrainingMode.ONLINE -> OnlineTrainingFields(
                    uiState = uiState,
                    item = item,
                    viewModel = viewModel
                )

                TrainingMode.ONSITE -> OnsiteTrainingFields(
                    uiState = uiState,
                    item = item,
                    viewModel = viewModel
                )

                null -> Unit
            }
        }

        RoleTargetEditor(
            draft = uiState.draft,
            roleCatalogue = uiState.roleCatalogue,
            item = item,
            onAppliesToAllChanged = viewModel::updateScheduleEditorAppliesToAll,
            onRoleToggled = viewModel::toggleScheduleEditorRole
        )

        TrainingApplicationClosingEditor(
            draft = uiState.draft,
            roleCatalogue = uiState.roleCatalogue,
            item = item,
            viewModel = viewModel
        )

        NotesEditor(
            value = item.notes,
            onValueChanged = viewModel::updateScheduleEditorNotes
        )
    }
}

@Composable
private fun OnlineTrainingFields(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    val shortNotice = CreatePostValidator.trainingStartsWithinShortNotice(item)
    val missingLink = item.meetingLink.isBlank()

    HorizontalDivider(color = ScheduleBorder)

    OutlinedTextField(
        value = item.onlinePlatform,
        onValueChange = viewModel::updateTrainingOnlinePlatform,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Online Platform (Optional)") },
        placeholder = { Text("Example: Google Meet") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )

    OutlinedTextField(
        value = item.meetingLink,
        onValueChange = viewModel::updateTrainingMeetingLink,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(if (shortNotice) "Meeting Link *" else "Meeting Link (Optional for now)")
        },
        placeholder = {
            Text(if (shortNotice) "Add the confirmed meeting link" else "Can be confirmed later")
        },
        isError = uiState.showScheduleErrors && shortNotice && missingLink,
        supportingText = {
            when {
                uiState.showScheduleErrors && shortNotice && missingLink ->
                    Text("The meeting link is required because this training starts within 3 days.")

                !shortNotice && missingLink ->
                    Text("You may leave this blank for now, but confirm the link at least 3 days before training.")
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )

    if (!shortNotice && missingLink) {
        ScheduleReminderCard(
            message = "Meeting link is still TBA. VolunteerLink will require it once the training is within 3 days."
        )
    } else if (shortNotice && missingLink) {
        ScheduleInlineErrorCard(
            message = "Add the online meeting link before saving this training."
        )
    }

    DisabledTimeZoneField()
}

@Composable
private fun OnsiteTrainingFields(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    val hasEventLocation =
        (uiState.draft.postType == VolunteerPostType.PHYSICAL ||
            uiState.draft.postType == VolunteerPostType.HYBRID) &&
            uiState.draft.physicalLocation != null
    val shortNotice = CreatePostValidator.trainingStartsWithinShortNotice(item)

    HorizontalDivider(color = ScheduleBorder)

    Text(
        text = "Training Location *",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = CreateGreen
    )

    if (hasEventLocation) {
        val eventLocation = uiState.draft.physicalLocation?.displayName.orEmpty()
        ScheduleChoiceOption(
            title = "Same as event location",
            description = eventLocation,
            selected = item.trainingLocationMode == TrainingLocationMode.EVENT_LOCATION,
            onClick = {
                viewModel.updateTrainingLocationMode(
                    TrainingLocationMode.EVENT_LOCATION
                )
            }
        )
    }

    ScheduleChoiceOption(
        title = "Choose another location",
        description = "Search and select a real venue or address with Geoapify.",
        selected = item.trainingLocationMode == TrainingLocationMode.CUSTOM,
        onClick = {
            viewModel.updateTrainingLocationMode(TrainingLocationMode.CUSTOM)
        }
    )

    if (item.trainingLocationMode == TrainingLocationMode.CUSTOM) {
        LocationAutocompleteField(
            query = item.trainingLocationQuery,
            selectedLocation = item.trainingLocation,
            suggestions = uiState.trainingLocationSuggestions,
            isSearching = uiState.isTrainingLocationSearching,
            searchError = uiState.trainingLocationSearchError,
            validationError = if (
                uiState.showScheduleErrors &&
                item.trainingLocation == null
            ) {
                "Select a location from the suggestions."
            } else {
                null
            },
            label = "Training Location",
            placeholder = "Search a venue or address",
            onQueryChanged = viewModel::onTrainingLocationQueryChanged,
            onLocationSelected = viewModel::onTrainingLocationSelected,
            onClearLocation = viewModel::clearTrainingLocation
        )
    }

    ScheduleChoiceOption(
        title = "To be confirmed",
        description = if (shortNotice) {
            "Unavailable because this session starts within 3 days."
        } else {
            "The location can be confirmed later while the session is still more than 3 days away."
        },
        selected = item.trainingLocationMode == TrainingLocationMode.TBA,
        enabled = !shortNotice,
        onClick = {
            viewModel.updateTrainingLocationMode(TrainingLocationMode.TBA)
        }
    )

    if (item.trainingLocationMode == TrainingLocationMode.TBA) {
        if (shortNotice) {
            ScheduleInlineErrorCard(
                message = "Confirm the on-site training location before saving. The session starts within 3 days."
            )
        } else {
            ScheduleReminderCard(
                message = "Location is still TBA. VolunteerLink will require a confirmed location once the training is within 3 days."
            )
        }
    }

    if (
        uiState.draft.postType == VolunteerPostType.REMOTE &&
        item.trainingLocationMode == TrainingLocationMode.TBA
    ) {
        DisabledTimeZoneField()
    }
}

@Composable
private fun ScheduleReminderCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ScheduleWarning
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A4B08),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScheduleInlineErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ScheduleAttention
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DisabledTimeZoneField() {
    OutlinedTextField(
        value = "Not enabled yet",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
        label = { Text("Time Zone") },
        supportingText = {
            Text("Timezone support is postponed for now and will be stored as NULL.")
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
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
            ScheduleType.TRAINING -> "Training may target Physical roles, Remote roles, or both."
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
                    ScheduleType.TRAINING -> "All Selected Roles"
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
private fun TrainingApplicationClosingEditor(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    var pendingMoveRoleId by remember(item.draftId) {
        mutableStateOf<String?>(null)
    }

    val applicableIds = CreatePostValidator.applicableScheduleRoleIds(
        draft = draft,
        scheduleType = ScheduleType.TRAINING,
        roleCatalogue = roleCatalogue
    )
    val targetedIds = if (item.appliesToAllRoles) {
        applicableIds
    } else {
        item.targetRoleTemplateIds
            .filter { roleId -> roleId in applicableIds }
            .distinct()
    }

    val rolesById = roleCatalogue.associateBy { role ->
        role.roleTemplateId
    }
    val targetedRoles = targetedIds.mapNotNull { roleId ->
        rolesById[roleId]
    }

    EditorGroup(
        title = "Application Closing",
        subtitle = "Each role can have only one training responsible for closing new applications or Instant Joins. A role may still attend other trainings."
    ) {
        if (targetedRoles.isEmpty()) {
            Text(
                text = "Choose at least one role in Applies To first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            targetedRoles.forEach { role ->
                val roleId = role.roleTemplateId
                val checked = roleId in item.closingRoleTemplateIds
                val otherCutoff = viewModel.otherTrainingApplicationCutoff(roleId)
                val currentStart = trainingStartOrderValue(item)
                val otherStart = otherCutoff?.let(::trainingStartOrderValue)
                val currentIsEarlier = otherCutoff != null &&
                    currentStart != null &&
                    otherStart != null &&
                    currentStart < otherStart
                val canMoveEarlier = !checked && currentIsEarlier

                val enabled = checked || otherCutoff == null || canMoveEarlier

                val supportingText = when {
                    checked && otherCutoff != null && currentIsEarlier ->
                        "Will move the cutoff from ${trainingCutoffText(otherCutoff)} to this earlier training when you save."

                    checked && otherCutoff != null ->
                        "This selection is no longer earlier than the existing cutoff. Uncheck it or move this training earlier before saving."

                    otherCutoff == null -> null

                    currentStart == null || otherStart == null ->
                        "${role.roleName} already has an application cutoff. Set this training's date and start time before deciding whether to move it."

                    canMoveEarlier ->
                        "Currently closes ${trainingCutoffText(otherCutoff)}. Selecting this will move the cutoff to this earlier training."

                    else ->
                        "Already closes ${trainingCutoffText(otherCutoff)}. This later training cannot create another cutoff."
                }

                RoleCheckRow(
                    role = role,
                    checked = checked,
                    enabled = enabled,
                    supportingText = supportingText,
                    onCheckedChange = {
                        when {
                            checked -> viewModel.toggleTrainingClosingRole(roleId)
                            otherCutoff == null ->
                                viewModel.toggleTrainingClosingRole(roleId)
                            canMoveEarlier -> pendingMoveRoleId = roleId
                        }
                    }
                )
            }

            Text(
                text = "If a role already has a cutoff, later trainings cannot select it again. An earlier training may take over only after you confirm the move.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    pendingMoveRoleId?.let { roleId ->
        val role = rolesById[roleId]
        val oldCutoff = viewModel.otherTrainingApplicationCutoff(roleId)

        if (role != null && oldCutoff != null) {
            AlertDialog(
                onDismissRequest = { pendingMoveRoleId = null },
                title = { Text("Move application cutoff?") },
                text = {
                    Text(
                        "${role.roleName} currently stops accepting new applications ${trainingCutoffText(oldCutoff)}. Moving it here will make this earlier training the only application-closing training for that role."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.moveTrainingApplicationCutoff(roleId)
                            pendingMoveRoleId = null
                        }
                    ) {
                        Text("Move Cutoff")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingMoveRoleId = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun trainingStartOrderValue(item: ScheduleItemDraft): Long? {
    val date = item.scheduleDateMillis ?: return null
    val startMinutes = item.startTimeMinutes ?: return null
    return CreatePostValidator.startOfDayMillis(date) +
        startMinutes * 60L * 1000L
}

private fun trainingCutoffText(item: ScheduleItemDraft): String {
    val date = item.scheduleDateMillis?.let(::formatScheduleDate) ?: "on an unset date"
    val time = formatScheduleTime(item.startTimeMinutes)
    val title = item.title.ifBlank { "another training" }
    return "on $date at $time from \"$title\""
}

@Composable
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
private fun AddScheduleButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
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
                    Locale.getDefault()
                ).format(Date(dateMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = SimpleDateFormat(
                    "dd MMM",
                    Locale.getDefault()
                ).format(Date(dateMillis)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) CreateGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ScheduleOverviewItemCard(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft,
    error: String?,
    warning: String?,
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

            itemLocationOrFormatText(draft, item)
                ?.takeIf { text -> text.isNotBlank() }
                ?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            trainingApplicationClosingSummary(
                roleCatalogue = roleCatalogue,
                item = item
            )?.let { text ->
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun itemEyebrow(item: ScheduleItemDraft): String {
    return when (item.scheduleType) {
        ScheduleType.PHYSICAL -> scheduleTimeRangeText(
            item.startTimeMinutes,
            item.endTimeMinutes
        )

        ScheduleType.REMOTE -> item.scheduleDateMillis
            ?.let(::formatScheduleDate)
            ?: "Date not set"

        ScheduleType.TRAINING -> buildString {
            item.scheduleDateMillis?.let { date ->
                append(formatScheduleDate(date))
            }
            if (item.startTimeMinutes != null || item.endTimeMinutes != null) {
                if (isNotEmpty()) append("  ·  ")
                append(scheduleTimeRangeText(
                    item.startTimeMinutes,
                    item.endTimeMinutes
                ))
            }
        }.ifBlank { "Training schedule" }
    }
}

private fun roleSummary(
    draft: CreatePostDraft,
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft
): String {
    if (item.appliesToAllRoles) {
        return when (item.scheduleType) {
            ScheduleType.PHYSICAL -> "All Physical Roles"
            ScheduleType.REMOTE -> "All Remote Roles"
            ScheduleType.TRAINING -> "All Selected Roles"
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

private fun trainingApplicationClosingSummary(
    roleCatalogue: List<CreateRoleTemplate>,
    item: ScheduleItemDraft
): String? {
    if (item.scheduleType != ScheduleType.TRAINING) return null

    if (item.closingRoleTemplateIds.isEmpty()) {
        return "Applications: no cutoff from this training"
    }

    val roleNamesById = roleCatalogue.associate { role ->
        role.roleTemplateId to role.roleName
    }
    val names = item.closingRoleTemplateIds
        .distinct()
        .mapNotNull { roleId -> roleNamesById[roleId] }

    return if (names.isEmpty()) {
        "Applications: closing roles need review"
    } else {
        "Closes applications on start: ${names.joinToString(" · ")}"
    }
}

private fun itemLocationOrFormatText(
    draft: CreatePostDraft,
    item: ScheduleItemDraft
): String? {
    return when (item.scheduleType) {
        ScheduleType.PHYSICAL -> item.location
            .takeIf { it.isNotBlank() }
            ?: "Main event location"

        ScheduleType.REMOTE -> null

        ScheduleType.TRAINING -> when (item.trainingMode) {
            TrainingMode.ONLINE -> buildString {
                append("Online")
                item.onlinePlatform.takeIf { it.isNotBlank() }?.let { platform ->
                    append(" · $platform")
                }
                if (item.meetingLink.isBlank()) {
                    append(" · Link TBA")
                }
            }

            TrainingMode.ONSITE -> when (item.trainingLocationMode) {
                TrainingLocationMode.EVENT_LOCATION ->
                    "On-site · Same as event location"

                TrainingLocationMode.CUSTOM ->
                    "On-site · ${item.trainingLocation?.displayName ?: item.location}"

                TrainingLocationMode.TBA ->
                    "On-site · Location TBA"

                null -> "On-site · Location not set"
            }

            null -> "Training format not set"
        }
    }
}

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

private fun remoteDateRangeText(draft: CreatePostDraft): String {
    val start = draft.remoteStartDateMillis ?: return "Remote dates not set"
    val due = draft.remoteDueDateMillis ?: return formatScheduleDate(start)
    return "${formatScheduleDate(start)} – ${formatScheduleDate(due)}"
}

private fun scheduleTimeRangeText(
    startMinutes: Int?,
    endMinutes: Int?
): String {
    return "${formatScheduleTime(startMinutes)} – ${formatScheduleTime(endMinutes)}"
}

private fun formatScheduleTime(minutes: Int?): String {
    if (minutes == null) return "Not set"

    val hour24 = minutes / 60
    val minute = minutes % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val adjusted = hour24 % 12) {
        0 -> 12
        else -> adjusted
    }
    return String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        hour12,
        minute,
        suffix
    )
}

private fun formatDayHeading(dateMillis: Long): String {
    return SimpleDateFormat(
        "EEE, dd MMM",
        Locale.getDefault()
    ).format(Date(dateMillis)).uppercase(Locale.getDefault())
}
