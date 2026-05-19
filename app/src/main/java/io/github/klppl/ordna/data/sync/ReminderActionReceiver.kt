package io.github.klppl.ordna.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.klppl.ordna.data.local.TaskDao
import io.github.klppl.ordna.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var repository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_TOP_TASK) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                taskDao.getActiveTasks(LocalDate.now()).firstOrNull()?.let { top ->
                    repository.toggleTaskDeferred(top)
                }
                if (notificationId >= 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TOP_TASK = "io.github.klppl.ordna.action.COMPLETE_TOP_TASK"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
