package com.taskig.android.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Reloads task data into the in-memory WidgetDataSource and triggers
 * a re-render of all Taskig widget instances.
 */
suspend fun updateAllWidgets(context: Context) {
    val appContext = context.applicationContext

    // Refresh in-memory data from Room so provideContent recomposes
    WidgetDataSource.reload(appContext)

    val manager = GlanceAppWidgetManager(appContext)
    val widget = TaskigWidget()
    val glanceIds = manager.getGlanceIds(TaskigWidget::class.java)

    glanceIds.forEach { glanceId ->
        widget.update(appContext, glanceId)
    }
}
