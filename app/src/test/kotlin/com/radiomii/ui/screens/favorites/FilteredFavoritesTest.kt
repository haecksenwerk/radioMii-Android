package com.radiomii.ui.screens.favorites

import com.radiomii.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the filterStationsByGroup pure function.
class FilteredFavoritesTest {

    private fun station(id: String) = Station(
        stationuuid = id,
        name = "Station $id",
        url = "https://stream.example.com/$id",
    )

    private val stationA = station("a")
    private val stationB = station("b")
    private val stationC = station("c")
    private val allStations = listOf(stationA, stationB, stationC)
    private val filters = listOf("Rock", "Jazz")
    private val filterMap = mapOf(
        "a" to listOf("Rock"),
        "b" to listOf("Rock", "Jazz"),
        // "c" is unassigned
    )

    @Test
    fun `activeIndex -1 returns all stations`() {
        val result = filterStationsByGroup(allStations, filters, -1, filterMap)
        assertEquals(allStations, result)
    }

    @Test
    fun `activeIndex equal to filters size returns all stations`() {
        val result = filterStationsByGroup(allStations, filters, filters.size, filterMap)
        assertEquals(allStations, result)
    }

    @Test
    fun `activeIndex beyond filters size returns all stations`() {
        val result = filterStationsByGroup(allStations, filters, 99, filterMap)
        assertEquals(allStations, result)
    }

    @Test
    fun `filter Rock returns stations a and b`() {
        val result = filterStationsByGroup(allStations, filters, 0, filterMap)
        assertEquals(listOf(stationA, stationB), result)
    }

    @Test
    fun `filter Jazz returns only station b`() {
        val result = filterStationsByGroup(allStations, filters, 1, filterMap)
        assertEquals(listOf(stationB), result)
    }

    @Test
    fun `empty favorites list returns empty result`() {
        val result = filterStationsByGroup(emptyList(), filters, 0, filterMap)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty filterMap returns empty result for valid index`() {
        val result = filterStationsByGroup(allStations, filters, 0, emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty filterNames with index 0 returns all stations`() {
        val result = filterStationsByGroup(allStations, emptyList(), 0, filterMap)
        assertEquals(allStations, result)
    }

    @Test
    fun `filter with no matching stations returns empty`() {
        val noMatchMap = mapOf("x" to listOf("Rock")) // "x" is not in allStations
        val result = filterStationsByGroup(allStations, filters, 0, noMatchMap)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preserves order of all-stations list`() {
        val reversed = listOf(stationC, stationB, stationA)
        val result = filterStationsByGroup(reversed, filters, 0, filterMap)
        // Only b and a are in "Rock"; order from input list is preserved
        assertEquals(listOf(stationB, stationA), result)
    }
}

