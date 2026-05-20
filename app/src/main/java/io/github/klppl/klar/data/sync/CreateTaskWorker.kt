package io.github.klppl.klar.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.klppl.klar.data.local.TaskDatabase
import io.github.klppl.klar.data.remote.GoogleTasksApi
import io.github.klppl.klar.data.repository.TaskRepository
import io.github.klppl.klar.widget.PendingOperations
import io.github.klppl.klar.widget.updateAllWidgets
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate

/**
 * Retries a failed task creation. The temp entity stays in the local DB
 * until this worker succeeds, then it's swapped for the real server entity.
 */
@HiltWorker
class CreateTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: GoogleTasksApi,
    private val repository: TaskRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val tempId = inputData.getString("temp_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: return Result.failure()
        val listId = inputData.getString("list_id") ?: return Result.failure()
        val listTitle = inputData.getString("list_title") ?: return Result.failure()
        val dueDateStr = inputData.getString("due_date")
        val notes = inputData.getString("notes")
        val email = repository.getAccountEmail() ?: return Result.failure()

        val dao = TaskDatabase.getInstance(applicationContext).taskDao()
        val dueDate = dueDateStr?.let { LocalDate.parse(it) }

        return try {
            val created = api.createTask(email, listId, title, dueDate, notes)
            val listColor = GoogleTasksApi.colorForListId(listId)
            // Swap temp entity for real server entity
            dao.deleteById(tempId)
            dao.upsertAll(listOf(
                io.github.klppl.klar.data.local.TaskEntity(
                    id = created.id,
                    title = title,
                    due = dueDate,
                    dueDateTime = created.due,
                    status = created.status ?: io.github.klppl.klar.data.local.TaskStatus.NEEDS_ACTION,
                    completedAt = null,
                    listId = listId,
                    listTitle = listTitle,
                    listColor = listColor,
                    notes = created.notes,
                    position = created.position ?: "",
                    updated = GoogleTasksApi.parseCompletedAt(created.updated) ?: Instant.now(),
                )
            ))
            PendingOperations.remove(tempId)
            updateAllWidgets(applicationContext)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount >= 3) {
                // Give up — temp task stays in local DB for user to see.
                // Next manual sync will clean it up or user can retry.
                PendingOperations.remove(tempId)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}
