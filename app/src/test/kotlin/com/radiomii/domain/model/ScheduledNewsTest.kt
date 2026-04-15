package com.radiomii.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for ScheduledNews computed properties and defaults.
class ScheduledNewsTest {

    @Test
    fun `intervalEnum returns HOURLY for default`() {
        assertEquals(NewsInterval.HOURLY, ScheduledNews().intervalEnum)
    }

    @Test
    fun `intervalEnum returns HALF_HOURLY when set`() {
        val news = ScheduledNews(interval = NewsInterval.HALF_HOURLY.name)
        assertEquals(NewsInterval.HALF_HOURLY, news.intervalEnum)
    }

    @Test
    fun `intervalEnum round-trips through name`() {
        NewsInterval.entries.forEach { interval ->
            val news = ScheduledNews(interval = interval.name)
            assertEquals(interval, news.intervalEnum)
        }
    }

    @Test
    fun `default state is disabled`() {
        assertFalse(ScheduledNews().enabled)
    }

    @Test
    fun `default stationId is empty`() {
        assertTrue(ScheduledNews().stationId.isEmpty())
    }

    @Test
    fun `default durationMinutes is 5`() {
        assertEquals(5, ScheduledNews().durationMinutes)
    }

    @Test
    fun `showSkipButton defaults to false`() {
        assertFalse(ScheduledNews().showSkipButton)
    }
}

