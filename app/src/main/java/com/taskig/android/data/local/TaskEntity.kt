package com.taskig.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val due: LocalDate?,
    val dueDateTime: String?, // Raw RFC 3339 from API, preserves time info for sorting
    val status: String, // "needsAction" or "completed"
    val completedAt: Instant?,
    val listId: String,
    val listTitle: String,
    val listColor: Int,
    val position: String,
    val updated: Instant,
) {
    companion object {
        private fun extractTimeSort(dueDateTime: String?): String {
            if (dueDateTime == null) return "99:99:99"
            val tIndex = dueDateTime.indexOf('T')
            if (tIndex < 0) return "99:99:99"
            val timePart = dueDateTime.substring(tIndex + 1).take(8)
            return if (timePart.startsWith("00:00:00")) "99:99:99" else timePart
        }

        fun flatComparator(listOrderIds: List<String>): Comparator<TaskEntity> {
            val orderMap = listOrderIds.withIndex().associate { (i, id) -> id to i }
            return compareBy<TaskEntity> { orderMap[it.listId] ?: Int.MAX_VALUE }
                .thenBy { it.due }
                .thenBy { extractTimeSort(it.dueDateTime) }
                .thenBy { it.position }
        }
    }
}
