package com.lu4p.fokuslauncher.data.iconpack

import android.content.Intent
import android.content.pm.PackageManager

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
)

object IconPackPackages {
    /**
     * Standard icon-pack discovery actions, as declared by packs themselves.
     * Case matters: the ADW convention is "org.adw.launcher.THEMES" (uppercase).
     */
    const val ACTION_ADW_THEMES = "org.adw.launcher.THEMES"
    const val ACTION_APEX_THEME = "com.anddoes.launcher.THEME"
    const val ACTION_GO_THEME = "com.gau.go.launcherex.theme"

    val DISCOVERY_ACTIONS: List<String> =
            listOf(
                    ACTION_ADW_THEMES,
                    ACTION_APEX_THEME,
                    ACTION_GO_THEME,
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
     *
     * When [thirdPartyAllowed] is false, only Arcticons variants may be auto-selected;
     * an unconsented third-party pack is never auto-applied (issue #295).
     */
    fun findPreferredInstalledPackage(
            packageManager: PackageManager,
            thirdPartyAllowed: Boolean,
    ): String? {
        val packs = findInstalledIconPacks(packageManager)
        val arcticons = packs.firstOrNull { ArcticonsPackages.isWhitelisted(it.packageName) }
        if (arcticons != null) return arcticons.packageName
        if (!thirdPartyAllowed) return null
        return packs.minByOrNull { it.packageName }?.packageName
    }

    private fun infoFor(packageManager: PackageManager, packageName: String): IconPackInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            IconPackInfo(
                    packageName = packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
