package com.lu4p.fokuslauncher.ui.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
                    HorizontalDivider(
                            modifier =
                                    Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            item(key = "${keyPrefix}_hdr_${section.id}") {
                Text(
                        text = section.title.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                                Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
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
                    HorizontalDivider(
                            modifier =
                                    Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            item(key = "${keyPrefix}_hdr_${section.id}") {
                Text(
                        text = section.title.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                                Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp),
                )
            }
        }
        hasEmitted = true

        val byApp =
                section.actions.groupBy { appShortcutGroupKey(it) }.entries.sortedBy { (_, acts) ->
                    acts.first().appLabel.lowercase(Locale.getDefault())
                }
        for ((groupKey, actionsForApp) in byApp) {
            val headline = actionsForApp.first().appLabel
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
                            compareBy<AppShortcutAction> { it.actionLabel != openAppLabelSort }
                                    .thenBy {
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
