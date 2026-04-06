package io.github.klppl.ordna.data.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.klppl.ordna.MainActivity
import io.github.klppl.ordna.R
import io.github.klppl.ordna.data.local.TaskDao
import io.github.klppl.ordna.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskDao: TaskDao,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val NOTIFICATION_ID_MORNING = 1001
        const val NOTIFICATION_ID_MIDDAY = 1002
        const val NOTIFICATION_ID_EVENING = 1003
        const val CHANNEL_ID = "task_reminders"
    }

    override suspend fun doWork(): Result {
        val slot = inputData.getString("slot") ?: return Result.failure()

        val tasks = taskDao.getActiveTasks(LocalDate.now())

        if (tasks.isNotEmpty()) {
            postNotification(slot, tasks)
        }

        // Reschedule for tomorrow
        reschedule(slot)

        return Result.success()
    }

    private fun postNotification(slot: String, tasks: List<io.github.klppl.ordna.data.local.TaskEntity>) {
        val taskCount = tasks.size
        val titleRes = when (slot) {
            ReminderScheduler.SLOT_MORNING -> R.string.reminder_title_morning
            ReminderScheduler.SLOT_MIDDAY -> R.string.reminder_title_midday
            ReminderScheduler.SLOT_EVENING -> R.string.reminder_title_evening
            else -> R.string.reminder_title_morning
        }

        val notificationId = when (slot) {
            ReminderScheduler.SLOT_MORNING -> NOTIFICATION_ID_MORNING
            ReminderScheduler.SLOT_MIDDAY -> NOTIFICATION_ID_MIDDAY
            ReminderScheduler.SLOT_EVENING -> NOTIFICATION_ID_EVENING
            else -> NOTIFICATION_ID_MORNING
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            putExtra("SYNC_ON_LAUNCH", true)
            putExtra("NAVIGATE_TODAY", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = appContext.getString(titleRes)
        val body = appContext.resources.getQuantityString(
            R.plurals.reminder_body, taskCount, taskCount
        )

        val inboxStyle = NotificationCompat.InboxStyle()
        tasks.take(5).forEach { inboxStyle.addLine(it.title) }
        if (taskCount > 5) {
            inboxStyle.setSummaryText("+${taskCount - 5} more")
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted — silently skip
        }
    }

    private suspend fun reschedule(slot: String) {
        val settings = SettingsRepository.readReminderSettings(appContext)
        if (!settings.enabled) return

        val (enabled, hour, minute) = when (slot) {
            ReminderScheduler.SLOT_MORNING -> Triple(settings.morningEnabled, settings.morningHour, settings.morningMinute)
            ReminderScheduler.SLOT_MIDDAY -> Triple(settings.middayEnabled, settings.middayHour, settings.middayMinute)
            ReminderScheduler.SLOT_EVENING -> Triple(settings.eveningEnabled, settings.eveningHour, settings.eveningMinute)
            else -> return
        }

        if (enabled) {
            ReminderScheduler(appContext).scheduleSlot(slot, hour, minute)
        }
    }
}
