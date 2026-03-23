package com.lu4p.fokuslauncher.data.model

/** Storage helpers for launcher font family (no bundled fonts). */
object LauncherFontPreferences {

    /** Empty string means Compose [androidx.compose.ui.text.font.FontFamily.Default]. */
    const val DEFAULT_FONT_FAMILY_STORAGE: String = ""

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
