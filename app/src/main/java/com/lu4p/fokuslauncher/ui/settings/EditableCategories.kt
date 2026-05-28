package com.lu4p.fokuslauncher.ui.settings

import com.lu4p.fokuslauncher.data.model.dynamicCategoryExtras

/**
 * Category names shown in the app categories editor: persisted definitions plus any category
 * labels present on installed apps (e.g. system-inferred defaults) not already in definitions.
 */
fun editableCategoriesForSettings(uiState: SettingsUiState): List<String> {
    val orderedDefined = uiState.categoryDefinitions.distinct()
    val appCategories =
            uiState.allApps.mapNotNull { app -> app.category.takeIf { it.isNotBlank() } }
    val extras =
            dynamicCategoryExtras(
                    appCategories = appCategories,
                    definedCategories = orderedDefined,
                    suppressedCategories = uiState.suppressedCategories,
            )
    return orderedDefined + extras
}
