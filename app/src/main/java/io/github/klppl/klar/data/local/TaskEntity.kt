package io.github.klppl.klar.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "due"]),
        Index(value = ["status", "completedAt"]),
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val due: LocalDate?,
    val status: String, // "needsAction" or "completed"
    val completedAt: Instant?,
    val listId: String,
    val listTitle: String,
    val listColor: Int,
    val notes: String? = null,
    val position: String,
    val updated: Instant,
) {
    companion object {
        // The Google Tasks API discards the time-of-day on due dates, so a time
        // is encoded in the task notes as a `t/HH:MM` tag (`t/07:00`, also accepts
        // a dot separator and single-digit hours: `t/7.00`). See README.
        private val NOTE_TIME = Regex("""(?i)\bt/(\d{1,2})[.:](\d{2})\b""")

        /** Minutes-of-day from a `t/HH:MM` tag in [notes], or null if absent/invalid. */
        fun parseNoteTime(notes: String?): Int? {
            val m = notes?.let { NOTE_TIME.find(it) } ?: return null
            val hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].toInt()
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }

        fun flatComparator(listOrderIds: List<String>): Comparator<TaskEntity> {
            val orderMap = listOrderIds.withIndex().associate { (i, id) -> id to i }
            // Unknown lists (not yet in the user's order) sort to the top (-1) so
            // newly added lists surface instead of sinking to the bottom.
            return compareBy<TaskEntity> { orderMap[it.listId] ?: -1 }
                .thenBy { it.due }
                // Timed tasks (t/HH:MM in notes) sort ascending; untimed sink below.
                .thenBy { parseNoteTime(it.notes) ?: Int.MAX_VALUE }
                .thenBy { it.position }
        }
    }
}
