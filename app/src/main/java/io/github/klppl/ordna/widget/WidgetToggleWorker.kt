package io.github.klppl.ordna.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.klppl.ordna.MainActivity
import io.github.klppl.ordna.OrdnaApplication
import io.github.klppl.ordna.R
import io.github.klppl.ordna.data.local.TaskDatabase
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.data.repository.TaskRepository
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
        val taskTitle = inputData.getString("task_title") ?: ""
        val email = repository.getAccountEmail() ?: return Result.failure()

        return try {
            if (completing) {
                api.completeTask(email, listId, taskId)
            } else {
                api.uncompleteTask(email, listId, taskId)
            }
            PendingToggles.remove(taskId)
            Result.success()
        } catch (_: Exception) {
            PendingToggles.remove(taskId)
            // API failed — revert optimistic update
            val dao = TaskDatabase.getInstance(applicationContext).taskDao()
            val revertStatus = if (completing) "needsAction" else "completed"
            val revertCompleted = if (completing) null else java.time.Instant.now()
            dao.updateTaskStatus(taskId, revertStatus, revertCompleted)
            updateAllWidgets(applicationContext)

            // Notify user of the failure
            try {
                val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("SYNC_ON_LAUNCH", true)
                    putExtra("NAVIGATE_TODAY", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val tapPendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    taskId.hashCode(),
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val notification = NotificationCompat.Builder(applicationContext, OrdnaApplication.SYNC_FAIL_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(applicationContext.getString(R.string.sync_fail_title))
                    .setContentText(applicationContext.getString(R.string.sync_fail_body, taskTitle))
                    .setContentIntent(tapPendingIntent)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(taskId.hashCode(), notification)
            } catch (_: SecurityException) {
                // No notification permission — silent revert
            }

            Result.failure()
        }
    }
}
