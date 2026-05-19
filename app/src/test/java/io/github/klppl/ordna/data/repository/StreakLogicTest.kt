package io.github.klppl.ordna.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakLogicTest {

    private val today = LocalDate.of(2026, 5, 19)
    private val yesterday = today.minusDays(1)
    private val twoDaysAgo = today.minusDays(2)

    @Test
    fun `first completion sets streak to 1`() {
        val next = computeStreakAfterAllDone(
            current = StreakState(count = 0, lastDate = null),
            today = today,
            vacationMode = false,
        )
        assertEquals(StreakState(1, today.toString()), next)
    }

    @Test
    fun `consecutive day increments streak`() {
        val next = computeStreakAfterAllDone(
            current = StreakState(count = 3, lastDate = yesterday.toString()),
            today = today,
            vacationMode = false,
        )
        assertEquals(StreakState(4, today.toString()), next)
    }

    @Test
    fun `gap of more than one day resets streak to 1`() {
        val next = computeStreakAfterAllDone(
            current = StreakState(count = 7, lastDate = twoDaysAgo.toString()),
            today = today,
            vacationMode = false,
        )
        assertEquals(StreakState(1, today.toString()), next)
    }

    @Test
    fun `recording twice on the same day is idempotent`() {
        val state = StreakState(count = 5, lastDate = today.toString())
        val next = computeStreakAfterAllDone(state, today, vacationMode = false)
        assertEquals(state, next)
    }

    @Test
    fun `vacation mode freezes the streak`() {
        val state = StreakState(count = 2, lastDate = twoDaysAgo.toString())
        val next = computeStreakAfterAllDone(state, today, vacationMode = true)
        assertEquals(state, next)
    }
}
