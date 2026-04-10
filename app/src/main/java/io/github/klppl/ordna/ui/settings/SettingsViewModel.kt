package io.github.klppl.ordna.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.klppl.ordna.data.remote.GoogleTasksApi
import io.github.klppl.ordna.data.sync.ReminderScheduler
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.klppl.ordna.data.repository.CompletionMethod
import io.github.klppl.ordna.data.repository.LayoutDensity
import io.github.klppl.ordna.data.repository.SettingsRepository
import io.github.klppl.ordna.data.repository.TaskRepository
import io.github.klppl.ordna.data.repository.WidgetBackground
import io.github.klppl.ordna.data.repository.WidgetSorting
import io.github.klppl.ordna.widget.updateAllWidgets
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

data class AppSettingsState(
    val language: String = "system",
    val appTheme: String = "SYSTEM",
    val groupByList: Boolean = false,
    val completionMethod: CompletionMethod = CompletionMethod.BOTH,
    val layoutDensity: LayoutDensity = LayoutDensity.DEFAULT,
)

data class WidgetSettingsState(
    val background: WidgetBackground = WidgetBackground.AUTO,
    val opacity: Float = 1f,
    val showCompleted: Boolean = true,
    val layoutDensity: LayoutDensity = LayoutDensity.DEFAULT,
    val sorting: WidgetSorting = WidgetSorting.FLAT,
)

data class ReminderSettingsState(
    val enabled: Boolean = false,
    val morningEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val middayEnabled: Boolean = true,
    val middayHour: Int = 12,
    val middayMinute: Int = 0,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val api: GoogleTasksApi,
    private val reminderScheduler: ReminderScheduler,
) : AndroidViewModel(application) {

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

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

    // Create list
    val createListTitle: StateFlow<String?> = settingsRepository.createListTitle
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

    val vacationMode: StateFlow<Boolean> = settingsRepository.vacationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setVacationMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVacationMode(enabled) }
    }

    // -- Reminders --
    val remindersEnabled: StateFlow<Boolean> = settingsRepository.remindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val reminderMorningEnabled: StateFlow<Boolean> = settingsRepository.reminderMorningEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val reminderMorningHour: StateFlow<Int> = settingsRepository.reminderMorningHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 8)

    val reminderMorningMinute: StateFlow<Int> = settingsRepository.reminderMorningMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val reminderMiddayEnabled: StateFlow<Boolean> = settingsRepository.reminderMiddayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val reminderMiddayHour: StateFlow<Int> = settingsRepository.reminderMiddayHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 12)

    val reminderMiddayMinute: StateFlow<Int> = settingsRepository.reminderMiddayMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val reminderEveningEnabled: StateFlow<Boolean> = settingsRepository.reminderEveningEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val reminderEveningHour: StateFlow<Int> = settingsRepository.reminderEveningHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 18)

    val reminderEveningMinute: StateFlow<Int> = settingsRepository.reminderEveningMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // -- Combined flows for efficient UI collection --

    val appSettingsState: StateFlow<AppSettingsState> = combine(
        settingsRepository.language,
        settingsRepository.appTheme,
        settingsRepository.groupByList,
        settingsRepository.completionMethod,
        settingsRepository.appLayoutDensity,
    ) { lang, theme, group, completion, density ->
        AppSettingsState(lang, theme, group, completion, density)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsState())

    val widgetSettingsState: StateFlow<WidgetSettingsState> = combine(
        settingsRepository.widgetBackground,
        settingsRepository.widgetOpacity,
        settingsRepository.widgetShowCompleted,
        settingsRepository.widgetLayoutDensity,
        settingsRepository.widgetSorting,
    ) { bg, opacity, show, density, sorting ->
        WidgetSettingsState(bg, opacity, show, density, sorting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WidgetSettingsState())

    val reminderSettingsState: StateFlow<ReminderSettingsState> = combine(
        settingsRepository.remindersEnabled,
        combine(
            settingsRepository.reminderMorningEnabled,
            settingsRepository.reminderMorningHour,
            settingsRepository.reminderMorningMinute,
        ) { e, h, m -> Triple(e, h, m) },
        combine(
            settingsRepository.reminderMiddayEnabled,
            settingsRepository.reminderMiddayHour,
            settingsRepository.reminderMiddayMinute,
        ) { e, h, m -> Triple(e, h, m) },
        combine(
            settingsRepository.reminderEveningEnabled,
            settingsRepository.reminderEveningHour,
            settingsRepository.reminderEveningMinute,
        ) { e, h, m -> Triple(e, h, m) },
    ) { enabled, morning, midday, evening ->
        ReminderSettingsState(
            enabled = enabled,
            morningEnabled = morning.first, morningHour = morning.second, morningMinute = morning.third,
            middayEnabled = midday.first, middayHour = midday.second, middayMinute = midday.third,
            eveningEnabled = evening.first, eveningHour = evening.second, eveningMinute = evening.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettingsState())

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(lang)
            val locales = if (lang == "system") LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(lang)
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

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

    fun setCreateList(listId: String, listTitle: String) {
        viewModelScope.launch { settingsRepository.setCreateList(listId, listTitle) }
    }

    fun clearCreateList() {
        viewModelScope.launch { settingsRepository.clearCreateList() }
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

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            if (enabled) reminderScheduler.scheduleAll()
            else reminderScheduler.cancelAll()
        }
    }

    fun setReminderMorningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderMorningEnabled(enabled)
            if (enabled) {
                val s = reminderSettingsState.value
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MORNING, s.morningHour, s.morningMinute)
            } else {
                reminderScheduler.cancelSlot(ReminderScheduler.SLOT_MORNING)
            }
        }
    }

    fun setReminderMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderMorningTime(hour, minute)
            val s = reminderSettingsState.value
            if (s.enabled && s.morningEnabled) {
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MORNING, hour, minute)
            }
        }
    }

    fun setReminderMiddayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderMiddayEnabled(enabled)
            if (enabled) {
                val s = reminderSettingsState.value
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MIDDAY, s.middayHour, s.middayMinute)
            } else {
                reminderScheduler.cancelSlot(ReminderScheduler.SLOT_MIDDAY)
            }
        }
    }

    fun setReminderMiddayTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderMiddayTime(hour, minute)
            val s = reminderSettingsState.value
            if (s.enabled && s.middayEnabled) {
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_MIDDAY, hour, minute)
            }
        }
    }

    fun setReminderEveningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEveningEnabled(enabled)
            if (enabled) {
                val s = reminderSettingsState.value
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_EVENING, s.eveningHour, s.eveningMinute)
            } else {
                reminderScheduler.cancelSlot(ReminderScheduler.SLOT_EVENING)
            }
        }
    }

    fun setReminderEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderEveningTime(hour, minute)
            val s = reminderSettingsState.value
            if (s.enabled && s.eveningEnabled) {
                reminderScheduler.scheduleSlot(ReminderScheduler.SLOT_EVENING, hour, minute)
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            reminderScheduler.cancelAll()
            taskRepository.clearAccount()
            onComplete()
        }
    }

    private suspend fun refreshWidget() {
        updateAllWidgets(getApplication())
    }
}
