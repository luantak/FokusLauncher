package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.appListStableKey
import java.util.Locale

/** Stable key for bucketing shortcut actions under one app headline (per profile section). */
private fun appShortcutGroupKey(action: AppShortcutAction): String =
        when (val t = action.target) {
            is ShortcutTarget.App -> "pkg:${t.packageName}"
            is ShortcutTarget.LauncherShortcut -> "pkg:${t.packageName}"
            is ShortcutTarget.DeepLink -> "intent:${t.intentUri}"
            is ShortcutTarget.PhoneDial -> "internal:phone"
            is ShortcutTarget.WidgetPage -> "internal:widget_page"
        }

/** Package name shown when two apps share the same display label (e.g. two "Phone" apps). */
private fun shortcutActionPackageForHeadlineDisambiguation(action: AppShortcutAction): String? =
        when (val t = action.target) {
            is ShortcutTarget.App -> t.packageName
            is ShortcutTarget.LauncherShortcut -> t.packageName
            is ShortcutTarget.DeepLink,
            is ShortcutTarget.PhoneDial,
            is ShortcutTarget.WidgetPage -> null
        }

/**
 * Emits profile section headers (same rules as the drawer: no header for the owner/personal block)
 * and app rows, with stable keys across sections.
 */
fun LazyListScope.profileGroupedAppItems(
        sections: List<DrawerProfileSectionUi>,
        keyPrefix: String,
        horizontalPadding: Dp = 24.dp,
        itemContent: @Composable LazyItemScope.(AppInfo) -> Unit,
) {
    var hasEmitted = false
    for (section in sections) {
        if (section.apps.isEmpty()) continue
        val showSectionLabel = section.id != "owner"
        if (showSectionLabel) {
            if (hasEmitted) {
                item(key = "${keyPrefix}_div_${section.id}") {
                    DrawerListSectionDivider(horizontalPadding = horizontalPadding)
                }
            }
            item(key = "${keyPrefix}_hdr_${section.id}") {
                DrawerListSectionHeader(
                        text = section.title,
                        horizontalPadding = horizontalPadding,
                )
            }
        }
        hasEmitted = true
        items(
                items = section.apps,
                key = { "${keyPrefix}_${section.id}_${appListStableKey(it)}" },
        ) { app ->
            itemContent(app)
        }
    }
}

/**
 * [profileGroupedAppItems] for [AppShortcutAction] rows: profile sections (when needed), then
 * **per-app headlines** and action rows under each app — same structure as before multi-profile.
 */
fun LazyListScope.profileGroupedShortcutItems(
        sections: List<DrawerProfileShortcutSectionUi>,
        keyPrefix: String,
        horizontalPadding: Dp = 16.dp,
        itemContent: @Composable LazyItemScope.(AppShortcutAction) -> Unit,
) {
    var hasEmitted = false
    val openAppLabelSort = AppShortcutAction.OPEN_APP_LABEL
    for (section in sections) {
        if (section.actions.isEmpty()) continue
        val showSectionLabel = section.id != "owner"
        if (showSectionLabel) {
            if (hasEmitted) {
                item(key = "${keyPrefix}_div_${section.id}") {
                    DrawerListSectionDivider(horizontalPadding = horizontalPadding)
                }
            }
            item(key = "${keyPrefix}_hdr_${section.id}") {
                DrawerListSectionHeader(
                        text = section.title,
                        horizontalPadding = horizontalPadding,
                )
            }
        }
        hasEmitted = true

        val byApp =
                section.actions.groupBy { appShortcutGroupKey(it) }.entries.sortedBy { (_, acts) ->
                    acts.first().appLabel.lowercase(Locale.getDefault())
                }
        val headlineNormCounts =
                byApp
                        .map { (_, acts) -> acts.first().appLabel.trim().lowercase(Locale.getDefault()) }
                        .groupingBy { it }
                        .eachCount()
        for ((groupKey, actionsForApp) in byApp) {
            val firstAction = actionsForApp.first()
            val baseHeadline = firstAction.appLabel
            val norm = baseHeadline.trim().lowercase(Locale.getDefault())
            val headline =
                    if ((headlineNormCounts[norm] ?: 0) > 1) {
                        val pkg = shortcutActionPackageForHeadlineDisambiguation(firstAction)
                        if (pkg != null) "$baseHeadline ($pkg)" else baseHeadline
                    } else {
                        baseHeadline
                    }
            item(key = "${keyPrefix}_app_${section.id}_$groupKey") {
                Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier =
                                Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
                )
            }
            val sortedForApp =
                    actionsForApp.sortedWith(
                            compareBy<AppShortcutAction> {
                                if (it.actionLabel == openAppLabelSort ||
                                                it.target is ShortcutTarget.PhoneDial
                                ) {
                                    0
                                } else {
                                    1
                                }
                            }.thenBy {
                                it.actionLabel.lowercase(Locale.getDefault())
                            }
                    )
            items(
                    items = sortedForApp,
                    key = { "${keyPrefix}_${section.id}_${it.id}" },
            ) { action ->
                itemContent(action)
            }
        }
    }
}
