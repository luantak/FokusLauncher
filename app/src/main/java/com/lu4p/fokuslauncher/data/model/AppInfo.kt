package com.lu4p.fokuslauncher.data.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.lu4p.fokuslauncher.utils.normalizedForSearch

/**
 * Represents an installed app on the device.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val category: String = "",
    /** Non-null for apps in a secondary Android user (work, clone, …); used with [LauncherApps]. */
    val userHandle: UserHandle? = null,
    /** Non-null when [userHandle] is set; the activity to start in that profile. */
    val componentName: ComponentName? = null,
    /** Non-null for pinned launcher shortcuts, including browser-created PWA shortcuts. */
    val launcherShortcutId: String? = null
) {
    val normalizedLabel: String by lazy(LazyThreadSafetyMode.NONE) { label.normalizedForSearch() }
}

/** Persisted on host-app metadata rows; distinct from legacy package-wide [LEGACY_PACKAGE_WIDE_METADATA]. */
const val HOST_APP_METADATA_SENTINEL = "__host__"

/** Migrated v4 metadata rows: applies to every row sharing package + profile. */
const val LEGACY_PACKAGE_WIDE_METADATA = ""

/** Shortcut-aware metadata identity; mirrors [appListStableKey] for PWAs. */
fun appMetadataKey(app: AppInfo): String {
    val base = appMetadataKey(app.packageName, app.userHandle)
    app.launcherShortcutId?.let { return "$base#shortcut:$it" }
    return base
}

fun launcherShortcutIdForMetadata(app: AppInfo): String =
        app.launcherShortcutId ?: HOST_APP_METADATA_SENTINEL

fun metadataSettingsStableKey(
        packageName: String,
        profileKey: String,
        launcherShortcutId: String,
): String {
    val base = appMetadataKey(packageName, profileKey)
    return when (launcherShortcutId) {
        LEGACY_PACKAGE_WIDE_METADATA,
        HOST_APP_METADATA_SENTINEL -> base
        else -> "$base#shortcut:$launcherShortcutId"
    }
}

fun isAppHiddenByMetadata(app: AppInfo, hiddenApps: List<com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity>): Boolean {
    if (hiddenApps.isEmpty()) return false
    val profileKey = appProfileKey(app.userHandle)
    val shortcutId = app.launcherShortcutId
    return hiddenApps.any { entity ->
        entity.packageName == app.packageName &&
                entity.profileKey == profileKey &&
                when (entity.launcherShortcutId) {
                    LEGACY_PACKAGE_WIDE_METADATA -> true
                    HOST_APP_METADATA_SENTINEL -> shortcutId == null
                    else -> shortcutId == entity.launcherShortcutId
                }
    }
}

fun overlayCustomName(
        app: AppInfo,
        renamedApps: List<com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity>,
): String? {
    val profileKey = appProfileKey(app.userHandle)
    val packageRows =
            renamedApps.filter { it.packageName == app.packageName && it.profileKey == profileKey }
    if (packageRows.isEmpty()) return null
    app.launcherShortcutId?.let { shortcutId ->
        packageRows.find { it.launcherShortcutId == shortcutId }?.customName?.let { return it }
    }
            ?: packageRows.find { it.launcherShortcutId == HOST_APP_METADATA_SENTINEL }?.customName?.let {
                return it
            }
    return packageRows.find { it.launcherShortcutId == LEGACY_PACKAGE_WIDE_METADATA }?.customName
}

fun overlayCategory(
        app: AppInfo,
        categories: List<com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity>,
): String? {
    val profileKey = appProfileKey(app.userHandle)
    val packageRows =
            categories.filter { it.packageName == app.packageName && it.profileKey == profileKey }
    if (packageRows.isEmpty()) return null
    app.launcherShortcutId?.let { shortcutId ->
        packageRows.find { it.launcherShortcutId == shortcutId }?.category?.let { return it }
    }
            ?: packageRows.find { it.launcherShortcutId == HOST_APP_METADATA_SENTINEL }?.category?.let {
                return it
            }
    return packageRows.find { it.launcherShortcutId == LEGACY_PACKAGE_WIDE_METADATA }?.category
}

fun resolveAppCategory(
        app: AppInfo,
        categoryEntities: List<com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity>,
        suppressedCategories: Set<String> = emptySet(),
): String {
    overlayCategory(app, categoryEntities)?.let { return it }
    val inferred = app.category.trim()
    if (inferred.isBlank()) return ""
    if (suppressedCategories.any { it.equals(inferred, ignoreCase = true) }) return ""
    return inferred
}

fun dynamicCategoryExtras(
        appCategories: Collection<String>,
        definedCategories: Collection<String>,
        suppressedCategories: Collection<String> = emptySet(),
): List<String> {
    val defined = definedCategories.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    val suppressedLower =
            suppressedCategories.map { it.trim() }.filter { it.isNotBlank() }.map { it.lowercase() }.toSet()
    return appCategories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.lowercase() in suppressedLower }
            .toSet()
            .minus(defined)
            .sorted()
}

/**
 * Stable LazyColumn/LazyRow key: same package and profile can have multiple launch activities
 * (e.g. Google app); [drawerOpenCountKey] alone is not enough for those rows.
 */
fun appListStableKey(app: AppInfo): String {
    val base = drawerOpenCountKey(app.packageName, app.userHandle)
    app.launcherShortcutId?.let { return "$base#shortcut:$it" }
    val cn = app.componentName ?: return base
    return "$base#${cn.flattenToString()}"
}
