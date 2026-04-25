package com.lu4p.fokuslauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedWidgetTest {

    @Test
    fun `parse blank returns empty list`() {
        assertTrue(parseHostedWidgets("").isEmpty())
        assertTrue(parseHostedWidgets("   ").isEmpty())
    }

    @Test
    fun `serialize and parse round trips widgets by position`() {
        val widgets =
                listOf(
                        HostedWidget(
                                id = "second",
                                appWidgetId = 12,
                                providerPackageName = "com.example",
                                providerClassName = "Two",
                                label = "Two",
                                heightDp = 220,
                                position = 1,
                        ),
                        HostedWidget(
                                id = "first",
                                appWidgetId = 11,
                                providerPackageName = "com.example",
                                providerClassName = "One",
                                label = "One",
                                heightDp = 180,
                                position = 0,
                        ),
                )

        assertEquals(widgets.sortedBy { it.position }, parseHostedWidgets(serializeHostedWidgets(widgets)))
    }

    @Test
    fun `parse invalid json returns empty list`() {
        assertTrue(parseHostedWidgets("{bad").isEmpty())
    }

    @Test
    fun `parse drops invalid entries and clamps height`() {
        val raw =
                """
                [
                  {"id":"bad","appWidgetId":-1,"providerPackageName":"pkg","providerClassName":"Cls"},
                  {"id":"small","appWidgetId":1,"providerPackageName":"pkg","providerClassName":"Cls","heightDp":10,"position":0},
                  {"id":"large","appWidgetId":2,"providerPackageName":"pkg","providerClassName":"Cls","heightDp":900,"position":1}
                ]
                """.trimIndent()

        val parsed = parseHostedWidgets(raw)

        assertEquals(2, parsed.size)
        assertEquals(HOSTED_WIDGET_MIN_HEIGHT_DP, parsed[0].heightDp)
        assertEquals(HOSTED_WIDGET_MAX_HEIGHT_DP, parsed[1].heightDp)
    }
}
