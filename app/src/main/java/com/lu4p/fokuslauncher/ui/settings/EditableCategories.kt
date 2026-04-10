package com.lu4p.fokuslauncher.ui.settings

/**
 * Category names shown in the app categories editor: persisted definitions plus any category
 * labels present on installed apps (e.g. system-inferred defaults) not already in definitions.
 */
fun editableCategoriesForSettings(uiState: SettingsUiState): List<String> {
    val orderedDefined = uiState.categoryDefinitions.distinct()
    val dynamic =
            uiState.allApps
                    .mapNotNull { app -> app.category.takeIf { it.isNotBlank() } }
                    .toSet()
    val extras = (dynamic - orderedDefined.toSet()).toList().sorted()
    return orderedDefined + extras
}
