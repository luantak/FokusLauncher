package com.lu4p.fokuslauncher.data.model

import java.util.Base64

/**
 * Represents a launchable shortcut target.
 *
 * Persisted format:
 * - App: "app:<packageName>"
 * - Deep link / intent URI: "intent:<intentUri>"
 * - Launcher shortcut action: "launcher:<base64(packageName)>:<base64(shortcutId)>"
 * - Default phone/dialer: "internal:phone" ([PhoneDial] — uses [android.content.Intent.ACTION_DIAL])
 *
 * Legacy values without a prefix are treated as app package names.
 */
sealed interface ShortcutTarget {
    data class App(val packageName: String) : ShortcutTarget
    data class DeepLink(val intentUri: String) : ShortcutTarget
    data class LauncherShortcut(val packageName: String, val shortcutId: String) : ShortcutTarget

    /** Opens the default dialer via [android.content.Intent.ACTION_DIAL] (no fixed package). */
    data object PhoneDial : ShortcutTarget

    companion object {
        private const val APP_PREFIX = "app:"
        private const val INTENT_PREFIX = "intent:"
        private const val LAUNCHER_PREFIX = "launcher:"
        private const val INTERNAL_PREFIX = "internal:"
        private const val PHONE_INTERNAL_KEY = "phone"

        /**
         * [FavoriteApp.packageName] for the built-in Health / phone row — not an installed package.
         * See [decode] / favorites migration for legacy dialer package names.
         */
        const val PHONE_FAVORITE_SENTINEL_PACKAGE = "com.lu4p.fokuslauncher.internal.phone"

        fun decode(raw: String): ShortcutTarget? {
            if (raw.isBlank()) return null
            return when {
                raw.startsWith(INTERNAL_PREFIX) ->
                        when (raw.removePrefix(INTERNAL_PREFIX).trim()) {
                            PHONE_INTERNAL_KEY -> PhoneDial
                            else -> null
                        }
                raw.startsWith(APP_PREFIX) -> {
                    val packageName = raw.removePrefix(APP_PREFIX).trim()
                    if (packageName.isBlank()) null else App(packageName)
                }
                raw.startsWith(INTENT_PREFIX) -> {
                    val uri = raw.removePrefix(INTENT_PREFIX).trim()
                    if (uri.isBlank()) null else DeepLink(uri)
                }
                raw.startsWith(LAUNCHER_PREFIX) -> {
                    val payload = raw.removePrefix(LAUNCHER_PREFIX)
                    val parts = payload.split(":", limit = 2)
                    if (parts.size != 2) return null
                    val packageName = decodePart(parts[0]).trim()
                    val shortcutId = decodePart(parts[1]).trim()
                    if (packageName.isBlank() || shortcutId.isBlank()) null
                    else LauncherShortcut(packageName, shortcutId)
                }
                else -> App(raw.trim())
            }
        }

        fun encode(target: ShortcutTarget?): String = when (target) {
            null -> ""
            is App -> APP_PREFIX + target.packageName
            is DeepLink -> INTENT_PREFIX + target.intentUri
            is LauncherShortcut ->
                LAUNCHER_PREFIX + encodePart(target.packageName) + ":" + encodePart(target.shortcutId)
            is PhoneDial -> INTERNAL_PREFIX + PHONE_INTERNAL_KEY
        }

        private fun encodePart(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

        private fun decodePart(value: String): String =
            try {
                String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                ""
            }
    }
}
