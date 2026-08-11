package com.lu4p.fokuslauncher.utils

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.View

/**
 * Work around an AOSP [android.view.ViewRootImpl] bug where
 * `mAttachInfo.mDisplayState` can stick at [Display.STATE_OFF] after the screen is woken by
 * another app (alarm, call, etc.) while this activity was not the top activity.
 *
 * When that happens, [android.view.ViewRootImpl] skips drawing (`performDraw` returns early) so
 * the launcher UI looks frozen while touch input still works. Soft invalidation / `recreate()`
 * does not clear the stuck attach-info state; restarting the activity does.
 *
 * Same class of bug addressed by Lawnchair PR #6050.
 */
object FrozenRendererRecovery {

    private const val TAG = "FrozenRendererRecovery"

    /** Delay after [Activity.onResume] before probing attach-info (lets display settle). */
    const val CHECK_DELAY_MS = 500L

    /** Avoid restart loops if reflection / lifecycle races. */
    const val MIN_RESTART_INTERVAL_MS = 5_000L

    @Volatile
    private var lastRestartElapsedMs: Long = 0L

    /**
     * Whether a stuck ViewRoot display state should trigger an activity restart.
     *
     * @param viewRootDisplayState value of `ViewRootImpl.mAttachInfo.mDisplayState`, or a negative
     *     sentinel when unavailable
     * @param actualDisplayState [Display.getState] for the activity's display
     */
    fun shouldRestartForStuckDisplayState(
            viewRootDisplayState: Int,
            actualDisplayState: Int,
            nowElapsedMs: Long = SystemClock.elapsedRealtime(),
            lastRestartElapsedMs: Long = this.lastRestartElapsedMs,
            minRestartIntervalMs: Long = MIN_RESTART_INTERVAL_MS,
    ): Boolean {
        if (viewRootDisplayState != Display.STATE_OFF) return false
        if (!isDisplayVisiblyOn(actualDisplayState)) return false
        return nowElapsedMs - lastRestartElapsedMs >= minRestartIntervalMs
    }

    fun isDisplayVisiblyOn(displayState: Int): Boolean =
            displayState == Display.STATE_ON ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                            displayState == Display.STATE_ON_SUSPEND)

    /**
     * Reads `ViewRootImpl.mAttachInfo.mDisplayState` via reflection.
     * @return display state constant, or `-1` if unavailable
     */
    fun readViewRootDisplayState(decorView: View): Int {
        return try {
            val viewRootImplMethod = View::class.java.getDeclaredMethod("getViewRootImpl")
            viewRootImplMethod.isAccessible = true
            val viewRootImpl = viewRootImplMethod.invoke(decorView) ?: return -1

            val attachInfoField = viewRootImpl.javaClass.getDeclaredField("mAttachInfo")
            attachInfoField.isAccessible = true
            val attachInfo = attachInfoField.get(viewRootImpl) ?: return -1

            val displayStateField = attachInfo.javaClass.getDeclaredField("mDisplayState")
            displayStateField.isAccessible = true
            displayStateField.getInt(attachInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read ViewRootImpl.mAttachInfo.mDisplayState", e)
            -1
        }
    }

    fun actualDisplayState(activity: Activity): Int {
        val display =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display
                } else {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay
                }
        return display?.state ?: Display.STATE_UNKNOWN
    }

    /**
     * If the ViewRoot attach-info still reports OFF while the display is on, restart the
     * activity so drawing resumes.
     *
     * @return true when a restart was started
     */
    fun maybeRestartIfFrozen(activity: Activity): Boolean {
        val viewRootState = readViewRootDisplayState(activity.window.decorView)
        val actualState = actualDisplayState(activity)
        val now = SystemClock.elapsedRealtime()
        if (!shouldRestartForStuckDisplayState(viewRootState, actualState, now)) {
            return false
        }
        Log.w(
                TAG,
                "Renderer appears frozen (viewRootDisplayState=$viewRootState, " +
                        "actualDisplayState=$actualState); restarting activity",
        )
        lastRestartElapsedMs = now
        restartMainActivity(activity)
        return true
    }

    private fun restartMainActivity(activity: Activity) {
        val intent =
                Intent(activity, activity.javaClass).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
        activity.startActivity(intent)
    }
}
