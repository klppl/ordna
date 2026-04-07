package io.github.klppl.ordna.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks task IDs with in-flight operations (toggles, edits, creates, deletes).
 * Sync preserves local state for these tasks so optimistic updates aren't
 * overwritten before the API call completes.
 */
object PendingOperations {
    private val pending: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun add(taskId: String) { pending.add(taskId) }
    fun remove(taskId: String) { pending.remove(taskId) }
    fun snapshot(): Set<String> = pending.toSet()
}
