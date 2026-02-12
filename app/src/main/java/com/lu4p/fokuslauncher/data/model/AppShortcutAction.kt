package com.lu4p.fokuslauncher.data.model

/**
 * A selectable shortcut action for right-side home shortcuts.
 * Includes the app launch action and launcher-published long-press actions.
 */
data class AppShortcutAction(
    val appLabel: String,
    val actionLabel: String,
    val target: ShortcutTarget
) {
    val id: String get() = ShortcutTarget.encode(target)

    val displayLabel: String
        get() = if (actionLabel == OPEN_APP_LABEL) appLabel else "$appLabel - $actionLabel"

    companion object {
        const val OPEN_APP_LABEL = "Open app"
    }
}
