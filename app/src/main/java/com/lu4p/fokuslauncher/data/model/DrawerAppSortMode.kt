package com.lu4p.fokuslauncher.data.model

import android.os.UserHandle

enum class DrawerAppSortMode {
    ALPHABETICAL,
    MOST_OPENED,
    /** Manual order (drag in drawer). Only honored when the vertical category sidebar is enabled. */
    CUSTOM;

    companion object {
        fun fromStorage(value: String?): DrawerAppSortMode =
                entries.find { it.name == value } ?: ALPHABETICAL
    }
}

/** Stable profile segment for [userHandle]: owner user is `"0"`. */
fun appProfileKey(userHandle: UserHandle?): String =
        if (userHandle == null) "0" else userHandle.hashCode().toString()

/** Stable key for open counts, list rows, and matching [FavoriteApp.profileKey]. */
fun drawerOpenCountKey(packageName: String, profileKey: String): String =
        "$profileKey|$packageName"

/** Stable key for open counts (owner profile and per-[UserHandle] for work / private space). */
fun drawerOpenCountKey(packageName: String, userHandle: UserHandle?): String =
        drawerOpenCountKey(packageName, appProfileKey(userHandle))
