package com.ordna.android.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.ordna.android.R
import com.ordna.android.data.local.TaskEntity
import com.ordna.android.data.repository.CompletionMethod
import com.ordna.android.data.repository.LayoutDensity
import com.ordna.android.ui.theme.LocalSemanticColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

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
    var overdueExpanded by rememberSaveable { mutableStateOf(true) }

    // Postpone dialog state
    var postponeTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.authExpired) {
        if (state.authExpired) onAuthExpired()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocalDate.now().format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    if (state.streak > 0) {
                        Text(
                            text = "\uD83D\uDD25${state.streak}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (state.totalCount > 0) {
                        Text(
                            text = "${state.completedCount}/${state.totalCount}",
                            style = MaterialTheme.typography.titleMedium,
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.totalCount == 0 && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_tasks_due_today),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                ) {
                    // Progress bar
                    if (state.totalCount > 0) {
                        item(key = "progress") {
                            val progress = state.completedCount.toFloat() / state.totalCount
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }

                    // All done celebration
                    if (state.allCompleted) {
                        item(key = "all_done") {
                            AllDoneBanner(streak = state.streak)
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
                                completionMethod = state.completionMethod,
                                layoutDensity = state.layoutDensity,
                                isOverdue = true,
                                onToggle = { viewModel.toggleTask(it) },
                                onPostpone = { postponeTask = it },
                            )
                        }
                    }

                    // Due Today section
                    if (state.todayTasks.isNotEmpty()) {
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
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            onToggle = { viewModel.toggleTask(it) },
                            onPostpone = { postponeTask = it },
                        )
                    }

                    // Completed section
                    if (state.completedTasks.isNotEmpty()) {
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
                            completionMethod = state.completionMethod,
                            layoutDensity = state.layoutDensity,
                            isCompleted = true,
                            onToggle = { viewModel.toggleTask(it) },
                        )
                    }

                    item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(88.dp)) }
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

    if (showCreateSheet) {
        val availableLists by viewModel.availableLists.collectAsState()
        val defaultListId by viewModel.createListId.collectAsState()
        val defaultListTitle by viewModel.createListTitle.collectAsState()

        CreateTaskSheet(
            availableLists = availableLists,
            defaultListId = defaultListId,
            defaultListTitle = defaultListTitle,
            onDismiss = { showCreateSheet = false },
            onCreate = { title, listId, listTitle ->
                viewModel.createTask(title, listId, listTitle)
                showCreateSheet = false
            },
        )
    }
}

private fun LazyListScope.taskSection(
    tasks: List<TaskEntity>,
    keyPrefix: String,
    groupByList: Boolean,
    completionMethod: CompletionMethod,
    layoutDensity: LayoutDensity,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
    onToggle: (TaskEntity) -> Unit,
    onPostpone: ((TaskEntity) -> Unit)? = null,
) {
    if (groupByList) {
        val grouped = tasks.groupBy { it.listTitle }
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
                completionMethod = completionMethod,
                layoutDensity = layoutDensity,
                isOverdue = isOverdue,
                isCompleted = isCompleted,
                showBadge = true,
            )
        }
    }
}

private val PostponeAmber = Color(0xFFF59E0B)

@Composable
private fun SwipeableTaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onPostpone: (() -> Unit)? = null,
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
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

@Composable
private fun PostponeDialog(
    showToday: Boolean,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = PostponeAmber,
            )
        },
        title = { Text(stringResource(R.string.postpone_title)) },
        text = {
            Column {
                if (showToday) {
                    TextButton(
                        onClick = { onSelect(today) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.postpone_today))
                    }
                }
                TextButton(
                    onClick = { onSelect(today.plusDays(1)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.postpone_tomorrow))
                }
                TextButton(
                    onClick = { onSelect(today.with(java.time.DayOfWeek.MONDAY).plusWeeks(1)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.postpone_next_week))
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskSheet(
    availableLists: List<com.ordna.android.ui.settings.TaskListOption>,
    defaultListId: String?,
    defaultListTitle: String?,
    onDismiss: () -> Unit,
    onCreate: (title: String, listId: String, listTitle: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val initialList = availableLists.find { it.id == defaultListId }
        ?: availableLists.firstOrNull()
    var selectedList by remember { mutableStateOf(initialList) }

    val canSubmit = title.isNotBlank() && selectedList != null

    fun submit() {
        val list = selectedList ?: return
        if (title.isBlank()) return
        onCreate(title.trim(), list.id, list.title)
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (list in availableLists) {
                        FilterChip(
                            selected = selectedList?.id == list.id,
                            onClick = { selectedList = list },
                            label = { Text(list.title) },
                        )
                    }
                }
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
