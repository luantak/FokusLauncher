package com.lu4p.fokuslauncher.media

import android.media.MediaMetadata
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
                MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist A")
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Subtitle B")
                        .build()

        assertEquals("Artist A", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameFallsBackToDisplaySubtitle() {
        val metadata =
                MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Artist B")
                        .build()

        assertEquals("Artist B", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameReadsCharSequenceMetadata() {
        val metadata =
                MediaMetadata.Builder()
                        .putText(MediaMetadata.METADATA_KEY_TITLE, "Song Title")
                        .putText(MediaMetadata.METADATA_KEY_ARTIST, "Spotify Artist")
                        .build()

        assertEquals("Song Title", MediaMetadataReader.trackTitle(metadata))
        assertEquals("Spotify Artist", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameUsesDescriptionSubtitle() {
        val metadata =
                MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, "Song Title")
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, "Artist Name")
                        .build()

        assertEquals("Song Title", MediaMetadataReader.trackTitle(metadata))
        assertEquals("Artist Name", MediaMetadataReader.artistName(metadata))
    }

    @Test
    fun artistNameReturnsNullWhenMissing() {
        assertNull(MediaMetadataReader.artistName(MediaMetadata.Builder().build()))
    }
}
