package io.github.klppl.ordna

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import io.github.klppl.ordna.data.repository.SettingsRepository
import io.github.klppl.ordna.data.repository.TaskRepository
import io.github.klppl.ordna.ui.navigation.OrdnaNavGraph
import io.github.klppl.ordna.ui.theme.AppTheme
import io.github.klppl.ordna.ui.theme.OrdnaTheme
import io.github.klppl.ordna.widget.updateAllWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore saved locale (only if AppCompat doesn't already have one set)
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            val saved = runBlocking { settingsRepository.language.first() }
            if (saved != "system") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
            }
        }

        // Trigger sync if launched from notification
        if (intent?.getBooleanExtra("SYNC_ON_LAUNCH", false) == true) {
            lifecycleScope.launch {
                try { taskRepository.sync() } catch (_: Exception) { }
            }
        }

        val navigateToday = intent?.getBooleanExtra("NAVIGATE_TODAY", false) == true

        setContent {
            val themeName by settingsRepository.appTheme.collectAsState(initial = "SYSTEM")
            val appTheme = AppTheme.entries.find { it.name == themeName } ?: AppTheme.SYSTEM

            OrdnaTheme(appTheme = appTheme) {
                OrdnaNavGraph(navigateToToday = navigateToday)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            updateAllWidgets(this@MainActivity)
        }
    }
}
