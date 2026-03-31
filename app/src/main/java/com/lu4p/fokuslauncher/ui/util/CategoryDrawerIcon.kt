package com.lu4p.fokuslauncher.ui.util

import android.content.Context
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.SystemCategoryKeys
import com.lu4p.fokuslauncher.ui.components.MinimalIcons
import java.util.Locale

/**
 * Resolves the [com.lu4p.fokuslauncher.ui.components.MinimalIcons] key for a drawer category,
 * using user overrides when present (same keys as [SystemCategoryKeys.normalize]).
 */
fun resolvedCategoryDrawerIconName(
        context: Context,
        category: String,
        overrides: Map<String, String>
): String {
    val key = SystemCategoryKeys.normalize(context, category)
    val override = overrides[key]
    if (!override.isNullOrBlank() && MinimalIcons.all.containsKey(override)) return override
    return categoryDrawerIconName(category)
}

/** Maps a drawer category id to a [com.lu4p.fokuslauncher.ui.components.MinimalIcons] key for the vertical rail. */
fun categoryDrawerIconName(category: String): String {
    val c = category.trim()
    return when {
        c.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) -> "apps"
        c.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) -> "lock"
        c.equals(ReservedCategoryNames.WORK, ignoreCase = true) -> "work"
        c.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true) -> "category"
        c.equals(SystemCategoryKeys.UTILITIES, ignoreCase = true) -> "settings"
        c.equals(SystemCategoryKeys.GAMES, ignoreCase = true) -> "game"
        c.equals(SystemCategoryKeys.PRODUCTIVITY, ignoreCase = true) -> "folder"
        c.equals(SystemCategoryKeys.SOCIAL, ignoreCase = true) -> "person"
        c.equals(SystemCategoryKeys.MEDIA, ignoreCase = true) -> "headset"
        else -> {
            val options = MinimalIcons.names
            val idx = (c.lowercase(Locale.getDefault()).hashCode() and Int.MAX_VALUE) % options.size
            options[idx]
        }
    }
}
