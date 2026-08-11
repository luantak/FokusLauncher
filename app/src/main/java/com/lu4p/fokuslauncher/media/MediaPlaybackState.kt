package com.lu4p.fokuslauncher.media

import android.media.session.PlaybackState

/** Helpers for interpreting [PlaybackState] session states in the media widget. */
object MediaPlaybackState {

    /** True for playing or buffering — session is actively engaged with media. */
    fun isActivelyPlaying(state: Int?): Boolean =
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING

    fun isBuffering(state: Int?): Boolean = state == PlaybackState.STATE_BUFFERING

    fun isShowable(state: Int?): Boolean =
            when (state) {
                null,
                PlaybackState.STATE_NONE,
                PlaybackState.STATE_STOPPED,
                PlaybackState.STATE_ERROR -> false
                else -> true
            }
}
