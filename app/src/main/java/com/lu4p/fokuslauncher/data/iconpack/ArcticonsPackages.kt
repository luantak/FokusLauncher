package com.lu4p.fokuslauncher.data.iconpack

import android.content.pm.PackageManager

/**
 * Official Arcticons icon-pack package names only. FokusLauncher intentionally does **not**
 * discover or load arbitrary third-party icon packs.
 *
 * Order is preference for auto-selection when more than one variant is installed (dark-first
 * launcher → prefer white-line Arcticons over Black).
 */
object ArcticonsPackages {
    const val NORMAL = "com.donnnno.arcticons"
    const val BLACK = "com.donnnno.arcticons.light"
    const val YOU = "com.donnnno.arcticons.you"
    const val YOU_PLAY = "com.donnnno.arcticons.you.play"
    const val DAY_NIGHT = "com.donnnno.arcticons.daynight"

    /** Whitelist used for install detection and icon loading. */
    val WHITELIST: List<String> =
            listOf(
                    NORMAL,
                    DAY_NIGHT,
                    YOU,
                    YOU_PLAY,
                    BLACK,
            )

    const val FDROID_INSTALL_URL = "https://f-droid.org/packages/com.donnnno.arcticons/"
    const val PLAY_STORE_DETAILS_URI = "market://details?id=com.donnnno.arcticons"
    const val PLAY_STORE_WEB_URL =
            "https://play.google.com/store/apps/details?id=com.donnnno.arcticons"

    fun isWhitelisted(packageName: String): Boolean = packageName in WHITELIST

    /**
     * Returns the preferred installed Arcticons package, or null if none of the whitelisted
     * packs are present.
     */
    fun findInstalledPackage(packageManager: PackageManager): String? {
        for (pkg in WHITELIST) {
            if (isPackageInstalled(packageManager, pkg)) return pkg
        }
        return null
    }

    fun isAnyInstalled(packageManager: PackageManager): Boolean =
            findInstalledPackage(packageManager) != null

    fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
