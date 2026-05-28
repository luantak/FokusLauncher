package com.lu4p.fokuslauncher.data.font

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFontStoreTest {

    @Test
    fun `isTtfFileName accepts ttf extension only`() {
        assertTrue(CustomFontStore.isTtfFileName("Inter.ttf"))
        assertTrue(CustomFontStore.isTtfFileName("My Font.TTF"))
        assertFalse(CustomFontStore.isTtfFileName("Inter.otf"))
        assertFalse(CustomFontStore.isTtfFileName("readme.txt"))
    }
}
