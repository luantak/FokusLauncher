package com.lu4p.fokuslauncher.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager

/** Small helpers for work / clone / parallel user profiles (OEMs vary). */
object ProfileHeuristics {

    fun isManagedProfileForUser(userManager: UserManager, user: UserHandle): Boolean {
        return try {
            val m =
                    UserManager::class.java.getMethod("isManagedProfile", UserHandle::class.java)
            m.invoke(userManager, user) as Boolean
        } catch (_: ReflectiveOperationException) {
            false
        }
    }

    /**
     * Parallel / dual-app profiles often expose a userType containing "clone" (OEM-dependent).
     * When unknown, returns false so a lone secondary user can be treated as work (see drawer).
     */
    fun isLikelyCloneOrParallelProfile(context: Context, user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val launcherApps =
                try {
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                } catch (_: Exception) {
                    null
                }
                        ?: return false
        return try {
            val type = launcherApps.getLauncherUserInfo(user)?.userType ?: return false
            type.contains("clone", ignoreCase = true) ||
                type.contains("parallel", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    /** Strips a leading OEM prefix like "Work WhatsApp" → "WhatsApp". */
    fun stripLeadingWorkPrefix(label: String): String? {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return null
        val stripped = Regex("(?i)^work\\s+").replace(trimmed, "").trim()
        return stripped.takeIf { it.isNotEmpty() }
    }
}
