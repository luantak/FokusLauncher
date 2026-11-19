package com.lu4p.fokuslauncher.ui.drawer

import android.graphics.drawable.Drawable
import androidx.compose.runtime.staticCompositionLocalOf
import com.lu4p.fokuslauncher.data.model.AppInfo

/**
 * Suspend loader for Arcticons drawer icons. Default returns null (placeholder). Provided by
 * [AppDrawerScreen] when the Arcticons preference is enabled.
 */
val LocalArcticonsIconLoader =
        staticCompositionLocalOf<suspend (AppInfo) -> Drawable?> { { null } }

/**
 * True when the active pack wants launcher tinting (Arcticons line icons).
 * Colored packs render untinted.
 */
val LocalIconPackMonochrome = staticCompositionLocalOf { true }
