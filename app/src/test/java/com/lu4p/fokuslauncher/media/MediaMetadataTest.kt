package com.lu4p.fokuslauncher.media

import android.support.v4.media.MediaMetadataCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaMetadataTest {

    @Test
    fun artistNamePrefersArtistKey() {
        val metadata =
                MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist A")
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Subtitle B")
                        .build()

        assertEquals("Artist A", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameFallsBackToDisplaySubtitle() {
        val metadata =
                MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Artist B")
                        .build()

        assertEquals("Artist B", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameReadsCharSequenceMetadata() {
        val metadata =
                MediaMetadataCompat.Builder()
                        .putText(MediaMetadataCompat.METADATA_KEY_TITLE, "Song Title")
                        .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, "Spotify Artist")
                        .build()

        assertEquals("Song Title", MediaMetadataReader.trackTitle(metadata))
        assertEquals("Spotify Artist", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameUsesDescriptionSubtitle() {
        val metadata =
                MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Song Title")
                        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Artist Name")
                        .build()

        assertEquals("Song Title", MediaMetadataReader.trackTitle(metadata))
        assertEquals("Artist Name", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameReturnsNullWhenMissing() {
        assertNull(MediaMetadataReader.artistName(MediaMetadataCompat.Builder().build()))
    }
}
