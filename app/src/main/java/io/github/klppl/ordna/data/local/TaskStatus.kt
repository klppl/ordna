package io.github.klppl.ordna.data.local

/**
 * Google Tasks status string values, used both locally (Room) and by the API.
 * Note: Room `@Query` SQL still embeds these literals directly — keep them in
 * sync if these values ever change.
 */
object TaskStatus {
    const val NEEDS_ACTION = "needsAction"
    const val COMPLETED = "completed"
}
