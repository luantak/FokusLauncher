package com.lu4p.fokuslauncher.data.local

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/** Last known app list on disk so the first drawer render does not wait for a LauncherApps scan. */
@Singleton
class AppListSnapshotStore @Inject constructor(@ApplicationContext context: Context) {

    data class Entry(
            val packageName: String,
            val label: String,
            val category: String,
            val profileKey: String,
            val componentName: String?,
            val launcherShortcutId: String?,
            val isArchived: Boolean,
    )

    private val file = AtomicFile(File(context.filesDir, FILE_NAME))

    /** Null when absent, corrupt, or written by another version. */
    @Synchronized fun read(): List<Entry>? {
        return try {
            val root = JSONObject(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
            if (root.optInt(KEY_VERSION) != VERSION) return null
            val rows = root.optJSONArray(KEY_APPS) ?: return null
            val entries = ArrayList<Entry>(rows.length())
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val packageName = row.optString(KEY_PACKAGE)
                val label = row.optString(KEY_LABEL)
                if (packageName.isBlank() || label.isBlank()) continue
                entries.add(
                        Entry(
                                packageName = packageName,
                                label = label,
                                category = row.optString(KEY_CATEGORY),
                                profileKey = row.optString(KEY_PROFILE, "0").ifBlank { "0" },
                                componentName =
                                        row.optString(KEY_COMPONENT).takeIf { it.isNotBlank() },
                                launcherShortcutId =
                                        row.optString(KEY_SHORTCUT_ID).takeIf { it.isNotBlank() },
                                isArchived = row.optBoolean(KEY_ARCHIVED, false),
                        )
                )
            }
            entries.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized fun write(entries: List<Entry>) {
        if (entries.isEmpty()) return
        var stream: FileOutputStream? = null
        try {
            val rows = JSONArray()
            entries.forEach { entry ->
                rows.put(
                        JSONObject().apply {
                            put(KEY_PACKAGE, entry.packageName)
                            put(KEY_LABEL, entry.label)
                            if (entry.category.isNotBlank()) put(KEY_CATEGORY, entry.category)
                            if (entry.profileKey != "0") put(KEY_PROFILE, entry.profileKey)
                            entry.componentName?.let { put(KEY_COMPONENT, it) }
                            entry.launcherShortcutId?.let { put(KEY_SHORTCUT_ID, it) }
                            if (entry.isArchived) put(KEY_ARCHIVED, true)
                        }
                )
            }
            val payload =
                    JSONObject().apply {
                        put(KEY_VERSION, VERSION)
                        put(KEY_APPS, rows)
                    }
            stream = file.startWrite()
            stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (_: Exception) {
            stream?.let(file::failWrite)
        }
    }

    private companion object {
        const val FILE_NAME = "app_list_snapshot.json"
        const val VERSION = 1
        const val KEY_VERSION = "version"
        const val KEY_APPS = "apps"
        const val KEY_PACKAGE = "pkg"
        const val KEY_LABEL = "label"
        const val KEY_CATEGORY = "category"
        const val KEY_PROFILE = "profile"
        const val KEY_COMPONENT = "component"
        const val KEY_SHORTCUT_ID = "shortcutId"
        const val KEY_ARCHIVED = "archived"
    }
}
