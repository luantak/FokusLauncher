package com.lu4p.fokuslauncher.data.model

/** Storage helpers for launcher font family (system names and imported `.ttf` files). */
object LauncherFontPreferences {

    /** Empty string means Compose [androidx.compose.ui.text.font.FontFamily.Default]. */
    const val DEFAULT_FONT_FAMILY_STORAGE: String = ""

    /** Prefix for a font copied into app-private storage ([CUSTOM_FONT_ACTIVE_FILE]). */
    const val CUSTOM_FONT_PREFIX: String = "custom:"

    const val CUSTOM_FONT_ACTIVE_FILE: String = "active.ttf"

    /** Stored preference value after a successful TTF import. */
    val CUSTOM_FONT_STORAGE: String = CUSTOM_FONT_PREFIX + CUSTOM_FONT_ACTIVE_FILE

    fun isCustomFont(storageValue: String?): Boolean =
            storageValue?.startsWith(CUSTOM_FONT_PREFIX) == true

    fun customFontRelativePath(storageValue: String): String? =
            if (!isCustomFont(storageValue)) {
                null
            } else {
                storageValue.removePrefix(CUSTOM_FONT_PREFIX).trim().takeIf { it.isNotEmpty() }
            }

    /** Human-readable label from a picked `.ttf` file name (extension stripped). */
    fun displayLabelFromTtfFileName(fileName: String): String {
        val trimmed = fileName.trim()
        return trimmed
                .removeSuffix(".ttf")
                .removeSuffix(".TTF")
                .ifBlank { trimmed }
    }

    /**
     * Maps legacy enum-style prefs and normalizes arbitrary stored names.
     */
    fun normalizeFontFamilyFromStorage(raw: String?): String {
        if (raw.isNullOrBlank()) return DEFAULT_FONT_FAMILY_STORAGE
        return when (raw) {
            "DEFAULT" -> DEFAULT_FONT_FAMILY_STORAGE
            "SANS_SERIF" -> "sans-serif"
            "SERIF" -> "serif"
            "MONOSPACE" -> "monospace"
            else -> raw.trim()
        }
    }
}
