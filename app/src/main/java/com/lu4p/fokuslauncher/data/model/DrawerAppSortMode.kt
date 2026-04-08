package com.lu4p.fokuslauncher.data.model

import android.os.UserHandle
import androidx.annotation.StringRes
import com.lu4p.fokuslauncher.R

enum class DrawerAppSortMode(@param:StringRes val labelRes: Int) {
    ALPHABETICAL(R.string.settings_drawer_app_sort_alphabetical),
    MOST_OPENED(R.string.settings_drawer_app_sort_most_opened),
    /** Manual order (drag in drawer). Only honored when the vertical category sidebar is enabled. */
    CUSTOM(R.string.settings_drawer_app_sort_custom);

    companion object {
        fun fromStorage(value: String?): DrawerAppSortMode =
                entries.find { it.name == value } ?: ALPHABETICAL
    }
}

/** Stable profile segment for [userHandle]: owner user is `"0"`. */
fun appProfileKey(userHandle: UserHandle?): String =
    userHandle?.hashCode()?.toString() ?: "0"

/** Stable key for open counts, list rows, and matching [FavoriteApp.profileKey]. */
fun drawerOpenCountKey(packageName: String, profileKey: String): String =
        "$profileKey|$packageName"

/** Stable key for open counts (owner profile and per-[UserHandle] for work / private space). */
fun drawerOpenCountKey(packageName: String, userHandle: UserHandle?): String =
        drawerOpenCountKey(packageName, appProfileKey(userHandle))

/** Stable identity for app-specific launcher metadata persisted across UI/database layers. */
fun appMetadataKey(packageName: String, profileKey: String): String =
        drawerOpenCountKey(packageName, profileKey)

/** Stable identity for app-specific launcher metadata using [AppInfo.userHandle]. */
fun appMetadataKey(packageName: String, userHandle: UserHandle?): String =
        appMetadataKey(packageName, appProfileKey(userHandle))
