package io.github.klppl.ordna.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.klppl.ordna.data.local.TaskDao
import io.github.klppl.ordna.data.local.TaskDatabase
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.widget.PendingToggles
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

    suspend fun postponeTask(task: TaskEntity, newDue: LocalDate): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        // Optimistic update — move the task in local DB immediately
        taskDao.updateTaskDue(task.id, newDue)
        updateAllWidgets(context)

        try {
            api.updateTaskDue(email, task.listId, task.id, newDue)
        } catch (e: Exception) {
            // Revert on failure
            task.due?.let { taskDao.updateTaskDue(task.id, it) }
            throw e
        }
    }

    suspend fun toggleTask(task: TaskEntity): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
        val isCompleting = task.status == "needsAction"

        // Optimistic update
        val now = if (isCompleting) Instant.now() else null
        val newStatus = if (isCompleting) "completed" else "needsAction"
        taskDao.updateTaskStatus(task.id, newStatus, now)
        updateAllWidgets(context)

        try {
            if (isCompleting) {
                api.completeTask(email, task.listId, task.id)
            } else {
                api.uncompleteTask(email, task.listId, task.id)
            }
        } catch (e: Exception) {
            // Revert on failure
            taskDao.updateTaskStatus(task.id, task.status, task.completedAt)
            throw e
        }
    }

    suspend fun updateTaskNotes(task: TaskEntity, notes: String?): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")

        // Optimistic update
        taskDao.updateTaskNotes(task.id, notes)

        try {
            api.updateTaskNotes(email, task.listId, task.id, notes)
        } catch (e: Exception) {
            // Revert on failure
            taskDao.updateTaskNotes(task.id, task.notes)
            throw e
        }
    }

    suspend fun createTask(title: String, listId: String, listTitle: String): Result<Unit> = runCatching {
        val email = getAccountEmail() ?: throw IllegalStateException("Not signed in")
        val today = LocalDate.now()
        val tempId = "temp-${java.util.UUID.randomUUID()}"
        val listColor = GoogleTasksApi.colorForListId(listId)

        // Optimistic insert
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
            // Replace temp entity with server entity (can't update PK, so delete + insert)
            taskDao.deleteById(tempId)
            taskDao.upsertAll(listOf(
                tempEntity.copy(
                    id = created.id,
                    position = created.position ?: "",
                    updated = GoogleTasksApi.parseCompletedAt(created.updated) ?: Instant.now(),
                )
            ))
            updateAllWidgets(context)
        } catch (e: Exception) {
            // Revert optimistic insert
            taskDao.deleteById(tempId)
            updateAllWidgets(context)
            throw e
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

            // Preserve optimistic updates for tasks with in-flight widget toggles.
            // Without this, sync would overwrite local state with stale API data.
            val pendingIds = PendingToggles.snapshot()
            if (pendingIds.isNotEmpty()) {
                val pendingLocal = pendingIds.mapNotNull { dao.getTaskById(it) }.associateBy { it.id }
                for (i in allTasks.indices) {
                    pendingLocal[allTasks[i].id]?.let { local ->
                        allTasks[i] = allTasks[i].copy(status = local.status, completedAt = local.completedAt)
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
