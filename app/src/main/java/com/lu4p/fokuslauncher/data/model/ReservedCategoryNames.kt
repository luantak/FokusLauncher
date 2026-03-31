package com.lu4p.fokuslauncher.data.model

/**
 * Fixed English names for special drawer categories. Used for persistence, filtering, and
 * comparisons. Must stay in sync with the default English `drawer_filter_all_apps`,
 * `drawer_filter_uncategorized`, `drawer_filter_private`, and `drawer_filter_work` strings
 * (translatable UI labels).
 */
object ReservedCategoryNames {
    const val ALL_APPS = "All apps"
    /** Apps with no assigned category (blank); synthetic drawer bucket, not stored in Room. */
    const val UNCATEGORIZED = "Uncategorized"
    const val PRIVATE = "Private"
    /** Managed / work-style profile apps; they still appear in other categories and All apps. */
    const val WORK = "Work"
}
