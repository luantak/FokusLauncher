package com.lu4p.fokuslauncher.utils

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android 15+ Private Space functionality.
 *
 * Private Space uses profile type "android.os.usertype.profile.PRIVATE" and
 * is controlled via quiet mode:
 *   - Locked = quiet mode enabled
 *   - Unlocked = quiet mode disabled
 *
 * Unlocking triggers the system authentication prompt (PIN / biometric).
 *
 * On Android < 15, all methods are safe to call and return empty/false.
 */
@Singleton
class PrivateSpaceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val userManager: UserManager by lazy {
        context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    /**
     * Emits whenever the Private Space profile becomes available (unlocked) or
     * unavailable (locked). Collectors should call [isPrivateSpaceUnlocked] and
     * [getPrivateSpaceApps] to read the new state.
     */
    private val _profileStateChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profileStateChanged: SharedFlow<Unit> = _profileStateChanged.asSharedFlow()

    /** Whether the device supports Private Space (Android 15 / API 35+). */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= 35

    init {
        if (isSupported) {
            registerProfileReceiver()
        }
    }

    /**
     * Listens for profile availability broadcasts so the launcher can
     * immediately refresh after the user unlocks or locks Private Space.
     */
    private fun registerProfileReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                _profileStateChanged.tryEmit(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PROFILE_AVAILABLE")
            addAction("android.intent.action.PROFILE_UNAVAILABLE")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    /**
     * Returns the UserHandle of the Private Space profile, or null if none
     * exists or the device is pre-Android 15.
     */
    fun getPrivateSpaceProfile(): UserHandle? {
        if (!isSupported) return null
        return try {
            val profiles = userManager.userProfiles
            profiles.firstOrNull { profile ->
                profile != android.os.Process.myUserHandle() &&
                    isPrivateProfile(profile)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if a profile is the Private Space profile by reading its
     * userType from [LauncherApps.getLauncherUserInfo].
     */
    private fun isPrivateProfile(profile: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < 35) return false
        return try {
            val info = launcherApps.getLauncherUserInfo(profile)
            info?.userType == "android.os.usertype.profile.PRIVATE"
        } catch (_: Exception) {
            false
        }
    }

    /** Returns apps installed in the Private Space profile. */
    fun getPrivateSpaceApps(): List<AppInfo> {
        val profile = getPrivateSpaceProfile() ?: return emptyList()
        return try {
            launcherApps.getActivityList(null, profile).map { activityInfo ->
                AppInfo(
                    packageName = activityInfo.applicationInfo.packageName,
                    label = activityInfo.label.toString(),
                    icon = activityInfo.getBadgedIcon(0),
                    userHandle = profile,
                    componentName = activityInfo.componentName
                )
            }.sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Launches a Private Space app via [LauncherApps.startMainActivity].
     * Regular [PackageManager.getLaunchIntentForPackage] does not work across
     * user profiles, so this is the only correct way to start an activity in
     * Private Space.
     *
     * @return true if the activity was started, false otherwise.
     */
    fun launchApp(componentName: ComponentName, userHandle: UserHandle): Boolean {
        return try {
            launcherApps.startMainActivity(componentName, userHandle, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the Private Space is currently unlocked.
     * "Quiet mode" = locked; not quiet = unlocked.
     */
    fun isPrivateSpaceUnlocked(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            !userManager.isQuietModeEnabled(profile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Requests unlocking of the Private Space.
     * This disables quiet mode, which triggers the system authentication
     * prompt (PIN, pattern, or biometric).
     * @return true if the request was initiated, false otherwise.
     */
    fun requestUnlock(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            // false = disable quiet mode = unlock
            userManager.requestQuietModeEnabled(false, profile)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Locks the Private Space by enabling quiet mode.
     * @return true if the request was initiated, false otherwise.
     */
    fun lock(): Boolean {
        if (!isSupported) return false
        val profile = getPrivateSpaceProfile() ?: return false
        return try {
            userManager.requestQuietModeEnabled(true, profile)
            true
        } catch (_: Exception) {
            false
        }
    }
}
