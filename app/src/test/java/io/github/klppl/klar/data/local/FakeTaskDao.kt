package io.github.klppl.klar.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate

/**
 * In-memory implementation of [TaskDao] for unit tests. Only the methods
 * exercised by tests need to be correct; the rest return empty defaults.
 */
class FakeTaskDao : TaskDao {

    private val store = mutableListOf<TaskEntity>()

    fun snapshot(): List<TaskEntity> = store.toList()

    fun seed(tasks: List<TaskEntity>) {
        store.clear()
        store.addAll(tasks)
    }

    override fun getOverdueTasks(today: LocalDate): Flow<List<TaskEntity>> =
        flowOf(store.filter { it.status == "needsAction" && it.due != null && it.due < today })

    override fun getTodayTasks(today: LocalDate): Flow<List<TaskEntity>> =
        flowOf(store.filter { it.status == "needsAction" && it.due == today })

    override fun getCompletedTasks(): Flow<List<TaskEntity>> =
        flowOf(store.filter { it.status == "completed" })

    override suspend fun getTaskById(taskId: String): TaskEntity? =
        store.find { it.id == taskId }

    override suspend fun getActiveTasks(today: LocalDate): List<TaskEntity> =
        store.filter { it.status == "needsAction" && it.due != null && it.due <= today }

    override suspend fun getActiveTaskCount(today: LocalDate): Int =
        getActiveTasks(today).size

    override suspend fun getCompletedTasksList(): List<TaskEntity> =
        store.filter { it.status == "completed" }

    override suspend fun upsertAll(tasks: List<TaskEntity>) {
        for (task in tasks) {
            store.removeAll { it.id == task.id }
            store.add(task)
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Instant?) {
        val idx = store.indexOfFirst { it.id == taskId }
        if (idx >= 0) store[idx] = store[idx].copy(status = status, completedAt = completedAt)
    }

    override suspend fun updateTaskTitle(taskId: String, title: String) {
        val idx = store.indexOfFirst { it.id == taskId }
        if (idx >= 0) store[idx] = store[idx].copy(title = title)
    }

    override suspend fun updateTaskDue(taskId: String, due: LocalDate) {
        val idx = store.indexOfFirst { it.id == taskId }
        if (idx >= 0) store[idx] = store[idx].copy(due = due)
    }

    override suspend fun updateTaskNotes(taskId: String, notes: String?) {
        val idx = store.indexOfFirst { it.id == taskId }
        if (idx >= 0) store[idx] = store[idx].copy(notes = notes)
    }

    override suspend fun deleteTasksNotIn(ids: List<String>) {
        store.removeAll { it.id !in ids && !it.id.startsWith("temp-") }
    }

    override suspend fun deleteById(taskId: String) {
        store.removeAll { it.id == taskId }
    }

    override suspend fun deleteAll() {
        store.clear()
    }
}
