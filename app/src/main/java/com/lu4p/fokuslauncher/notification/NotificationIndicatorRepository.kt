package com.lu4p.fokuslauncher.notification

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks which package+profile keys currently have active notifications for home/drawer indicators.
 *
 * Tracking is gated by [setTrackingEnabled] so media notification access can stay connected without
 * publishing indicator state when the feature is off. Multiple UI consumers (home, drawer) register
 * independently; tracking stays on while any consumer requests it.
 */
@Singleton
class NotificationIndicatorRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {

    private val _appsWithNotifications = MutableStateFlow<Set<String>>(emptySet())
    val appsWithNotifications: StateFlow<Set<String>> = _appsWithNotifications.asStateFlow()

    private val trackingConsumers = mutableSetOf<String>()
    private var connectedService: NotificationListenerService? = null

    private val trackingEnabled: Boolean
        get() = trackingConsumers.isNotEmpty()

    /**
     * Registers or clears a named consumer's request to track notifications.
     *
     * @param consumerId stable id such as `"home"` or `"drawer"`
     */
    @MainThread
    fun setTrackingEnabled(consumerId: String, enabled: Boolean) {
        if (enabled) {
            trackingConsumers += consumerId
        } else {
            trackingConsumers -= consumerId
        }
        if (!trackingEnabled) {
            _appsWithNotifications.value = emptySet()
            return
        }
        connectedService?.let(::resyncFromService)
    }

    @MainThread
    fun onListenerConnected(service: NotificationListenerService) {
        connectedService = service
        if (trackingEnabled) {
            resyncFromService(service)
        } else {
            _appsWithNotifications.value = emptySet()
        }
    }

    @MainThread
    fun onListenerDisconnected() {
        connectedService = null
        _appsWithNotifications.value = emptySet()
    }

    @MainThread
    fun onNotificationPosted(_sbn: StatusBarNotification) {
        resyncFromConnectedService()
    }

    @MainThread
    fun onNotificationRemoved(_sbn: StatusBarNotification) {
        resyncFromConnectedService()
    }

    @MainThread
    private fun resyncFromConnectedService() {
        if (!trackingEnabled) {
            _appsWithNotifications.value = emptySet()
            return
        }
        val service = connectedService ?: return
        resyncFromService(service)
    }

    @MainThread
    private fun resyncFromService(service: NotificationListenerService) {
        val active =
                try {
                    service.activeNotifications
                } catch (_: SecurityException) {
                    null
                } catch (_: RuntimeException) {
                    null
                }
        val rankingMap =
                try {
                    service.currentRanking
                } catch (_: SecurityException) {
                    null
                } catch (_: RuntimeException) {
                    null
                }
        val ranking = NotificationListenerService.Ranking()
        _appsWithNotifications.value =
                appsWithNotificationsFrom(active, context.packageName) { notificationKey ->
                    // Fall back to flag filters alone when ranking is unavailable.
                    if (rankingMap == null || !rankingMap.getRanking(notificationKey, ranking)) {
                        true
                    } else {
                        ranking.canShowBadge()
                    }
                }
    }

    companion object {
        const val CONSUMER_HOME = "home"
        const val CONSUMER_DRAWER = "drawer"
    }
}
