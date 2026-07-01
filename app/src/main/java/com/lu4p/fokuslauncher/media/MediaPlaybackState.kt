package com.lu4p.fokuslauncher.media

import android.support.v4.media.session.PlaybackStateCompat

/** Helpers for interpreting [PlaybackStateCompat] session states in the media widget. */
object MediaPlaybackState {

    /** True for playing or buffering — session is actively engaged with media. */
    fun isActivelyPlaying(state: Int?): Boolean =
            state == PlaybackStateCompat.STATE_PLAYING ||
                    state == PlaybackStateCompat.STATE_BUFFERING

    fun isBuffering(state: Int?): Boolean = state == PlaybackStateCompat.STATE_BUFFERING

    fun isShowable(state: Int?): Boolean =
            when (state) {
                null,
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_ERROR -> false
                else -> true
            }
}
