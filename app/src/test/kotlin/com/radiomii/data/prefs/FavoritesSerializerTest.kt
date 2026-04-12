package com.radiomii.data.prefs

import com.radiomii.domain.model.Station
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

// Unit tests for FavoritesSerializer — roundtrip, error tolerance, and forward-compatibility.
class FavoritesSerializerTest {

    private fun station(id: String, name: String = "Station $id") = Station(
        stationuuid = id,
        name = name,
        url = "https://stream.example.com/$id",
        urlResolved = "https://stream.example.com/$id",
    )

    @Test
    fun `roundtrip preserves stations`() = runTest {
        val data = FavoritesData(
            stations = listOf(station("a"), station("b")),
        )
        val bytes = ByteArrayOutputStream().also { FavoritesSerializer.writeTo(data, it) }.toByteArray()
        val restored = FavoritesSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(data.stations, restored.stations)
    }

    @Test
    fun `roundtrip preserves filters`() = runTest {
        val data = FavoritesData(
            stations = listOf(station("a")),
            filters = listOf("Rock", "Jazz"),
        )
        val bytes = ByteArrayOutputStream().also { FavoritesSerializer.writeTo(data, it) }.toByteArray()
        val restored = FavoritesSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(data.filters, restored.filters)
    }

    @Test
    fun `roundtrip preserves filterMap`() = runTest {
        val data = FavoritesData(
            stations = listOf(station("a")),
            filters = listOf("Rock"),
            filterMap = mapOf("a" to listOf("Rock")),
        )
        val bytes = ByteArrayOutputStream().also { FavoritesSerializer.writeTo(data, it) }.toByteArray()
        val restored = FavoritesSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(data.filterMap, restored.filterMap)
    }

    @Test
    fun `roundtrip with empty data`() = runTest {
        val data = FavoritesData()
        val bytes = ByteArrayOutputStream().also { FavoritesSerializer.writeTo(data, it) }.toByteArray()
        val restored = FavoritesSerializer.readFrom(ByteArrayInputStream(bytes))
        assertEquals(data, restored)
    }

    @Test
    fun `readFrom returns default for empty input`() = runTest {
        val result = FavoritesSerializer.readFrom(ByteArrayInputStream(ByteArray(0)))
        assertEquals(FavoritesSerializer.defaultValue, result)
    }

    @Test
    fun `readFrom returns default for corrupted JSON`() = runTest {
        val corrupt = "{ this is not valid json !!!".toByteArray()
        val result = FavoritesSerializer.readFrom(ByteArrayInputStream(corrupt))
        assertEquals(FavoritesSerializer.defaultValue, result)
    }

    @Test
    fun `readFrom returns default for non-JSON binary data`() = runTest {
        val binary = byteArrayOf(0x00, 0xFF.toByte(), 0x42, 0x13)
        val result = FavoritesSerializer.readFrom(ByteArrayInputStream(binary))
        assertEquals(FavoritesSerializer.defaultValue, result)
    }

    @Test
    fun `readFrom ignores unknown fields in JSON`() = runTest {
        val json = """
            {
                "stations": [],
                "filters": [],
                "filterMap": {},
                "futureField": "some value",
                "anotherNewField": 42
            }
        """.trimIndent().toByteArray()
        val result = FavoritesSerializer.readFrom(ByteArrayInputStream(json))
        assertEquals(FavoritesData(), result)
    }

    @Test
    fun `readFrom ignores unknown fields inside station objects`() = runTest {
        val json = """
            {
                "stations": [{
                    "stationuuid": "x1",
                    "name": "Test",
                    "url": "http://x",
                    "newStationField": true
                }],
                "filters": [],
                "filterMap": {}
            }
        """.trimIndent().toByteArray()
        val result = FavoritesSerializer.readFrom(ByteArrayInputStream(json))
        assertEquals(1, result.stations.size)
        assertEquals("x1", result.stations[0].stationuuid)
        assertEquals("Test", result.stations[0].name)
    }

    @Test
    fun `defaultValue has empty collections`() {
        val default = FavoritesSerializer.defaultValue
        assertTrue(default.stations.isEmpty())
        assertTrue(default.filters.isEmpty())
        assertTrue(default.filterMap.isEmpty())
    }
}

