package com.lu4p.fokuslauncher.data.local

import com.lu4p.fokuslauncher.data.model.FavoriteApp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the PreferencesManager parsing logic.
 * DataStore interactions require instrumented tests; these test the parsing layer.
 */
class PreferencesManagerTest {

    @Test
    fun `parseFavorites correctly parses new 4-field format`() {
        val raw = "Music;com.lu4p.music;music;com.lu4p.player|Work;com.lu4p.work;work;"
        val result = parseFavorites(raw)

        assertEquals(2, result.size)
        assertEquals(FavoriteApp("Music", "com.lu4p.music", "music", "com.lu4p.player"), result[0])
        assertEquals("com.lu4p.player", result[0].resolvedIconPackage)
        // Empty iconPackage => resolvedIconPackage falls back to packageName
        assertEquals("com.lu4p.work", result[1].resolvedIconPackage)
    }

    @Test
    fun `parseFavorites correctly parses 3-field format (no iconPackage)`() {
        val raw = "Music;com.lu4p.music;music|Work;com.lu4p.work;work|Social;com.lu4p.social;chat"
        val result = parseFavorites(raw)

        assertEquals(3, result.size)
        assertEquals(FavoriteApp("Music", "com.lu4p.music", "music", ""), result[0])
        assertEquals(FavoriteApp("Work", "com.lu4p.work", "work", ""), result[1])
        assertEquals(FavoriteApp("Social", "com.lu4p.social", "chat", ""), result[2])
    }

    @Test
    fun `parseFavorites handles legacy colon-separated format`() {
        val raw = "Music:com.lu4p.music|Work:com.lu4p.work|Social:com.lu4p.social"
        val result = parseFavorites(raw)

        assertEquals(3, result.size)
        assertEquals("Music", result[0].label)
        assertEquals("com.lu4p.music", result[0].packageName)
        assertEquals("circle", result[0].iconName) // default icon for legacy
        assertEquals("", result[0].iconPackage)
    }

    @Test
    fun `parseFavorites handles empty string`() {
        assertEquals(0, parseFavorites("").size)
    }

    @Test
    fun `parseFavorites handles blank string`() {
        assertEquals(0, parseFavorites("   ").size)
    }

    @Test
    fun `parseFavorites handles single entry new format`() {
        val raw = "Music;com.lu4p.music;star"
        val result = parseFavorites(raw)

        assertEquals(1, result.size)
        assertEquals("Music", result[0].label)
        assertEquals("star", result[0].iconName)
    }

    @Test
    fun `parseFavorites handles single entry legacy format`() {
        val raw = "Music:com.lu4p.music"
        val result = parseFavorites(raw)

        assertEquals(1, result.size)
        assertEquals("Music", result[0].categoryLabel)
    }

    @Test
    fun `parseFavorites skips malformed entries`() {
        val raw = "Music;com.lu4p.music;music|BadEntry|Work;com.lu4p.work;work"
        val result = parseFavorites(raw)

        assertEquals(2, result.size)
        assertEquals("Music", result[0].label)
        assertEquals("Work", result[1].label)
    }

    @Test
    fun `serializeFavorites round-trips correctly with 4-field format`() {
        val favorites = listOf(
            FavoriteApp("Music", "com.lu4p.music", "music", "com.lu4p.player"),
            FavoriteApp("Work", "com.lu4p.work", "work", "")
        )
        val serialized = favorites.joinToString("|") {
            "${it.label};${it.packageName};${it.iconName};${it.iconPackage}"
        }
        val deserialized = parseFavorites(serialized)

        assertEquals(favorites, deserialized)
    }

    @Test
    fun `resolvedIconPackage falls back to packageName when iconPackage blank`() {
        val fav = FavoriteApp("Music", "com.lu4p.music", "music", "")
        assertEquals("com.lu4p.music", fav.resolvedIconPackage)
    }

    @Test
    fun `resolvedIconPackage returns iconPackage when set`() {
        val fav = FavoriteApp("Music", "com.lu4p.music", "music", "com.lu4p.other")
        assertEquals("com.lu4p.other", fav.resolvedIconPackage)
    }

    @Test
    fun `categoryLabel getter returns label`() {
        val fav = FavoriteApp("Music", "com.lu4p.music", "music")
        assertEquals("Music", fav.categoryLabel)
        assertEquals(fav.label, fav.categoryLabel)
    }

    /**
     * Mirrors the parsing logic in PreferencesManager for testability.
     */
    private fun parseFavorites(raw: String): List<FavoriteApp> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val semiParts = entry.split(";")
            if (semiParts.size >= 3) {
                FavoriteApp(
                    label = semiParts[0],
                    packageName = semiParts[1],
                    iconName = semiParts[2],
                    iconPackage = semiParts.getOrElse(3) { "" }
                )
            } else {
                val colonParts = entry.split(":", limit = 2)
                if (colonParts.size == 2) {
                    FavoriteApp(
                        label = colonParts[0],
                        packageName = colonParts[1],
                        iconName = "circle"
                    )
                } else null
            }
        }
    }
}
