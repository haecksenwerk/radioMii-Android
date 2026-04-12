package com.radiomii.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// Unit tests for SleepTimerState computed properties.
class SleepTimerStateTest {

    @Test
    fun `remainingMinutes rounds down correctly`() {
        val state = SleepTimerState(remainingMs = 90_500L)  // 1 min 30.5 s
        assertEquals(1, state.remainingMinutes)
    }

    @Test
    fun `remainingMinutes is 0 when remainingMs is 0`() {
        assertEquals(0, SleepTimerState(remainingMs = 0L).remainingMinutes)
    }

    @Test
    fun `remainingMinutes is 30 for a full 30-minute timer`() {
        val state = SleepTimerState(remainingMs = 30 * 60_000L)
        assertEquals(30, state.remainingMinutes)
    }

    @Test
    fun `progress is 0 when timer is not active`() {
        val state = SleepTimerState(
            isActive = false,
            selectedMinutes = 30,
            remainingMs = 15 * 60_000L,
        )
        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun `progress is 0 when selectedMinutes is 0`() {
        val state = SleepTimerState(
            isActive = true,
            selectedMinutes = 0,
            remainingMs = 60_000L,
        )
        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun `progress is 1 at the very start of a 30-minute timer`() {
        val state = SleepTimerState(
            isActive = true,
            selectedMinutes = 30,
            remainingMs = 30 * 60_000L,
        )
        assertEquals(1f, state.progress, 0.0001f)
    }

    @Test
    fun `progress is 0_5 at half time`() {
        val state = SleepTimerState(
            isActive = true,
            selectedMinutes = 10,
            remainingMs = 5 * 60_000L,
        )
        assertEquals(0.5f, state.progress, 0.0001f)
    }

    @Test
    fun `progress is 0 when remaining is 0`() {
        val state = SleepTimerState(
            isActive = true,
            selectedMinutes = 30,
            remainingMs = 0L,
        )
        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun `default state is not active`() {
        assertFalse(SleepTimerState().isActive)
    }

    @Test
    fun `default selectedMinutes is 30`() {
        assertEquals(30, SleepTimerState().selectedMinutes)
    }
}

