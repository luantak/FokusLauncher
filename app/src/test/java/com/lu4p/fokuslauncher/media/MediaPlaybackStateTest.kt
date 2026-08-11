package com.lu4p.fokuslauncher.media

import android.media.session.PlaybackState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackStateTest {

    @Test
    fun activelyPlayingIncludesPlayingAndBuffering() {
        assertTrue(MediaPlaybackState.isActivelyPlaying(PlaybackState.STATE_PLAYING))
        assertTrue(MediaPlaybackState.isActivelyPlaying(PlaybackState.STATE_BUFFERING))
        assertFalse(MediaPlaybackState.isActivelyPlaying(PlaybackState.STATE_PAUSED))
    }

    @Test
    fun bufferingDetectedSeparately() {
        assertTrue(MediaPlaybackState.isBuffering(PlaybackState.STATE_BUFFERING))
        assertFalse(MediaPlaybackState.isBuffering(PlaybackState.STATE_PLAYING))
    }

    @Test
    fun showableIncludesBufferingAndPaused() {
        assertTrue(MediaPlaybackState.isShowable(PlaybackState.STATE_BUFFERING))
        assertTrue(MediaPlaybackState.isShowable(PlaybackState.STATE_PAUSED))
        assertFalse(MediaPlaybackState.isShowable(PlaybackState.STATE_STOPPED))
    }
}
