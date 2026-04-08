package com.lu4p.fokuslauncher.ui.util

import android.content.Context
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.SystemCategoryKeys

/** Localized label for drawer category chips ([ReservedCategoryNames] keys stay English for storage). */
fun categoryChipDisplayLabel(context: Context, category: String): String {
    return when {
        category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) ->
            context.getString(R.string.drawer_filter_all_apps)
        category.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) ->
            context.getString(R.string.drawer_filter_private)
        category.equals(ReservedCategoryNames.WORK, ignoreCase = true) ->
            context.getString(R.string.drawer_filter_work)
        category.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true) ->
            context.getString(R.string.drawer_filter_uncategorized)
        else -> SystemCategoryKeys.displayLabel(context, category)
    }
}

fun formatShortcutTargetDisplay(
        context: Context,
        target: ShortcutTarget?,
        allApps: List<AppInfo>,
        notSetLabel: String,
        resolvedLauncherActionLabel: String? = null,
        profileKey: String = "0",
): String {
    if (target == null) return notSetLabel
    return when (target) {
        is ShortcutTarget.App ->
                allApps.find {
                    it.packageName == target.packageName &&
                            appProfileKey(it.userHandle) == profileKey
                }?.label ?: target.packageName
        is ShortcutTarget.DeepLink -> {
            val uri = target.intentUri.trim()
            uri.ifEmpty { context.getString(R.string.shortcut_target_deep_link) }
        }
        is ShortcutTarget.PhoneDial -> context.getString(R.string.shortcut_target_phone)
        is ShortcutTarget.LauncherShortcut -> {
            val appName =
                    allApps.find {
                        it.packageName == target.packageName &&
                                appProfileKey(it.userHandle) == profileKey
                    }?.label ?: target.packageName
            val rawAction = resolvedLauncherActionLabel ?: context.getString(R.string.shortcut_generic_label)
            val actionDisplay =
                    if (rawAction == AppShortcutAction.OPEN_APP_LABEL) {
                        context.getString(R.string.shortcut_open_app)
                    } else {
                        rawAction
                    }
            context.getString(R.string.shortcut_target_app_with_label, appName, actionDisplay)
        }
    }
}
