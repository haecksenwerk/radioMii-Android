package com.radiomii.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

// Unit tests for StationDto.toDomain, specifically the name-cleaning logic.
class StationDtoTest {

    private fun dtoWithName(name: String) = StationDto(
        stationuuid = "test-uuid",
        name = name,
        url = "https://stream.example.com",
    )

    @Test
    fun `toDomain removes bracketed meta-info`() {
        val station = dtoWithName("Radio X [MP3 128]").toDomain()
        assertEquals("Radio X", station.name)
    }

    @Test
    fun `toDomain removes multiple bracketed sections`() {
        val station = dtoWithName("Radio X [MP3 128] [Germany]").toDomain()
        assertEquals("Radio X", station.name)
    }

    @Test
    fun `toDomain removes parenthesised meta-info`() {
        val station = dtoWithName("Radio X (Germany)").toDomain()
        assertEquals("Radio X", station.name)
    }

    @Test
    fun `toDomain removes multiple parenthesised sections`() {
        val station = dtoWithName("Radio X (Germany) (Pop)").toDomain()
        assertEquals("Radio X", station.name)
    }

    @Test
    fun `toDomain strips everything after pipe`() {
        val station = dtoWithName("RadioX | MP3 128 | POP").toDomain()
        assertEquals("RadioX", station.name)
    }

    @Test
    fun `toDomain trims whitespace around pipe result`() {
        val station = dtoWithName("  RadioX  | MP3 128").toDomain()
        assertEquals("RadioX", station.name)
    }

    @Test
    fun `toDomain handles brackets and parentheses together`() {
        val station = dtoWithName("Cool FM [128] (Berlin)").toDomain()
        assertEquals("Cool FM", station.name)
    }

    @Test
    fun `toDomain handles brackets and pipe together`() {
        val station = dtoWithName("Cool FM [AAC] | Berlin").toDomain()
        assertEquals("Cool FM", station.name)
    }

    @Test
    fun `toDomain preserves name without meta-info`() {
        val station = dtoWithName("Radio Paradise").toDomain()
        assertEquals("Radio Paradise", station.name)
    }

    @Test
    fun `toDomain handles empty name`() {
        val station = dtoWithName("").toDomain()
        assertEquals("", station.name)
    }

    @Test
    fun `toDomain handles name that is only meta-info in brackets`() {
        // " [MP3 128]" requires a leading space, so bare "[MP3]" stays as-is
        val station = dtoWithName("[MP3]").toDomain()
        assertEquals("[MP3]", station.name)
    }

    @Test
    fun `toDomain trims trailing whitespace from cleaned name`() {
        val station = dtoWithName("Radio X   ").toDomain()
        assertEquals("Radio X", station.name)
    }

    @Test
    fun `toDomain maps all DTO fields to domain`() {
        val dto = StationDto(
            stationuuid = "uuid-1",
            name = "Test",
            url = "http://url",
            urlResolved = "http://resolved",
            favicon = "http://icon.png",
            homepage = "http://home",
            votes = 42,
            clickcount = 100,
            codec = "MP3",
            bitrate = 128,
            tags = "rock,pop",
            country = "Germany",
            countrycode = "DE",
            state = "Berlin",
            language = "german",
        )
        val station = dto.toDomain()
        assertEquals("uuid-1", station.stationuuid)
        assertEquals("Test", station.name)
        assertEquals("http://url", station.url)
        assertEquals("http://resolved", station.urlResolved)
        assertEquals("http://icon.png", station.favicon)
        assertEquals("http://home", station.homepage)
        assertEquals(42, station.votes)
        assertEquals(100, station.clickcount)
        assertEquals("MP3", station.codec)
        assertEquals(128, station.bitrate)
        assertEquals("rock,pop", station.tags)
        assertEquals("Germany", station.country)
        assertEquals("DE", station.countrycode)
        assertEquals("Berlin", station.state)
        assertEquals("german", station.language)
    }
}

