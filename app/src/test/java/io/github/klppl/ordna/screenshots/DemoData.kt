package io.github.klppl.ordna.screenshots

import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.ui.today.TodayUiState
import java.time.Instant
import java.time.LocalDate

object DemoData {

    private val TODAY: LocalDate = LocalDate.of(2026, 4, 11)
    private val YESTERDAY: LocalDate = TODAY.minusDays(1)
    private val NOW: Instant = Instant.parse("2026-04-11T12:00:00Z")

    const val STREAK = 12

    // --- Lists ---------------------------------------------------------------

    private const val LIST_DAILY = "list-daily"
    private const val LIST_WORK = "list-work"
    private const val LIST_FUN = "list-fun"

    private val dailyColor = GoogleTasksApi.colorForListId(LIST_DAILY)
    private val workColor = GoogleTasksApi.colorForListId(LIST_WORK)
    private val funColor = GoogleTasksApi.colorForListId(LIST_FUN)

    // --- Overdue tasks -------------------------------------------------------

    val overdueTasks: List<TaskEntity> = listOf(
        TaskEntity(
            id = "overdue-1",
            title = "Take out trash",
            due = YESTERDAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_DAILY,
            listTitle = "Daily",
            listColor = dailyColor,
            position = "00000000000000000000",
            updated = NOW,
        ),
        TaskEntity(
            id = "overdue-2",
            title = "Review pull requests",
            due = YESTERDAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_WORK,
            listTitle = "Work",
            listColor = workColor,
            position = "00000000000000000001",
            updated = NOW,
        ),
    )

    // --- Today tasks ---------------------------------------------------------

    val todayTasks: List<TaskEntity> = listOf(
        TaskEntity(
            id = "today-1",
            title = "Take supplements",
            due = TODAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_DAILY,
            listTitle = "Daily",
            listColor = dailyColor,
            position = "00000000000000000002",
            updated = NOW,
        ),
        TaskEntity(
            id = "today-2",
            title = "Water plants",
            due = TODAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_DAILY,
            listTitle = "Daily",
            listColor = dailyColor,
            position = "00000000000000000003",
            updated = NOW,
        ),
        TaskEntity(
            id = "today-3",
            title = "Write standup notes",
            due = TODAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_WORK,
            listTitle = "Work",
            listColor = workColor,
            position = "00000000000000000004",
            updated = NOW,
        ),
        TaskEntity(
            id = "today-4",
            title = "Practice guitar",
            due = TODAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_FUN,
            listTitle = "Fun",
            listColor = funColor,
            position = "00000000000000000005",
            updated = NOW,
        ),
        TaskEntity(
            id = "today-5",
            title = "Try new recipe",
            due = TODAY,
            dueDateTime = null,
            status = "needsAction",
            completedAt = null,
            listId = LIST_FUN,
            listTitle = "Fun",
            listColor = funColor,
            position = "00000000000000000006",
            updated = NOW,
        ),
    )

    // --- Completed tasks -----------------------------------------------------

    val completedTasks: List<TaskEntity> = listOf(
        TaskEntity(
            id = "done-1",
            title = "Make bed",
            due = TODAY,
            dueDateTime = null,
            status = "completed",
            completedAt = NOW,
            listId = LIST_DAILY,
            listTitle = "Daily",
            listColor = dailyColor,
            position = "00000000000000000007",
            updated = NOW,
        ),
        TaskEntity(
            id = "done-2",
            title = "Update project board",
            due = TODAY,
            dueDateTime = null,
            status = "completed",
            completedAt = NOW,
            listId = LIST_WORK,
            listTitle = "Work",
            listColor = workColor,
            position = "00000000000000000008",
            updated = NOW,
        ),
        TaskEntity(
            id = "done-3",
            title = "Read 20 pages",
            due = TODAY,
            dueDateTime = null,
            status = "completed",
            completedAt = NOW,
            listId = LIST_FUN,
            listTitle = "Fun",
            listColor = funColor,
            position = "00000000000000000009",
            updated = NOW,
        ),
    )

    // --- UI state ------------------------------------------------------------

    val uiState: TodayUiState = TodayUiState(
        overdueTasks = overdueTasks,
        todayTasks = todayTasks,
        completedTasks = completedTasks,
        isLoading = false,
        streak = STREAK,
        lastSync = NOW,
    )
}
