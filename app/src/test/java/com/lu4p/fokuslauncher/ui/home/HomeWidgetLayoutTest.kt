package com.lu4p.fokuslauncher.ui.home

import com.lu4p.fokuslauncher.data.model.HomeAlignment
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWidgetLayoutTest {

    @Test
    fun `left home alignment places widgets at start`() {
        assertEquals(HomeWidgetAlignment.START, HomeWidgetAlignment.from(HomeAlignment.LEFT))
    }

    @Test
    fun `center and middle home alignments center widgets`() {
        assertEquals(HomeWidgetAlignment.CENTER, HomeWidgetAlignment.from(HomeAlignment.CENTER))
        assertEquals(HomeWidgetAlignment.CENTER, HomeWidgetAlignment.from(HomeAlignment.MIDDLE))
    }

    @Test
    fun `right home alignment places widgets at end`() {
        assertEquals(HomeWidgetAlignment.END, HomeWidgetAlignment.from(HomeAlignment.RIGHT))
    }

    @Test
    fun `widget control icons use one unscaled base size`() {
        assertEquals(24f, WidgetControlIconBaseSizeDp, 0f)
    }
}
