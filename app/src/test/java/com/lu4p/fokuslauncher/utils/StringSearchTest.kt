package com.lu4p.fokuslauncher.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringSearchTest {

    @Test
    fun `normalizedForSearch strips Latin accents and lowercases`() {
        assertEquals("camera", "Càmera".normalizedForSearch())
        assertEquals("resume", "Résumé".normalizedForSearch())
    }

    @Test
    fun `normalizedForSearch ignores punctuation and spaces for matching`() {
        assertEquals("fdroid", "F-Droid".normalizedForSearch())
        assertEquals("myapp", "My_App".normalizedForSearch())
        assertEquals("one2", "One.2!".normalizedForSearch())
    }

    @Test
    fun `containsNormalizedSearch matches ascii query against accented label`() {
        assertTrue("càmera".containsNormalizedSearch("cam"))
        assertTrue("Càmera".containsNormalizedSearch("cam"))
    }

    @Test
    fun `containsNormalizedSearch matches when query has accents`() {
        assertTrue("Camera".containsNormalizedSearch("càm"))
    }

    @Test
    fun `containsNormalizedSearch mismatch when no shared fold`() {
        assertFalse("foo".containsNormalizedSearch("bar"))
    }

    @Test
    fun `containsNormalizedSearch empty needle matches like String_contains`() {
        assertTrue("anything".containsNormalizedSearch(""))
    }

    @Test
    fun `containsNormalizedSearch matches when label has separators query does not`() {
        assertTrue("F-Droid".containsNormalizedSearch("fdroid"))
        assertTrue("F-Droid".containsNormalizedSearch("fdr"))
    }
}
