package io.github.klppl.ordna.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.klppl.ordna.R
import io.github.klppl.ordna.data.local.TaskEntity
import io.github.klppl.ordna.data.repository.CompletionMethod
import io.github.klppl.ordna.data.repository.LayoutDensity
import io.github.klppl.ordna.data.repository.SettingsRepository
import io.github.klppl.ordna.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class TodayUiState(
    val overdueTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val groupByList: Boolean = false,
    val completionMethod: CompletionMethod = CompletionMethod.BOTH,
    val layoutDensity: LayoutDensity = LayoutDensity.DEFAULT,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastSync: Instant? = null,
    val error: String? = null,
    val authExpired: Boolean = false,
    val streak: Int = 0,
) {
    val completedCount: Int get() = completedTasks.size
    val totalCount: Int get() = overdueTasks.size + todayTasks.size + completedTasks.size
    val allCompleted: Boolean get() = totalCount > 0 && completedCount == totalCount
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    private val _authExpired = MutableStateFlow(false)

    private val _streakRecorded = MutableStateFlow(false)

    private val _availableLists = MutableStateFlow<List<io.github.klppl.ordna.ui.settings.TaskListOption>>(emptyList())
    val availableLists: StateFlow<List<io.github.klppl.ordna.ui.settings.TaskListOption>> = _availableLists.asStateFlow()

    val createListId: StateFlow<String?> = settingsRepository.createListId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val createListTitle: StateFlow<String?> = settingsRepository.createListTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<TodayUiState> = combine(
        combine(
            repository.getOverdueTasks(),
            repository.getTodayTasks(),
            repository.getCompletedTasks(),
            repository.lastSyncTime,
            settingsRepository.groupByList,
        ) { overdue, today, completed, lastSync, groupByList ->
            arrayOf(overdue, today, completed, lastSync, groupByList)
        },
        combine(
            listOf<kotlinx.coroutines.flow.Flow<Any?>>(
                settingsRepository.completionMethod,
                settingsRepository.appLayoutDensity,
                _isRefreshing,
                _error,
                _authExpired,
                settingsRepository.streak,
                settingsRepository.listOrder,
            ),
        ) { values -> values },
    ) { first, second ->
        @Suppress("UNCHECKED_CAST")
        val groupByList = first[4] as Boolean
        val overdue = first[0] as List<TaskEntity>
        val today = first[1] as List<TaskEntity>
        val listOrder = second[6] as List<String>
        val comparator = TaskEntity.flatComparator(listOrder)
        TodayUiState(
            overdueTasks = if (groupByList) overdue else overdue.sortedWith(comparator),
            todayTasks = if (groupByList) today else today.sortedWith(comparator),
            completedTasks = first[2] as List<TaskEntity>,
            lastSync = first[3] as Instant?,
            groupByList = groupByList,
            completionMethod = second[0] as CompletionMethod,
            layoutDensity = second[1] as LayoutDensity,
            isRefreshing = second[2] as Boolean,
            isLoading = false,
            error = second[3] as String?,
            authExpired = second[4] as Boolean,
            streak = second[5] as Int,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    init {
        refresh()
        // Record streak when all tasks become completed; reset if overdue tasks exist
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.overdueTasks.isNotEmpty()) {
                    settingsRepository.resetStreak()
                } else if (state.allCompleted && !_streakRecorded.value) {
                    _streakRecorded.value = true
                    settingsRepository.recordAllDone()
                } else if (!state.allCompleted) {
                    _streakRecorded.value = false
                }
            }
        }
        // Populate available lists from sync cache (no extra API call)
        viewModelScope.launch {
            repository.cachedTaskLists.collect { lists ->
                _availableLists.value = lists.map {
                    io.github.klppl.ordna.ui.settings.TaskListOption(it.id, it.title, it.color)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = repository.sync()
            result.onFailure { e ->
                if (isAuthError(e)) {
                    _authExpired.value = true
                } else {
                    _error.value = e.localizedMessage ?: getString(R.string.error_sync_failed)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.error_task_update_failed)
            }
        }
    }

    fun updateTaskNotes(task: TaskEntity, notes: String?) {
        viewModelScope.launch {
            repository.updateTaskNotes(task, notes).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.error_task_update_failed)
            }
        }
    }

    fun postponeTask(task: TaskEntity, newDue: java.time.LocalDate) {
        viewModelScope.launch {
            repository.postponeTask(task, newDue).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.error_task_update_failed)
            }
        }
    }

    fun createTask(title: String, listId: String, listTitle: String) {
        viewModelScope.launch {
            repository.createTask(title, listId, listTitle).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.create_task_error)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun isAuthError(e: Throwable): Boolean {
        val message = e.message?.lowercase() ?: return false
        return "401" in message || "auth" in message || "unauthorized" in message
    }
}
