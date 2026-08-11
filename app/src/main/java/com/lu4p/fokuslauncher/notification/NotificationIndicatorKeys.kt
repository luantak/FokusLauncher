package com.lu4p.fokuslauncher.notification

import android.app.Notification
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import com.lu4p.fokuslauncher.data.model.appMetadataKey
import com.lu4p.fokuslauncher.data.model.appProfileKey

/**
 * Notification flags that should not drive home/drawer indicators.
 *
 * Group summaries are not real alerts. Ongoing and foreground-service notifications are typically
 * sticky keep-alives (push listeners, mail sync, media) and match what stock/Nova-style badge
 * filtering treats as non-dot-worthy.
 */
private const val IGNORED_NOTIFICATION_FLAGS =
        Notification.FLAG_GROUP_SUMMARY or
                Notification.FLAG_ONGOING_EVENT or
                Notification.FLAG_FOREGROUND_SERVICE

/** Builds the stable package+profile key used to match home/drawer rows. */
fun notificationAppKey(packageName: String, userHandle: UserHandle?): String =
        appMetadataKey(packageName, appProfileKey(userHandle))

/**
 * Returns the app key when a notification should contribute to indicators, or null when it should
 * be ignored (group summaries, ongoing/FGS keep-alives, suppressed badges, own package, blank
 * package).
 */
fun notificationAppKeyOrNull(
        packageName: String?,
        flags: Int,
        userHandle: UserHandle?,
        ownPackageName: String,
        canShowBadge: Boolean = true,
): String? {
    if (!canShowBadge) return null
    if (flags and IGNORED_NOTIFICATION_FLAGS != 0) return null
    if (packageName.isNullOrBlank() || packageName == ownPackageName) return null
    return notificationAppKey(packageName, userHandle)
}

/**
 * Returns the app key for [sbn] when it should contribute to notification indicators, or null when
 * the notification should be ignored.
 */
fun notificationAppKeyOrNull(
        sbn: StatusBarNotification,
        ownPackageName: String,
        canShowBadge: Boolean = true,
): String? =
        notificationAppKeyOrNull(
                packageName = sbn.packageName,
                flags = sbn.notification.flags,
                userHandle = sbn.user,
                ownPackageName = ownPackageName,
                canShowBadge = canShowBadge,
        )

/**
 * Builds the set of apps with active (non-ignored) notifications from a listener snapshot.
 *
 * @param canShowBadgeForKey optional lookup from [android.service.notification.NotificationListenerService.RankingMap];
 * when null, only flag-based filtering applies.
 */
fun appsWithNotificationsFrom(
        notifications: Array<StatusBarNotification>?,
        ownPackageName: String,
        canShowBadgeForKey: ((notificationKey: String) -> Boolean)? = null,
): Set<String> {
    if (notifications.isNullOrEmpty()) return emptySet()
    return buildSet {
        for (sbn in notifications) {
            val canShowBadge = canShowBadgeForKey?.invoke(sbn.key) ?: true
            notificationAppKeyOrNull(sbn, ownPackageName, canShowBadge)?.let(::add)
        }
    }
}

/** Builds the set of apps with notifications from package/flag/user triples (for unit tests). */
fun appsWithNotificationsFromEntries(
        entries: List<Triple<String?, Int, UserHandle?>>,
        ownPackageName: String,
): Set<String> =
        buildSet {
            for ((packageName, flags, userHandle) in entries) {
                notificationAppKeyOrNull(packageName, flags, userHandle, ownPackageName)?.let(::add)
            }
        }
