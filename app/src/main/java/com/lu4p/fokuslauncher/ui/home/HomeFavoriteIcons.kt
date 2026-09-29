package com.lu4p.fokuslauncher.ui.home

import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.favoriteLauncherShortcutId

enum class HomeAppIconMode {
    TEXT, WITH_LABEL, ICON_ONLY;

    companion object {
        fun fromStored(value: String?): HomeAppIconMode =
            entries.find { it.name == value } ?: TEXT
    }
}

internal fun findHomeFavoriteIconApp(favorite: FavoriteApp, installedApps: List<AppInfo>): AppInfo? {
    val shortcutId = favoriteLauncherShortcutId(favorite)
    return installedApps.find { app ->
        app.packageName == favorite.packageName &&
            appProfileKey(app.userHandle) == favorite.profileKey &&
            app.launcherShortcutId == shortcutId
    }
}
