package com.ordna.android.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Triggers a re-render of all Ordna widget instances.
 *
 * Each update() call causes Glance to recompose provideContent, which
 * collects fresh data from Room Flows and DataStore Flows automatically.
 * No manual data loading needed — the sources of truth are reactive.
 */
suspend fun updateAllWidgets(context: Context) {
    val appContext = context.applicationContext
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
