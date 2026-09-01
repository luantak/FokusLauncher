package com.lu4p.fokuslauncher.ui.home

import com.lu4p.fokuslauncher.data.model.HomeAlignment

/** Horizontal placement shared by home widgets and the favorites section. */
enum class HomeWidgetAlignment {
    START,
    CENTER,
    END;

    companion object {
        fun from(homeAlignment: HomeAlignment): HomeWidgetAlignment =
            when (homeAlignment) {
                HomeAlignment.LEFT -> START
                HomeAlignment.CENTER, HomeAlignment.MIDDLE -> CENTER
                HomeAlignment.RIGHT -> END
            }
    }
}

/** Base size passed to LauncherIcon, which applies launcher font scaling exactly once. */
internal const val WidgetControlIconBaseSizeDp = 24f
