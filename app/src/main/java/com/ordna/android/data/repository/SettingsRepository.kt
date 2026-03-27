package com.ordna.android.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore by preferencesDataStore(name = "ordna_settings")

enum class CompletionMethod { CHECKBOX, SWIPE, BOTH }

enum class WidgetBackground { AUTO, WHITE, DARK, BLACK }

enum class LayoutDensity { COMFORTABLE, DEFAULT, COMPACT }

enum class WidgetSorting { FLAT, BY_LIST }

data class WidgetSettings(
    val background: WidgetBackground = WidgetBackground.AUTO,
    val opacity: Float = 1f,
    val showCompleted: Boolean = true,
    val layoutDensity: LayoutDensity = LayoutDensity.DEFAULT,
    val sorting: WidgetSorting = WidgetSorting.FLAT,
    val theme: String = "SYSTEM",
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val languageKey = stringPreferencesKey("language")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val groupByListKey = booleanPreferencesKey("group_by_list")
    private val completionMethodKey = stringPreferencesKey("completion_method")
    private val appLayoutDensityKey = stringPreferencesKey("app_layout_density")
    private val shareListIdKey = stringPreferencesKey("share_list_id")
    private val shareListTitleKey = stringPreferencesKey("share_list_title")
    private val createListIdKey = stringPreferencesKey("create_list_id")
    private val createListTitleKey = stringPreferencesKey("create_list_title")

    private val streakCountKey = intPreferencesKey("streak_count")
    private val streakLastDateKey = stringPreferencesKey("streak_last_date")

    private val listOrderKey = stringPreferencesKey("list_order")

    private val widgetBgKey = stringPreferencesKey("widget_bg")
    private val widgetOpacityKey = floatPreferencesKey("widget_opacity")
    private val widgetShowCompletedKey = booleanPreferencesKey("widget_show_completed")
    private val widgetLayoutDensityKey = stringPreferencesKey("widget_layout_density")
    private val widgetSortingKey = stringPreferencesKey("widget_sorting")

    // -- App settings --

    /** Language: "system", "en", or "sv" */
    val language: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[languageKey] ?: "system"
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[languageKey] = lang }
    }

    val appTheme: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[appThemeKey] ?: "SYSTEM"
    }

    suspend fun setAppTheme(theme: String) {
        context.settingsDataStore.edit { it[appThemeKey] = theme }
    }

    val groupByList: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[groupByListKey] ?: false
    }

    val completionMethod: Flow<CompletionMethod> = context.settingsDataStore.data.map { prefs ->
        when (prefs[completionMethodKey]) {
            "CHECKBOX" -> CompletionMethod.CHECKBOX
            "SWIPE" -> CompletionMethod.SWIPE
            "BOTH" -> CompletionMethod.BOTH
            else -> CompletionMethod.BOTH
        }
    }

    val appLayoutDensity: Flow<LayoutDensity> = context.settingsDataStore.data.map { prefs ->
        LayoutDensity.entries.find { it.name == prefs[appLayoutDensityKey] } ?: LayoutDensity.DEFAULT
    }

    suspend fun setGroupByList(enabled: Boolean) {
        context.settingsDataStore.edit { it[groupByListKey] = enabled }
    }

    suspend fun setCompletionMethod(method: CompletionMethod) {
        context.settingsDataStore.edit { it[completionMethodKey] = method.name }
    }

    suspend fun setAppLayoutDensity(density: LayoutDensity) {
        context.settingsDataStore.edit { it[appLayoutDensityKey] = density.name }
    }

    // -- List order --

    val listOrder: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[listOrderKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setListOrder(order: List<String>) {
        context.settingsDataStore.edit { it[listOrderKey] = order.joinToString(",") }
    }

    // -- Share settings --

    val shareListId: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[shareListIdKey]
    }

    val shareListTitle: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[shareListTitleKey]
    }

    suspend fun setShareList(listId: String, listTitle: String) {
        context.settingsDataStore.edit {
            it[shareListIdKey] = listId
            it[shareListTitleKey] = listTitle
        }
    }

    suspend fun getShareListId(): String? =
        context.settingsDataStore.data.first()[shareListIdKey]

    suspend fun getShareListTitle(): String? =
        context.settingsDataStore.data.first()[shareListTitleKey]

    // -- Create list settings --

    val createListId: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[createListIdKey]
    }

    val createListTitle: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[createListTitleKey]
    }

    suspend fun setCreateList(listId: String, listTitle: String) {
        context.settingsDataStore.edit {
            it[createListIdKey] = listId
            it[createListTitleKey] = listTitle
        }
    }

    suspend fun getCreateListId(): String? =
        context.settingsDataStore.data.first()[createListIdKey]

    suspend fun getCreateListTitle(): String? =
        context.settingsDataStore.data.first()[createListTitleKey]

    // -- Streak --

    val streak: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[streakCountKey] ?: 0
    }

    suspend fun recordAllDone() {
        context.settingsDataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            val lastDate = prefs[streakLastDateKey]

            if (lastDate == today) return@edit // already recorded today

            val yesterday = LocalDate.now().minusDays(1).toString()
            val currentStreak = prefs[streakCountKey] ?: 0

            prefs[streakCountKey] = if (lastDate == yesterday) currentStreak + 1 else 1
            prefs[streakLastDateKey] = today
        }
    }

    // -- Widget settings --

    val widgetBackground: Flow<WidgetBackground> = context.settingsDataStore.data.map { prefs ->
        WidgetBackground.entries.find { it.name == prefs[widgetBgKey] } ?: WidgetBackground.AUTO
    }

    val widgetOpacity: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[widgetOpacityKey] ?: 1f
    }

    val widgetShowCompleted: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[widgetShowCompletedKey] ?: true
    }

    val widgetLayoutDensity: Flow<LayoutDensity> = context.settingsDataStore.data.map { prefs ->
        LayoutDensity.entries.find { it.name == prefs[widgetLayoutDensityKey] } ?: LayoutDensity.DEFAULT
    }

    val widgetSorting: Flow<WidgetSorting> = context.settingsDataStore.data.map { prefs ->
        WidgetSorting.entries.find { it.name == prefs[widgetSortingKey] } ?: WidgetSorting.FLAT
    }

    suspend fun setWidgetBackground(bg: WidgetBackground) {
        context.settingsDataStore.edit { it[widgetBgKey] = bg.name }
    }

    suspend fun setWidgetOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[widgetOpacityKey] = opacity }
    }

    suspend fun setWidgetShowCompleted(show: Boolean) {
        context.settingsDataStore.edit { it[widgetShowCompletedKey] = show }
    }

    suspend fun setWidgetLayoutDensity(density: LayoutDensity) {
        context.settingsDataStore.edit { it[widgetLayoutDensityKey] = density.name }
    }

    suspend fun setWidgetSorting(sorting: WidgetSorting) {
        context.settingsDataStore.edit { it[widgetSortingKey] = sorting.name }
    }

    companion object {
        /** Reactive Flow for widget — emits whenever any widget setting changes. */
        fun widgetSettingsFlow(context: Context): Flow<WidgetSettings> =
            context.settingsDataStore.data.map { prefs -> prefsToWidgetSettings(prefs) }

        /** Reactive Flow for widget — emits whenever list order changes. */
        fun listOrderFlow(context: Context): Flow<List<String>> =
            context.settingsDataStore.data.map { prefs ->
                prefs[stringPreferencesKey("list_order")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            }

        /** Snapshot read (suspend) — for one-off reads outside composition. */
        suspend fun readListOrder(context: Context): List<String> {
            val prefs = context.settingsDataStore.data.first()
            return prefs[stringPreferencesKey("list_order")]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }

        /** Snapshot read (suspend) — for one-off reads outside composition. */
        suspend fun readWidgetSettings(context: Context): WidgetSettings {
            val prefs = context.settingsDataStore.data.first()
            return prefsToWidgetSettings(prefs)
        }

        private fun prefsToWidgetSettings(prefs: androidx.datastore.preferences.core.Preferences): WidgetSettings =
            WidgetSettings(
                background = WidgetBackground.entries.find {
                    it.name == prefs[stringPreferencesKey("widget_bg")]
                } ?: WidgetBackground.AUTO,
                opacity = prefs[floatPreferencesKey("widget_opacity")] ?: 1f,
                showCompleted = prefs[booleanPreferencesKey("widget_show_completed")] ?: true,
                layoutDensity = LayoutDensity.entries.find {
                    it.name == prefs[stringPreferencesKey("widget_layout_density")]
                } ?: LayoutDensity.DEFAULT,
                sorting = WidgetSorting.entries.find {
                    it.name == prefs[stringPreferencesKey("widget_sorting")]
                } ?: WidgetSorting.FLAT,
                theme = prefs[stringPreferencesKey("app_theme")] ?: "SYSTEM",
            )
    }
}
