package com.lu4p.fokuslauncher.data.iconpack

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Third-party icon pack discovery.
 *
 * A package advertises itself as an icon pack by publishing an activity/service intent filter
 * for one of the community-standard actions. Delta, Arcticons, Whicons, and most other packs
 * ship one or both of these. FokusLauncher loads any discovered pack through its
 * `appfilter.xml`; nothing is hardcoded per pack.
 */
data class IconPackInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
) {
    /** Stable key for preference storage; empty string means automatic selection. */
    val storageValue: String
        get() = packageName
}

object IconPackPackages {
    /** Standard icon-pack discovery actions (ADW / Nova conventions). */
    const val ACTION_ADW_THEME = "org.adw.launcher.themes"
    const val ACTION_NOVA_APPFILTER = "com.novaline.appfilter"

    val DISCOVERY_ACTIONS: List<String> =
            listOf(
                    ACTION_ADW_THEME,
                    ACTION_NOVA_APPFILTER,
            )

    /**
     * Returns every installed icon pack: packages advertising a discovery action, plus any
     * whitelisted Arcticons package as a fallback. Arcticons variants are ordered first so
     * automatic selection keeps its historic preference.
     */
    fun findInstalledIconPacks(packageManager: PackageManager): List<IconPackInfo> {
        val seen = LinkedHashSet<String>()
        for (pkg in ArcticonsPackages.WHITELIST) {
            if (ArcticonsPackages.isPackageInstalled(packageManager, pkg)) seen.add(pkg)
        }
        for (action in DISCOVERY_ACTIONS) {
            // No CATEGORY_DEFAULT: icon packs commonly declare the action bare.
            val intent = Intent(action)
            val resolved = try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (_: Exception) {
                emptyList()
            }
            for (info in resolved) {
                val pkg = info.activityInfo?.applicationInfo?.packageName ?: continue
                seen.add(pkg)
            }
        }
        return seen.mapNotNull { pkg -> infoFor(packageManager, pkg) }
    }

    /**
     * Automatic pack selection: first Arcticons variant when installed, else the first
     * discovered pack alphabetically for determinism.
     */
    fun findInstalledPackage(packageManager: PackageManager): String? {
        val packs = findInstalledIconPacks(packageManager)
        val arcticons = packs.firstOrNull { ArcticonsPackages.isWhitelisted(it.packageName) }
        if (arcticons != null) return arcticons.packageName
        return packs.minByOrNull { it.packageName }?.packageName
    }

    private fun infoFor(packageManager: PackageManager, packageName: String): IconPackInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            IconPackInfo(
                    packageName = packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (_: Exception) {
                        null
                    },
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
