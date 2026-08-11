package com.lu4p.fokuslauncher.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import com.lu4p.fokuslauncher.data.model.AppInfo

/**
 * Lazy loader for launcher app icons. [AppInfo.icon] is intentionally left null in the app
 * cache for memory; call this when a setting needs to show icons.
 */
object AppIconLoader {
    fun load(context: Context, app: AppInfo): Drawable? {
        app.icon?.let {
            return it.constantState?.newDrawable()?.mutate() ?: it
        }

        val launcherApps =
                context.getSystemService(LauncherApps::class.java) ?: return loadPackageIcon(context, app)
        val user: UserHandle = app.userHandle ?: Process.myUserHandle()
        val density = context.resources.displayMetrics.densityDpi

        app.launcherShortcutId?.let { shortcutId ->
            return loadShortcutIcon(launcherApps, app.packageName, shortcutId, user, density)
                    ?: loadActivityIcon(launcherApps, app, user, density)
                    ?: loadPackageIcon(context, app)
        }

        return loadActivityIcon(launcherApps, app, user, density) ?: loadPackageIcon(context, app)
    }

    private fun loadShortcutIcon(
            launcherApps: LauncherApps,
            packageName: String,
            shortcutId: String,
            user: UserHandle,
            density: Int,
    ): Drawable? {
        return try {
            val query =
                    LauncherApps.ShortcutQuery()
                            .setPackage(packageName)
                            .setShortcutIds(listOf(shortcutId))
                            .setQueryFlags(
                                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                            )
            val info = launcherApps.getShortcuts(query, user)?.firstOrNull() ?: return null
            launcherApps.getShortcutIconDrawable(info, density)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadActivityIcon(
            launcherApps: LauncherApps,
            app: AppInfo,
            user: UserHandle,
            density: Int,
    ): Drawable? {
        return try {
            val activities = launcherApps.getActivityList(app.packageName, user)
            val match =
                    app.componentName?.let { cn ->
                        activities.firstOrNull { it.componentName == cn }
                    }
                            ?: activities.firstOrNull()
            match?.getBadgedIcon(density)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPackageIcon(context: Context, app: AppInfo): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }
}
