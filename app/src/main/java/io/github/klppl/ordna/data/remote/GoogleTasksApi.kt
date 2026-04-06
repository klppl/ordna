package io.github.klppl.ordna.data.remote

import android.accounts.Account
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleTasksApi @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cachedEmail: String? = null
    private var cachedService: Tasks? = null

    private fun getService(accountEmail: String): Tasks {
        cachedService?.let { if (cachedEmail == accountEmail) return it }
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(TasksScopes.TASKS)
        )
        credential.selectedAccount = Account(accountEmail, "com.google")
        return Tasks.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Ordna").build().also {
            cachedEmail = accountEmail
            cachedService = it
        }
    }

    suspend fun fetchTaskLists(accountEmail: String): List<TaskList> = withContext(Dispatchers.IO) {
        getService(accountEmail).tasklists().list()
            .setMaxResults(100)
            .execute()
            .items ?: emptyList()
    }

    suspend fun fetchTasks(
        accountEmail: String,
        listId: String,
    ): List<Task> = withContext(Dispatchers.IO) {
        val allTasks = mutableListOf<Task>()
        var pageToken: String? = null

        do {
            val request = getService(accountEmail).tasks().list(listId)
                .setShowCompleted(true)
                .setShowHidden(true)
                .setMaxResults(100)
            if (pageToken != null) request.pageToken = pageToken

            val result = request.execute()
            result.items?.let { allTasks.addAll(it) }
            pageToken = result.nextPageToken
        } while (pageToken != null)

        allTasks
    }

    suspend fun completeTask(
        accountEmail: String,
        listId: String,
        taskId: String,
    ) = withContext(Dispatchers.IO) {
        val patch = Task().apply { status = "completed" }
        getService(accountEmail).tasks().patch(listId, taskId, patch).execute()
    }

    suspend fun uncompleteTask(
        accountEmail: String,
        listId: String,
        taskId: String,
    ) = withContext(Dispatchers.IO) {
        val patch = Task().apply { status = "needsAction" }
        getService(accountEmail).tasks().patch(listId, taskId, patch).execute()
    }

    suspend fun createTask(
        accountEmail: String,
        listId: String,
        title: String,
        due: java.time.LocalDate? = null,
        notes: String? = null,
    ): com.google.api.services.tasks.model.Task = withContext(Dispatchers.IO) {
        val task = Task().setTitle(title).setStatus("needsAction")
        if (due != null) {
            task.due = "${due}T00:00:00.000Z"
        }
        if (!notes.isNullOrBlank()) {
            task.notes = notes
        }
        getService(accountEmail).tasks().insert(listId, task).execute()
    }

    suspend fun updateTaskNotes(
        accountEmail: String,
        listId: String,
        taskId: String,
        notes: String?,
    ) = withContext(Dispatchers.IO) {
        val service = getService(accountEmail)
        val task = service.tasks().get(listId, taskId).execute()
        task.notes = notes
        service.tasks().update(listId, taskId, task).execute()
    }

    suspend fun updateTaskDue(
        accountEmail: String,
        listId: String,
        taskId: String,
        newDue: LocalDate,
    ) = withContext(Dispatchers.IO) {
        val service = getService(accountEmail)
        val task = service.tasks().get(listId, taskId).execute()
        // Google Tasks expects due as RFC 3339 date-time at midnight UTC
        task.due = "${newDue}T00:00:00.000Z"
        service.tasks().update(listId, taskId, task).execute()
    }

    companion object {
        // Deterministic color assignment from list ID
        private val PALETTE = intArrayOf(
            0xFFF44336.toInt(), // Red
            0xFFFF9800.toInt(), // Orange
            0xFFFFC107.toInt(), // Amber
            0xFF4CAF50.toInt(), // Green
            0xFF009688.toInt(), // Teal
            0xFF2196F3.toInt(), // Blue
            0xFF9C27B0.toInt(), // Purple
            0xFFE91E63.toInt(), // Pink
        )

        fun colorForListId(listId: String): Int {
            val index = (listId.hashCode() and Int.MAX_VALUE) % PALETTE.size
            return PALETTE[index]
        }

        fun parseDueDate(rfc3339: String?): LocalDate? {
            if (rfc3339 == null) return null
            // Google Tasks due dates are calendar dates stored as midnight UTC
            // e.g. "2026-03-24T00:00:00.000Z" means March 24, NOT a point in time.
            // Always extract the date portion directly — never timezone-convert.
            return try {
                LocalDate.parse(rfc3339.substring(0, 10))
            } catch (_: Exception) {
                null
            }
        }

        fun parseCompletedAt(rfc3339: String?): Instant? {
            if (rfc3339 == null) return null
            return try {
                Instant.parse(rfc3339)
            } catch (_: Exception) {
                null
            }
        }
    }
}
