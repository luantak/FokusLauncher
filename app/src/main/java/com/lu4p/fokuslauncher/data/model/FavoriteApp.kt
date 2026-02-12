package com.lu4p.fokuslauncher.data.model

/**
 * A row on the home screen.
 *
 * Left side  = [label] that launches [packageName].
 * Right side = minimal icon ([iconName]) that launches [iconPackage].
 *
 * The two sides are independent; tapping the label opens one app,
 * tapping the icon opens another (or the same if [iconPackage] is empty/equal).
 */
data class FavoriteApp(
    val label: String,
    val packageName: String,
    val iconName: String = "circle",
    val iconPackage: String = "" // Encoded ShortcutTarget or legacy package name.
) {
    /** Kept for backwards compat */
    val categoryLabel: String get() = label

    /** Resolved icon target with legacy fallback to package-based launch. */
    val resolvedIconTarget: ShortcutTarget
        get() = ShortcutTarget.decode(iconPackage) ?: ShortcutTarget.App(packageName)

    /** Legacy helper still used in tests and older call sites. */
    val resolvedIconPackage: String
        get() = (resolvedIconTarget as? ShortcutTarget.App)?.packageName ?: packageName
}
