package com.radiomii.data.player

import com.radiomii.domain.model.NewsInterval
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

// Unit tests for the msUntilNextInterval function.
class ScheduledNewsIntervalTest {

    private fun calendarAt(minutes: Int, seconds: Int, millis: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, seconds)
            set(Calendar.MILLISECOND, millis)
        }

    @Test
    fun `hourly - exactly on the hour returns 60 minutes`() {
        val now = calendarAt(minutes = 0, seconds = 0, millis = 0)
        assertEquals(3_600_000L, msUntilNextInterval(NewsInterval.HOURLY, now))
    }

    @Test
    fun `hourly - half past the hour returns 30 minutes`() {
        val now = calendarAt(minutes = 30, seconds = 0, millis = 0)
        assertEquals(1_800_000L, msUntilNextInterval(NewsInterval.HOURLY, now))
    }

    @Test
    fun `hourly - one second before the hour returns ~1 second`() {
        val now = calendarAt(minutes = 59, seconds = 59, millis = 0)
        // (60 - 59) * 60_000 - (59 * 1_000) = 60_000 - 59_000 = 1_000 ms
        assertEquals(1_000L, msUntilNextInterval(NewsInterval.HOURLY, now))
    }

    @Test
    fun `hourly - one millisecond before the hour returns 1 ms`() {
        val now = calendarAt(minutes = 59, seconds = 59, millis = 999)
        // (60 - 59) * 60_000 - (59 * 1_000 + 999) = 60_000 - 59_999 = 1 ms
        assertEquals(1L, msUntilNextInterval(NewsInterval.HOURLY, now))
    }

    @Test
    fun `hourly - 45 minutes past returns 15 minutes`() {
        val now = calendarAt(minutes = 45, seconds = 0, millis = 0)
        assertEquals(900_000L, msUntilNextInterval(NewsInterval.HOURLY, now))
    }

    @Test
    fun `half-hourly - exactly at zero-minutes returns 30 minutes`() {
        val now = calendarAt(minutes = 0, seconds = 0, millis = 0)
        assertEquals(1_800_000L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly - at 10 minutes returns 20 minutes`() {
        val now = calendarAt(minutes = 10, seconds = 0, millis = 0)
        // (30 - 10) * 60_000 = 1_200_000 ms
        assertEquals(1_200_000L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly - one millisecond before 30-minute mark returns 1 ms`() {
        val now = calendarAt(minutes = 29, seconds = 59, millis = 999)
        // (30 - 29) * 60_000 - (59 * 1_000 + 999) = 60_000 - 59_999 = 1 ms
        assertEquals(1L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly - exactly at 30-minute mark returns 30 minutes`() {
        val now = calendarAt(minutes = 30, seconds = 0, millis = 0)
        // (60 - 30) * 60_000 = 1_800_000 ms
        assertEquals(1_800_000L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly - at 45 minutes returns 15 minutes`() {
        val now = calendarAt(minutes = 45, seconds = 0, millis = 0)
        // (60 - 45) * 60_000 = 900_000 ms
        assertEquals(900_000L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly - one millisecond before the hour returns 1 ms`() {
        val now = calendarAt(minutes = 59, seconds = 59, millis = 999)
        // (60 - 59) * 60_000 - (59 * 1_000 + 999) = 60_000 - 59_999 = 1 ms
        assertEquals(1L, msUntilNextInterval(NewsInterval.HALF_HOURLY, now))
    }

    @Test
    fun `half-hourly delay at zero-minute mark equals delay at 30-minute mark`() {
        val atZero = calendarAt(minutes = 0, seconds = 0, millis = 0)
        val atThirty = calendarAt(minutes = 30, seconds = 0, millis = 0)
        assertEquals(
            msUntilNextInterval(NewsInterval.HALF_HOURLY, atZero),
            msUntilNextInterval(NewsInterval.HALF_HOURLY, atThirty),
        )
    }
}
