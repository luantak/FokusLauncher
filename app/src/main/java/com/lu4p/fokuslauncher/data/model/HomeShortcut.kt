package com.lu4p.fokuslauncher.data.model

/**
 * One right-side shortcut icon on the home screen.
 *
 * [profileKey] matches [appProfileKey] / [FavoriteApp.profileKey] (`"0"` = owner profile).
 */
data class HomeShortcut(
    val iconName: String = "circle",
    val target: ShortcutTarget,
    val profileKey: String = "0",
)

/** Matches [com.lu4p.fokuslauncher.data.model.AppShortcutAction.id] for the same target/profile. */
fun HomeShortcut.stableSelectionKey(): String = "$profileKey|${ShortcutTarget.encode(target)}"

internal fun HomeShortcut.targetsPackage(packageName: String, profileKey: String): Boolean =
    this.profileKey == profileKey && when (val target = this.target) {
        is ShortcutTarget.App -> target.packageName == packageName
        is ShortcutTarget.LauncherShortcut -> target.packageName == packageName
        else -> false
    }
