package com.lu4p.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFontPreferencesTest {

    @Test
    fun `isCustomFont detects custom prefix`() {
        assertTrue(LauncherFontPreferences.isCustomFont(LauncherFontPreferences.CUSTOM_FONT_STORAGE))
        assertFalse(LauncherFontPreferences.isCustomFont("Roboto"))
        assertFalse(LauncherFontPreferences.isCustomFont(""))
    }

    @Test
    fun `customFontRelativePath returns file name for custom storage`() {
        assertEquals(
                LauncherFontPreferences.CUSTOM_FONT_ACTIVE_FILE,
                LauncherFontPreferences.customFontRelativePath(
                        LauncherFontPreferences.CUSTOM_FONT_STORAGE
                ),
        )
        assertEquals(null, LauncherFontPreferences.customFontRelativePath("sans-serif"))
    }

    @Test
    fun `displayLabelFromTtfFileName strips extension as fallback`() {
        assertEquals("Roboto", LauncherFontPreferences.displayLabelFromTtfFileName("Roboto.ttf"))
        assertEquals("My Font", LauncherFontPreferences.displayLabelFromTtfFileName("My Font.TTF"))
    }

    @Test
    fun `normalizeFontFamilyFromStorage maps legacy values`() {
        assertEquals("", LauncherFontPreferences.normalizeFontFamilyFromStorage("DEFAULT"))
        assertEquals("sans-serif", LauncherFontPreferences.normalizeFontFamilyFromStorage("SANS_SERIF"))
        assertEquals(
                LauncherFontPreferences.CUSTOM_FONT_STORAGE,
                LauncherFontPreferences.normalizeFontFamilyFromStorage(
                        LauncherFontPreferences.CUSTOM_FONT_STORAGE
                ),
        )
    }
}
