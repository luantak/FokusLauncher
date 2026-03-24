package com.lu4p.fokuslauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.ReservedCategoryNames
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.ProfileHeuristics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Repository responsible for loading and caching installed apps from the system, and managing
 * hidden/renamed/categorized apps via Room.
 */
@Singleton
class AppRepository
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val appDao: AppDao,
        private val privateSpaceManager: PrivateSpaceManager
) {
    private var cachedApps: List<AppInfo>? = null
    private val installedAppsVersion = MutableStateFlow(0L)
    private val packageChangeReceiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val replacing = intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true
                    when (intent?.action) {
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_CHANGED,
                        Intent.ACTION_PACKAGE_REMOVED,
                        Intent.ACTION_PACKAGE_REPLACED -> {
                            if (intent.action == Intent.ACTION_PACKAGE_REMOVED && replacing) return
                            invalidateCache()
                        }
                    }
                }
            }

    private val profileChangeReceiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_MANAGED_PROFILE_ADDED,
                        Intent.ACTION_MANAGED_PROFILE_REMOVED -> invalidateCache()
                    }
                }
            }

    init {
        registerPackageChangeReceiver()
        registerProfileChangeReceiver()
    }

    // --- App Loading ---

    /**
     * Returns all launchable apps installed on the device, sorted alphabetically. Results are
     * cached in memory after the first load.
     */
    fun getInstalledApps(): List<AppInfo> {
        cachedApps?.let {
            return it
        }

        val apps = loadInstalledAppsMergedAcrossProfiles()
        cachedApps = apps
        return apps
    }

    /**
     * Loads launchable activities per [UserManager.userProfiles] via [LauncherApps.getActivityList],
     * so cloned / parallel / work-profile installs (same package as the primary user) still
     * appear. Private Space is skipped here; those apps stay in the dedicated Private drawer
     * section.
     */
    private fun loadInstalledAppsMergedAcrossProfiles(): List<AppInfo> {
        val launcherApps =
                try {
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                } catch (_: Exception) {
                    null
                }
        val userManager =
                try {
                    context.getSystemService(Context.USER_SERVICE) as? UserManager
                } catch (_: Exception) {
                    null
                }

        if (launcherApps == null || userManager == null) {
            return loadInstalledAppsLegacyQuery()
        }

        val myUser = Process.myUserHandle()
        val rawEntries = mutableListOf<RawLauncherEntry>()

        for (user in userManager.userProfiles) {
            if (privateSpaceManager.isPrivateSpaceProfile(user)) continue

            val activities =
                    try {
                        launcherApps.getActivityList(null, user)
                    } catch (_: Exception) {
                        emptyList()
                    }

            for (info in activities) {
                val packageName = info.applicationInfo.packageName
                if (packageName == context.packageName) continue

                val rawLabel =
                        try {
                            info.label.toString()
                        } catch (_: Exception) {
                            packageName
                        }
                val isPrimary = user == myUser
                val icon =
                        try {
                            info.getBadgedIcon(0)
                        } catch (_: Exception) {
                            null
                        }
                rawEntries.add(
                        RawLauncherEntry(
                                packageName = packageName,
                                rawLabel = rawLabel,
                                user = user,
                                isPrimary = isPrimary,
                                icon = icon,
                                category = inferCategoryFromApplicationInfo(info.applicationInfo),
                                componentName = info.componentName
                        )
                )
            }
        }

        val ownerLabels: Map<String, String> =
                rawEntries
                        .filter { it.isPrimary }
                        .distinctBy { it.packageName }
                        .associate { it.packageName to it.rawLabel.trim().ifEmpty { it.packageName } }

        val collected =
                rawEntries.map { e ->
                    val finalLabel =
                            if (e.isPrimary) {
                                e.rawLabel.trim().ifEmpty { e.packageName }
                            } else {
                                ownerLabels[e.packageName]?.takeIf { it.isNotBlank() }
                                        ?: ProfileHeuristics.stripLeadingWorkPrefix(e.rawLabel)
                                        ?: e.rawLabel.trim().ifEmpty { e.packageName }
                            }
                    AppInfo(
                            packageName = e.packageName,
                            label = finalLabel,
                            icon = e.icon,
                            category = e.category,
                            userHandle = if (e.isPrimary) null else e.user,
                            componentName = if (e.isPrimary) null else e.componentName
                    )
                }

        val primary = collected.filter { it.userHandle == null }.distinctBy { it.packageName }
        val secondary =
                collected.filter { it.userHandle != null }.distinctBy {
                    "${it.packageName}|${it.componentName?.flattenToString()}"
                }

        return (primary + secondary).sortedBy { it.label.lowercase() }
    }

    private data class RawLauncherEntry(
            val packageName: String,
            val rawLabel: String,
            val user: UserHandle,
            val isPrimary: Boolean,
            val icon: Drawable?,
            val category: String,
            val componentName: ComponentName
    )

    /** Fallback when [LauncherApps] / [UserManager] are unavailable (e.g. partial test doubles). */
    private fun loadInstalledAppsLegacyQuery(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent =
                try {
                    Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                } catch (_: Exception) {
                    Intent()
                }

        val resolveInfos: List<ResolveInfo> =
                try {
                    pm.queryIntentActivities(mainIntent, 0)
                } catch (_: Exception) {
                    emptyList()
                }

        return resolveInfos
                .asSequence()
                .filter { it.activityInfo.packageName != context.packageName }
                .map { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    val label =
                            resolveInfo.nonLocalizedLabel
                                    ?.toString()
                                    ?.takeIf { it.isNotBlank() }
                                    ?: try {
                                        resolveInfo.loadLabel(pm).toString()
                                    } catch (_: Exception) {
                                        packageName
                                    }
                    AppInfo(
                            packageName = packageName,
                            label = label,
                            icon =
                                    try {
                                        resolveInfo.loadIcon(pm)
                                    } catch (_: Exception) {
                                        null
                                    },
                            category =
                                    inferCategoryFromApplicationInfo(
                                            resolveInfo.activityInfo.applicationInfo
                                                    ?: try {
                                                        pm.getApplicationInfo(packageName, 0)
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                    )
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
    }

    /** Clears the cached app list, forcing a reload on next access. */
    fun invalidateCache() {
        cachedApps = null
        installedAppsVersion.value += 1
    }

    fun getInstalledAppsVersion(): StateFlow<Long> = installedAppsVersion

    /**
     * Launches an app by its package name.
     * @param options optional [android.app.ActivityOptions] bundle for custom transition
     * animations.
     * @return true if the app was launched successfully, false otherwise.
     */
    fun launchApp(packageName: String, options: Bundle? = null): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent, options)
            true
        } else {
            false
        }
    }

    /**
     * Launches an Android launcher shortcut action (long-press shortcut).
     */
    fun launchLauncherShortcut(packageName: String, shortcutId: String): Boolean {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns all selectable actions for right-side shortcuts:
     * - One "Open app" action per app
     * - Launcher long-press actions published by each app (if available)
     */
    fun getAllShortcutActions(): List<AppShortcutAction> {
        val apps = getInstalledApps()
        val actions = mutableListOf<AppShortcutAction>()

        val launcherApps =
            try {
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            } catch (_: Exception) {
                null
            }

        apps.forEach { app ->
            actions.add(
                AppShortcutAction(
                    appLabel = app.label,
                    actionLabel = AppShortcutAction.OPEN_APP_LABEL,
                    target = ShortcutTarget.App(app.packageName)
                )
            )

            if (launcherApps == null) return@forEach

            val shortcuts = try {
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty()
            } catch (_: Exception) {
                emptyList()
            }

            shortcuts
                .asSequence()
                .filter { it.isEnabled }
                .distinctBy { it.id }
                .forEach { info ->
                    val shortcutLabel =
                        info.shortLabel?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: info.longLabel?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: context.getString(R.string.shortcut_generic_label)
                    actions.add(
                        AppShortcutAction(
                            appLabel = app.label,
                            actionLabel = shortcutLabel,
                            target = ShortcutTarget.LauncherShortcut(
                                packageName = app.packageName,
                                shortcutId = info.id
                            )
                        )
                    )
                }
        }

        return actions.sortedWith(
            compareBy<AppShortcutAction> { it.appLabel.lowercase() }
                .thenBy { it.actionLabel.lowercase() }
        )
    }

    /** Searches the installed apps list by a query string, matching against the app label. */
    fun searchApps(query: String): List<AppInfo> {
        if (query.isBlank()) return getInstalledApps()
        return getInstalledApps().filter { it.label.contains(query, ignoreCase = true) }
    }

    /** Filters apps by category. Returns all apps if category is blank or "All apps". */
    fun filterByCategory(category: String): List<AppInfo> {
        if (category.isBlank() || category.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) {
            return getInstalledApps()
        }
        return getInstalledApps().filter { it.category.equals(category, ignoreCase = true) }
    }

    // --- Hidden Apps (Room) ---

    /** Returns a Flow of all hidden package names. */
    fun getHiddenPackageNames(): Flow<List<String>> = appDao.getHiddenPackageNames()

    /** Hides an app by package name. */
    suspend fun hideApp(packageName: String) {
        appDao.hideApp(HiddenAppEntity(packageName))
    }

    /** Unhides an app by package name. */
    suspend fun unhideApp(packageName: String) {
        appDao.unhideApp(HiddenAppEntity(packageName))
    }

    /** Checks if an app is hidden. */
    suspend fun isAppHidden(packageName: String): Boolean = appDao.isAppHidden(packageName)

    // --- Renamed Apps (Room) ---

    /** Returns a Flow of all renamed app entities. */
    fun getAllRenamedApps(): Flow<List<RenamedAppEntity>> = appDao.getAllRenamedApps()

    /** Renames an app with a custom display name. */
    suspend fun renameApp(packageName: String, customName: String) {
        appDao.renameApp(RenamedAppEntity(packageName, customName))
    }

    /** Removes a custom app name (reverts to system name). */
    suspend fun removeRename(packageName: String) {
        appDao.removeRename(packageName)
    }

    /** Returns the custom name for an app, or null if not renamed. */
    suspend fun getCustomName(packageName: String): String? = appDao.getCustomName(packageName)

    // --- App Categories (Room) ---

    /** Returns a Flow of all app category assignments. */
    fun getAllAppCategories(): Flow<List<AppCategoryEntity>> = appDao.getAllAppCategories()

    /** Assigns a category to an app. */
    suspend fun setAppCategory(packageName: String, category: String) {
        appDao.setAppCategory(AppCategoryEntity(packageName, category.trim()))
    }

    /** Returns a Flow of user-defined category names. */
    fun getAllCategoryDefinitions(): Flow<List<AppCategoryDefinitionEntity>> =
            appDao.getAllCategoryDefinitions()

    /** Adds a user-defined category. */
    suspend fun addCategoryDefinition(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        if (normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        val existing = appDao.getCategoryDefinitionPosition(normalized)
        if (existing != null) return
        val nextPosition = appDao.getMaxCategoryDefinitionPosition() + 1
        appDao.upsertCategoryDefinition(
                AppCategoryDefinitionEntity(name = normalized, position = nextPosition)
        )
    }

    /** Renames a category across assignments and user-defined categories. */
    suspend fun renameCategory(oldName: String, newName: String) {
        val oldNormalized = oldName.trim()
        val newNormalized = newName.trim()
        if (oldNormalized.isBlank() || newNormalized.isBlank()) return
        if (oldNormalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (oldNormalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return

        val installed = getInstalledApps()
        val explicitMap = appDao.getAllAppCategories().first().associate { it.packageName to it.category }
        installed.forEach { app ->
            val explicit = explicitMap[app.packageName]
            val effective = explicit ?: app.category
            if (effective.equals(oldNormalized, ignoreCase = true)) {
                appDao.setAppCategory(AppCategoryEntity(app.packageName, newNormalized))
            }
        }

        appDao.renameCategoryAssignments(oldNormalized, newNormalized)
        val previousPosition = appDao.getCategoryDefinitionPosition(oldNormalized)
        appDao.removeCategoryDefinition(oldNormalized)
        val newPosition = previousPosition ?: (appDao.getMaxCategoryDefinitionPosition() + 1)
        appDao.upsertCategoryDefinition(
                AppCategoryDefinitionEntity(name = newNormalized, position = newPosition)
        )
    }

    /** Deletes a category and removes its app memberships. */
    suspend fun deleteCategory(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) return
        if (normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return

        val installed = getInstalledApps()
        val explicitMap = appDao.getAllAppCategories().first().associate { it.packageName to it.category }
        installed.forEach { app ->
            val explicit = explicitMap[app.packageName]
            val effective = explicit ?: app.category
            if (effective.equals(normalized, ignoreCase = true)) {
                appDao.setAppCategory(AppCategoryEntity(app.packageName, ""))
            }
        }

        appDao.removeCategoryAssignments(normalized)
        appDao.removeCategoryDefinition(normalized)
    }

    suspend fun reorderCategoryDefinitions(categories: List<String>) {
        val normalized =
                categories.asSequence()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .filterNot { it.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) }
                        .filterNot { it.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) }
                        .distinct()
                        .toList()
        val entities = normalized.mapIndexed { index, name ->
            AppCategoryDefinitionEntity(name = name, position = index)
        }
        appDao.replaceCategoryDefinitions(entities)
    }

    /** Returns the assigned category for an app, or null. */
    suspend fun getAppCategory(packageName: String): String? = appDao.getAppCategory(packageName)

    /** Clears all app-specific data (hidden apps, renamed apps, categories). */
    suspend fun clearAllAppData() {
        appDao.clearAllHiddenApps()
        appDao.clearAllRenamedApps()
        appDao.clearAllAppCategories()
        appDao.clearAllCategoryDefinitions()
    }

    private fun registerPackageChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            ContextCompat.registerReceiver(
                    context,
                    packageChangeReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            // Unit tests may provide a mock Context that cannot register real receivers.
        }
    }

    private fun registerProfileChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        try {
            ContextCompat.registerReceiver(
                    context,
                    profileChangeReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            // Unit tests may provide a mock Context that cannot register real receivers.
        }
    }

    private fun inferCategoryFromApplicationInfo(applicationInfo: ApplicationInfo?): String {
        return inferCategoryFromSystem(applicationInfo)
                ?: context.getString(R.string.inferred_category_utilities)
    }

    private fun inferCategoryFromSystem(applicationInfo: ApplicationInfo?): String? {
        if (applicationInfo == null) return null
        return when (applicationInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> context.getString(R.string.inferred_category_games)
            ApplicationInfo.CATEGORY_PRODUCTIVITY ->
                    context.getString(R.string.inferred_category_productivity)
            ApplicationInfo.CATEGORY_SOCIAL -> context.getString(R.string.inferred_category_social)
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE,
            ApplicationInfo.CATEGORY_NEWS -> context.getString(R.string.inferred_category_media)
            else -> null
        }
    }
}
