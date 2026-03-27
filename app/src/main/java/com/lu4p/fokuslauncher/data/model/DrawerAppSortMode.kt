package com.lu4p.fokuslauncher.data.model

import android.os.UserHandle

enum class DrawerAppSortMode {
    ALPHABETICAL,
    MOST_OPENED;

    companion object {
        fun fromStorage(value: String?): DrawerAppSortMode =
                entries.find { it.name == value } ?: ALPHABETICAL
    }
}

/** Stable key for open counts (owner profile and per-[UserHandle] for work / private space). */
fun drawerOpenCountKey(packageName: String, userHandle: UserHandle?): String {
    val userPart = if (userHandle == null) "0" else userHandle.hashCode().toString()
    return "$userPart|$packageName"
}
