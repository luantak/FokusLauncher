package com.lu4p.fokuslauncher.data.repository

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
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
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.model.SystemCategoryKeys
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.ProfileHeuristics
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
    private val removedPackages = MutableSharedFlow<RemovedApp>(extraBufferCapacity = 8)
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
                            if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                                extractRemovedApp(intent)?.let(removedPackages::tryEmit)
                            }
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
     * cached in memory after the first successful non-empty load.
     *
     * Empty lists are not cached: [LauncherApps.getActivityList] / package events can briefly
     * yield no activities; caching that would hide every app until process death (e.g. force stop).
     */
    fun getInstalledApps(): List<AppInfo> {
        cachedApps?.let {
            return it
        }

        val apps = loadInstalledAppsMergedAcrossProfiles()
        if (apps.isNotEmpty()) {
            cachedApps = apps
        }
        return apps
    }

    suspend fun getInstalledAppsOnBackground(): List<AppInfo> =
            withContext(Dispatchers.IO) { getInstalledApps() }

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
    fun getRemovedPackages(): SharedFlow<RemovedApp> = removedPackages.asSharedFlow()

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
     * Launches a web-style search in the given app or via the system resolver. [target] null uses
     * [Intent.ACTION_WEB_SEARCH] without [Intent.setPackage]. [ShortcutTarget.DeepLink] URIs may
     * include `%q` for the URL-encoded search text (legacy saved templates may still use `%s` or
     * `{query}`; those are expanded the same way).
     */
    fun launchDotSearch(
            profileKey: String,
            target: ShortcutTarget?,
            query: String,
    ): Boolean {
        val launchContext = dotSearchLaunchContext(profileKey, target)
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        when (target) {
            null -> {
                val intent =
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, query)
                            addFlags(flags)
                        }
                return startDotSearchActivity(launchContext, intent)
            }
            is ShortcutTarget.App -> {
                val pkg = target.packageName
                val webSearch =
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            setPackage(pkg)
                            putExtra(SearchManager.QUERY, query)
                            addFlags(flags)
                        }
                if (startDotSearchActivity(launchContext, webSearch)) return true
                val inAppSearch =
                        Intent(Intent.ACTION_SEARCH).apply {
                            setPackage(pkg)
                            putExtra(SearchManager.QUERY, query)
                            putExtra("query", query)
                            addFlags(flags)
                        }
                return startDotSearchActivity(launchContext, inAppSearch)
            }
            is ShortcutTarget.DeepLink -> {
                val expanded = expandDotSearchDeepLink(target.intentUri, query)
                val intent =
                        try {
                            Intent.parseUri(expanded, Intent.URI_INTENT_SCHEME).apply {
                                addFlags(flags)
                            }
                        } catch (_: Exception) {
                            Intent(Intent.ACTION_VIEW, Uri.parse(expanded)).apply { addFlags(flags) }
                        }
                if (!intentUriContainsQueryPlaceholder(target.intentUri)) {
                    intent.putExtra(SearchManager.QUERY, query)
                    intent.putExtra("query", query)
                }
                return startDotSearchActivity(launchContext, intent)
            }
            is ShortcutTarget.LauncherShortcut -> return false
        }
    }

    private fun intentUriContainsQueryPlaceholder(uri: String): Boolean =
            uri.contains("%q") || uri.contains("%s") || uri.contains("{query}")

    private fun expandDotSearchDeepLink(intentUri: String, query: String): String {
        val encoded = Uri.encode(query)
        return intentUri
                .replace("{query}", encoded)
                .replace("%s", encoded)
                .replace("%q", encoded)
    }

    private fun resolveDotSearchUserHandle(
            profileKey: String,
            target: ShortcutTarget?,
    ): UserHandle {
        if (profileKey == "0") return Process.myUserHandle()
        val pkg =
                when (target) {
                    is ShortcutTarget.App -> target.packageName
                    is ShortcutTarget.LauncherShortcut -> target.packageName
                    is ShortcutTarget.DeepLink -> null
                    null -> null
                } ?: return Process.myUserHandle()
        val app =
                getInstalledApps().firstOrNull {
                    it.packageName == pkg && appProfileKey(it.userHandle) == profileKey
                }
        return app?.userHandle ?: Process.myUserHandle()
    }

    private fun dotSearchLaunchContext(profileKey: String, target: ShortcutTarget?): Context {
        val user = resolveDotSearchUserHandle(profileKey, target)
        if (user == Process.myUserHandle()) return context
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context
        return runCatching {
            val m =
                    Context::class.java.getMethod(
                            "createContextAsUser",
                            UserHandle::class.java,
                            Int::class.javaPrimitiveType
                    )
            m.invoke(context, user, 0) as Context
        }.getOrDefault(context)
    }

    private fun startDotSearchActivity(ctx: Context, intent: Intent): Boolean {
        return try {
            ctx.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * True if [app] exposes an activity that handles [Intent.ACTION_WEB_SEARCH] or
     * [Intent.ACTION_SEARCH] for its package (same convention as [launchDotSearch] for app targets).
     */
    fun appSupportsWebSearch(app: AppInfo): Boolean {
        val pm = contextForAppUser(app).packageManager
        val pkg = app.packageName
        return packageResolvesSearchAction(pm, pkg, Intent.ACTION_WEB_SEARCH) ||
                packageResolvesSearchAction(pm, pkg, Intent.ACTION_SEARCH)
    }

    private fun packageResolvesSearchAction(
            pm: PackageManager,
            packageName: String,
            action: String
    ): Boolean {
        val probe =
                Intent(action).apply {
                    setPackage(packageName)
                    putExtra(SearchManager.QUERY, ".")
                }
        return pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }

    fun filterAppsForDotSearchAppPicker(apps: List<AppInfo>): List<AppInfo> =
            apps.filter { appSupportsWebSearch(it) }

    private fun contextForAppUser(app: AppInfo): Context {
        val uh = app.userHandle
                ?: return context
        if (uh == Process.myUserHandle()) return context
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return context
        return runCatching {
            val m =
                    Context::class.java.getMethod(
                            "createContextAsUser",
                            UserHandle::class.java,
                            Int::class.javaPrimitiveType
                    )
            m.invoke(context, uh, 0) as Context
        }.getOrDefault(context)
    }

    /**
     * Starts a launchable activity in another Android user (e.g. work profile) via [LauncherApps].
     */
    fun launchMainActivity(componentName: ComponentName, userHandle: UserHandle, options: Bundle? = null): Boolean {
        return try {
            val launcherApps = ContextCompat.getSystemService(context, LauncherApps::class.java) ?: return false
            launcherApps.startMainActivity(componentName, userHandle, null, options)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches an Android launcher shortcut action (long-press shortcut).
     */
    fun launchLauncherShortcut(
            packageName: String,
            shortcutId: String,
            userHandle: UserHandle? = null
    ): Boolean {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val user = userHandle ?: Process.myUserHandle()
            launcherApps.startShortcut(packageName, shortcutId, null, null, user)
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

        val myUser = Process.myUserHandle()
        apps.forEach { app ->
            val profileKey = appProfileKey(app.userHandle)
            actions.add(
                AppShortcutAction(
                    appLabel = app.label,
                    actionLabel = AppShortcutAction.OPEN_APP_LABEL,
                    target = ShortcutTarget.App(app.packageName),
                    profileKey = profileKey,
                )
            )

            if (launcherApps == null) return@forEach

            val shortcutUser = app.userHandle ?: myUser
            val shortcuts = try {
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                launcherApps.getShortcuts(query, shortcutUser).orEmpty()
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
                            ),
                            profileKey = profileKey,
                        )
                    )
                }
        }

        return actions.sortedWith(
            compareBy<AppShortcutAction> { it.profileKey }
                .thenBy { it.appLabel.lowercase() }
                .thenBy { it.actionLabel.lowercase() }
        )
    }

    /** Searches the installed apps list by a query string, matching against the app label. */
    fun searchApps(query: String): List<AppInfo> {
        if (query.isBlank()) return getInstalledApps()
        return getInstalledApps().filter { it.label.containsNormalizedSearch(query) }
    }

    /** Filters apps by category. Returns all apps if category is blank or "All apps". */
    fun filterByCategory(category: String): List<AppInfo> {
        val normalized = normalizeCategory(category)
        if (normalized.isBlank() || normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) {
            return getInstalledApps()
        }
        if (normalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) {
            return getInstalledApps().filter { it.category.isBlank() }
        }
        return getInstalledApps().filter { it.category.equals(normalized, ignoreCase = true) }
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
    fun getAllAppCategories(): Flow<List<AppCategoryEntity>> =
            appDao.getAllAppCategories().map { categories ->
                categories.map { entity ->
                    entity.copy(category = normalizeCategory(entity.category))
                }
            }

    /** Assigns a category to an app. */
    suspend fun setAppCategory(packageName: String, category: String) {
        appDao.setAppCategory(
                AppCategoryEntity(packageName, normalizeCategory(category))
        )
    }

    /** Returns a Flow of user-defined category names. */
    fun getAllCategoryDefinitions(): Flow<List<AppCategoryDefinitionEntity>> =
            appDao.getAllCategoryDefinitions().map { definitions ->
                definitions.map { entity ->
                    entity.copy(name = normalizeCategory(entity.name))
                }.distinctBy { it.name.lowercase() }
            }

    /** Adds a user-defined category. */
    suspend fun addCategoryDefinition(name: String) {
        val normalized = normalizeCategory(name)
        if (normalized.isBlank()) return
        if (normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.WORK, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) return
        val existing =
                appDao.getAllCategoryDefinitions().first().any { entity ->
                    normalizeCategory(entity.name).equals(normalized, ignoreCase = true)
                }
        if (existing) return
        val nextPosition = appDao.getMaxCategoryDefinitionPosition() + 1
        appDao.upsertCategoryDefinition(
                AppCategoryDefinitionEntity(name = normalized, position = nextPosition)
        )
    }

    /** Renames a category across assignments and user-defined categories. */
    suspend fun renameCategory(oldName: String, newName: String) {
        val oldNormalized = normalizeCategory(oldName)
        val newNormalized = normalizeCategory(newName)
        if (oldNormalized.isBlank() || newNormalized.isBlank()) return
        if (oldNormalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (oldNormalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        if (oldNormalized.equals(ReservedCategoryNames.WORK, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.WORK, ignoreCase = true)) return
        if (oldNormalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) return
        if (newNormalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) return

        val rawAssignments = appDao.getAllAppCategories().first()
        rawAssignments.forEach { assignment ->
            if (normalizeCategory(assignment.category).equals(oldNormalized, ignoreCase = true)) {
                appDao.setAppCategory(AppCategoryEntity(assignment.packageName, newNormalized))
            }
        }

        appDao.renameCategoryAssignments(oldNormalized, newNormalized)
        val previousPosition =
                appDao.getAllCategoryDefinitions().first().firstOrNull { entity ->
                    normalizeCategory(entity.name).equals(oldNormalized, ignoreCase = true)
                }?.position
        appDao.removeCategoryDefinition(oldNormalized)
        val newPosition = previousPosition ?: (appDao.getMaxCategoryDefinitionPosition() + 1)
        appDao.upsertCategoryDefinition(
                AppCategoryDefinitionEntity(name = newNormalized, position = newPosition)
        )
    }

    /** Deletes a category and removes its app memberships. */
    suspend fun deleteCategory(name: String) {
        val normalized = normalizeCategory(name)
        if (normalized.isBlank()) return
        if (normalized.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.WORK, ignoreCase = true)) return
        if (normalized.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true)) return

        val assignmentsByPackage =
                appDao.getAllAppCategories().first().associateBy { it.packageName }

        // Include apps whose category is only from system inference (no Room row); otherwise the
        // chip/list entry comes back immediately after removing the definition.
        val appsToUncategorize =
                getInstalledApps().mapNotNull { app ->
                    val stored = assignmentsByPackage[app.packageName]
                    val effective =
                            if (stored != null) {
                                normalizeCategory(stored.category)
                            } else {
                                normalizeCategory(app.category)
                            }
                    if (effective.equals(normalized, ignoreCase = true)) {
                        AppCategoryEntity(app.packageName, "")
                    } else {
                        null
                    }
                }

        appDao.deleteCategoryWithAppResets(appsToUncategorize, normalized)
    }

    suspend fun reorderCategoryDefinitions(categories: List<String>) {
        val normalized =
                categories.asSequence()
                        .map(::normalizeCategory)
                        .filter { it.isNotBlank() }
                        .filterNot { it.equals(ReservedCategoryNames.ALL_APPS, ignoreCase = true) }
                        .filterNot { it.equals(ReservedCategoryNames.PRIVATE, ignoreCase = true) }
                        .filterNot { it.equals(ReservedCategoryNames.WORK, ignoreCase = true) }
                        .filterNot { it.equals(ReservedCategoryNames.UNCATEGORIZED, ignoreCase = true) }
                        .distinct()
                        .toList()
        val entities = normalized.mapIndexed { index, name ->
            AppCategoryDefinitionEntity(name = name, position = index)
        }
        appDao.replaceCategoryDefinitions(entities)
    }

    /** Returns the assigned category for an app, or null. */
    suspend fun getAppCategory(packageName: String): String? =
            appDao.getAppCategory(packageName)?.let(::normalizeCategory)

    /** Clears all app-specific data (hidden apps, renamed apps, categories). */
    suspend fun clearAllAppData() {
        val defaults =
                SystemCategoryKeys.defaultOrderedCategoryNames().mapIndexed { index, name ->
                    AppCategoryDefinitionEntity(name = name, position = index)
                }
        appDao.resetAllAppData(defaults)
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
                ?: SystemCategoryKeys.UTILITIES
    }

    private fun extractRemovedApp(intent: Intent): RemovedApp? {
        val packageName = intent.data?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return null
        val removedUser = extractUserHandle(intent)
        val profileKey =
                if (removedUser == null || removedUser == Process.myUserHandle()) {
                    "0"
                } else {
                    appProfileKey(removedUser)
                }
        return RemovedApp(packageName = packageName, profileKey = profileKey)
    }

    private fun extractUserHandle(intent: Intent): UserHandle? {
        val extraUser =
                try {
                    intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java)
                } catch (_: NoSuchMethodError) {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_USER) as? UserHandle
                } catch (_: Exception) {
                    null
                }
        if (extraUser != null) return extraUser

        val uid = intent.getIntExtra(Intent.EXTRA_UID, -1)
        return if (uid >= 0) {
            try {
                UserHandle.getUserHandleForUid(uid)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    private fun inferCategoryFromSystem(applicationInfo: ApplicationInfo?): String? {
        if (applicationInfo == null) return null
        return when (applicationInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> SystemCategoryKeys.GAMES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> SystemCategoryKeys.PRODUCTIVITY
            ApplicationInfo.CATEGORY_SOCIAL -> SystemCategoryKeys.SOCIAL
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE,
            ApplicationInfo.CATEGORY_NEWS -> SystemCategoryKeys.MEDIA
            else -> null
        }
    }

    private fun normalizeCategory(category: String): String =
            SystemCategoryKeys.normalize(context, category)
}

data class RemovedApp(
        val packageName: String,
        val profileKey: String
)
