package com.lu4p.fokuslauncher.data.model

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Represents an installed app on the device.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val category: String = "",
    /** Non-null for Private Space apps; used to launch via [LauncherApps]. */
    val userHandle: UserHandle? = null,
    /** Non-null for Private Space apps; the activity to start. */
    val componentName: ComponentName? = null
)
