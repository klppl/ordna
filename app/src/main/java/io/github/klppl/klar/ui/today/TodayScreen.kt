package io.github.klppl.klar.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.klppl.klar.R
import io.github.klppl.klar.data.local.TaskEntity
import io.github.klppl.klar.data.repository.CompletionMethod
import io.github.klppl.klar.data.repository.LayoutDensity
import io.github.klppl.klar.data.repository.RoutinesPosition
import io.github.klppl.klar.ui.theme.LocalSemanticColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private data class UndoableAction(
    val message: String,
    val undo: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAuthExpired: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.snackbar_undo)
    var overdueExpanded by rememberSaveable { mutableStateOf(true) }
    var routinesExpanded by rememberSaveable { mutableStateOf(true) }

    // Postpone dialog state
    var postponeTask by remember { mutableStateOf<TaskEntity?>(null) }
    var detailTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showStreakSheet by remember { mutableStateOf(false) }
    var pendingUndo by remember { mutableStateOf<UndoableAction?>(null) }

    LaunchedEffect(state.authExpired) {
        if (state.authExpired) onAuthExpired()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(pendingUndo) {
        pendingUndo?.let { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                action.undo()
            }
            pendingUndo = null
        }
    }

    val toggleWithUndo: (TaskEntity) -> Unit = { task ->
        val wasNeedsAction = task.status == io.github.klppl.klar.data.local.TaskStatus.NEEDS_ACTION
        viewModel.toggleTask(task)
        if (wasNeedsAction) {
            pendingUndo = UndoableAction(
                message = context.getString(R.string.snackbar_completed, task.title),
                undo = { viewModel.toggleTask(task.copy(status = io.github.klppl.klar.data.local.TaskStatus.COMPLETED)) },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                title = {
                    if (state.totalCount > 0) {
                        val progress = state.completedCount.toFloat() / state.totalCount
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        val progressColor = MaterialTheme.colorScheme.primary

                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(42.dp)) {
                                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                val padding = stroke.width / 2
                                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                                val topLeft = Offset(padding, padding)

                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = stroke,
                                )
                                if (progress > 0f) {
                                    drawArc(
                                        color = progressColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = stroke,
                                    )
                                }
                            }
                            Text(
                                text = "${state.completedCount}/${state.totalCount}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                },
                actions = {
                    if (state.streak > 0) {
                        Text(
                            text = "\uD83D\uDD25${state.streak}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { showStreakSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_task))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.filterableLists.size > 1) {
                ListFilterRow(
                    lists = state.filterableLists,
                    hiddenIds = state.hiddenListIds,
                    onToggle = { viewModel.toggleListVisibility(it) },
                )
            }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.totalCount == 0 && !isRefreshing) {
                EmptyDayState()
            } else {
                // Pre-compute grouping so it's not recalculated on every recomposition
                val groupedOverdue = remember(state.overdueTasks) { state.overdueTasks.groupBy { it.listTitle } }
                val groupedToday = remember(state.todayTasks) { state.todayTasks.groupBy { it.listTitle } }
                val groupedCompleted = remember(state.completedTasks) { state.completedTasks.groupBy { it.listTitle } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        ,
                ) {
                    // All done celebration
                    if (state.allCompleted) {
                        item(key = "all_done") {
                            AllDoneBanner(streak = state.streak)
                        }
                    }

                    // Routines section — top placement
                    if (state.routineTasks.isNotEmpty() &&
                        state.routinesPosition == RoutinesPosition.TOP
                    ) {
                        routinesSection(
                            tasks = state.routineTasks,
                            expanded = routinesExpanded,
                            onToggleExpanded = { routinesExpanded = !routinesExpanded },
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            onToggle = { toggleWithUndo(it) },
                            onTap = { detailTask = it },
                        )
                        if (state.overdueTasks.isNotEmpty() ||
                            state.todayTasks.isNotEmpty() ||
                            state.completedTasks.isNotEmpty()
                        ) {
                            item(key = "routines_top_spacer") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    // Overdue section (collapsible)
                    if (state.overdueTasks.isNotEmpty()) {
                        item(key = "overdue_header") {
                            SectionHeader(
                                title = stringResource(R.string.section_overdue),
                                count = state.overdueTasks.size,
                                color = LocalSemanticColors.current.overdueRed,
                                collapsible = true,
                                expanded = overdueExpanded,
                                onToggle = { overdueExpanded = !overdueExpanded },
                            )
                        }
                        if (overdueExpanded) {
                            taskSection(
                                tasks = state.overdueTasks,
                                keyPrefix = "overdue",
                                groupByList = state.groupByList,
                                grouped = groupedOverdue,
                                completionMethod = state.completionMethod,
                                layoutDensity = state.layoutDensity,
                                isOverdue = true,
                                onToggle = { toggleWithUndo(it) },
                                onPostpone = { postponeTask = it },
                                onTap = { detailTask = it },
                            )
                        }
                    }

                    // Due Today section
                    if (state.todayTasks.isNotEmpty()) {
                        if (state.overdueTasks.isNotEmpty()) {
                            item(key = "today_spacer") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        item(key = "today_header") {
                            SectionHeader(
                                title = stringResource(R.string.section_due_today),
                                count = state.todayTasks.size,
                            )
                        }
                        taskSection(
                            tasks = state.todayTasks,
                            keyPrefix = "today",
                            groupByList = state.groupByList,
                            grouped = groupedToday,
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            onToggle = { toggleWithUndo(it) },
                            onPostpone = { postponeTask = it },
                            onTap = { detailTask = it },
                        )
                    }

                    // Completed section
                    if (state.completedTasks.isNotEmpty()) {
                        item(key = "completed_spacer") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item(key = "completed_header") {
                            SectionHeader(
                                title = stringResource(R.string.section_completed),
                                count = state.completedTasks.size,
                                color = LocalSemanticColors.current.completedGreen,
                            )
                        }
                        taskSection(
                            tasks = state.completedTasks,
                            keyPrefix = "completed",
                            groupByList = state.groupByList,
                            grouped = groupedCompleted,
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            isCompleted = true,
                            onToggle = { toggleWithUndo(it) },
                            onTap = { detailTask = it },
                        )
                    }

                    // Routines section — bottom placement (default)
                    if (state.routineTasks.isNotEmpty() &&
                        state.routinesPosition == RoutinesPosition.BOTTOM
                    ) {
                        item(key = "routines_bottom_spacer") {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        routinesSection(
                            tasks = state.routineTasks,
                            expanded = routinesExpanded,
                            onToggleExpanded = { routinesExpanded = !routinesExpanded },
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            onToggle = { toggleWithUndo(it) },
                            onTap = { detailTask = it },
                        )
                    }

                    state.lastSync?.let { syncTime ->
                        item(key = "last_sync") {
                            val now = java.time.Instant.now()
                            val minutes = java.time.Duration.between(syncTime, now).toMinutes()
                            val text = when {
                                minutes < 1 -> stringResource(R.string.last_sync_just_now)
                                minutes < 60 -> stringResource(R.string.last_sync_minutes, minutes)
                                else -> {
                                    val hours = minutes / 60
                                    if (hours < 24) stringResource(R.string.last_sync_hours, hours)
                                    else stringResource(R.string.last_sync_long_ago)
                                }
                            }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }

                    item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
            }
        }
    }

    // Postpone dialog
    postponeTask?.let { task ->
        val today = LocalDate.now()
        val isOverdue = task.due != null && task.due < today

        PostponeDialog(
            showToday = isOverdue,
            onDismiss = { postponeTask = null },
            onSelect = { newDue ->
                viewModel.postponeTask(task, newDue)
                postponeTask = null
            },
        )
    }

    // Task detail sheet
    detailTask?.let { task ->
        TaskDetailSheet(
            task = task,
            onDismiss = { detailTask = null },
            onNotesChanged = { notes ->
                viewModel.updateTaskNotes(task, notes)
            },
            onTitleChanged = { title ->
                viewModel.updateTaskTitle(task, title)
            },
            onDelete = {
                viewModel.deleteTask(task)
                detailTask = null
                pendingUndo = UndoableAction(
                    message = context.getString(R.string.snackbar_deleted, task.title),
                    undo = {
                        viewModel.createTask(
                            title = task.title,
                            listId = task.listId,
                            listTitle = task.listTitle,
                            due = task.due ?: LocalDate.now(),
                            notes = task.notes,
                        )
                    },
                )
            },
        )
    }

    if (showCreateSheet) {
        val availableLists by viewModel.availableLists.collectAsState()
        val defaultListId by viewModel.createListId.collectAsState()
        val defaultListTitle by viewModel.createListTitle.collectAsState()

        CreateTaskSheet(
            availableLists = availableLists,
            defaultListId = defaultListId,
            defaultListTitle = defaultListTitle,
            onDismiss = { showCreateSheet = false },
            onCreate = { title, listId, listTitle, due ->
                viewModel.createTask(title, listId, listTitle, due)
                showCreateSheet = false
            },
        )
    }

    if (showStreakSheet) {
        val history by viewModel.streakHistory.collectAsState()
        StreakHistorySheet(
            streak = state.streak,
            history = history,
            onDismiss = { showStreakSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreakHistorySheet(
    streak: Int,
    history: Set<LocalDate>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }
    // Last 35 days, oldest first, as five rows of seven ending today.
    val days = remember { (0L until 35L).map { today.minusDays(34L - it) } }
    val completedGreen = LocalSemanticColors.current.completedGreen
    val emptyCell = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🔥 " + stringResource(R.string.streak_days, streak),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(20.dp))
            for (week in days.chunked(7)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 3.dp),
                ) {
                    for (day in week) {
                        val done = day in history
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = if (done) completedGreen else emptyCell,
                                    shape = RoundedCornerShape(8.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${day.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (done) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayState() {
    val greetingRes = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> R.string.reminder_title_morning
            in 12..17 -> R.string.reminder_title_midday
            else -> R.string.reminder_title_evening
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(greetingRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.no_tasks_due_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListFilterRow(
    lists: List<ListChip>,
    hiddenIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (list in lists) {
            FilterChip(
                selected = list.id !in hiddenIds,
                onClick = { onToggle(list.id) },
                label = { Text(list.title) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(list.color), RoundedCornerShape(3.dp)),
                    )
                },
            )
        }
    }
}

private fun LazyListScope.taskSection(
    tasks: List<TaskEntity>,
    keyPrefix: String,
    groupByList: Boolean,
    grouped: Map<String, List<TaskEntity>> = emptyMap(),
    completionMethod: CompletionMethod,
    layoutDensity: LayoutDensity,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
    onToggle: (TaskEntity) -> Unit,
    onPostpone: ((TaskEntity) -> Unit)? = null,
    onTap: ((TaskEntity) -> Unit)? = null,
) {
    if (groupByList) {
        for ((listTitle, listTasks) in grouped) {
            val firstTask = listTasks.first()
            item(key = "${keyPrefix}_group_$listTitle") {
                ListGroupHeader(
                    listTitle = listTitle,
                    color = Color(firstTask.listColor),
                    count = listTasks.size,
                )
            }
            items(listTasks, key = { "${keyPrefix}_${it.id}" }) { task ->
                SwipeableTaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onPostpone = onPostpone?.let { { it(task) } },
                    onTap = onTap?.let { { it(task) } },
                    completionMethod = completionMethod,
                    layoutDensity = layoutDensity,
                    isOverdue = isOverdue,
                    isCompleted = isCompleted,
                    showBadge = false,
                )
            }
        }
    } else {
        items(tasks, key = { "${keyPrefix}_${it.id}" }) { task ->
            SwipeableTaskRow(
                task = task,
                onToggle = { onToggle(task) },
                onPostpone = onPostpone?.let { { it(task) } },
                onTap = onTap?.let { { it(task) } },
                completionMethod = completionMethod,
                layoutDensity = layoutDensity,
                isOverdue = isOverdue,
                isCompleted = isCompleted,
                showBadge = true,
            )
        }
    }
}

/**
 * Dedicated section for the designated dailies list. Routines are rendered flat
 * (single list) with a distinct tertiary-colored, collapsible header. Postpone is
 * intentionally disabled — repeating tasks roll forward via Google's recurrence.
 */
private fun LazyListScope.routinesSection(
    tasks: List<TaskEntity>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    completionMethod: CompletionMethod,
    layoutDensity: LayoutDensity,
    onToggle: (TaskEntity) -> Unit,
    onTap: (TaskEntity) -> Unit,
) {
    item(key = "routines_header") {
        SectionHeader(
            title = "🔁 " + stringResource(R.string.section_routines),
            count = tasks.size,
            color = MaterialTheme.colorScheme.tertiary,
            collapsible = true,
            expanded = expanded,
            onToggle = onToggleExpanded,
        )
    }
    if (expanded) {
        taskSection(
            tasks = tasks,
            keyPrefix = "routines",
            groupByList = false,
            completionMethod = completionMethod,
            layoutDensity = layoutDensity,
            onToggle = onToggle,
            onTap = onTap,
        )
    }
}

private val PostponeAmber = Color(0xFFF59E0B)

@Composable
private fun SwipeableTaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onPostpone: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    completionMethod: CompletionMethod,
    layoutDensity: LayoutDensity,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
    showBadge: Boolean = true,
) {
    val swipeToCompleteEnabled = completionMethod != CompletionMethod.CHECKBOX
    val postponeEnabled = onPostpone != null && !isCompleted
    val swipeEnabled = swipeToCompleteEnabled || postponeEnabled

    if (swipeEnabled) {
        var rowWidth by remember { mutableStateOf(1f) }
        val dragX = remember { Animatable(0f) }
        val threshold = 0.30f
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        // Left swipe progress (complete) — dragX is negative
        val completeProgress = if (rowWidth > 0f) ((-dragX.value) / rowWidth).coerceIn(0f, 1f) else 0f
        val pastCompleteThreshold = completeProgress >= threshold

        // Right swipe progress (postpone) — dragX is positive
        val postponeProgress = if (rowWidth > 0f) (dragX.value / rowWidth).coerceIn(0f, 1f) else 0f
        val pastPostponeThreshold = postponeProgress >= threshold

        // Track whether we already fired the threshold haptic during this drag
        var thresholdHapticFired by remember { mutableStateOf(false) }

        // Track icon pop animation
        val iconPopScale = remember { Animatable(1f) }

        // Exit animation state
        var exitAnimating by remember { mutableStateOf(false) }
        val rowAlpha = remember { Animatable(1f) }
        val rowHeight = remember { Animatable(1f) }

        // Determine which threshold we're past (if any)
        val pastAnyThreshold = pastCompleteThreshold || pastPostponeThreshold

        // Fire threshold haptic and icon pop when crossing either threshold
        LaunchedEffect(pastAnyThreshold) {
            if (pastAnyThreshold && !thresholdHapticFired) {
                thresholdHapticFired = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                iconPopScale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = tween(durationMillis = 100),
                )
                iconPopScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh,
                    ),
                )
            }
        }

        // TalkBack users can't perform the swipe gesture, so expose the same
        // operations as custom accessibility actions.
        val a11yLabelComplete = stringResource(if (isCompleted) R.string.cd_uncomplete else R.string.cd_complete)
        val a11yLabelPostpone = stringResource(R.string.postpone_title)
        val a11yActions = remember(isCompleted, postponeEnabled, onToggle, onPostpone) {
            buildList {
                add(CustomAccessibilityAction(a11yLabelComplete) { onToggle(); true })
                if (postponeEnabled && onPostpone != null) {
                    add(CustomAccessibilityAction(a11yLabelPostpone) { onPostpone(); true })
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { customActions = a11yActions }
                .clipToBounds()
                .onSizeChanged { rowWidth = it.width.toFloat() }
                .graphicsLayer {
                    alpha = rowAlpha.value
                    scaleY = rowHeight.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !exitAnimating,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val raw = dragX.value + delta
                            // Gate each direction independently
                            val clamped = when {
                                raw < 0f && !swipeToCompleteEnabled -> 0f  // block left if checkbox-only
                                raw > 0f && !postponeEnabled -> 0f         // block right if no postpone
                                else -> raw
                            }
                            dragX.snapTo(clamped)
                        }
                    },
                    onDragStarted = {
                        thresholdHapticFired = false
                    },
                    onDragStopped = {
                        scope.launch {
                            when {
                                // LEFT SWIPE: Complete
                                pastCompleteThreshold && !isCompleted -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    exitAnimating = true

                                    dragX.animateTo(
                                        targetValue = -rowWidth,
                                        animationSpec = tween(durationMillis = 200),
                                    )
                                    delay(80)
                                    rowHeight.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 150),
                                    )

                                    onToggle()

                                    dragX.snapTo(0f)
                                    rowHeight.snapTo(1f)
                                    rowAlpha.snapTo(1f)
                                    exitAnimating = false
                                }

                                // LEFT SWIPE: Un-complete
                                pastCompleteThreshold && isCompleted -> {
                                    onToggle()
                                    dragX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }

                                // RIGHT SWIPE: Postpone
                                pastPostponeThreshold && postponeEnabled -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    // Show dialog immediately, snap row back
                                    onPostpone()
                                    dragX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessHigh,
                                        ),
                                    )
                                }

                                // Below threshold: spring back
                                else -> {
                                    dragX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ),
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

            // Determine which reveal strip to show based on drag direction
            val isDraggingLeft = dragX.value < 0f
            val isDraggingRight = dragX.value > 0f

            // Reveal strip behind the row
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        when {
                            isDraggingRight && postponeEnabled -> PostponeAmber
                            else -> primaryColor
                        }
                    ),
                contentAlignment = if (isDraggingRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (isDraggingLeft) {
                    // Complete: checkmark on the right
                    val baseScale = if (pastCompleteThreshold) 1.0f else (completeProgress / threshold).coerceIn(0.5f, 1.0f)
                    val finalScale = baseScale * iconPopScale.value
                    val iconAlpha = (completeProgress * 3f).coerceAtMost(1f)

                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = onPrimaryColor.copy(alpha = iconAlpha),
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                            },
                    )
                } else if (isDraggingRight && postponeEnabled) {
                    // Postpone: arrow/calendar icon on the left
                    val baseScale = if (pastPostponeThreshold) 1.0f else (postponeProgress / threshold).coerceIn(0.5f, 1.0f)
                    val finalScale = baseScale * iconPopScale.value
                    val iconAlpha = (postponeProgress * 3f).coerceAtMost(1f)

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = iconAlpha),
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                            },
                    )
                }
            }

            // The task row that slides with your finger
            TaskRow(
                task = task,
                onToggle = onToggle,
                onTap = onTap,
                completionMethod = completionMethod,
                layoutDensity = layoutDensity,
                isOverdue = isOverdue,
                isCompleted = isCompleted,
                showBadge = showBadge,
                modifier = Modifier.offset { IntOffset(dragX.value.roundToInt(), 0) },
            )
        }
    } else {
        TaskRow(
            task = task,
            onToggle = onToggle,
            onTap = onTap,
            completionMethod = completionMethod,
            layoutDensity = layoutDensity,
            isOverdue = isOverdue,
            isCompleted = isCompleted,
            showBadge = showBadge,
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onTap: (() -> Unit)? = null,
    completionMethod: CompletionMethod,
    layoutDensity: LayoutDensity,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
    showBadge: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val showCheckbox = completionMethod != CompletionMethod.SWIPE
    val verticalPad = when (layoutDensity) {
        LayoutDensity.COMFORTABLE -> 6.dp
        LayoutDensity.DEFAULT -> 2.dp
        LayoutDensity.COMPACT -> 0.dp
    }
    val textStyle = when (layoutDensity) {
        LayoutDensity.COMFORTABLE -> MaterialTheme.typography.bodyLarge
        LayoutDensity.DEFAULT -> MaterialTheme.typography.bodyLarge
        LayoutDensity.COMPACT -> MaterialTheme.typography.bodyMedium
    }
    val noCheckboxVerticalPad = when (layoutDensity) {
        LayoutDensity.COMFORTABLE -> 16.dp
        LayoutDensity.DEFAULT -> 12.dp
        LayoutDensity.COMPACT -> 8.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
            .padding(
                start = if (showCheckbox) 8.dp else 16.dp,
                end = 8.dp,
                top = verticalPad,
                bottom = verticalPad,
            )
            .then(if (isCompleted) Modifier.alpha(0.6f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheckbox) {
            val haptic = LocalHapticFeedback.current
            Checkbox(
                checked = isCompleted,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                colors = if (isOverdue) {
                    CheckboxDefaults.colors(uncheckedColor = LocalSemanticColors.current.overdueRed)
                } else {
                    CheckboxDefaults.colors()
                },
            )
        }

        Text(
            text = task.title,
            style = textStyle,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
            color = if (isCompleted) LocalSemanticColors.current.completedGray
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = if (showCheckbox) 0.dp else noCheckboxVerticalPad),
        )

        if (!task.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }

        if (showBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            ListBadge(listTitle = task.listTitle, color = Color(task.listColor))
        }
    }
}

@Composable
private fun ListGroupHeader(listTitle: String, color: Color, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = listTitle,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color? = null,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val headerColor = color ?: MaterialTheme.colorScheme.onSurface
    val bgColor = color?.copy(alpha = 0.08f)
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .background(bgColor)
            .then(if (collapsible && onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = headerColor,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            color = headerColor.copy(alpha = 0.7f),
        )
        if (collapsible) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onToggle?.invoke() }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse)
                    else stringResource(R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ListBadge(listTitle: String, color: Color) {
    Text(
        text = listTitle,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun AllDoneBanner(streak: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val completedGreen = LocalSemanticColors.current.completedGreen

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = completedGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(32.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = completedGreen,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.all_done_title),
                style = MaterialTheme.typography.headlineSmall,
                color = completedGreen,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.all_done_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (streak > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\uD83D\uDD25 " + stringResource(R.string.streak_days, streak),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onNotesChanged: (String?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember(task.id) { mutableStateOf(task.title) }
    var notesText by remember(task.id) { mutableStateOf(task.notes ?: "") }
    val dayFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    ModalBottomSheet(
        onDismissRequest = {
            val trimmedTitle = titleText.trim()
            if (trimmedTitle.isNotEmpty() && trimmedTitle != task.title) {
                onTitleChanged(trimmedTitle)
            }
            val newNotes = notesText.trim().ifBlank { null }
            if (newNotes != task.notes) {
                onNotesChanged(newNotes)
            }
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Editable title
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(8.dp))

            // List badge + due date + delete button
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListBadge(listTitle = task.listTitle, color = Color(task.listColor))
                task.due?.let { due ->
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.task_detail_due, due.format(dayFormat)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.task_detail_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Notes field
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.task_detail_notes_hint))
                },
                minLines = 3,
                maxLines = 8,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostponeDialog(
    showToday: Boolean,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val laterThisWeek = today.plusDays(2)
    val thisSaturday = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SATURDAY))
    val nextMonday = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))

    val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d")

    // Track whether to show the date picker instead
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = tomorrow.atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        onSelect(picked)
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    // Collect options, skipping duplicates
    data class PostponeOption(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val subtitle: String,
        val date: LocalDate,
    )

    val options = mutableListOf<PostponeOption>()

    if (showToday) {
        options += PostponeOption(
            icon = Icons.Default.WbSunny,
            label = stringResource(R.string.postpone_today),
            subtitle = today.format(dayFormat),
            date = today,
        )
    }

    options += PostponeOption(
        icon = Icons.Outlined.WbSunny,
        label = stringResource(R.string.postpone_tomorrow),
        subtitle = tomorrow.format(dayFormat),
        date = tomorrow,
    )

    // Later this week: only Mon–Wed (so +2 lands on Wed–Fri)
    val showLaterThisWeek = today.dayOfWeek.value <= 3 // Mon=1, Tue=2, Wed=3
    if (showLaterThisWeek) {
        options += PostponeOption(
            icon = Icons.Default.WorkOutline,
            label = stringResource(R.string.postpone_later_this_week),
            subtitle = laterThisWeek.format(dayFormat),
            date = laterThisWeek,
        )
    }

    // This weekend: only if Saturday isn't tomorrow (i.e., not Friday/Sat/Sun)
    if (thisSaturday != tomorrow && today.dayOfWeek.value <= 5) {
        options += PostponeOption(
            icon = Icons.Default.Weekend,
            label = stringResource(R.string.postpone_this_weekend),
            subtitle = thisSaturday.format(dayFormat),
            date = thisSaturday,
        )
    }

    // Next week: only if Monday isn't tomorrow (i.e., not Sunday)
    if (nextMonday != tomorrow) {
        options += PostponeOption(
            icon = Icons.Default.CalendarMonth,
            label = stringResource(R.string.postpone_next_week),
            subtitle = nextMonday.format(dayFormat),
            date = nextMonday,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.postpone_title)) },
        text = {
            Column {
                options.forEach { option ->
                    PostponeRow(
                        icon = option.icon,
                        label = option.label,
                        subtitle = option.subtitle,
                        onClick = { onSelect(option.date) },
                    )
                }
                PostponeRow(
                    icon = Icons.Default.EditCalendar,
                    label = stringResource(R.string.postpone_pick_date),
                    subtitle = null,
                    onClick = { showDatePicker = true },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun PostponeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PostponeAmber,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    availableLists: List<io.github.klppl.klar.ui.settings.TaskListOption>,
    defaultListId: String?,
    defaultListTitle: String?,
    onDismiss: () -> Unit,
    onCreate: (title: String, listId: String, listTitle: String, due: LocalDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val today = remember { LocalDate.now() }
    val tomorrow = remember { today.plusDays(1) }
    var selectedDue by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialList = if (defaultListId != null) {
        availableLists.find { it.id == defaultListId }
    } else if (availableLists.size == 1) {
        availableLists.first()
    } else {
        null
    }
    var selectedList by remember { mutableStateOf(initialList) }

    val canSubmit = title.isNotBlank() && selectedList != null

    fun submit() {
        val list = selectedList ?: return
        if (title.isBlank()) return
        onCreate(title.trim(), list.id, list.title, selectedDue)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDue.atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDue = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.create_task_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
            )

            if (availableLists.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                val listChipState = rememberLazyListState()
                LaunchedEffect(Unit) {
                    val idx = availableLists.indexOfFirst { it.id == selectedList?.id }
                    if (idx > 0) listChipState.scrollToItem(idx)
                }
                LazyRow(
                    state = listChipState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableLists, key = { it.id }) { list ->
                        FilterChip(
                            selected = selectedList?.id == list.id,
                            onClick = { selectedList = list },
                            label = { Text(list.title) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val customDateLabel = selectedDue.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            val isCustomDate = selectedDue != today && selectedDue != tomorrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedDue == today,
                    onClick = { selectedDue = today },
                    label = { Text(stringResource(R.string.postpone_today)) },
                )
                FilterChip(
                    selected = selectedDue == tomorrow,
                    onClick = { selectedDue = tomorrow },
                    label = { Text(stringResource(R.string.postpone_tomorrow)) },
                )
                FilterChip(
                    selected = isCustomDate,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(if (isCustomDate) customDateLabel else stringResource(R.string.postpone_pick_date))
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cd_add_task))
            }
        }
    }
}
