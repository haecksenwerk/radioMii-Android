package com.radiomii.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for IcyMetadata computed properties.
class IcyMetadataTest {

    @Test
    fun `hasContent is true when artist is set`() {
        assertTrue(IcyMetadata(artist = "Pink Floyd").hasContent)
    }

    @Test
    fun `hasContent is true when title is set`() {
        assertTrue(IcyMetadata(title = "Comfortably Numb").hasContent)
    }

    @Test
    fun `hasContent is true when both artist and title are set`() {
        assertTrue(IcyMetadata(artist = "Pink Floyd", title = "Comfortably Numb").hasContent)
    }

    @Test
    fun `hasContent is false when artist and title are empty`() {
        assertFalse(IcyMetadata(rawTitle = "some raw").hasContent)
    }

    @Test
    fun `hasContent is false for default instance`() {
        assertFalse(IcyMetadata().hasContent)
    }

    @Test
    fun `display shows artist and title with en-dash when both set`() {
        val meta = IcyMetadata(artist = "Radiohead", title = "Creep")
        assertEquals("Radiohead – Creep", meta.display)
    }

    @Test
    fun `display shows title only when artist is empty`() {
        val meta = IcyMetadata(title = "Creep")
        assertEquals("Creep", meta.display)
    }

    @Test
    fun `display falls back to rawTitle when artist and title are empty`() {
        val meta = IcyMetadata(rawTitle = "Radiohead - Creep")
        assertEquals("Radiohead - Creep", meta.display)
    }

    @Test
    fun `display is empty when all fields are empty`() {
        assertEquals("", IcyMetadata().display)
    }

    @Test
    fun `display prefers artist and title over rawTitle`() {
        val meta = IcyMetadata(artist = "A", title = "B", rawTitle = "raw")
        assertEquals("A – B", meta.display)
    }

    @Test
    fun `display prefers title over rawTitle when artist is empty`() {
        val meta = IcyMetadata(title = "B", rawTitle = "raw")
        assertEquals("B", meta.display)
    }
}

