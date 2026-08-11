package com.lu4p.fokuslauncher.notification

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIndicatorKeysTest {

    @Test
    fun `notificationAppKey uses profile pipe package format`() {
        assertEquals("0|com.example.app", notificationAppKey("com.example.app", null))
    }

    @Test
    fun `notificationAppKeyOrNull ignores group summaries`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.example.app",
                        flags = Notification.FLAG_GROUP_SUMMARY,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull ignores ongoing keep-alives`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.example.mail",
                        flags = Notification.FLAG_ONGOING_EVENT,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull ignores foreground service notifications`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.example.chat",
                        flags = Notification.FLAG_FOREGROUND_SERVICE,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull ignores suppressed badges`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.example.mail",
                        flags = 0,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                        canShowBadge = false,
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull ignores own package`() {
        assertNull(
                notificationAppKeyOrNull(
                        packageName = "com.lu4p.fokuslauncher",
                        flags = 0,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                )
        )
    }

    @Test
    fun `notificationAppKeyOrNull returns key for normal notification`() {
        assertEquals(
                "0|com.example.mail",
                notificationAppKeyOrNull(
                        packageName = "com.example.mail",
                        flags = 0,
                        userHandle = null,
                        ownPackageName = "com.lu4p.fokuslauncher",
                ),
        )
    }

    @Test
    fun `appsWithNotificationsFromEntries builds unique keys and skips ignored`() {
        val own = "com.lu4p.fokuslauncher"
        val keys =
                appsWithNotificationsFromEntries(
                        listOf(
                                Triple("com.example.mail", 0, null),
                                Triple("com.example.mail", Notification.FLAG_GROUP_SUMMARY, null),
                                Triple("com.example.mail", Notification.FLAG_ONGOING_EVENT, null),
                                Triple(
                                        "com.example.push",
                                        Notification.FLAG_FOREGROUND_SERVICE,
                                        null,
                                ),
                                Triple(own, 0, null),
                                Triple("com.example.chat", 0, null),
                        ),
                        own,
                )

        assertEquals(setOf("0|com.example.mail", "0|com.example.chat"), keys)
    }

    @Test
    fun `appsWithNotificationsFrom returns empty for null or empty`() {
        assertTrue(appsWithNotificationsFrom(null, "com.lu4p.fokuslauncher").isEmpty())
        assertTrue(appsWithNotificationsFrom(emptyArray(), "com.lu4p.fokuslauncher").isEmpty())
    }
}
