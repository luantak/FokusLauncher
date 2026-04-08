package com.lu4p.fokuslauncher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Runs [block] whenever the host lifecycle reaches [Lifecycle.Event.ON_RESUME].
 *
 * If [alsoRunIfAlreadyResumed] is true, [block] is invoked once immediately when the effect is first
 * composed while already in [Lifecycle.State.RESUMED] or above (mirrors a common
 * `DisposableEffect` + observer pattern used when `ON_RESUME` is not re-fired for new observers).
 */
@Composable
fun OnResumeEffect(vararg keys: Any?, alsoRunIfAlreadyResumed: Boolean = false, block: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, *keys) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                block()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (alsoRunIfAlreadyResumed &&
                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            block()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
