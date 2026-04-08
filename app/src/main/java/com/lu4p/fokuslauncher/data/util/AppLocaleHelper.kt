package com.lu4p.fokuslauncher.data.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lu4p.fokuslauncher.data.local.APP_LOCALE_TAG_KEY
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object AppLocaleHelper {

    fun applyLocaleTag(tag: String) {
        val locales =
                if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag.trim())
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Read persisted tag and apply before the first activity attaches.
     *
     * Must not use [runBlocking] on the main thread: DataStore can dispatch to the main looper,
     * which deadlocks while the UI thread is blocked inside [runBlocking].
     */
    fun applyStoredLocaleFromDisk(context: Context) {
        val appContext = context.applicationContext
        val tag =
                try {
                    val holder = arrayOfNulls<String>(1)
                    val worker =
                            Thread(
                                    {
                                        holder[0] =
                                                runBlocking {
                                                    appContext.fokusLauncherPreferencesDataStore
                                                            .data
                                                            .first()[APP_LOCALE_TAG_KEY] ?: ""
                                                }
                                    },
                                    "fokus-locale-bootstrap"
                            )
                    worker.start()
                    worker.join()
                    holder[0] ?: ""
                } catch (_: Exception) {
                    ""
                }
        try {
            applyLocaleTag(tag)
        } catch (_: Exception) {
            // Avoid taking down the process if AppCompat locale APIs fail on a specific device.
        }
    }
}
