package com.lu4p.fokuslauncher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.lu4p.fokuslauncher.MainActivity

/** [Intent] that starts this app's main activity as the home (launcher) target. */
fun Context.launcherMainActivityIntent(): Intent =
    Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        component = ComponentName(this@launcherMainActivityIntent, MainActivity::class.java)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
    }

/**
 * Tries to bring the launcher main activity to the foreground.
 * @return Pair of (success, error simple class name if failure).
 */
fun Context.tryStartLauncherMainActivity(): Pair<Boolean, String?> =
    runCatching { startActivity(launcherMainActivityIntent()) }.fold(
        onSuccess = { true to null },
        onFailure = { false to it.javaClass.simpleName }
    )
