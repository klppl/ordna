package io.github.klppl.ordna.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.klppl.ordna.MainActivity
import io.github.klppl.ordna.R
import io.github.klppl.ordna.data.local.TaskDatabase
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.repository.LayoutDensity
import io.github.klppl.ordna.data.repository.SettingsRepository
import io.github.klppl.ordna.data.repository.WidgetBackground
import io.github.klppl.ordna.data.repository.WidgetSettings
import io.github.klppl.ordna.data.repository.WidgetSorting
import io.github.klppl.ordna.ui.theme.AppTheme
import io.github.klppl.ordna.ui.theme.appThemeColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val DefaultOverdueColor = Color(0xFFD32F2F)
private val DefaultCompletedColor = Color(0xFF4CAF50)

class OrdnaWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = TaskDatabase.getInstance(context).taskDao()
        val today = LocalDate.now()
        val settingsFlow = SettingsRepository.widgetSettingsFlow(context)
        val listOrderFlow = SettingsRepository.listOrderFlow(context)
        val streakFlow = SettingsRepository.streakFlow(context)

        // Snapshot data eagerly so the first frame renders real content
        // instead of empty lists. Flows still drive live updates afterwards.
        val initialOverdue = dao.getOverdueTasks(today).first()
        val initialToday = dao.getTodayTasks(today).first()
        val initialCompleted = dao.getCompletedTasks().first()
        val initialSettings = settingsFlow.first()
        val initialListOrder = listOrderFlow.first()
        val initialStreak = streakFlow.first()

        provideContent {
            val overdueTasks by dao.getOverdueTasks(today).collectAsState(initial = initialOverdue)
            val todayTasks by dao.getTodayTasks(today).collectAsState(initial = initialToday)
            val completedTasksAll by dao.getCompletedTasks().collectAsState(initial = initialCompleted)
            val widgetSettings by settingsFlow.collectAsState(initial = initialSettings)
            val listOrder by listOrderFlow.collectAsState(initial = initialListOrder)
            val streak by streakFlow.collectAsState(initial = initialStreak)

            // Derive display properties from current settings
            val appTheme = AppTheme.entries.find { it.name == widgetSettings.theme } ?: AppTheme.SYSTEM
            val themeColors = appThemeColors(appTheme)
            val bg = widgetSettings.background
            val opacity = widgetSettings.opacity
            val density = widgetSettings.layoutDensity
            val showCompleted = widgetSettings.showCompleted
            val sorting = widgetSettings.sorting

            val effectiveBgColor = if (themeColors != null) {
                val themeBg = themeColors.colorScheme.background
                Color(red = themeBg.red, green = themeBg.green, blue = themeBg.blue, alpha = opacity.coerceIn(0f, 1f))
            } else {
                resolveBackgroundColor(bg, opacity)
            }
            val isLight = if (themeColors != null) false else isLightBackground(bg)
            val overdueColor = themeColors?.overdueRed ?: DefaultOverdueColor
            val completedColor = themeColors?.completedGreen ?: DefaultCompletedColor
            val primaryBarColor = themeColors?.colorScheme?.primary?.let { ColorProvider(it) }
                ?: ColorProvider(Color(0xFF6750A4))

            // Counts always reflect all tasks; showCompleted only controls the list display
            val totalCount = overdueTasks.size + todayTasks.size + completedTasksAll.size
            val completedCount = completedTasksAll.size
            val completedTasks = if (showCompleted) completedTasksAll else emptyList()

            GlanceTheme {
                WidgetContent(
                    overdueTasks = overdueTasks,
                    todayTasks = todayTasks,
                    completedTasks = completedTasks,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    bgColor = effectiveBgColor,
                    isLight = isLight,
                    overdueColor = overdueColor,
                    completedColor = completedColor,
                    primaryBarColor = primaryBarColor,
                    density = density,
                    sorting = sorting,
                    listOrder = listOrder,
                    streak = streak,
                )
            }
        }
    }
}

private fun resolveBackgroundColor(bg: WidgetBackground, opacity: Float): Color {
    val base = when (bg) {
        WidgetBackground.AUTO -> Color.White
        WidgetBackground.WHITE -> Color.White
        WidgetBackground.DARK -> Color(0xFF2D2D2D)
        WidgetBackground.BLACK -> Color.Black
    }
    return Color(red = base.red, green = base.green, blue = base.blue, alpha = opacity.coerceIn(0f, 1f))
}

private fun isLightBackground(bg: WidgetBackground): Boolean =
    bg == WidgetBackground.AUTO || bg == WidgetBackground.WHITE

@Composable
private fun WidgetContent(
    overdueTasks: List<TaskEntity>,
    todayTasks: List<TaskEntity>,
    completedTasks: List<TaskEntity>,
    completedCount: Int,
    totalCount: Int,
    bgColor: Color,
    isLight: Boolean,
    overdueColor: Color,
    completedColor: Color,
    primaryBarColor: ColorProvider,
    density: LayoutDensity,
    sorting: WidgetSorting,
    listOrder: List<String>,
    streak: Int,
) {
    val rowPadding = when (density) {
        LayoutDensity.COMFORTABLE -> 5.dp
        LayoutDensity.DEFAULT -> 3.dp
        LayoutDensity.COMPACT -> 1.dp
    }
    val fontSize = when (density) {
        LayoutDensity.COMFORTABLE -> 15.sp
        LayoutDensity.DEFAULT -> 14.sp
        LayoutDensity.COMPACT -> 12.sp
    }

    val textColor = if (isLight) ColorProvider(Color(0xFF1C1B1F)) else ColorProvider(Color(0xFFE6E1E5))
    val subtextColor = if (isLight) ColorProvider(Color(0xFF49454F)) else ColorProvider(Color(0xFFCAC4D0))
    val iconTint = if (isLight) ColorProvider(Color(0xFF49454F)) else ColorProvider(Color(0xFFCAC4D0))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColor),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            val ctx = LocalContext.current

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (totalCount > 0) {
                    Text(
                        text = ctx.getString(R.string.widget_done_count, completedCount, totalCount),
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor),
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = ctx.getString(R.string.widget_refresh),
                    modifier = GlanceModifier
                        .size(18.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    colorFilter = androidx.glance.ColorFilter.tint(iconTint),
                )
            }

            if (totalCount > 0) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                val progress = completedCount.toFloat() / totalCount
                val trackColor = if (isLight) ColorProvider(Color(0xFFE0E0E0)) else ColorProvider(Color(0xFF444444))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = primaryBarColor,
                    backgroundColor = trackColor,
                )
            }
            Spacer(modifier = GlanceModifier.height(10.dp))

            val allCompleted = totalCount > 0 && completedCount == totalCount

            if (totalCount == 0) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = ctx.getString(R.string.widget_no_tasks), style = TextStyle(fontSize = 14.sp, color = subtextColor))
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else if (allCompleted) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "\u2713",
                        style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ColorProvider(completedColor)),
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = ctx.getString(R.string.widget_all_done),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = ctx.getString(R.string.widget_all_done_subtitle),
                        style = TextStyle(fontSize = 12.sp, color = subtextColor),
                    )
                    if (streak > 0) {
                        Spacer(modifier = GlanceModifier.height(12.dp))
                        Text(
                            text = ctx.getString(R.string.widget_streak, streak),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = subtextColor),
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else {
                // Apply sorting within sections
                val comparator = TaskEntity.flatComparator(listOrder)
                val sortedOverdue = when (sorting) {
                    WidgetSorting.FLAT -> overdueTasks.sortedWith(comparator)
                    WidgetSorting.BY_LIST -> overdueTasks.sortedBy { it.listTitle }
                }
                val sortedToday = when (sorting) {
                    WidgetSorting.FLAT -> todayTasks.sortedWith(comparator)
                    WidgetSorting.BY_LIST -> todayTasks.sortedBy { it.listTitle }
                }
                val sortedCompleted = when (sorting) {
                    WidgetSorting.FLAT -> completedTasks.sortedByDescending { it.completedAt }
                    WidgetSorting.BY_LIST -> completedTasks.sortedBy { it.listTitle }
                }

                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    if (sortedOverdue.isNotEmpty()) {
                        item(itemId = -1L) {
                            WidgetSectionHeader(ctx.getString(R.string.section_overdue).uppercase(), overdueColor, sortedOverdue.size, subtextColor)
                        }
                        if (sorting == WidgetSorting.BY_LIST) {
                            val grouped = sortedOverdue.groupBy { it.listTitle to it.listColor }
                            for ((listInfo, tasks) in grouped) {
                                item(itemId = (listInfo.first.hashCode() + 100000).toLong()) {
                                    WidgetListHeader(listInfo.first, listInfo.second, subtextColor)
                                }
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    WidgetTaskRow(task, false, textColor, subtextColor, rowPadding, fontSize)
                                }
                            }
                        } else {
                            items(sortedOverdue, itemId = { it.id.hashCode().toLong() }) { task ->
                                WidgetTaskRow(task, false, textColor, subtextColor, rowPadding, fontSize)
                            }
                        }
                    }
                    if (sortedToday.isNotEmpty()) {
                        if (sorting == WidgetSorting.BY_LIST) {
                            val grouped = sortedToday.groupBy { it.listTitle to it.listColor }
                            for ((listInfo, tasks) in grouped) {
                                item(itemId = (listInfo.first.hashCode() + 200000).toLong()) {
                                    WidgetListHeader(listInfo.first, listInfo.second, subtextColor)
                                }
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    WidgetTaskRow(task, false, textColor, subtextColor, rowPadding, fontSize)
                                }
                            }
                        } else {
                            items(sortedToday, itemId = { it.id.hashCode().toLong() }) { task ->
                                WidgetTaskRow(task, false, textColor, subtextColor, rowPadding, fontSize)
                            }
                        }
                    }
                    if (sortedCompleted.isNotEmpty()) {
                        item(itemId = -3L) {
                            WidgetSectionHeader(ctx.getString(R.string.section_completed).uppercase(), completedColor, sortedCompleted.size, subtextColor)
                        }
                        if (sorting == WidgetSorting.BY_LIST) {
                            val grouped = sortedCompleted.groupBy { it.listTitle to it.listColor }
                            for ((listInfo, tasks) in grouped) {
                                item(itemId = (listInfo.first.hashCode() + 300000).toLong()) {
                                    WidgetListHeader(listInfo.first, listInfo.second, subtextColor)
                                }
                                items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                                    WidgetTaskRow(task, true, textColor, subtextColor, rowPadding, fontSize)
                                }
                            }
                        } else {
                            items(sortedCompleted, itemId = { it.id.hashCode().toLong() }) { task ->
                                WidgetTaskRow(task, true, textColor, subtextColor, rowPadding, fontSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSectionHeader(text: String, color: Color?, count: Int, subtextColor: ColorProvider) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (color != null) ColorProvider(color) else subtextColor))
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(text = "$count", style = TextStyle(fontSize = 11.sp, color = subtextColor))
    }
}

@Composable
private fun WidgetListHeader(listTitle: String, listColor: Int, subtextColor: ColorProvider) {
    Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(Color(listColor))) {}
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(text = listTitle, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = subtextColor))
    }
}

@Composable
private fun WidgetTaskRow(
    task: TaskEntity, isCompleted: Boolean, textColor: ColorProvider, subtextColor: ColorProvider,
    rowPadding: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit,
) {
    val taskIdKey = ActionParameters.Key<String>("task_id")
    val listIdKey = ActionParameters.Key<String>("list_id")
    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = rowPadding), verticalAlignment = Alignment.CenterVertically) {
        CheckBox(
            checked = isCompleted,
            onCheckedChange = actionRunCallback<ToggleTaskAction>(actionParametersOf(taskIdKey to task.id, listIdKey to task.listId)),
        )
        Spacer(modifier = GlanceModifier.width(2.dp))
        Text(
            text = task.title,
            style = TextStyle(fontSize = fontSize, color = if (isCompleted) subtextColor else textColor, textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None),
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(text = task.listTitle, style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(task.listColor))))
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            // Full sync — fetches from API, updates Room, triggers updateAllWidgets()
            io.github.klppl.ordna.data.repository.TaskRepository.syncForWidget(context)
        } catch (_: Exception) {
            // Fall back to async WorkManager sync
            val workRequest = OneTimeWorkRequestBuilder<io.github.klppl.ordna.data.sync.SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskIdKey = ActionParameters.Key<String>("task_id")
        val listIdKey = ActionParameters.Key<String>("list_id")
        val taskId = parameters[taskIdKey] ?: return
        val listId = parameters[listIdKey] ?: return

        val dao = TaskDatabase.getInstance(context).taskDao()

        val task = dao.getTaskById(taskId) ?: return

        val isCompleting = task.status == "needsAction"
        val newStatus = if (isCompleting) "completed" else "needsAction"
        val completedAt = if (isCompleting) java.time.Instant.now() else null

        // Mark as pending so sync won't overwrite this optimistic update
        PendingToggles.add(taskId)

        // Optimistic update — move task immediately in the UI
        dao.updateTaskStatus(taskId, newStatus, completedAt)

        // Enqueue background API call
        val workRequest = OneTimeWorkRequestBuilder<WidgetToggleWorker>()
            .setInputData(workDataOf(
                "task_id" to taskId,
                "list_id" to listId,
                "completing" to isCompleting,
                "task_title" to task.title,
            ))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // Force all widgets to refresh with the updated data
        updateAllWidgets(context)
    }
}
