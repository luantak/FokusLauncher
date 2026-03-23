package com.lu4p.fokuslauncher.data.model

/**
 * Fixed English names for special drawer categories. Used for persistence, filtering, and
 * comparisons. Must stay in sync with the default English `drawer_filter_all_apps` and
 * `drawer_filter_private` strings (translatable UI labels).
 */
object ReservedCategoryNames {
    const val ALL_APPS = "All apps"
    const val PRIVATE = "Private"
}
