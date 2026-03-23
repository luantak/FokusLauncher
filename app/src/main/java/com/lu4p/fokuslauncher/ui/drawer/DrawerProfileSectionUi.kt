package com.lu4p.fokuslauncher.ui.drawer

import com.lu4p.fokuslauncher.data.model.AppInfo

/** One app-drawer block for a single Android user profile (owner, work, clone, …). */
data class DrawerProfileSectionUi(
        val id: String,
        val title: String,
        val apps: List<AppInfo>
)
