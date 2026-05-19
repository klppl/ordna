package io.github.klppl.ordna.data.repository

import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import io.github.klppl.ordna.data.local.FakeTaskDao
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.widget.PendingOperations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TaskRepositoryPerformSyncTest {

    private val email = "test@example.com"
    private val today: LocalDate = LocalDate.now()
    private val yesterday: LocalDate = today.minusDays(1)
    private val tomorrow: LocalDate = today.plusDays(1)

    private lateinit var dao: FakeTaskDao
    private lateinit var api: GoogleTasksApi

    @Before
    fun setUp() {
        dao = FakeTaskDao()
        api = mockk()
        PendingOperations.snapshot().forEach { PendingOperations.remove(it) }
    }

    @After
    fun tearDown() {
        PendingOperations.snapshot().forEach { PendingOperations.remove(it) }
    }

    private fun taskList(id: String, title: String) = TaskList().apply {
        this.id = id
        this.title = title
    }

    private fun apiTask(
        id: String,
        title: String,
        due: LocalDate? = null,
        status: String = "needsAction",
        completedAt: Instant? = null,
    ) = Task().apply {
        this.id = id
        this.title = title
        this.status = status
        if (due != null) this.due = "${due}T00:00:00.000Z"
        if (completedAt != null) this.completed = completedAt.toString()
    }

    @Test
    fun `performSync includes overdue and today, ignores future and no-due`() = runTest {
        coEvery { api.fetchTaskLists(email) } returns listOf(taskList("list1", "Work"))
        coEvery { api.fetchTasks(email, "list1") } returns listOf(
            apiTask("t1", "Today task", due = today),
            apiTask("t2", "Overdue task", due = yesterday),
            apiTask("t3", "Future task", due = tomorrow),
            apiTask("t4", "No due date"),
        )

        TaskRepository.performSync(dao, api, email)

        val storedIds = dao.snapshot().map { it.id }.sorted()
        assertEquals(listOf("t1", "t2"), storedIds)
    }

    @Test
    fun `performSync includes tasks completed today, excludes those completed earlier`() = runTest {
        coEvery { api.fetchTaskLists(email) } returns listOf(taskList("list1", "Work"))
        coEvery { api.fetchTasks(email, "list1") } returns listOf(
            apiTask(
                id = "c1",
                title = "Completed today",
                status = "completed",
                completedAt = Instant.now(),
            ),
            apiTask(
                id = "c2",
                title = "Completed yesterday",
                status = "completed",
                completedAt = Instant.now().minusSeconds(60L * 60L * 30L), // ~30h ago
            ),
        )

        TaskRepository.performSync(dao, api, email)

        val storedIds = dao.snapshot().map { it.id }
        assertEquals(listOf("c1"), storedIds)
    }

    @Test
    fun `performSync collects failed list titles when fetchTasks throws`() = runTest {
        coEvery { api.fetchTaskLists(email) } returns listOf(
            taskList("list1", "Work"),
            taskList("list2", "Personal"),
        )
        coEvery { api.fetchTasks(email, "list1") } returns emptyList()
        coEvery { api.fetchTasks(email, "list2") } throws RuntimeException("403")

        val outcome = TaskRepository.performSync(dao, api, email)

        assertEquals(listOf("Personal"), outcome.failedListTitles)
        assertEquals(2, outcome.taskLists.size)
    }

    @Test
    fun `performSync preserves local state for tasks in PendingOperations`() = runTest {
        // Local DB has t1 marked completed — represents an in-flight optimistic update.
        val localTask = TaskEntity(
            id = "t1",
            title = "Locally completed",
            due = today,
            dueDateTime = "${today}T00:00:00.000Z",
            status = "completed",
            completedAt = Instant.now(),
            listId = "list1",
            listTitle = "Work",
            listColor = GoogleTasksApi.colorForListId("list1"),
            position = "",
            updated = Instant.now(),
        )
        dao.seed(listOf(localTask))
        PendingOperations.add("t1")

        // API still reports t1 as needsAction.
        coEvery { api.fetchTaskLists(email) } returns listOf(taskList("list1", "Work"))
        coEvery { api.fetchTasks(email, "list1") } returns listOf(
            apiTask("t1", "Locally completed", due = today, status = "needsAction"),
        )

        TaskRepository.performSync(dao, api, email)

        val stored = dao.snapshot()
        assertEquals(1, stored.size)
        assertEquals("completed", stored[0].status)
    }
}
