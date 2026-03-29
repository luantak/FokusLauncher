package com.lu4p.fokuslauncher.ui.drawer

import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction

/** One app-drawer block for a single Android user profile (owner, work, clone, …). */
data class DrawerProfileSectionUi(
        val id: String,
        val title: String,
        val apps: List<AppInfo>
)

/** Same profile bucketing as [DrawerProfileSectionUi], for edit-shortcuts action rows. */
data class DrawerProfileShortcutSectionUi(
        val id: String,
        val title: String,
        val actions: List<AppShortcutAction>
)
