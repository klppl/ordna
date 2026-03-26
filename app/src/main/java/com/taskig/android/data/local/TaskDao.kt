package com.taskig.android.data.local

import androidx.room.Dao
import androidx.room.Query
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

    @Query("SELECT * FROM tasks WHERE status = 'needsAction' AND due IS NOT NULL AND due <= :today")
    suspend fun getActiveTasks(today: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'completed'")
    suspend fun getCompletedTasksList(): List<TaskEntity>

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: java.time.Instant?)

    @Query("UPDATE tasks SET due = :due WHERE id = :taskId")
    suspend fun updateTaskDue(taskId: String, due: LocalDate)

    @Query("DELETE FROM tasks WHERE id NOT IN (:ids)")
    suspend fun deleteTasksNotIn(ids: List<String>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
