package com.lu4p.fokuslauncher.usage

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object DigitalWellbeingHelper {

    private const val GOOGLE_WELLBEING_PACKAGE = "com.google.android.apps.wellbeing"

    private val explicitDashboardActivities =
            listOf(
                    "$GOOGLE_WELLBEING_PACKAGE/.settings.TopLevelSettingsActivity",
                    "$GOOGLE_WELLBEING_PACKAGE/.ui.overview.TodayDashboardActivity",
                    "$GOOGLE_WELLBEING_PACKAGE/.settings.WellbeingSettingsActivity",
            )

    private val fallbackWellbeingPackages =
            listOf(
                    GOOGLE_WELLBEING_PACKAGE,
                    "com.samsung.android.forest",
                    "com.samsung.android.sm.usage",
            )

    /** Opens the system Digital Wellbeing / screen time dashboard when available. */
    fun openDashboard(context: Context): Boolean {
        for (activity in explicitDashboardActivities) {
            if (startComponent(context, activity)) return true
        }
        for (packageName in fallbackWellbeingPackages) {
            if (startLaunchIntent(context, packageName)) return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent =
                    Intent(Settings.ACTION_APP_USAGE_SETTINGS).apply {
                        setPackage(GOOGLE_WELLBEING_PACKAGE)
                    }
            if (startIntent(context, intent)) return true
        }
        return false
    }

    private fun startComponent(context: Context, flattenedComponent: String): Boolean {
        val separator = flattenedComponent.indexOf('/')
        if (separator <= 0) return false
        val packageName = flattenedComponent.substring(0, separator)
        val className = flattenedComponent.substring(separator + 1).removePrefix("/")
        val component =
                ComponentName(
                        packageName,
                        if (className.startsWith(".")) packageName + className else className,
                )
        return startIntent(context, Intent(Intent.ACTION_MAIN).setComponent(component))
    }

    private fun startLaunchIntent(context: Context, packageName: String): Boolean {
        val launchIntent =
                context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return startIntent(context, launchIntent)
    }

    private fun startIntent(context: Context, intent: Intent): Boolean {
        if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            return false
        }
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
