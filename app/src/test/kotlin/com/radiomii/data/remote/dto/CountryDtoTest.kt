package com.radiomii.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

// Unit tests for CountryDto.toDomain field mapping.
class CountryDtoTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = CountryDto(
            name = "Germany",
            iso31661 = "DE",
            stationcount = 1234,
        )
        val country = dto.toDomain()
        assertEquals("Germany", country.name)
        assertEquals("DE", country.iso31661)
        assertEquals(1234, country.stationcount)
    }

    @Test
    fun `toDomain handles empty name`() {
        val dto = CountryDto(name = "", iso31661 = "XX", stationcount = 0)
        val country = dto.toDomain()
        assertEquals("", country.name)
    }

    @Test
    fun `toDomain preserves zero station count`() {
        val dto = CountryDto(name = "Atlantis", iso31661 = "AT", stationcount = 0)
        assertEquals(0, dto.toDomain().stationcount)
    }

    @Test
    fun `default DTO values produce empty country`() {
        val country = CountryDto().toDomain()
        assertEquals("", country.name)
        assertEquals("", country.iso31661)
        assertEquals(0, country.stationcount)
    }
}

