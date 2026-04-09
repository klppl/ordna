package io.github.klppl.ordna.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.klppl.ordna.MainActivity
import io.github.klppl.ordna.R
import io.github.klppl.ordna.data.local.TaskDatabase
import io.github.klppl.ordna.data.repository.SettingsRepository
import io.github.klppl.ordna.data.repository.WidgetBackground
import io.github.klppl.ordna.data.repository.WidgetSettings
import io.github.klppl.ordna.ui.theme.AppTheme
import io.github.klppl.ordna.ui.theme.appThemeColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class CounterWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = TaskDatabase.getInstance(context).taskDao()
        val today = LocalDate.now()
        val settingsFlow = SettingsRepository.widgetSettingsFlow(context)
        val streakFlow = SettingsRepository.streakFlow(context)

        // Snapshot for instant first frame
        val initialOverdue = dao.getOverdueTasks(today).first()
        val initialToday = dao.getTodayTasks(today).first()
        val initialCompleted = dao.getCompletedTasks().first()
        val initialSettings = settingsFlow.first()
        val initialStreak = streakFlow.first()

        provideContent {
            // Reactive Flows keep widget in sync when composition is alive
            val overdueTasks by dao.getOverdueTasks(today).collectAsState(initial = initialOverdue)
            val todayTasks by dao.getTodayTasks(today).collectAsState(initial = initialToday)
            val completedTasks by dao.getCompletedTasks().collectAsState(initial = initialCompleted)
            val settings by settingsFlow.collectAsState(initial = initialSettings)
            val streak by streakFlow.collectAsState(initial = initialStreak)

            val activeCount = overdueTasks.size + todayTasks.size
            val completedCount = completedTasks.size
            val totalCount = activeCount + completedCount

            GlanceTheme {
                CounterContent(
                    remaining = activeCount,
                    totalCount = totalCount,
                    completedCount = completedCount,
                    settings = settings,
                    streak = streak,
                )
            }
        }
    }
}

class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()
}

@Composable
private fun CounterContent(
    remaining: Int,
    totalCount: Int,
    completedCount: Int,
    settings: WidgetSettings,
    streak: Int,
) {
    val ctx = LocalContext.current
    val appTheme = AppTheme.entries.find { it.name == settings.theme } ?: AppTheme.SYSTEM
    val themeColors = appThemeColors(appTheme)

    val bgColor = if (themeColors != null) {
        val bg = themeColors.colorScheme.background
        Color(red = bg.red, green = bg.green, blue = bg.blue, alpha = settings.opacity.coerceIn(0f, 1f))
    } else {
        val base = when (settings.background) {
            WidgetBackground.AUTO, WidgetBackground.WHITE -> Color.White
            WidgetBackground.DARK -> Color(0xFF2D2D2D)
            WidgetBackground.BLACK -> Color.Black
        }
        Color(red = base.red, green = base.green, blue = base.blue, alpha = settings.opacity.coerceIn(0f, 1f))
    }

    val isLight = themeColors == null &&
            (settings.background == WidgetBackground.AUTO || settings.background == WidgetBackground.WHITE)
    val textColor = if (isLight) ColorProvider(Color(0xFF1C1B1F)) else ColorProvider(Color(0xFFE6E1E5))
    val subtextColor = if (isLight) ColorProvider(Color(0xFF49454F)) else ColorProvider(Color(0xFFCAC4D0))
    val accentColor = themeColors?.colorScheme?.primary?.let { ColorProvider(it) }
        ?: ColorProvider(Color(0xFF6750A4))
    val trackColor = if (isLight) ColorProvider(Color(0xFFE0E0E0)) else ColorProvider(Color(0xFF444444))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (remaining > 0) "$remaining" else ctx.getString(R.string.counter_widget_done),
                style = TextStyle(
                    fontSize = if (remaining > 0) 32.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                ),
            )
            if (remaining > 0) {
                Text(
                    text = ctx.getString(R.string.counter_widget_left),
                    style = TextStyle(fontSize = 12.sp, color = subtextColor),
                )
            } else if (streak > 0) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = ctx.getString(R.string.widget_streak, streak),
                    style = TextStyle(fontSize = 11.sp, color = subtextColor),
                )
            }
            if (totalCount > 0) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = completedCount.toFloat() / totalCount,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = accentColor,
                    backgroundColor = trackColor,
                )
            }
        }
    }
}
