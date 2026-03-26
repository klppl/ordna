package com.taskig.android.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskig.android.data.remote.GoogleTasksApi
import com.taskig.android.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetToggleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: GoogleTasksApi,
    private val repository: TaskRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val listId = inputData.getString("list_id") ?: return Result.failure()
        val completing = inputData.getBoolean("completing", true)
        val email = repository.getAccountEmail() ?: return Result.failure()

        return try {
            if (completing) {
                api.completeTask(email, listId, taskId)
            } else {
                api.uncompleteTask(email, listId, taskId)
            }
            Result.success()
        } catch (_: Exception) {
            // API failed — revert optimistic update and refresh widget
            val db = com.taskig.android.data.local.TaskDatabase.getWidgetInstance(applicationContext)
            val revertStatus = if (completing) "needsAction" else "completed"
            val revertCompleted = if (completing) null else java.time.Instant.now()
            db.taskDao().updateTaskStatus(taskId, revertStatus, revertCompleted)
            // Reload in-memory data so widget recomposes with reverted state
            WidgetDataSource.reload(applicationContext)
            Result.retry()
        }
    }
}
