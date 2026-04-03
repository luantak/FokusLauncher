package com.lu4p.fokuslauncher.data.model

/**
 * Target for drawer dot-search (`. query` or `.<alias> query`). When [target] is null, the system
 * default web search handler is used (no package restriction).
 */
data class DotSearchTargetPreference(
        val profileKey: String = "0",
        val target: ShortcutTarget? = null,
)
