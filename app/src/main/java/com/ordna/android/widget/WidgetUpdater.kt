package com.ordna.android.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val updateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

@Volatile
private var pendingJob: Job? = null

/**
 * Triggers a re-render of all Ordna widget instances with 300ms debounce.
 *
 * Rapid successive calls (e.g. toggling multiple tasks) are coalesced into
 * a single update. The caller suspends until the debounced update completes.
 */
suspend fun updateAllWidgets(context: Context) {
    val appContext = context.applicationContext
    pendingJob?.cancel()
    val job = updateScope.launch {
        delay(300)
        val manager = GlanceAppWidgetManager(appContext)
        coroutineScope {
            val mainIds = manager.getGlanceIds(OrdnaWidget::class.java)
            val counterIds = manager.getGlanceIds(CounterWidget::class.java)
            val jobs = mainIds.map { id ->
                async { OrdnaWidget().update(appContext, id) }
            } + counterIds.map { id ->
                async { CounterWidget().update(appContext, id) }
            }
            jobs.awaitAll()
        }
    }
    pendingJob = job
    job.join()
}
