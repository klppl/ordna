package io.github.klppl.ordna.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.ide.common.rendering.api.SessionParams
import io.github.klppl.ordna.ui.theme.AppTheme
import io.github.klppl.ordna.ui.theme.OrdnaTheme
import io.github.klppl.ordna.ui.theme.appThemeColors
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
    )

    private val rosePine = appThemeColors(AppTheme.ROSE_PINE)!!

    @Test
    fun todayScreen() {
        paparazzi.snapshot {
            OrdnaTheme(appTheme = AppTheme.ROSE_PINE) {
                TodayScreenDemo(state = DemoData.uiState)
            }
        }
    }

    @Test
    fun counterWidget() {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 540, screenHeight = 270),
            renderingMode = SessionParams.RenderingMode.SHRINK,
        )
        paparazzi.snapshot {
            Box(modifier = Modifier.size(width = 200.dp, height = 100.dp)) {
                CounterWidgetDemo(
                    remaining = DemoData.overdueTasks.size + DemoData.todayTasks.size,
                    totalCount = DemoData.uiState.totalCount,
                    completedCount = DemoData.uiState.completedCount,
                    streak = DemoData.STREAK,
                    accentColor = rosePine.colorScheme.primary,
                    bgColor = rosePine.colorScheme.background,
                    textColor = rosePine.colorScheme.onBackground,
                    subtextColor = rosePine.colorScheme.onSurfaceVariant,
                    trackColor = rosePine.colorScheme.surfaceVariant,
                )
            }
        }
    }

    @Test
    fun ordnaWidget() {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 920, screenHeight = 920),
            renderingMode = SessionParams.RenderingMode.SHRINK,
        )
        paparazzi.snapshot {
            Box(modifier = Modifier.size(340.dp)) {
                OrdnaWidgetDemo(
                    overdueTasks = DemoData.overdueTasks,
                    todayTasks = DemoData.todayTasks,
                    completedTasks = DemoData.completedTasks,
                    completedCount = DemoData.uiState.completedCount,
                    totalCount = DemoData.uiState.totalCount,
                    streak = DemoData.STREAK,
                    accentColor = rosePine.colorScheme.primary,
                    bgColor = rosePine.colorScheme.background,
                    textColor = rosePine.colorScheme.onBackground,
                    subtextColor = rosePine.colorScheme.onSurfaceVariant,
                    overdueColor = rosePine.overdueRed,
                    completedColor = rosePine.completedGreen,
                    trackColor = rosePine.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
