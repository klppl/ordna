package io.github.klppl.ordna.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks task IDs with in-flight widget toggle operations.
 * Sync skips overwriting these tasks so optimistic updates aren't reverted
 * before the API call completes.
 */
object PendingToggles {
    private val pending: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun add(taskId: String) { pending.add(taskId) }
    fun remove(taskId: String) { pending.remove(taskId) }
    fun contains(taskId: String): Boolean = taskId in pending
    fun snapshot(): Set<String> = pending.toSet()
}
