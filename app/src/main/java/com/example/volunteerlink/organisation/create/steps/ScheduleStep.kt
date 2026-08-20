package com.example.volunteerlink.organisation.create.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.create.CreatePostValidator
import com.example.volunteerlink.organisation.create.components.CreateGreen
import com.example.volunteerlink.organisation.create.components.CreateLightGreen
import com.example.volunteerlink.organisation.create.model.CreatePostUiState
import com.example.volunteerlink.organisation.create.model.ScheduleItemDraft
import com.example.volunteerlink.organisation.create.model.ScheduleType
import com.example.volunteerlink.organisation.create.model.VolunteerPostType
import com.example.volunteerlink.organisation.viewmodel.CreatePostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Step 4 of Create Post: optional Physical, Remote and Training schedules. */
@Composable
fun ScheduleStep(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onBack: () -> Unit
) {
    val editorDraft = uiState.scheduleEditorDraft

    if (uiState.isScheduleEditorOpen && editorDraft != null) {
        ScheduleItemEditorScreen(
            uiState = uiState,
            item = editorDraft,
            viewModel = viewModel
        )
    } else {
        ScheduleOverview(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

@Composable
fun ScheduleOverview(
    uiState: CreatePostUiState,
    viewModel: CreatePostViewModel,
    onBack: () -> Unit
) {
    val draft = uiState.draft
    val context = LocalContext.current
    val sections = scheduleSectionsForPostType(draft.postType)
    val activeSection = uiState.activeScheduleSection
        ?.takeIf { section -> section in sections }
        ?: sections.firstOrNull()

    val physicalDates = CreatePostValidator.physicalScheduleDates(draft)
    val selectedPhysicalDate = uiState.selectedPhysicalScheduleDateMillis
        ?.let(CreatePostValidator::startOfDayMillis)
        ?.takeIf { date -> date in physicalDates }
        ?: physicalDates.firstOrNull()

    var copySourceDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var replaceSourceDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var replaceTargetDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var overviewDeleteItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var continueWarning by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSection) {
        if (
            activeSection != null &&
            uiState.activeScheduleSection != activeSection
        ) {
            viewModel.selectScheduleSection(activeSection)
        }
    }

    LaunchedEffect(selectedPhysicalDate) {
        if (
            selectedPhysicalDate != null &&
            uiState.selectedPhysicalScheduleDateMillis != selectedPhysicalDate
        ) {
            viewModel.selectPhysicalScheduleDate(selectedPhysicalDate)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 16.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScheduleHeader(onBack = onBack)
        }

        item {
            OptionalScheduleInfoCard()
        }

        if (sections.isNotEmpty() && activeSection != null) {
            item {
                ScheduleSectionSelector(
                    sections = sections,
                    selectedSection = activeSection,
                    itemCounts = sections.associateWith { section ->
                        draft.scheduleItems.count { item ->
                            item.scheduleType == section
                        }
                    },
                    onSectionSelected = viewModel::selectScheduleSection
                )
            }

            val pausedDraft = uiState.scheduleEditorDraft
                ?.takeIf { item ->
                    !uiState.isScheduleEditorOpen &&
                        item.scheduleType == activeSection
                }

            if (pausedDraft != null) {
                item {
                    PausedScheduleDraftCard(
                        item = pausedDraft,
                        isEditingExisting = uiState.editingScheduleItemId != null,
                        onResume = viewModel::resumeScheduleEditorDraft,
                        onDiscard = viewModel::discardScheduleEditorDraft
                    )
                }
            }

            when (activeSection) {
                ScheduleType.PHYSICAL -> {
                    if (selectedPhysicalDate != null) {
                        item {
                            PhysicalScheduleSection(
                                draft = draft,
                                roleCatalogue = uiState.roleCatalogue,
                                physicalDates = physicalDates,
                                selectedDate = selectedPhysicalDate,
                                getItemError = viewModel::getScheduleItemValidationMessage,
                                onDateSelected = viewModel::selectPhysicalScheduleDate,
                                onAddItem = viewModel::addPhysicalScheduleItem,
                                onEditItem = viewModel::openScheduleItemEditor,
                                onDeleteItem = { itemId ->
                                    overviewDeleteItemId = itemId
                                },
                                onCopyDay = { sourceDate ->
                                    copySourceDate = sourceDate
                                },
                                canCopyDay = viewModel::canCopyPhysicalScheduleDay
                            )
                        }
                    }
                }

                ScheduleType.REMOTE -> {
                    item {
                        RemoteScheduleSection(
                            draft = draft,
                            roleCatalogue = uiState.roleCatalogue,
                            getItemError = viewModel::getScheduleItemValidationMessage,
                            onAddItem = viewModel::addRemoteScheduleItem,
                            onEditItem = viewModel::openScheduleItemEditor,
                            onDeleteItem = { itemId ->
                                overviewDeleteItemId = itemId
                            }
                        )
                    }
                }

                ScheduleType.TRAINING -> {
                    item {
                        TrainingScheduleSection(
                            draft = draft,
                            roleCatalogue = uiState.roleCatalogue,
                            getItemError = viewModel::getScheduleItemValidationMessage,
                            getItemWarning = viewModel::getScheduleItemWarning,
                            onAddItem = viewModel::addTrainingScheduleItem,
                            onEditItem = viewModel::openScheduleItemEditor,
                            onDeleteItem = { itemId ->
                                overviewDeleteItemId = itemId
                            }
                        )
                    }
                }
            }
        }

        val overviewError = uiState.scheduleError
        if (uiState.showScheduleErrors && overviewError != null) {
            item {
                ScheduleErrorCard(overviewError)
            }
        }

        uiState.publishError?.let { publishError ->
            item {
                ScheduleErrorCard(publishError)
            }
        }

        item {
            Button(
                onClick = {
                    if (viewModel.validateScheduleForContinue()) {
                        val warning = viewModel.getScheduleProceedWarning()
                        if (warning == null) {
                            viewModel.publishPost(context)
                        } else {
                            continueWarning = warning
                        }
                    }
                },
                enabled = !uiState.isPublishing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CreateGreen
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (uiState.isPublishing) "Publishing..." else "Publish Post",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    val sourceDate = copySourceDate
    if (sourceDate != null) {
        CopyPhysicalDayDialog(
            sourceDate = sourceDate,
            targetDates = physicalDates.filter { date ->
                CreatePostValidator.startOfDayMillis(date) !=
                    CreatePostValidator.startOfDayMillis(sourceDate)
            },
            targetHasItems = viewModel::physicalScheduleDayHasItems,
            onDismiss = {
                copySourceDate = null
            },
            onTargetSelected = { targetDate ->
                copySourceDate = null
                if (viewModel.physicalScheduleDayHasItems(targetDate)) {
                    replaceSourceDate = sourceDate
                    replaceTargetDate = targetDate
                } else {
                    viewModel.copyPhysicalScheduleDay(
                        sourceDateMillis = sourceDate,
                        targetDateMillis = targetDate,
                        replaceExisting = false
                    )
                }
            }
        )
    }

    val replaceSource = replaceSourceDate
    val replaceTarget = replaceTargetDate
    if (replaceSource != null && replaceTarget != null) {
        AlertDialog(
            onDismissRequest = {
                replaceSourceDate = null
                replaceTargetDate = null
            },
            title = {
                Text(
                    text = "Replace this day's timetable?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "${formatScheduleDate(replaceTarget)} already has Physical activities. " +
                        "Copying ${formatScheduleDate(replaceSource)} will replace only the " +
                        "Physical activities on that day. Training and Remote items are unchanged."
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        replaceSourceDate = null
                        replaceTargetDate = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.copyPhysicalScheduleDay(
                            sourceDateMillis = replaceSource,
                            targetDateMillis = replaceTarget,
                            replaceExisting = true
                        )
                        replaceSourceDate = null
                        replaceTargetDate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    )
                ) {
                    Text("Replace & Copy")
                }
            }
        )
    }

    val deleteItemId = overviewDeleteItemId
    if (deleteItemId != null) {
        AlertDialog(
            onDismissRequest = {
                overviewDeleteItemId = null
            },
            title = {
                Text(
                    text = "Delete this schedule item?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This removes the item from the current Create Post draft.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        overviewDeleteItemId = null
                    }
                ) {
                    Text("Keep")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeScheduleItem(deleteItemId)
                        overviewDeleteItemId = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }

    val warning = continueWarning
    if (warning != null) {
        AlertDialog(
            onDismissRequest = {
                continueWarning = null
            },
            title = {
                Text(
                    text = "Publish with this schedule?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(warning)
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        continueWarning = null
                    }
                ) {
                    Text("Go Back")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        continueWarning = null
                        viewModel.publishPost(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    )
                ) {
                    Text("Publish Anyway")
                }
            }
        )
    }
}

@Composable
fun ScheduleItemEditorScreen(
    uiState: CreatePostUiState,
    item: ScheduleItemDraft,
    viewModel: CreatePostViewModel
) {
    val listState = rememberLazyListState()
    val isEditingExisting = uiState.editingScheduleItemId != null
    var saveWarning by rememberSaveable(item.draftId) {
        mutableStateOf<String?>(null)
    }
    var showDeleteDialog by rememberSaveable(item.draftId) {
        mutableStateOf(false)
    }

    LaunchedEffect(item.draftId) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 16.dp,
            end = 20.dp,
            bottom = 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScheduleEditorHeader(
                item = item,
                isEditingExisting = isEditingExisting,
                onBack = viewModel::closeScheduleItemEditor
            )
        }

        item {
            ScheduleItemEditor(
                uiState = uiState,
                item = item,
                viewModel = viewModel
            )
        }

        val editorError = uiState.scheduleError
        if (uiState.showScheduleErrors && editorError != null) {
            item {
                ScheduleErrorCard(editorError)
            }
        }

        if (isEditingExisting) {
            item {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Delete this schedule item",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::closeScheduleItemEditor,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        if (viewModel.validateScheduleEditor()) {
                            val warning = viewModel.getScheduleEditorWarning()
                            if (warning == null) {
                                viewModel.saveScheduleEditor()
                            } else {
                                saveWarning = warning
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isEditingExisting) "Save Changes" else "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    val warning = saveWarning
    if (warning != null) {
        AlertDialog(
            onDismissRequest = {
                saveWarning = null
            },
            title = {
                Text(
                    text = "Short-notice training",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(warning)
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        saveWarning = null
                    }
                ) {
                    Text("Go Back")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveWarning = null
                        viewModel.saveScheduleEditor()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateGreen
                    )
                ) {
                    Text("Schedule Anyway")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(
                    text = "Delete this schedule item?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This removes the saved item from the current Create Post draft.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Keep")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val itemId = uiState.editingScheduleItemId
                        if (itemId != null) {
                            viewModel.removeScheduleItem(itemId)
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}

@Composable
private fun PausedScheduleDraftCard(
    item: ScheduleItemDraft,
    isEditingExisting: Boolean,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CreateLightGreen
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isEditingExisting) {
                    "Unsaved changes kept"
                } else {
                    "Unfinished ${when (item.scheduleType) {
                        ScheduleType.PHYSICAL -> "Physical activity"
                        ScheduleType.REMOTE -> "Remote milestone"
                        ScheduleType.TRAINING -> "Training / briefing"
                    }}"
                },
                style = MaterialTheme.typography.titleSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your input is still saved in Step 4. Resume it when you are ready, or discard it to start over.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDiscard) {
                    Text(
                        text = "Discard",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(onClick = onResume) {
                    Text(
                        text = "Resume",
                        color = CreateGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleHeader(
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
                contentDescription = "Back",
                modifier = Modifier.size(30.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.headlineSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step 4 of 4",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScheduleEditorHeader(
    item: ScheduleItemDraft,
    isEditingExisting: Boolean,
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
                contentDescription = "Back to schedule",
                modifier = Modifier.size(30.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = when (item.scheduleType) {
                    ScheduleType.PHYSICAL -> if (isEditingExisting) {
                        "Edit Physical Activity"
                    } else {
                        "Add Physical Activity"
                    }

                    ScheduleType.REMOTE -> if (isEditingExisting) {
                        "Edit Remote Milestone"
                    } else {
                        "Add Remote Milestone"
                    }

                    ScheduleType.TRAINING -> if (isEditingExisting) {
                        "Edit Training / Briefing"
                    } else {
                        "Add Training / Briefing"
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                color = CreateGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step 4 of 4 · Schedule",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OptionalScheduleInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreateLightGreen,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(R.drawable.calendar),
                contentDescription = null,
                modifier = Modifier.size(21.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Optional schedule",
                    color = CreateGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add useful timing details now, or continue without them. Only saved items appear in the schedule overview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScheduleErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFE9E7),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ScheduleWarningCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF4E5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = Color(0xFF8A4B08),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatScheduleDate(dateMillis: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(dateMillis))
}

private fun scheduleSectionsForPostType(
    postType: VolunteerPostType?
): List<ScheduleType> {
    return when (postType) {
        VolunteerPostType.PHYSICAL -> listOf(
            ScheduleType.PHYSICAL,
            ScheduleType.TRAINING
        )

        VolunteerPostType.REMOTE -> listOf(
            ScheduleType.REMOTE,
            ScheduleType.TRAINING
        )

        VolunteerPostType.HYBRID -> listOf(
            ScheduleType.PHYSICAL,
            ScheduleType.REMOTE,
            ScheduleType.TRAINING
        )

        null -> emptyList()
    }
}
