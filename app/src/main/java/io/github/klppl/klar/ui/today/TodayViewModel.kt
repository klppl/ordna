package io.github.klppl.klar.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.klppl.klar.R
import io.github.klppl.klar.data.local.TaskEntity
import io.github.klppl.klar.data.remote.GoogleTasksApi
import io.github.klppl.klar.data.repository.CompletionMethod
import io.github.klppl.klar.data.repository.LayoutDensity
import io.github.klppl.klar.data.repository.RoutinesPosition
import io.github.klppl.klar.data.repository.SettingsRepository
import io.github.klppl.klar.data.repository.TaskRepository
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
    val routineTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val routinesPosition: RoutinesPosition = RoutinesPosition.BOTTOM,
    val groupByList: Boolean = false,
    val completionMethod: CompletionMethod = CompletionMethod.BOTH,
    val layoutDensity: LayoutDensity = LayoutDensity.DEFAULT,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastSync: Instant? = null,
    val error: String? = null,
    val authExpired: Boolean = false,
    val streak: Int = 0,
    val filterableLists: List<ListChip> = emptyList(),
    val hiddenListIds: Set<String> = emptySet(),
) {
    val completedCount: Int get() = completedTasks.size
    val totalCount: Int
        get() = overdueTasks.size + todayTasks.size + routineTasks.size + completedTasks.size
    val allCompleted: Boolean get() = totalCount > 0 && completedCount == totalCount
}

@androidx.compose.runtime.Immutable
data class ListChip(val id: String, val title: String, val color: Int)

internal data class TaskInputs(
    val overdue: List<TaskEntity>,
    val today: List<TaskEntity>,
    val completed: List<TaskEntity>,
    val lastSync: Instant?,
    val groupByList: Boolean,
)

internal data class PartialSettings(
    val completionMethod: CompletionMethod,
    val layoutDensity: LayoutDensity,
    val isRefreshing: Boolean,
    val error: String?,
    val authExpired: Boolean,
)

internal data class SettingsInputs(
    val completionMethod: CompletionMethod,
    val layoutDensity: LayoutDensity,
    val isRefreshing: Boolean,
    val error: String?,
    val authExpired: Boolean,
    val streak: Int,
    val listOrder: List<String>,
    val hiddenListIds: Set<String>,
    val dailiesListId: String?,
    val routinesPosition: RoutinesPosition,
)

internal data class RoutinesConfig(
    val dailiesListId: String?,
    val position: RoutinesPosition,
)

internal fun buildUiState(tasks: TaskInputs, settings: SettingsInputs): TodayUiState {
    val comparator = TaskEntity.flatComparator(settings.listOrder)
    val hidden = settings.hiddenListIds

    // Chips reflect every list that currently has a task, regardless of the
    // active filter, so a hidden list can always be toggled back on.
    val filterable = (tasks.overdue + tasks.today + tasks.completed)
        .map { ListChip(it.listId, it.listTitle, it.listColor) }
        .distinctBy { it.id }
        .sortedBy { it.title }

    fun List<TaskEntity>.maybeSort() = if (tasks.groupByList) this else sortedWith(comparator)
    fun List<TaskEntity>.visible() = filter { it.listId !in hidden }

    // Active tasks in the designated dailies list are pulled into their own
    // Routines section and never shown as overdue/today. Completed routines are
    // left in the normal Completed section.
    val dailiesId = settings.dailiesListId
    fun TaskEntity.isRoutine() = dailiesId != null && listId == dailiesId

    val routines = (tasks.overdue + tasks.today).filter { it.isRoutine() }
    val overdue = tasks.overdue.filter { !it.isRoutine() }
    val today = tasks.today.filter { !it.isRoutine() }

    return TodayUiState(
        overdueTasks = overdue.maybeSort().visible(),
        todayTasks = today.maybeSort().visible(),
        routineTasks = routines.sortedWith(comparator).visible(),
        completedTasks = tasks.completed.visible(),
        routinesPosition = settings.routinesPosition,
        lastSync = tasks.lastSync,
        groupByList = tasks.groupByList,
        completionMethod = settings.completionMethod,
        layoutDensity = settings.layoutDensity,
        isRefreshing = settings.isRefreshing,
        isLoading = false,
        error = settings.error,
        authExpired = settings.authExpired,
        streak = settings.streak,
        filterableLists = filterable,
        hiddenListIds = hidden,
    )
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    private val _authExpired = MutableStateFlow(false)

    private val _streakRecorded = MutableStateFlow(false)

    private val _availableLists = MutableStateFlow<List<io.github.klppl.klar.ui.settings.TaskListOption>>(emptyList())
    val availableLists: StateFlow<List<io.github.klppl.klar.ui.settings.TaskListOption>> = _availableLists.asStateFlow()

    val createListId: StateFlow<String?> = settingsRepository.createListId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val createListTitle: StateFlow<String?> = settingsRepository.createListTitle
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val streakHistory: StateFlow<Set<java.time.LocalDate>> = settingsRepository.streakHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val taskFlow: kotlinx.coroutines.flow.Flow<TaskInputs> = combine(
        repository.getOverdueTasks(),
        repository.getTodayTasks(),
        repository.getCompletedTasks(),
        repository.lastSyncTime,
        settingsRepository.groupByList,
    ) { overdue, today, completed, lastSync, groupByList ->
        TaskInputs(overdue, today, completed, lastSync, groupByList)
    }

    // Settings has 7 inputs — combine's max typed overload is 5,
    // so split into two stages and merge.
    private val settingsFlow: kotlinx.coroutines.flow.Flow<SettingsInputs> = combine(
        combine(
            settingsRepository.completionMethod,
            settingsRepository.appLayoutDensity,
            _isRefreshing,
            _error,
            _authExpired,
        ) { method, density, refreshing, error, authExpired ->
            PartialSettings(method, density, refreshing, error, authExpired)
        },
        settingsRepository.streak,
        settingsRepository.listOrder,
        settingsRepository.hiddenListIds,
        combine(
            settingsRepository.dailiesListId,
            settingsRepository.routinesPosition,
        ) { dailiesListId, position -> RoutinesConfig(dailiesListId, position) },
    ) { partial, streak, listOrder, hiddenListIds, routines ->
        SettingsInputs(
            completionMethod = partial.completionMethod,
            layoutDensity = partial.layoutDensity,
            isRefreshing = partial.isRefreshing,
            error = partial.error,
            authExpired = partial.authExpired,
            streak = streak,
            listOrder = listOrder,
            hiddenListIds = hiddenListIds,
            dailiesListId = routines.dailiesListId,
            routinesPosition = routines.position,
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(taskFlow, settingsFlow, ::buildUiState)
        .stateIn(
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
                    io.github.klppl.klar.ui.settings.TaskListOption(it.id, it.title, it.color)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            val result = repository.sync()
            result.onSuccess { failedTitles ->
                if (failedTitles.isNotEmpty()) {
                    _error.value = getString(
                        R.string.error_partial_sync,
                        failedTitles.joinToString(", "),
                    )
                }
            }
            result.onFailure { e ->
                if (GoogleTasksApi.isAuthError(e)) {
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

    fun updateTaskTitle(task: TaskEntity, title: String) {
        viewModelScope.launch {
            repository.updateTaskTitle(task, title).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.error_task_update_failed)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.error_task_delete_failed)
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

    fun createTask(
        title: String,
        listId: String,
        listTitle: String,
        due: java.time.LocalDate = java.time.LocalDate.now(),
        notes: String? = null,
    ) {
        viewModelScope.launch {
            repository.createTask(title, listId, listTitle, due, notes).onFailure { e ->
                _error.value = e.localizedMessage ?: getString(R.string.create_task_error)
            }
        }
    }

    fun toggleListVisibility(listId: String) {
        viewModelScope.launch {
            settingsRepository.toggleListHidden(listId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
