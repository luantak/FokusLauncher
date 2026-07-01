package com.lu4p.fokuslauncher.media

import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackStateTest {

    @Test
    fun activelyPlayingIncludesPlayingAndBuffering() {
        assertTrue(MediaPlaybackState.isActivelyPlaying(PlaybackStateCompat.STATE_PLAYING))
        assertTrue(MediaPlaybackState.isActivelyPlaying(PlaybackStateCompat.STATE_BUFFERING))
        assertFalse(MediaPlaybackState.isActivelyPlaying(PlaybackStateCompat.STATE_PAUSED))
    }

    @Test
    fun bufferingDetectedSeparately() {
        assertTrue(MediaPlaybackState.isBuffering(PlaybackStateCompat.STATE_BUFFERING))
        assertFalse(MediaPlaybackState.isBuffering(PlaybackStateCompat.STATE_PLAYING))
    }

    @Test
    fun showableIncludesBufferingAndPaused() {
        assertTrue(MediaPlaybackState.isShowable(PlaybackStateCompat.STATE_BUFFERING))
        assertTrue(MediaPlaybackState.isShowable(PlaybackStateCompat.STATE_PAUSED))
        assertFalse(MediaPlaybackState.isShowable(PlaybackStateCompat.STATE_STOPPED))
    }
}
