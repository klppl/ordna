package io.github.klppl.ordna.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.klppl.ordna.data.local.TaskDao
import io.github.klppl.ordna.data.local.TaskDatabase
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.data.sync.CreateTaskWorker
import io.github.klppl.ordna.data.sync.MutationWorker
import io.github.klppl.ordna.widget.PendingOperations
import io.github.klppl.ordna.widget.updateAllWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "ordna_prefs")

data class TaskListInfo(val id: String, val title: String, val color: Int)

@Singleton
class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val api: GoogleTasksApi,
) {
    private val accountEmailKey = stringPreferencesKey("account_email")
    private val lastSyncKey = longPreferencesKey("last_sync")

    private val _cachedTaskLists = MutableStateFlow<List<TaskListInfo>>(emptyList())
    val cachedTaskLists: StateFlow<List<TaskListInfo>> = _cachedTaskLists.asStateFlow()

    val accountEmail: Flow<String?> = context.dataStore.data.map { it[accountEmailKey] }

    val lastSyncTime: Flow<Instant?> = context.dataStore.data.map { prefs ->
        prefs[lastSyncKey]?.let { Instant.ofEpochMilli(it) }
    }

    suspend fun getAccountEmail(): String? =
        context.dataStore.data.first()[accountEmailKey]

    suspend fun saveAccountEmail(email: String) {
        context.dataStore.edit { it[accountEmailKey] = email }
    }

    suspend fun clearAccount() {
        context.dataStore.edit { it.clear() }
        taskDao.deleteAll()
    }

    fun getOverdueTasks(): Flow<List<TaskEntity>> =
        taskDao.getOverdueTasks(LocalDate.now())

    fun getTodayTasks(): Flow<List<TaskEntity>> =
        taskDao.getTodayTasks(LocalDate.now())

    fun getCompletedTasks(): Flow<List<TaskEntity>> =
        taskDao.getCompletedTasks()

    suspend fun sync(): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
        val taskLists = performSync(taskDao, api, email)
        _cachedTaskLists.value = taskLists
        context.dataStore.edit { it[lastSyncKey] = Instant.now().toEpochMilli() }
        updateAllWidgets(context)
    }

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun enqueueMutation(taskId: String, listId: String, operation: String, extras: Map<String, String?> = emptyMap()) {
        PendingOperations.add(taskId)
        val data = workDataOf(
            "task_id" to taskId,
            "list_id" to listId,
            "operation" to operation,
            *extras.map { (k, v) -> k to v }.toTypedArray(),
        )
        val work = OneTimeWorkRequestBuilder<MutationWorker>()
            .setInputData(data)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    suspend fun postponeTask(task: TaskEntity, newDue: LocalDate): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        taskDao.updateTaskDue(task.id, newDue)
        updateAllWidgets(context)

        try {
            api.updateTaskDue(email, task.listId, task.id, newDue)
        } catch (_: Exception) {
            enqueueMutation(task.id, task.listId, "due", mapOf("due" to newDue.toString()))
        }
    }

    suspend fun toggleTask(task: TaskEntity): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
        val isCompleting = task.status == "needsAction"

        val now = if (isCompleting) Instant.now() else null
        val newStatus = if (isCompleting) "completed" else "needsAction"
        taskDao.updateTaskStatus(task.id, newStatus, now)
        updateAllWidgets(context)

        try {
            if (isCompleting) api.completeTask(email, task.listId, task.id)
            else api.uncompleteTask(email, task.listId, task.id)
        } catch (_: Exception) {
            enqueueMutation(task.id, task.listId, if (isCompleting) "complete" else "uncomplete")
        }
    }

    suspend fun updateTaskTitle(task: TaskEntity, title: String): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        taskDao.updateTaskTitle(task.id, title)
        updateAllWidgets(context)

        try {
            api.updateTaskTitle(email, task.listId, task.id, title)
        } catch (_: Exception) {
            enqueueMutation(task.id, task.listId, "title", mapOf("title" to title))
        }
    }

    suspend fun deleteTask(task: TaskEntity): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        taskDao.deleteById(task.id)
        updateAllWidgets(context)

        try {
            api.deleteTask(email, task.listId, task.id)
        } catch (_: Exception) {
            enqueueMutation(task.id, task.listId, "delete")
        }
    }

    suspend fun updateTaskNotes(task: TaskEntity, notes: String?): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        taskDao.updateTaskNotes(task.id, notes)

        try {
            api.updateTaskNotes(email, task.listId, task.id, notes)
        } catch (_: Exception) {
            enqueueMutation(task.id, task.listId, "notes", mapOf("notes" to notes))
        }
    }

    suspend fun createTask(title: String, listId: String, listTitle: String): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
        val today = LocalDate.now()
        val tempId = "temp-${java.util.UUID.randomUUID()}"
        val listColor = GoogleTasksApi.colorForListId(listId)

        val tempEntity = TaskEntity(
            id = tempId,
            title = title,
            due = today,
            dueDateTime = "${today}T00:00:00.000Z",
            status = "needsAction",
            completedAt = null,
            listId = listId,
            listTitle = listTitle,
            listColor = listColor,
            position = "",
            updated = Instant.now(),
        )
        taskDao.upsertAll(listOf(tempEntity))
        updateAllWidgets(context)

        try {
            val created = api.createTask(email, listId, title, today)
            taskDao.deleteById(tempId)
            taskDao.upsertAll(listOf(
                tempEntity.copy(
                    id = created.id,
                    position = created.position ?: "",
                    updated = GoogleTasksApi.parseCompletedAt(created.updated) ?: Instant.now(),
                )
            ))
            updateAllWidgets(context)
        } catch (_: Exception) {
            // Keep temp task visible, enqueue retry with network constraint
            PendingOperations.add(tempId)
            val work = OneTimeWorkRequestBuilder<CreateTaskWorker>()
                .setInputData(workDataOf(
                    "temp_id" to tempId,
                    "title" to title,
                    "list_id" to listId,
                    "list_title" to listTitle,
                    "due_date" to today.toString(),
                ))
                .setConstraints(networkConstraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }

    companion object {
        /**
         * Standalone sync for use outside of Hilt (e.g. widget refresh action).
         * Creates its own API + DB instances, reuses the shared sync logic.
         */
        suspend fun syncForWidget(context: Context) {
            val appContext = context.applicationContext
            val api = GoogleTasksApi(appContext)
            val dao = TaskDatabase.getInstance(appContext).taskDao()
            val prefs = appContext.dataStore.data.first()
            val email = prefs[stringPreferencesKey("account_email")] ?: return

            performSync(dao, api, email)

            appContext.dataStore.edit { it[longPreferencesKey("last_sync")] = Instant.now().toEpochMilli() }
            updateAllWidgets(appContext)
        }

        /**
         * Core sync logic shared by both Hilt-injected sync() and standalone syncForWidget().
         */
        private suspend fun performSync(dao: TaskDao, api: GoogleTasksApi, email: String): List<TaskListInfo> {
            val today = LocalDate.now()
            val taskLists = api.fetchTaskLists(email)
            val allTasks = mutableListOf<TaskEntity>()

            for (list in taskLists) {
                val listId = list.id ?: continue
                val listTitle = list.title ?: "Untitled"
                val listColor = GoogleTasksApi.colorForListId(listId)

                val tasks = try {
                    api.fetchTasks(email, listId)
                } catch (_: Exception) {
                    continue
                }

                for (task in tasks) {
                    val taskId = task.id ?: continue
                    val title = task.title ?: ""
                    if (title.isBlank()) continue

                    val due = GoogleTasksApi.parseDueDate(task.due)
                    val status = task.status ?: "needsAction"
                    val completedAt = GoogleTasksApi.parseCompletedAt(task.completed)

                    val includeActive = status == "needsAction" && due != null && due <= today
                    val isCompletedToday = status == "completed" && completedAt != null &&
                            completedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today

                    if (includeActive || isCompletedToday) {
                        allTasks.add(
                            TaskEntity(
                                id = taskId,
                                title = title,
                                due = due,
                                dueDateTime = task.due,
                                status = status,
                                completedAt = completedAt,
                                listId = listId,
                                listTitle = listTitle,
                                listColor = listColor,
                                notes = task.notes,
                                position = task.position ?: "",
                                updated = GoogleTasksApi.parseCompletedAt(task.updated)
                                    ?: Instant.now(),
                            )
                        )
                    }
                }
            }

            // Preserve optimistic updates for tasks with in-flight operations.
            // Without this, sync would overwrite local state with stale API data.
            val pendingIds = PendingOperations.snapshot()
            if (pendingIds.isNotEmpty()) {
                val pendingLocal = pendingIds.mapNotNull { dao.getTaskById(it) }.associateBy { it.id }
                for (i in allTasks.indices) {
                    pendingLocal[allTasks[i].id]?.let { local ->
                        // Replace entirely — any field could have been optimistically updated
                        allTasks[i] = local
                    }
                }
                // Also ensure pending tasks that sync excluded (e.g. just-completed) stay in the list
                for ((id, local) in pendingLocal) {
                    if (allTasks.none { it.id == id }) {
                        allTasks.add(local)
                    }
                }
            }

            dao.syncReplace(allTasks)
            return taskLists.mapNotNull { list ->
                val id = list.id ?: return@mapNotNull null
                TaskListInfo(id, list.title ?: "Untitled", GoogleTasksApi.colorForListId(id))
            }
        }
    }
}
