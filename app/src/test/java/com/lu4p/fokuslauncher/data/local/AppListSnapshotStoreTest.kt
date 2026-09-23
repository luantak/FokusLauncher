package com.lu4p.fokuslauncher.data.local

import android.content.Context
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppListSnapshotStoreTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val store = AppListSnapshotStore(context)

    private fun entry(
            packageName: String,
            label: String = packageName.substringAfterLast('.'),
            profileKey: String = "0",
            componentName: String? = null,
            launcherShortcutId: String? = null,
            isArchived: Boolean = false,
    ) =
            AppListSnapshotStore.Entry(
                    packageName = packageName,
                    label = label,
                    category = "",
                    profileKey = profileKey,
                    componentName = componentName,
                    launcherShortcutId = launcherShortcutId,
                    isArchived = isArchived,
            )

    @Test
    fun `write then read round-trips all fields`() {
        val entries =
                listOf(
                        entry("com.example.one", label = "One"),
                        entry(
                                "com.example.work",
                                label = "Work App",
                                profileKey = "10",
                                componentName = "com.example.work/.MainActivity",
                        ),
                        entry(
                                "com.example.pwa",
                                label = "PWA",
                                launcherShortcutId = "shortcut-1",
                        ),
                        entry("com.example.archived", label = "Archived", isArchived = true),
                )
        store.write(entries)
        assertEquals(entries, store.read())
    }

    @Test
    fun `corrupt snapshot file reads as null`() {
        store.write(listOf(entry("com.example.one")))
        File(context.filesDir, "app_list_snapshot.json").writeText("{not json")
        assertNull(store.read())
    }

}
