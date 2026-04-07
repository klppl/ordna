package io.github.klppl.ordna.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.data.repository.TaskRepository
import io.github.klppl.ordna.widget.PendingOperations
import io.github.klppl.ordna.widget.updateAllWidgets
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Retries a failed task mutation (toggle, title, notes, due, delete).
 * The optimistic local update stays in place while this worker retries.
 * On permanent failure, the next sync will correct local state from the API.
 */
@HiltWorker
class MutationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: GoogleTasksApi,
    private val repository: TaskRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val listId = inputData.getString("list_id") ?: return Result.failure()
        val operation = inputData.getString("operation") ?: return Result.failure()
        val email = repository.getAccountEmail() ?: return Result.failure()

        return try {
            when (operation) {
                "complete" -> api.completeTask(email, listId, taskId)
                "uncomplete" -> api.uncompleteTask(email, listId, taskId)
                "title" -> {
                    val title = inputData.getString("title") ?: return Result.failure()
                    api.updateTaskTitle(email, listId, taskId, title)
                }
                "notes" -> {
                    val notes = inputData.getString("notes") // null = clear notes
                    api.updateTaskNotes(email, listId, taskId, notes)
                }
                "due" -> {
                    val dueStr = inputData.getString("due") ?: return Result.failure()
                    api.updateTaskDue(email, listId, taskId, LocalDate.parse(dueStr))
                }
                "delete" -> api.deleteTask(email, listId, taskId)
                else -> return Result.failure()
            }
            PendingOperations.remove(taskId)
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount >= 3) {
                // Give up — next sync will correct local state from the API
                PendingOperations.remove(taskId)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
