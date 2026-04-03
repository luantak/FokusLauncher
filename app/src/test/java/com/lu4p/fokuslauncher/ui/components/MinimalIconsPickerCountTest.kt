package com.lu4p.fokuslauncher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MinimalIcons.names] loads outlined icons via reflection; Robolectric provides a classpath close
 * enough for unit tests. The constant documents how many distinct glyphs appear in icon pickers when
 * categories / the material index change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MinimalIconsPickerCountTest {

    @Test
    fun minimalIconPicker_namesCount_matchesExpectedCatalogSize() {
        val names = MinimalIcons.names
        assertEquals(
                "Update EXPECTED_PICKER_ICON_COUNT when MaterialOutlinedIconIndex, " +
                        "MaterialOutlinedIconCategories, or picker filters change.",
                EXPECTED_PICKER_ICON_COUNT,
                names.size
        )
    }

    @Test
    fun minimalIconPicker_namesAreUniqueAndCoverAllSections() {
        val names = MinimalIcons.names
        assertEquals(names.size, names.toSet().size)
        val fromSections = MinimalIcons.iconPickerSections.sumOf { it.names.size }
        assertEquals(names.size, fromSections)
    }

    @Test
    fun minimalIconPicker_isNonTrivial() {
        assertTrue(MinimalIcons.names.size >= 100)
    }

    companion object {
        /** Distinct [MinimalIcons.names] entries offered in pickers (baseline; bump when catalog changes). */
        const val EXPECTED_PICKER_ICON_COUNT: Int = 1140
    }
}
