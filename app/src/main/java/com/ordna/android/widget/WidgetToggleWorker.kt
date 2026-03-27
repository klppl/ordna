package com.ordna.android.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ordna.android.data.local.TaskDatabase
import com.ordna.android.data.remote.GoogleTasksApi
import com.ordna.android.data.repository.TaskRepository
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
            // API failed — revert optimistic update
            val dao = TaskDatabase.getInstance(applicationContext).taskDao()
            val revertStatus = if (completing) "needsAction" else "completed"
            val revertCompleted = if (completing) null else java.time.Instant.now()
            dao.updateTaskStatus(taskId, revertStatus, revertCompleted)
            // Room Flow will re-emit, but also trigger update() as safety
            updateAllWidgets(applicationContext)
            Result.retry()
        }
    }
}
