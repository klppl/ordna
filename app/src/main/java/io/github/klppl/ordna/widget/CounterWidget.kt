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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
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
        val vacationFlow = SettingsRepository.vacationModeFlow(context)

        // Snapshot for instant first frame
        val initialOverdue = dao.getOverdueTasks(today).first()
        val initialToday = dao.getTodayTasks(today).first()
        val initialCompleted = dao.getCompletedTasks().first()
        val initialSettings = settingsFlow.first()
        val initialStreak = streakFlow.first()
        val initialVacation = vacationFlow.first()

        provideContent {
            // Reactive Flows keep widget in sync when composition is alive
            val overdueTasks by dao.getOverdueTasks(today).collectAsState(initial = initialOverdue)
            val todayTasks by dao.getTodayTasks(today).collectAsState(initial = initialToday)
            val completedTasks by dao.getCompletedTasks().collectAsState(initial = initialCompleted)
            val settings by settingsFlow.collectAsState(initial = initialSettings)
            val streak by streakFlow.collectAsState(initial = initialStreak)
            val vacationMode by vacationFlow.collectAsState(initial = initialVacation)

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
                    vacationMode = vacationMode,
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
    vacationMode: Boolean,
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

    // Rail and progress bar share one accent. Grey when on vacation,
    // otherwise theme primary or fallback purple.
    val vacationGrey = Color(0xFF9E9E9E)
    val accentColor = if (vacationMode) ColorProvider(vacationGrey)
        else themeColors?.colorScheme?.primary?.let { ColorProvider(it) }
            ?: ColorProvider(Color(0xFF6750A4))
    // Opaque variant of the accent for the rail — must NOT respect the user's
    // opacity slider. The rail is the visual anchor; dissolving it with the
    // background defeats the whole point.
    val railOpaqueColor = if (vacationMode) vacationGrey
        else themeColors?.colorScheme?.primary ?: Color(0xFF6750A4)

    val trackColor = if (isLight) ColorProvider(Color(0xFFE0E0E0)) else ColorProvider(Color(0xFF444444))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            // Accent rail — flush with the left edge, full height. Parent Box's
            // cornerRadius(16dp) clips its top-left/bottom-left corners.
            Box(
                modifier = GlanceModifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(railOpaqueColor),
            ) {}

            // Content column — left-aligned text, bar pinned to the bottom.
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.Start,
                // When the progress bar is present, let the weighted spacer below push it
                // to the bottom while text stays at the top. When there's no bar, center
                // the text vertically so state (d) doesn't hang from the top of the rail.
                verticalAlignment = if (totalCount > 0) Alignment.Top else Alignment.CenterVertically,
            ) {
                if (remaining > 0) {
                    Text(
                        text = "$remaining",
                        style = TextStyle(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        ),
                    )
                    Text(
                        text = ctx.getString(R.string.counter_widget_left),
                        style = TextStyle(fontSize = 12.sp, color = subtextColor),
                    )
                } else {
                    Text(
                        text = ctx.getString(R.string.counter_widget_done),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        ),
                    )
                    if (streak > 0) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = ctx.getString(R.string.widget_streak, streak),
                            style = TextStyle(fontSize = 11.sp, color = subtextColor),
                        )
                    }
                }

                if (totalCount > 0) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    LinearProgressIndicator(
                        progress = completedCount.toFloat() / totalCount,
                        modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                        color = accentColor,
                        backgroundColor = trackColor,
                    )
                }
            }

            if (vacationMode) {
                Text(
                    text = "☀",
                    style = TextStyle(fontSize = 11.sp, color = subtextColor),
                    modifier = GlanceModifier.padding(top = 6.dp, end = 6.dp),
                )
            }
        }
    }
}
