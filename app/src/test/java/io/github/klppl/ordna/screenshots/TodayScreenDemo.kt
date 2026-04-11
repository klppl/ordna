package io.github.klppl.ordna.screenshots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.ui.theme.LocalSemanticColors
import io.github.klppl.ordna.ui.today.TodayUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreenDemo(state: TodayUiState) {
    val progress = if (state.totalCount > 0) {
        state.completedCount.toFloat() / state.totalCount
    } else {
        0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                title = {
                    if (state.totalCount > 0) {
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
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Overdue section
            if (state.overdueTasks.isNotEmpty()) {
                item(key = "overdue_header") {
                    DemoSectionHeader(
                        title = "OVERDUE",
                        count = state.overdueTasks.size,
                        color = LocalSemanticColors.current.overdueRed,
                    )
                }
                items(state.overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    DemoTaskRow(task = task, isOverdue = true)
                }
            }

            // Spacer between overdue and today
            if (state.overdueTasks.isNotEmpty() && state.todayTasks.isNotEmpty()) {
                item(key = "today_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Today section
            if (state.todayTasks.isNotEmpty()) {
                item(key = "today_header") {
                    DemoSectionHeader(
                        title = "DUE TODAY",
                        count = state.todayTasks.size,
                    )
                }
                items(state.todayTasks, key = { "today_${it.id}" }) { task ->
                    DemoTaskRow(task = task)
                }
            }

            // Completed section
            if (state.completedTasks.isNotEmpty()) {
                item(key = "completed_spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item(key = "completed_header") {
                    DemoSectionHeader(
                        title = "COMPLETED",
                        count = state.completedTasks.size,
                        color = LocalSemanticColors.current.completedGreen,
                    )
                }
                items(state.completedTasks, key = { "completed_${it.id}" }) { task ->
                    DemoTaskRow(task = task, isCompleted = true)
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun DemoTaskRow(
    task: TaskEntity,
    isOverdue: Boolean = false,
    isCompleted: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            .then(if (isCompleted) Modifier.alpha(0.6f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = null,
            colors = if (isOverdue) {
                CheckboxDefaults.colors(uncheckedColor = LocalSemanticColors.current.overdueRed)
            } else {
                CheckboxDefaults.colors()
            },
        )

        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
            color = if (isCompleted) LocalSemanticColors.current.completedGray
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))
        DemoListBadge(listTitle = task.listTitle, color = Color(task.listColor))
    }
}

@Composable
private fun DemoSectionHeader(
    title: String,
    count: Int,
    color: Color? = null,
) {
    val headerColor = color ?: MaterialTheme.colorScheme.onSurface
    val bgColor = color?.copy(alpha = 0.08f)
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
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
    }
}

@Composable
private fun DemoListBadge(listTitle: String, color: Color) {
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
