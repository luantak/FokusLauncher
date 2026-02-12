package com.lu4p.fokuslauncher.data.model

/**
 * One right-side shortcut icon on the home screen.
 */
data class HomeShortcut(
    val iconName: String = "circle",
    val target: ShortcutTarget
)
