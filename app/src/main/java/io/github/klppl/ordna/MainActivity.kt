package io.github.klppl.ordna

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    // Bumped on each notification/widget intent requesting the today screen.
    // A counter (not a Boolean) so repeated taps re-trigger navigation.
    private val navigateTodayTrigger = mutableIntStateOf(0)

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

        handleIntentExtras(intent)

        setContent {
            val themeName by settingsRepository.appTheme.collectAsState(initial = "SYSTEM")
            val appTheme = AppTheme.entries.find { it.name == themeName } ?: AppTheme.SYSTEM

            OrdnaTheme(appTheme = appTheme) {
                OrdnaNavGraph(navigateTodayTrigger = navigateTodayTrigger.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    // Handles notification / widget intents in both cold start (onCreate) and
    // foregrounded (onNewIntent, requires launchMode=singleTop) cases.
    private fun handleIntentExtras(intent: Intent?) {
        if (intent?.getBooleanExtra("SYNC_ON_LAUNCH", false) == true) {
            lifecycleScope.launch {
                try { taskRepository.sync() } catch (_: Exception) { }
            }
        }
        if (intent?.getBooleanExtra("NAVIGATE_TODAY", false) == true) {
            navigateTodayTrigger.intValue++
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            updateAllWidgets(this@MainActivity)
        }
    }
}
