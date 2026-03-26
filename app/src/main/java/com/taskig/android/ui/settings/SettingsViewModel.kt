package com.taskig.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskig.android.data.remote.GoogleTasksApi
import com.taskig.android.data.repository.CompletionMethod
import com.taskig.android.data.repository.LayoutDensity
import com.taskig.android.data.repository.SettingsRepository
import com.taskig.android.data.repository.TaskRepository
import com.taskig.android.data.repository.WidgetBackground
import com.taskig.android.data.repository.WidgetSorting
import com.taskig.android.widget.updateAllWidgets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListOption(val id: String, val title: String, val color: Int = 0)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val api: GoogleTasksApi,
) : AndroidViewModel(application) {

    val groupByList: StateFlow<Boolean> = settingsRepository.groupByList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val completionMethod: StateFlow<CompletionMethod> = settingsRepository.completionMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompletionMethod.BOTH)

    val appTheme: StateFlow<String> = settingsRepository.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    val appLayoutDensity: StateFlow<LayoutDensity> = settingsRepository.appLayoutDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LayoutDensity.DEFAULT)

    // Share list
    val shareListTitle: StateFlow<String?> = settingsRepository.shareListTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _availableLists = MutableStateFlow<List<TaskListOption>>(emptyList())
    val availableLists: StateFlow<List<TaskListOption>> = _availableLists.asStateFlow()

    private val _listsLoading = MutableStateFlow(false)
    val listsLoading: StateFlow<Boolean> = _listsLoading.asStateFlow()

    val widgetBackground: StateFlow<WidgetBackground> = settingsRepository.widgetBackground
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetBackground.AUTO)

    val widgetOpacity: StateFlow<Float> = settingsRepository.widgetOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    val widgetShowCompleted: StateFlow<Boolean> = settingsRepository.widgetShowCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val widgetLayoutDensity: StateFlow<LayoutDensity> = settingsRepository.widgetLayoutDensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LayoutDensity.DEFAULT)

    val widgetSorting: StateFlow<WidgetSorting> = settingsRepository.widgetSorting
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetSorting.FLAT)

    val listOrder: StateFlow<List<String>> = settingsRepository.listOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orderedLists: StateFlow<List<TaskListOption>> = combine(
        settingsRepository.listOrder,
        _availableLists,
    ) { order, lists ->
        if (lists.isEmpty()) return@combine emptyList()
        val listsById = lists.associateBy { it.id }
        val ordered = order.mapNotNull { listsById[it] }
        val remaining = lists.filter { it.id !in order }
        ordered + remaining
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val streak: StateFlow<Int> = settingsRepository.streak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
            refreshWidget()
        }
    }

    fun setGroupByList(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGroupByList(enabled) }
    }

    fun setCompletionMethod(method: CompletionMethod) {
        viewModelScope.launch { settingsRepository.setCompletionMethod(method) }
    }

    fun setAppLayoutDensity(density: LayoutDensity) {
        viewModelScope.launch { settingsRepository.setAppLayoutDensity(density) }
    }

    fun setWidgetBackground(bg: WidgetBackground) {
        viewModelScope.launch {
            settingsRepository.setWidgetBackground(bg)
            refreshWidget()
        }
    }

    fun setWidgetOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.setWidgetOpacity(opacity)
            refreshWidget()
        }
    }

    fun setWidgetShowCompleted(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWidgetShowCompleted(show)
            refreshWidget()
        }
    }

    fun setWidgetLayoutDensity(density: LayoutDensity) {
        viewModelScope.launch {
            settingsRepository.setWidgetLayoutDensity(density)
            refreshWidget()
        }
    }

    fun setWidgetSorting(sorting: WidgetSorting) {
        viewModelScope.launch {
            settingsRepository.setWidgetSorting(sorting)
            refreshWidget()
        }
    }

    fun setShareList(listId: String, listTitle: String) {
        viewModelScope.launch { settingsRepository.setShareList(listId, listTitle) }
    }

    fun loadAvailableLists() {
        if (_availableLists.value.isNotEmpty() || _listsLoading.value) return
        viewModelScope.launch {
            _listsLoading.value = true
            try {
                val email = taskRepository.getAccountEmail() ?: return@launch
                val lists = api.fetchTaskLists(email)
                _availableLists.value = lists.mapNotNull { list ->
                    val id = list.id ?: return@mapNotNull null
                    val title = list.title ?: "Untitled"
                    TaskListOption(id, title, GoogleTasksApi.colorForListId(id))
                }
                // Initialize list order with any new lists
                val current = listOrder.value
                val allIds = _availableLists.value.map { it.id }
                val merged = current.filter { it in allIds } + allIds.filter { it !in current }
                if (merged != current) {
                    settingsRepository.setListOrder(merged)
                }
            } catch (_: Exception) {
                // Silently fail — lists will be empty
            } finally {
                _listsLoading.value = false
            }
        }
    }

    fun moveListInOrder(listId: String, direction: Int) {
        viewModelScope.launch {
            // Use the displayed order (orderedLists) as the source of truth
            val current = orderedLists.value.map { it.id }.toMutableList()
            if (current.isEmpty()) return@launch
            val index = current.indexOf(listId)
            if (index < 0) return@launch
            val newIndex = (index + direction).coerceIn(0, current.size - 1)
            if (newIndex == index) return@launch
            current.removeAt(index)
            current.add(newIndex, listId)
            settingsRepository.setListOrder(current)
        }
    }

    private suspend fun refreshWidget() {
        updateAllWidgets(getApplication())
    }
}
