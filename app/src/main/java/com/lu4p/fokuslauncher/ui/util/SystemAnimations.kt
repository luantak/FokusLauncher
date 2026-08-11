package com.lu4p.fokuslauncher.ui.util

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Whether Android system animator animations are enabled.
 *
 * Developer Options "Animator duration scale" and Accessibility "Remove animations" set
 * [Settings.Global.ANIMATOR_DURATION_SCALE] to `0`, which Compose motion already respects for
 * timed animations. Gesture-driven motion still needs an explicit check.
 */
fun Context.areSystemAnimationsEnabled(): Boolean =
        Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
        ) != 0f

/** Re-reads [areSystemAnimationsEnabled] on resume so developer/accessibility changes apply. */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(context.areSystemAnimationsEnabled()) }
    OnResumeEffect(alsoRunIfAlreadyResumed = true) {
        enabled = context.areSystemAnimationsEnabled()
    }
    return enabled
}
