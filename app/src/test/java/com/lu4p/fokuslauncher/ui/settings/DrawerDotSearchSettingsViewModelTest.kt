package com.lu4p.fokuslauncher.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerDotSearchSettingsViewModelTest {

    @Test
    fun `isValidDotSearchUrlTemplate accepts percent q`() {
        assertTrue(
                DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(
                        "https://example.com/search?q=%q"
                )
        )
        assertTrue(
                DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(
                        "https://example.com/s?k=%q&x=1"
                )
        )
    }

    @Test
    fun `isValidDotSearchUrlTemplate rejects legacy placeholders alone`() {
        assertFalse(
                DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(
                        "https://example.com/s?k=%s"
                )
        )
        assertFalse(
                DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate(
                        "https://example.com/search?q={query}&x=1"
                )
        )
    }

    @Test
    fun `isValidDotSearchUrlTemplate rejects missing placeholder`() {
        assertFalse(DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate("https://example.com/"))
        assertFalse(DrawerDotSearchSettingsViewModel.isValidDotSearchUrlTemplate("   "))
    }
}
