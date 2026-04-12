package com.radiomii.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

// Unit tests for SortOrder API values and SearchOptions defaults.
class SearchOptionsTest {

    @Test
    fun `CLICK_COUNT maps to clickcount`() {
        assertEquals("clickcount", SortOrder.CLICK_COUNT.apiValue)
    }

    @Test
    fun `VOTES maps to votes`() {
        assertEquals("votes", SortOrder.VOTES.apiValue)
    }

    @Test
    fun `COUNTRY maps to country`() {
        assertEquals("country", SortOrder.COUNTRY.apiValue)
    }

    @Test
    fun `BITRATE maps to bitrate`() {
        assertEquals("bitrate", SortOrder.BITRATE.apiValue)
    }

    @Test
    fun `every SortOrder entry has a non-blank apiValue`() {
        SortOrder.entries.forEach { order ->
            assert(order.apiValue.isNotBlank()) {
                "SortOrder.${order.name} has a blank apiValue"
            }
        }
    }

    @Test
    fun `default searchMode is TAG`() {
        assertEquals(SearchMode.TAG, SearchOptions().searchMode)
    }

    @Test
    fun `default sortOrder is CLICK_COUNT`() {
        assertEquals(SortOrder.CLICK_COUNT, SearchOptions().sortOrder)
    }

    @Test
    fun `default country is empty (all countries)`() {
        assertEquals("", SearchOptions().country)
    }

    @Test
    fun `default hidebroken is true`() {
        assertEquals(true, SearchOptions().hidebroken)
    }

    @Test
    fun `default bitrateMin is 96`() {
        assertEquals(96, SearchOptions().bitrateMin)
    }

    @Test
    fun `DEFAULT_TAG_LIST is not empty`() {
        assert(DEFAULT_TAG_LIST.isNotEmpty())
    }

    @Test
    fun `DEFAULT_TAG_LIST has no blank entries`() {
        DEFAULT_TAG_LIST.forEach { tag ->
            assertNotNull("tag must not be null", tag)
            assert(tag.isNotBlank()) { "Blank tag found in DEFAULT_TAG_LIST" }
        }
    }

    @Test
    fun `DEFAULT_TAG_LIST has no duplicates`() {
        assertEquals(DEFAULT_TAG_LIST.size, DEFAULT_TAG_LIST.toSet().size)
    }
}

