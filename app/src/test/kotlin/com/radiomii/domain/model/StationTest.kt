package com.radiomii.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for Station computed properties.
class StationTest {

    @Test
    fun `streamUrl returns urlResolved when both are set`() {
        val station = Station(url = "http://a.com", urlResolved = "http://b.com")
        assertEquals("http://b.com", station.streamUrl)
    }

    @Test
    fun `streamUrl falls back to url when urlResolved is blank`() {
        val station = Station(url = "http://a.com", urlResolved = "")
        assertEquals("http://a.com", station.streamUrl)
    }

    @Test
    fun `streamUrl falls back to url when urlResolved is whitespace`() {
        val station = Station(url = "http://a.com", urlResolved = "   ")
        assertEquals("http://a.com", station.streamUrl)
    }

    @Test
    fun `tagList parses comma-separated tags`() {
        val station = Station(tags = "rock,pop,jazz")
        assertEquals(listOf("rock", "pop", "jazz"), station.tagList)
    }

    @Test
    fun `tagList trims whitespace around tags`() {
        val station = Station(tags = " rock , pop , jazz ")
        assertEquals(listOf("rock", "pop", "jazz"), station.tagList)
    }

    @Test
    fun `tagList returns empty list for blank tags`() {
        assertTrue(Station(tags = "").tagList.isEmpty())
    }

    @Test
    fun `tagList filters out empty entries from consecutive commas`() {
        val station = Station(tags = "rock,,jazz")
        assertEquals(listOf("rock", "jazz"), station.tagList)
    }

    @Test
    fun `tagList with single tag returns single-element list`() {
        val station = Station(tags = "news")
        assertEquals(listOf("news"), station.tagList)
    }
}

