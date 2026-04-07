package io.github.klppl.ordna.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'needsAction' AND due IS NOT NULL AND due < :today
        ORDER BY due ASC, listTitle ASC
        """
    )
    fun getOverdueTasks(today: LocalDate): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'needsAction' AND due = :today
        ORDER BY position ASC, listTitle ASC
        """
    )
    fun getTodayTasks(today: LocalDate): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status = 'completed'
        ORDER BY completedAt DESC
        """
    )
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'needsAction' AND due IS NOT NULL AND due <= :today")
    suspend fun getActiveTasks(today: LocalDate): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'needsAction' AND due IS NOT NULL AND due <= :today")
    suspend fun getActiveTaskCount(today: LocalDate): Int

    @Query("SELECT * FROM tasks WHERE status = 'completed'")
    suspend fun getCompletedTasksList(): List<TaskEntity>

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: java.time.Instant?)

    @Query("UPDATE tasks SET title = :title WHERE id = :taskId")
    suspend fun updateTaskTitle(taskId: String, title: String)

    @Query("UPDATE tasks SET due = :due WHERE id = :taskId")
    suspend fun updateTaskDue(taskId: String, due: LocalDate)

    @Query("UPDATE tasks SET notes = :notes WHERE id = :taskId")
    suspend fun updateTaskNotes(taskId: String, notes: String?)

    @Query("DELETE FROM tasks WHERE id NOT IN (:ids) AND id NOT LIKE 'temp-%'")
    suspend fun deleteTasksNotIn(ids: List<String>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Transaction
    suspend fun syncReplace(tasks: List<TaskEntity>) {
        upsertAll(tasks)
        val validIds = tasks.map { it.id }
        if (validIds.isNotEmpty()) {
            deleteTasksNotIn(validIds)
        } else {
            deleteAll()
        }
    }
}
