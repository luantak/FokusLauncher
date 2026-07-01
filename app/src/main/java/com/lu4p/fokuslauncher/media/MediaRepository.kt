package com.lu4p.fokuslauncher.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Now-playing snapshot for the home media widget; null when nothing is actively playing. */
data class MediaPlaybackUiState(
        val title: String,
        val artist: String?,
        /** True for [PlaybackStateCompat.STATE_PLAYING] and [PlaybackStateCompat.STATE_BUFFERING]. */
        val isPlaying: Boolean,
        val isBuffering: Boolean = false,
        /** False when the active app does not advertise [PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS]. */
        val canSkipToPrevious: Boolean,
        /** False when the active app does not advertise [PlaybackStateCompat.ACTION_SKIP_TO_NEXT]. */
        val canSkipToNext: Boolean,
        val like: MediaCustomActionButton? = null,
        val save: MediaCustomActionButton? = null,
)

/**
 * Surfaces the now-playing session for user-registered media apps and forwards transport controls
 * to them via notification access ([MediaNotificationListenerService] +
 * [MediaSessionManager.getActiveSessions]).
 *
 * All session interaction happens on the main thread.
 */
@Singleton
class MediaRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<MediaPlaybackUiState?>(null)
    val state: StateFlow<MediaPlaybackUiState?> = _state.asStateFlow()

    /** Session controllers from notification access, keyed by package name. */
    private val sessionControllers = LinkedHashMap<String, MediaControllerCompat>()

    private var widgetEnabled = false
    private var listenerComponent: ComponentName? = null
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    /** True once a session has stayed paused past the grace period, so the widget hides until it
     *  plays again. Many apps leave a paused session alive after they're closed; this clears it up. */
    private var pausedGraceExpired = false
    private var hideScheduled = false
    private var optimisticToggle: OptimisticCustomActionToggle? = null
    private val hideRunnable = Runnable {
        hideScheduled = false
        pausedGraceExpired = true
        publishState()
    }

    private val sessionCallback =
            object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = publishState()

                override fun onMetadataChanged(metadata: android.support.v4.media.MediaMetadataCompat?) =
                        publishState()

                override fun onSessionDestroyed() {
                    refreshNotificationSessions()
                }
            }

    /** Enable or disable the home media widget; requires notification access when enabling. */
    @MainThread
    fun setWidgetEnabled(enabled: Boolean) {
        widgetEnabled = enabled
        if (!enabled || !MediaNotificationHelper.isListenerEnabled(context)) {
            resetPauseGrace()
            optimisticToggle = null
            _state.value = null
            return
        }
        refreshNotificationSessions()
        publishState()
    }

    @MainThread
    fun stop() {
        setWidgetEnabled(false)
    }

    @MainThread
    fun onNotificationListenerConnected(component: ComponentName) {
        listenerComponent = component
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
        sessionsListener =
                MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                    mainHandler.post { updateSessionControllers(controllers) }
                }
        try {
            manager.addOnActiveSessionsChangedListener(
                    sessionsListener!!,
                    component,
                    mainHandler,
            )
            updateSessionControllers(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            onNotificationListenerDisconnected()
        }
    }

    @MainThread
    fun onNotificationListenerDisconnected() {
        sessionsListener?.let { listener ->
            try {
                context.getSystemService(MediaSessionManager::class.java)
                        ?.removeOnActiveSessionsChangedListener(listener)
            } catch (_: Exception) {}
        }
        sessionsListener = null
        listenerComponent = null
        sessionControllers.values.forEach { it.unregisterCallback(sessionCallback) }
        sessionControllers.clear()
        publishState()
    }

    /** Re-read active sessions when the home screen resumes or registered apps change. */
    @MainThread
    fun refreshNotificationSessions() {
        if (!widgetEnabled || !MediaNotificationHelper.isListenerEnabled(context)) return
        val component = listenerComponent ?: MediaNotificationHelper.componentName(context)
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
        try {
            updateSessionControllers(manager.getActiveSessions(component))
        } catch (_: SecurityException) {}
    }

    @MainThread fun playPause() {
        val controller = activeController() ?: return
        if (MediaPlaybackState.isActivelyPlaying(controller.playbackState?.state)) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    /** Opens the playing app — its now-playing screen via [MediaControllerCompat.getSessionActivity]
     *  when offered, otherwise the app's launcher entry. */
    @MainThread fun openMediaApp() {
        val playback = resolveActivePlayback() ?: return
        val controller = playback.controller
        controller.sessionActivity?.let { pending ->
            try {
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
        launchPackage(controller.packageName)
    }

    @MainThread
    fun skipToPrevious() {
        val controller = activeController() ?: return
        val actions = controller.playbackState?.actions ?: return
        if (actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS == 0L) return
        controller.transportControls.skipToPrevious()
    }

    @MainThread
    fun skipToNext() {
        val controller = activeController() ?: return
        val actions = controller.playbackState?.actions ?: return
        if (actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT == 0L) return
        controller.transportControls.skipToNext()
    }

    @MainThread
    fun invokeLikeAction() {
        val current = _state.value ?: return
        val like = current.like ?: return
        val trackKey = trackKey(current.title, current.artist)
        val previous = optimisticToggle?.takeIf { it.trackKey == trackKey }
        optimisticToggle =
                OptimisticCustomActionToggle(
                        trackKey = trackKey,
                        likeActive = !like.active,
                        saveActive = previous?.saveActive,
                )
        _state.value = current.copy(like = like.copy(active = !like.active))
        invokeCustomAction(like)
    }

    @MainThread
    fun invokeSaveAction() {
        val current = _state.value ?: return
        val save = current.save ?: return
        val trackKey = trackKey(current.title, current.artist)
        val previous = optimisticToggle?.takeIf { it.trackKey == trackKey }
        optimisticToggle =
                OptimisticCustomActionToggle(
                        trackKey = trackKey,
                        likeActive = previous?.likeActive,
                        saveActive = !save.active,
                )
        _state.value = current.copy(save = save.copy(active = !save.active))
        invokeCustomAction(save)
    }

    private fun invokeCustomAction(button: MediaCustomActionButton) {
        val controller = activeController() ?: return
        controller.transportControls.sendCustomAction(button.actionId, button.extras ?: Bundle())
    }

    private fun updateSessionControllers(frameworkControllers: List<MediaController>?) {
        val incoming =
                frameworkControllers.orEmpty().filter { it.packageName != context.packageName }
        val incomingPackages = incoming.map { it.packageName }.toSet()

        (sessionControllers.keys - incomingPackages).toList().forEach { packageName ->
            sessionControllers.remove(packageName)?.unregisterCallback(sessionCallback)
        }

        for (frameworkController in incoming) {
            val packageName = frameworkController.packageName
            val compatToken = MediaSessionCompat.Token.fromToken(frameworkController.sessionToken)
            val existing = sessionControllers[packageName]
            if (existing != null && existing.sessionToken == compatToken) continue
            existing?.unregisterCallback(sessionCallback)
            val compat = MediaControllerCompat(context, compatToken)
            compat.registerCallback(sessionCallback, mainHandler)
            sessionControllers[packageName] = compat
        }
        publishState()
    }

    private fun allShowableControllers(): List<MediaControllerCompat> =
            sessionControllers.values.filter {
                MediaPlaybackState.isShowable(it.playbackState?.state)
            }

    private fun activeController(): MediaControllerCompat? {
        val controllers = allShowableControllers()
        val active =
                controllers.filter {
                    MediaPlaybackState.isActivelyPlaying(it.playbackState?.state)
                }
        if (active.isNotEmpty()) {
            return active.maxWithOrNull(
                    compareBy { it.playbackState?.lastPositionUpdateTime ?: 0L }
            )
        }
        return controllers.maxWithOrNull(
                compareBy { it.playbackState?.lastPositionUpdateTime ?: 0L }
        )
    }

    private fun resolveActivePlayback(): ActivePlayback? {
        val controller = activeController()
        val metadata = controller?.metadata
        val playbackState = controller?.playbackState
        val title = MediaMetadataReader.trackTitle(metadata)
        if (controller == null || playbackState == null || title.isNullOrBlank()) return null

        val state = playbackState.state
        return ActivePlayback(
                title = title,
                artist = MediaMetadataReader.artistName(metadata, title),
                isPlaying = MediaPlaybackState.isActivelyPlaying(state),
                isBuffering = MediaPlaybackState.isBuffering(state),
                controller = controller,
                packageName = controller.packageName,
        )
    }

    private fun publishState() {
        if (!widgetEnabled || !MediaNotificationHelper.isListenerEnabled(context)) {
            _state.value = null
            return
        }

        val playback = resolveActivePlayback()
        if (playback == null) {
            resetPauseGrace()
            optimisticToggle = null
            _state.value = null
            return
        }

        if (playback.isPlaying) {
            resetPauseGrace()
        } else if (pausedGraceExpired) {
            _state.value = null
            return
        } else {
            schedulePauseHide()
        }

        val actions = playback.controller.playbackState?.actions ?: 0L
        val playbackState = playback.controller.playbackState
        val metadata = playback.controller.metadata
        val trackKey = trackKey(playback.title, playback.artist)
        var like = MediaCustomActionsReader.likeButton(playbackState, metadata)
        var save = MediaCustomActionsReader.saveButton(playbackState, metadata)
        reconcileOptimisticToggle(trackKey, like, save)
        val optimistic = optimisticToggle?.takeIf { it.trackKey == trackKey }
        like = applyOptimisticToggle(like, optimistic?.likeActive)
        save = applyOptimisticToggle(save, optimistic?.saveActive)
        _state.value =
                MediaPlaybackUiState(
                        title = playback.title,
                        artist = playback.artist,
                        isPlaying = playback.isPlaying,
                        isBuffering = playback.isBuffering,
                        canSkipToPrevious =
                                actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L,
                        canSkipToNext =
                                actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L,
                        like = like,
                        save = save,
                )
    }

    private fun trackKey(title: String, artist: String?): String = "$title|${artist.orEmpty()}"

    private fun applyOptimisticToggle(
            button: MediaCustomActionButton?,
            activeOverride: Boolean?,
    ): MediaCustomActionButton? {
        button ?: return null
        return if (activeOverride != null) button.copy(active = activeOverride) else button
    }

    private fun reconcileOptimisticToggle(
            trackKey: String,
            like: MediaCustomActionButton?,
            save: MediaCustomActionButton?,
    ) {
        val optimistic = optimisticToggle ?: return
        if (optimistic.trackKey != trackKey) {
            optimisticToggle = null
            return
        }
        val likeMatches = optimistic.likeActive == null || like?.active == optimistic.likeActive
        val saveMatches = optimistic.saveActive == null || save?.active == optimistic.saveActive
        if (likeMatches && saveMatches) {
            optimisticToggle = null
        }
    }

    private fun launchPackage(packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launch)
        } catch (_: Exception) {}
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

    private data class OptimisticCustomActionToggle(
            val trackKey: String,
            val likeActive: Boolean? = null,
            val saveActive: Boolean? = null,
    )

    private data class ActivePlayback(
            val title: String,
            val artist: String?,
            val isPlaying: Boolean,
            val isBuffering: Boolean,
            val controller: MediaControllerCompat,
            val packageName: String,
    )

    companion object {
        /** Hide a session that stays paused this long, so closed apps don't leave the widget up. */
        private const val PAUSE_HIDE_DELAY_MS = 60_000L

        /** First non-blank artist-like field; many players only populate display subtitle. */
        fun artistName(metadata: android.support.v4.media.MediaMetadataCompat?): String? =
                MediaMetadataReader.artistName(metadata)
    }
}
