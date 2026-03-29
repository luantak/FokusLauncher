package com.lu4p.fokuslauncher.ui.drawer

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.utils.ProfileHeuristics

/** Case-insensitive label order (matches drawer alphabetical mode). */
val alphabeticalAppComparatorForProfiles =
        compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }

fun sortAppsAlphabeticallyByProfileSection(apps: List<AppInfo>): List<AppInfo> =
        apps.sortedWith(alphabeticalAppComparatorForProfiles)

/**
 * Groups [apps] into the same profile sections as the app drawer (personal, work, clone, …),
 * applying [sortWithinSection] inside each section (drawer uses this for alphabetical vs
 * most-opened).
 */
fun groupAppsIntoProfileSections(
        context: Context,
        apps: List<AppInfo>,
        sortWithinSection: (List<AppInfo>) -> List<AppInfo>,
): List<DrawerProfileSectionUi> {
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager == null) {
        return buildProfileSectionsWithoutUserManager(context, apps, sortWithinSection)
    }

    val ownerApps = sortWithinSection(apps.filter { it.userHandle == null })
    val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    val orderedUsers =
            byUser.keys.sortedBy { uh ->
                try {
                    userManager.getSerialNumberForUser(uh)
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            }

    return buildList {
        if (ownerApps.isNotEmpty()) {
            add(
                    DrawerProfileSectionUi(
                            id = "owner",
                            title = context.getString(R.string.drawer_section_personal),
                            apps = ownerApps,
                    )
            )
        }
        for (user in orderedUsers) {
            val list = sortWithinSection(byUser.getValue(user))
            add(
                    DrawerProfileSectionUi(
                            id = "u_${user.hashCode()}",
                            title =
                                    profileSectionTitleForUser(
                                            context = context,
                                            user = user,
                                            userManager = userManager,
                                            totalSecondaryProfiles = orderedUsers.size,
                                    ),
                            apps = list,
                    )
            )
        }
    }
}

private fun buildProfileSectionsWithoutUserManager(
        context: Context,
        apps: List<AppInfo>,
        sortWithinSection: (List<AppInfo>) -> List<AppInfo>,
): List<DrawerProfileSectionUi> {
    val ownerApps = sortWithinSection(apps.filter { it.userHandle == null })
    val byUser = apps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    return buildList {
        if (ownerApps.isNotEmpty()) {
            add(
                    DrawerProfileSectionUi(
                            id = "owner",
                            title = context.getString(R.string.drawer_section_personal),
                            apps = ownerApps,
                    )
            )
        }
        for (user in byUser.keys) {
            val list = sortWithinSection(byUser.getValue(user))
            val title =
                    when {
                        byUser.keys.size == 1 &&
                                !ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_work_profile)
                        ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_clone_profile)
                        else -> context.getString(R.string.drawer_section_other_profile)
                    }
            add(
                    DrawerProfileSectionUi(
                            id = "u_${user.hashCode()}",
                            title = title,
                            apps = list,
                    )
            )
        }
    }
}

/** Profile subtitle for an [AppInfo] row (null = personal / owner). Drawer-style naming. */
fun profileOriginLabelForApp(context: Context, app: AppInfo): String? {
    val user = app.userHandle ?: return null
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager != null) {
        val myUser = Process.myUserHandle()
        val totalSecondary = userManager.userProfiles.count { it != myUser }
        return profileSectionTitleForUser(
                context,
                user,
                userManager,
                totalSecondary.coerceAtLeast(1),
        )
    }
    return when {
        ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                context.getString(R.string.drawer_section_clone_profile)
        else -> context.getString(R.string.drawer_section_work_profile)
    }
}

/**
 * Short label for the profile a home-screen favorite came from (null = personal / owner).
 * Uses the same naming rules as the app drawer when [matchingApp] supplies a [UserHandle].
 */
/** Profile subtitle for a saved right-side shortcut (null = personal / owner). */
fun profileOriginLabelForHomeShortcut(context: Context, shortcut: HomeShortcut, allApps: List<AppInfo>): String? {
    if (shortcut.profileKey == "0") return null
    matchingAppInfoForHomeShortcut(shortcut, allApps)?.let { return profileOriginLabelForApp(context, it) }
    return context.getString(R.string.drawer_section_work_profile)
}

private fun matchingAppInfoForHomeShortcut(shortcut: HomeShortcut, allApps: List<AppInfo>): AppInfo? {
    val packageName =
            when (val t = shortcut.target) {
                is ShortcutTarget.App -> t.packageName
                is ShortcutTarget.LauncherShortcut -> t.packageName
                else -> return null
            }
    return allApps.find {
        it.packageName == packageName && appProfileKey(it.userHandle) == shortcut.profileKey
    }
}

fun profileOriginLabelForFavorite(
        context: Context,
        fav: FavoriteApp,
        matchingApp: AppInfo?,
): String? {
    if (fav.profileKey == "0") return null
    val app =
            matchingApp?.takeIf {
                it.packageName == fav.packageName &&
                        appProfileKey(it.userHandle) == fav.profileKey
            }
    return app?.let { profileOriginLabelForApp(context, it) }
            ?: context.getString(R.string.drawer_section_work_profile)
}

internal fun profileSectionTitleForUser(
        context: Context,
        user: UserHandle,
        userManager: UserManager,
        totalSecondaryProfiles: Int,
): String {
    if (ProfileHeuristics.isManagedProfileForUser(userManager, user)) {
        return context.getString(R.string.drawer_section_work_profile)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcherApps =
                try {
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                } catch (_: Exception) {
                    null
                }
        if (launcherApps != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        ) {
            try {
                when (launcherApps.getLauncherUserInfo(user)?.userType) {
                    "android.os.usertype.profile.MANAGED" ->
                            return context.getString(R.string.drawer_section_work_profile)
                }
            } catch (_: Exception) {}
        }
    }
    if (ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user)) {
        return context.getString(R.string.drawer_section_clone_profile)
    }
    if (totalSecondaryProfiles == 1) {
        return context.getString(R.string.drawer_section_work_profile)
    }
    val serial =
            try {
                userManager.getSerialNumberForUser(user)
            } catch (_: Exception) {
                -1L
            }
    return if (serial >= 0L) {
        context.getString(R.string.drawer_section_profile_numbered, serial)
    } else {
        context.getString(R.string.drawer_section_other_profile)
    }
}

private val alphabeticalShortcutComparator =
        compareBy<AppShortcutAction, String>(String.CASE_INSENSITIVE_ORDER) { it.appLabel }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.actionLabel }

fun sortShortcutActionsAlphabetically(actions: List<AppShortcutAction>): List<AppShortcutAction> =
        actions.sortedWith(alphabeticalShortcutComparator)

/**
 * Buckets shortcut actions by [AppShortcutAction.profileKey] using the same ordering as
 * [groupAppsIntoProfileSections] (owner block, then secondary profiles by user serial).
 */
fun groupShortcutActionsIntoProfileSections(
        context: Context,
        actions: List<AppShortcutAction>,
        allApps: List<AppInfo>,
): List<DrawerProfileShortcutSectionUi> {
    if (actions.isEmpty()) return emptyList()
    val byProfile = actions.groupBy { it.profileKey }
    val userManager =
            try {
                context.getSystemService(Context.USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }
    if (userManager == null) {
        return buildShortcutSectionsWithoutUserManager(context, byProfile, allApps)
    }
    val byUser = allApps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
    val orderedUsers =
            byUser.keys.sortedBy { uh ->
                try {
                    userManager.getSerialNumberForUser(uh)
                } catch (_: Exception) {
                    Long.MAX_VALUE
                }
            }
    return buildList {
        val ownerActions = sortShortcutActionsAlphabetically(byProfile["0"].orEmpty())
        if (ownerActions.isNotEmpty()) {
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "owner",
                            title = context.getString(R.string.drawer_section_personal),
                            actions = ownerActions,
                    )
            )
        }
        for (user in orderedUsers) {
            val pk = appProfileKey(user)
            val sectionActions = sortShortcutActionsAlphabetically(byProfile[pk].orEmpty())
            if (sectionActions.isEmpty()) continue
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "u_${user.hashCode()}",
                            title =
                                    profileSectionTitleForUser(
                                            context = context,
                                            user = user,
                                            userManager = userManager,
                                            totalSecondaryProfiles = orderedUsers.size,
                                    ),
                            actions = sectionActions,
                    )
            )
        }
    }
}

private fun buildShortcutSectionsWithoutUserManager(
        context: Context,
        byProfile: Map<String, List<AppShortcutAction>>,
        allApps: List<AppInfo>,
): List<DrawerProfileShortcutSectionUi> {
    return buildList {
        val ownerActions = sortShortcutActionsAlphabetically(byProfile["0"].orEmpty())
        if (ownerActions.isNotEmpty()) {
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "owner",
                            title = context.getString(R.string.drawer_section_personal),
                            actions = ownerActions,
                    )
            )
        }
        val byUser = allApps.filter { it.userHandle != null }.groupBy { it.userHandle!! }
        for (user in byUser.keys) {
            val pk = appProfileKey(user)
            val sectionActions = sortShortcutActionsAlphabetically(byProfile[pk].orEmpty())
            if (sectionActions.isEmpty()) continue
            val title =
                    when {
                        byUser.keys.size == 1 &&
                                !ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_work_profile)
                        ProfileHeuristics.isLikelyCloneOrParallelProfile(context, user) ->
                                context.getString(R.string.drawer_section_clone_profile)
                        else -> context.getString(R.string.drawer_section_other_profile)
                    }
            add(
                    DrawerProfileShortcutSectionUi(
                            id = "u_${user.hashCode()}",
                            title = title,
                            actions = sectionActions,
                    )
            )
        }
    }
}
