package io.github.klppl.ordna.data.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.klppl.ordna.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val SLOT_MORNING = "morning"
        const val SLOT_MIDDAY = "midday"
        const val SLOT_EVENING = "evening"

        private fun workName(slot: String) = "reminder_$slot"

        /** Static version for use from BootReceiver (no DI). */
        suspend fun scheduleAllFromContext(context: Context) {
            val settings = SettingsRepository.readReminderSettings(context)
            if (!settings.enabled) return
            val scheduler = ReminderScheduler(context)
            if (settings.morningEnabled) scheduler.scheduleSlot(SLOT_MORNING, settings.morningHour, settings.morningMinute)
            if (settings.middayEnabled) scheduler.scheduleSlot(SLOT_MIDDAY, settings.middayHour, settings.middayMinute)
            if (settings.eveningEnabled) scheduler.scheduleSlot(SLOT_EVENING, settings.eveningHour, settings.eveningMinute)
        }
    }

    suspend fun scheduleAll() {
        val settings = SettingsRepository.readReminderSettings(context)
        if (!settings.enabled) {
            cancelAll()
            return
        }
        if (settings.morningEnabled) scheduleSlot(SLOT_MORNING, settings.morningHour, settings.morningMinute)
        else cancelSlot(SLOT_MORNING)
        if (settings.middayEnabled) scheduleSlot(SLOT_MIDDAY, settings.middayHour, settings.middayMinute)
        else cancelSlot(SLOT_MIDDAY)
        if (settings.eveningEnabled) scheduleSlot(SLOT_EVENING, settings.eveningHour, settings.eveningMinute)
        else cancelSlot(SLOT_EVENING)
    }

    fun scheduleSlot(slot: String, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val delayMillis = Duration.between(now, target).toMillis()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf("slot" to slot))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(slot),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelSlot(slot: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(slot))
    }

    fun cancelAll() {
        cancelSlot(SLOT_MORNING)
        cancelSlot(SLOT_MIDDAY)
        cancelSlot(SLOT_EVENING)
    }
}
