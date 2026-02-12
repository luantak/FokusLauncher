package com.lu4p.fokuslauncher.data.model

/**
 * Constants for system categories that cannot be modified by users.
 */
object CategoryConstants {
    const val ALL_APPS = "All apps"
    const val PRIVATE = "Private"
    
    /**
     * Returns true if the given category name is a system category that cannot be modified.
     */
    fun isSystemCategory(categoryName: String): Boolean {
        return categoryName == ALL_APPS || categoryName == PRIVATE
    }
}
