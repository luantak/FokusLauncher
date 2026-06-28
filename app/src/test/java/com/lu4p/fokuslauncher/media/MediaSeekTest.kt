package com.lu4p.fokuslauncher.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSeekTest {

    @Test
    fun rewindSubtractsFifteenSeconds() {
        val target = MediaRepository.seekTarget(60_000L, -MediaRepository.REWIND_MS)
        assertEquals(45_000L, target)
    }

    @Test
    fun forwardAddsThirtySeconds() {
        val target = MediaRepository.seekTarget(60_000L, MediaRepository.FORWARD_MS)
        assertEquals(90_000L, target)
    }

    @Test
    fun rewindClampsAtTrackStart() {
        val target = MediaRepository.seekTarget(5_000L, -MediaRepository.REWIND_MS)
        assertEquals(0L, target)
    }

    @Test
    fun forwardFromStartIsExactlyThirtySeconds() {
        val target = MediaRepository.seekTarget(0L, MediaRepository.FORWARD_MS)
        assertEquals(30_000L, target)
    }
}
