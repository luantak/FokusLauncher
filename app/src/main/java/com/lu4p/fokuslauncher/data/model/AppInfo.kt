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
    val componentName: ComponentName? = null
) {
    val normalizedLabel: String by lazy(LazyThreadSafetyMode.NONE) { label.normalizedForSearch() }
}

/**
 * Stable LazyColumn/LazyRow key: same package and profile can have multiple launch activities
 * (e.g. Google app); [drawerOpenCountKey] alone is not enough for those rows.
 */
fun appListStableKey(app: AppInfo): String {
    val base = drawerOpenCountKey(app.packageName, app.userHandle)
    val cn = app.componentName ?: return base
    return "$base#${cn.flattenToString()}"
}
