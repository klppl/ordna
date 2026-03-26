package com.taskig.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.taskig.android.data.repository.SettingsRepository
import com.taskig.android.ui.navigation.TaskigNavGraph
import com.taskig.android.ui.theme.AppTheme
import com.taskig.android.ui.theme.TaskigTheme
import com.taskig.android.widget.updateAllWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeName by settingsRepository.appTheme.collectAsState(initial = "SYSTEM")
            val appTheme = AppTheme.entries.find { it.name == themeName } ?: AppTheme.SYSTEM

            TaskigTheme(appTheme = appTheme) {
                TaskigNavGraph()
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
