package com.lu4p.fokuslauncher.ui.components

import com.lu4p.fokuslauncher.ui.components.generated.MaterialPickerAllowlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MinimalIcons.names] uses the curated picker allowlist; Robolectric suffices for unit tests.
 * Regenerate shipped icons / allowlist via `scripts/gen_shipped_outlined_icons.py` when
 * `scripts/icon_picker_allowlist.txt` changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MinimalIconsPickerCountTest {

    @Test
    fun minimalIconPicker_namesCount_matchesExpectedCatalogSize() {
        val names = MinimalIcons.names
        assertEquals(
                "Update EXPECTED_PICKER_ICON_COUNT when scripts/icon_picker_allowlist.txt changes.",
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
    fun minimalIconPicker_staysCuratedNotFullCatalog() {
        // Curated allowlist — useful coverage without shipping the full Material catalog (~1700).
        assertTrue(MinimalIcons.names.size < 800)
        assertTrue(MinimalIcons.names.size >= 200)
        // Allowlist plus legacy AutoMirrored `send` (not an Icons.Outlined.* allowlist entry).
        assertTrue(MinimalIcons.names.size <= MaterialPickerAllowlist.NAMES.size + 1)
    }

    @Test
    fun minimalIconPicker_includesEverydayShortcutIcons() {
        val names = MinimalIcons.names.toSet()
        for (name in
                listOf(
                        "Language",
                        "Public",
                        "Web",
                        "CalendarMonth",
                        "Event",
                        "EditNote",
                        "Draw",
                        "StickyNote2",
                        "Notes",
                        "Article",
                        "Description",
                )) {
            assertTrue("$name should be offered in the icon picker", name in names)
        }
    }

    @Test
    fun minimalIconPicker_searchAliasesMatchCommonIntents() {
        fun matches(query: String): List<String> =
                MinimalIcons.names.filter { name ->
                    name.contains(query, ignoreCase = true) ||
                            MinimalIcons.materialOutlinedSearchHaystack(name)
                                    .contains(query, ignoreCase = true)
                }

        assertTrue(matches("globe").any { it == "Language" || it == "Public" })
        assertTrue(matches("calendar").contains("CalendarMonth"))
        assertTrue(matches("pencil").any { it == "EditNote" || it == "Draw" })
        assertTrue(matches("browser").any { it == "Language" || it == "Web" || it == "OpenInBrowser" })
        assertTrue("send should match message", matches("message").contains("send"))
        assertTrue(matches("message").any { it == "Chat" || it == "Sms" || it == "Mail" })
        assertTrue(matches("gym").contains("FitnessCenter"))
        assertTrue(matches("uber").contains("LocalTaxi"))
        assertTrue(matches("spotify").contains("MusicNote"))
    }

    @Test
    fun minimalIconPicker_omitsCatalogNoise() {
        val names = MinimalIcons.names.toSet()
        for (name in
                listOf(
                        "FormatBold",
                        "BorderLeft",
                        "Wifi1Bar",
                        "SignalWifi4Bar",
                        "LaptopMac",
                        "ArrowBackIos",
                        "KeyboardDoubleArrowDown",
                        "SmartDisplay",
                )) {
            assertTrue("$name should stay out of the curated picker", name !in names)
        }
    }

    companion object {
        /** Distinct [MinimalIcons.names] entries offered in pickers (baseline; bump when allowlist changes). */
        const val EXPECTED_PICKER_ICON_COUNT: Int = 548
    }
}
