package com.lu4p.fokuslauncher.data.model

import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppInfoMetadataTest {

    private val browserPackage = "com.vivaldi.browser"

    private fun browserHost() = AppInfo(browserPackage, "Vivaldi", null)

    private fun pwa(shortcutId: String, label: String) =
            AppInfo(
                    packageName = browserPackage,
                    label = label,
                    icon = null,
                    launcherShortcutId = shortcutId,
            )

    @Test
    fun `overlayCategory uses per-PWA assignment when present`() {
        val categories =
                listOf(
                        AppCategoryEntity(
                                browserPackage,
                                "0",
                                "Social",
                                launcherShortcutId = "pwa-twitter",
                        ),
                        AppCategoryEntity(
                                browserPackage,
                                "0",
                                "News",
                                launcherShortcutId = "pwa-reddit",
                        ),
                )

        assertEquals("Social", overlayCategory(pwa("pwa-twitter", "Twitter"), categories))
        assertEquals("News", overlayCategory(pwa("pwa-reddit", "Reddit"), categories))
        assertNull(overlayCategory(browserHost(), categories))
    }

    @Test
    fun `overlayCategory prefers per-PWA row over host fallback`() {
        val categories =
                listOf(
                        AppCategoryEntity(
                                browserPackage,
                                "0",
                                "Social",
                                launcherShortcutId = HOST_APP_METADATA_SENTINEL,
                        ),
                        AppCategoryEntity(
                                browserPackage,
                                "0",
                                "News",
                                launcherShortcutId = "pwa-reddit",
                        ),
                )

        assertEquals("Social", overlayCategory(browserHost(), categories))
        assertEquals("Social", overlayCategory(pwa("pwa-twitter", "Twitter"), categories))
        assertEquals("News", overlayCategory(pwa("pwa-reddit", "Reddit"), categories))
    }

    @Test
    fun `resolveAppCategory returns overlay when assigned`() {
        val categories =
                listOf(
                        AppCategoryEntity(browserPackage, "0", "Social"),
                )
        assertEquals("Social", resolveAppCategory(browserHost(), categories))
    }

    @Test
    fun `resolveAppCategory clears inferred category when suppressed`() {
        val app = browserHost().copy(category = "Games")
        assertEquals("", resolveAppCategory(app, emptyList(), setOf("Games")))
    }

    @Test
    fun `resolveAppCategory keeps inferred category when not suppressed`() {
        val app = browserHost().copy(category = "Games")
        assertEquals("Games", resolveAppCategory(app, emptyList()))
    }

    @Test
    fun `dynamicCategoryExtras excludes defined and suppressed categories`() {
        val extras =
                dynamicCategoryExtras(
                        appCategories = listOf("Games", "Social", "Games"),
                        definedCategories = listOf("Social"),
                        suppressedCategories = listOf("Games"),
                )
        assertEquals(emptyList<String>(), extras)
    }

    @Test
    fun `dynamicCategoryExtras returns sorted undefined non-suppressed categories`() {
        val extras =
                dynamicCategoryExtras(
                        appCategories = listOf("Finance", "Games"),
                        definedCategories = emptyList(),
                        suppressedCategories = listOf("Games"),
                )
        assertEquals(listOf("Finance"), extras)
    }
}
