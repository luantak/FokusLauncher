package com.lu4p.fokuslauncher.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.content.pm.PackageManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.appProfileKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun loadHomeShortcutIcon(
    context: Context,
    shortcut: HomeShortcut,
    installedApps: List<AppInfo>,
    loadIcon: suspend (AppInfo) -> Drawable?,
): Drawable? = withContext(Dispatchers.IO) {
    val target = shortcut.target
    val component = when (target) {
        ShortcutTarget.PhoneDial -> resolveShortcutIconComponent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
        is ShortcutTarget.DeepLink -> {
            val intent = try {
                Intent.parseUri(target.intentUri, Intent.URI_INTENT_SCHEME)
            } catch (_: Exception) {
                return@withContext null
            }
            intent.component ?: resolveShortcutIconComponent(context, intent)
        }
        else -> null
    }
    val packageName = when (target) {
        is ShortcutTarget.App -> target.packageName
        is ShortcutTarget.LauncherShortcut -> target.packageName
        ShortcutTarget.PhoneDial, is ShortcutTarget.DeepLink -> component?.packageName
        ShortcutTarget.WidgetPage -> null
    } ?: return@withContext null
    val shortcutId = (target as? ShortcutTarget.LauncherShortcut)?.shortcutId
    val app = installedApps.find {
        it.packageName == packageName && appProfileKey(it.userHandle) == shortcut.profileKey &&
            it.launcherShortcutId == shortcutId
    } ?: return@withContext null
    loadIcon(if (component == null) app else app.copy(componentName = component))
}

private fun resolveShortcutIconComponent(context: Context, intent: Intent): ComponentName? =
    try {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.let {
            ComponentName(it.packageName, it.name)
        }
    } catch (_: RuntimeException) {
        null
    }
