package com.lu4p.fokuslauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository responsible for loading and caching installed apps from the system, and managing
 * hidden/renamed/categorized apps via Room.
 */
@Singleton
class AppRepository
@Inject
constructor(@ApplicationContext private val context: Context, private val appDao: AppDao) {
    private var cachedApps: List<AppInfo>? = null

    // --- App Loading ---

    /**
     * Returns all launchable apps installed on the device, sorted alphabetically. Results are
     * cached in memory after the first load.
     */
    fun getInstalledApps(): List<AppInfo> {
        cachedApps?.let {
            return it
        }

        val pm = context.packageManager
        val mainIntent =
                Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

        val apps =
                resolveInfos
                        .filter { it.activityInfo.packageName != context.packageName }
                        .map { resolveInfo ->
                            val packageName = resolveInfo.activityInfo.packageName
                            val label = resolveInfo.loadLabel(pm).toString()
                            AppInfo(
                                    packageName = packageName,
                                    label = label,
                                    icon =
                                            try {
                                                resolveInfo.loadIcon(pm)
                                            } catch (_: Exception) {
                                                null
                                            },
                                    category = inferCategory(packageName, label)
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                        .distinctBy { it.packageName }

        cachedApps = apps
        return apps
    }

    /** Clears the cached app list, forcing a reload on next access. */
    fun invalidateCache() {
        cachedApps = null
    }

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1 || launcherApps == null) return@forEach

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
                            ?: "Shortcut"
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
        if (category.isBlank() || category == "All apps") return getInstalledApps()
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
        if (normalized.equals("All apps", ignoreCase = true)) return
        if (normalized.equals("Private", ignoreCase = true)) return
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
        if (oldNormalized.equals("All apps", ignoreCase = true)) return
        if (oldNormalized.equals("Private", ignoreCase = true)) return
        if (newNormalized.equals("All apps", ignoreCase = true)) return
        if (newNormalized.equals("Private", ignoreCase = true)) return

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
        if (normalized.equals("All apps", ignoreCase = true)) return
        if (normalized.equals("Private", ignoreCase = true)) return

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
                categories.map { it.trim() }
                        .filter { it.isNotBlank() }
                        .filterNot { it.equals("All apps", ignoreCase = true) }
                        .filterNot { it.equals("Private", ignoreCase = true) }
                        .distinct()
        val entities = normalized.mapIndexed { index, name ->
            AppCategoryDefinitionEntity(name = name, position = index)
        }
        appDao.replaceCategoryDefinitions(entities)
    }

    /** Removes the category assignment for an app. */
    suspend fun removeAppCategory(packageName: String) {
        appDao.removeAppCategory(packageName)
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

    private fun inferCategory(packageName: String, label: String): String {
        val value = "$packageName ${label.lowercase()}"
        return when {
            value.contains("mail") || value.contains("calendar") || value.contains("docs") ||
                value.contains("office") || value.contains("task") || value.contains("slack") ->
                "Productivity"
            value.contains("bank") || value.contains("wallet") || value.contains("pay") ||
                value.contains("finance") || value.contains("invest") || value.contains("crypto") ->
                "Finance"
            value.contains("chat") || value.contains("messag") || value.contains("whatsapp") ||
                value.contains("telegram") || value.contains("social") || value.contains("instagram") ->
                "Social"
            value.contains("fit") || value.contains("health") || value.contains("med") ||
                value.contains("workout") || value.contains("run") ->
                "Health"
            value.contains("music") || value.contains("video") || value.contains("photo") ||
                value.contains("camera") || value.contains("gallery") || value.contains("tv") ->
                "Media"
            value.contains("game") || value.contains("play") || value.contains("puzzle") ||
                value.contains("chess") || value.contains("sudoku") || value.contains("casino") ||
                value.contains("slot") || value.contains("arcade") ->
                "Games"
            else -> "Utilities"
        }
    }
}
