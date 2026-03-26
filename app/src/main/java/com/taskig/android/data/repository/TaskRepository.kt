package com.taskig.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskig.android.data.local.TaskDao
import com.taskig.android.data.local.TaskEntity
import com.taskig.android.data.remote.GoogleTasksApi
import com.taskig.android.widget.updateAllWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "taskig_prefs")

@Singleton
class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val api: GoogleTasksApi,
) {
    private val accountEmailKey = stringPreferencesKey("account_email")
    private val lastSyncKey = longPreferencesKey("last_sync")

    val accountEmail: Flow<String?> = context.dataStore.data.map { it[accountEmailKey] }

    val lastSyncTime: Flow<Instant?> = context.dataStore.data.map { prefs ->
        prefs[lastSyncKey]?.let { Instant.ofEpochMilli(it) }
    }

    suspend fun getAccountEmail(): String? =
        context.dataStore.data.first()[accountEmailKey]

    suspend fun saveAccountEmail(email: String) {
        context.dataStore.data.first() // ensure initialized
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
                // If one list fails, continue with others
                continue
            }

            for (task in tasks) {
                val taskId = task.id ?: continue
                val title = task.title ?: ""
                if (title.isBlank()) continue // skip blank tasks

                val due = GoogleTasksApi.parseDueDate(task.due)
                val status = task.status ?: "needsAction"
                val completedAt = GoogleTasksApi.parseCompletedAt(task.completed)

                // Include active tasks that are due today or overdue (must have a due date)
                val includeActive = status == "needsAction" && due != null && due <= today

                // Include completed tasks only if completed today
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
                            position = task.position ?: "",
                            updated = GoogleTasksApi.parseCompletedAt(task.updated)
                                ?: Instant.now(),
                        )
                    )
                }
            }
        }

        taskDao.upsertAll(allTasks)
        val validIds = allTasks.map { it.id }
        if (validIds.isNotEmpty()) {
            taskDao.deleteTasksNotIn(validIds)
        } else {
            taskDao.deleteAll()
        }

        context.dataStore.edit { it[lastSyncKey] = Instant.now().toEpochMilli() }
        updateAllWidgets(context)
    }

    companion object {
        /**
         * Standalone sync for use outside of Hilt (e.g. widget refresh action).
         * Creates its own API + DB instances and runs the full sync cycle.
         */
        suspend fun syncForWidget(context: Context) {
            val appContext = context.applicationContext
            val api = GoogleTasksApi(appContext)
            val db = com.taskig.android.data.local.TaskDatabase.getWidgetInstance(appContext)
            val dao = db.taskDao()
            val prefs = appContext.dataStore.data.first()
            val email = prefs[stringPreferencesKey("account_email")] ?: return
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
                                position = task.position ?: "",
                                updated = GoogleTasksApi.parseCompletedAt(task.updated)
                                    ?: Instant.now(),
                            )
                        )
                    }
                }
            }

            dao.upsertAll(allTasks)
            val validIds = allTasks.map { it.id }
            if (validIds.isNotEmpty()) {
                dao.deleteTasksNotIn(validIds)
            } else {
                dao.deleteAll()
            }

            appContext.dataStore.edit { it[longPreferencesKey("last_sync")] = Instant.now().toEpochMilli() }
            updateAllWidgets(appContext)
        }
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
}
