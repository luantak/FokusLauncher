package com.lu4p.fokuslauncher.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An installed app that exposes a MediaBrowserService we can attempt to connect to. */
data class MediaAppInfo(val packageName: String, val label: String)

/** Now-playing snapshot for the home media widget; null when nothing is actively playing. */
data class MediaPlaybackUiState(
        val title: String,
        val artist: String?,
        val isPlaying: Boolean,
        /** False when the active app does not advertise [PlaybackStateCompat.ACTION_SEEK_TO]. */
        val canSeek: Boolean,
)

/**
 * Surfaces the now-playing session for user-registered media apps and forwards transport controls
 * to them. Unlike a notification listener, this connects directly to each app's MediaBrowserService
 * via [MediaBrowserCompat], so it needs no special permission — but an app only appears if it allows
 * outside connections (its `onGetRoot` accepts us). Apps that whitelist only system callers (some
 * mainstream players) simply never connect, which is why registration is per-app and opt-in.
 *
 * All session interaction happens on the main thread.
 */
@Singleton
class MediaRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MediaPlaybackUiState?>(null)
    val state: StateFlow<MediaPlaybackUiState?> = _state.asStateFlow()

    /** Live connections keyed by package name. */
    private val connections = LinkedHashMap<String, AppConnection>()

    /** True once a session has stayed paused past the grace period, so the widget hides until it
     *  plays again. Many apps leave a paused session alive after they're closed; this clears it up. */
    private var pausedGraceExpired = false
    private var hideScheduled = false
    private val hideRunnable = Runnable {
        hideScheduled = false
        pausedGraceExpired = true
        publishState()
    }

    /** Installed apps advertising a MediaBrowserService, for the registration picker. */
    fun discoverMediaApps(): List<MediaAppInfo> {
        val pm = context.packageManager
        return pm.queryIntentServices(Intent(SERVICE_INTERFACE), 0)
                .mapNotNull { it.serviceInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { MediaAppInfo(it.packageName, it.loadLabel(pm).toString()) }
                .sortedBy { it.label.lowercase() }
    }

    /** Reconcile live connections with the registered set: drop removed apps, connect new ones. */
    @MainThread
    fun setRegisteredApps(packages: Set<String>) {
        (connections.keys - packages).toList().forEach { disconnect(it) }
        (packages - connections.keys).forEach { connect(it) }
        publishState()
    }

    @MainThread
    fun stop() {
        connections.keys.toList().forEach { disconnect(it) }
        resetPauseGrace()
        _state.value = null
    }

    @MainThread fun playPause() {
        val controller = activeController() ?: return
        if (controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    /** Opens the playing app — its now-playing screen via [MediaControllerCompat.getSessionActivity]
     *  when offered, otherwise the app's launcher entry. */
    @MainThread fun openMediaApp() {
        val controller = activeController() ?: return
        controller.sessionActivity?.let { pending ->
            try {
                // Android 14+ drops a sent PendingIntent's activity start unless the sender grants
                // it, even from the foreground; opt in so the app's now-playing screen opens.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options =
                            ActivityOptions.makeBasic()
                                    .setPendingIntentBackgroundActivityStartMode(
                                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                    )
                                    .toBundle()
                    pending.send(context, 0, null, null, null, null, options)
                } else {
                    pending.send()
                }
                return
            } catch (_: PendingIntent.CanceledException) {
                // Stale PendingIntent; fall through to a plain launch.
            }
        }
        val launch =
                context.packageManager.getLaunchIntentForPackage(controller.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launch)
        } catch (_: Exception) {}
    }

    @MainThread fun rewind() = seekBy(-REWIND_MS)

    @MainThread fun forward() = seekBy(FORWARD_MS)

    private fun seekBy(deltaMs: Long) {
        val controller = activeController() ?: return
        val playbackState = controller.playbackState ?: return
        if (playbackState.actions and PlaybackStateCompat.ACTION_SEEK_TO == 0L) return
        controller.transportControls.seekTo(seekTarget(playbackState.currentPosition(), deltaMs))
    }

    private fun connect(packageName: String) {
        val component = resolveServiceComponent(packageName) ?: return
        val connection = AppConnection(packageName)
        connections[packageName] = connection
        val browser = MediaBrowserCompat(context, component, connection.browserCallback, null)
        connection.browser = browser
        try {
            browser.connect()
        } catch (_: IllegalStateException) {
            // Already connecting/connected.
        }
    }

    private fun disconnect(packageName: String) {
        connections.remove(packageName)?.release()
    }

    private fun resolveServiceComponent(packageName: String): ComponentName? {
        val intent = Intent(SERVICE_INTERFACE).setPackage(packageName)
        val service =
                context.packageManager.queryIntentServices(intent, 0).firstOrNull()?.serviceInfo
                        ?: return null
        return ComponentName(service.packageName, service.name)
    }

    /** Active = a connected controller, preferring one that is playing, then most recently updated. */
    private fun activeController(): MediaControllerCompat? =
            connections.values
                    .mapNotNull { it.controller }
                    .filter { it.playbackState.isShowable() }
                    .maxWithOrNull(
                            compareBy(
                                    { if (it.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) 1 else 0 },
                                    { it.playbackState?.lastPositionUpdateTime ?: 0L },
                            )
                    )

    private fun publishState() {
        val controller = activeController()
        val metadata = controller?.metadata
        val playbackState = controller?.playbackState
        val title =
                metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                        ?: metadata?.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
        if (controller == null || playbackState == null || title.isNullOrBlank()) {
            resetPauseGrace()
            _state.value = null
            return
        }
        val isPlaying = playbackState.state == PlaybackStateCompat.STATE_PLAYING
        if (isPlaying) {
            // Playing again cancels any pending hide and clears the expired flag.
            resetPauseGrace()
        } else if (pausedGraceExpired) {
            // Still paused after the grace period: keep the stale session hidden.
            _state.value = null
            return
        } else {
            schedulePauseHide()
        }
        _state.value =
                MediaPlaybackUiState(
                        title = title,
                        artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST),
                        isPlaying = isPlaying,
                        canSeek = playbackState.actions and PlaybackStateCompat.ACTION_SEEK_TO != 0L,
                )
    }

    private fun schedulePauseHide() {
        if (hideScheduled) return
        hideScheduled = true
        mainHandler.postDelayed(hideRunnable, PAUSE_HIDE_DELAY_MS)
    }

    private fun resetPauseGrace() {
        pausedGraceExpired = false
        hideScheduled = false
        mainHandler.removeCallbacks(hideRunnable)
    }

    /** One app's browser + controller pair, with callbacks that republish on any change. */
    private inner class AppConnection(val packageName: String) {
        var browser: MediaBrowserCompat? = null
        var controller: MediaControllerCompat? = null

        val browserCallback =
                object : MediaBrowserCompat.ConnectionCallback() {
                    override fun onConnected() {
                        val token = browser?.sessionToken ?: return
                        val ctrl = MediaControllerCompat(context, token)
                        controller = ctrl
                        ctrl.registerCallback(controllerCallback, mainHandler)
                        publishState()
                    }

                    override fun onConnectionSuspended() {
                        detachController()
                        publishState()
                    }

                    override fun onConnectionFailed() {
                        // The app refused our connection; leave it disconnected.
                    }
                }

        private val controllerCallback =
                object : MediaControllerCompat.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = publishState()
                    override fun onMetadataChanged(metadata: MediaMetadataCompat?) = publishState()
                    override fun onSessionDestroyed() {
                        detachController()
                        publishState()
                    }
                }

        private fun detachController() {
            controller?.unregisterCallback(controllerCallback)
            controller = null
        }

        fun release() {
            detachController()
            try {
                browser?.disconnect()
            } catch (_: Exception) {}
            browser = null
        }
    }

    private fun PlaybackStateCompat?.isShowable(): Boolean =
            when (this?.state) {
                null,
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_ERROR -> false
                else -> true
            }

    private fun PlaybackStateCompat.currentPosition(): Long {
        if (state != PlaybackStateCompat.STATE_PLAYING) return position
        val elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return position + (elapsed * playbackSpeed).toLong()
    }

    companion object {
        const val REWIND_MS = 15_000L
        const val FORWARD_MS = 30_000L
        /** Hide a session that stays paused this long, so closed apps don't leave the widget up. */
        private const val PAUSE_HIDE_DELAY_MS = 60_000L
        private const val SERVICE_INTERFACE = "android.media.browse.MediaBrowserService"

        /** Seek destination clamped to the start of the track; extracted for unit testing. */
        fun seekTarget(currentPosition: Long, deltaMs: Long): Long =
                (currentPosition + deltaMs).coerceAtLeast(0L)
    }
}
