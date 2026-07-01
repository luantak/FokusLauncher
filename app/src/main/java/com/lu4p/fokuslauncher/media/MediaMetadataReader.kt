package com.lu4p.fokuslauncher.media

import android.support.v4.media.MediaMetadataCompat

/** Reads now-playing fields the way Android media notifications do (getText + description). */
object MediaMetadataReader {

    fun trackTitle(metadata: MediaMetadataCompat?): String? {
        metadata ?: return null
        return metadata.readText(MediaMetadataCompat.METADATA_KEY_TITLE)
                ?: metadata.readText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                ?: metadata.description.title?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }

    fun artistName(metadata: MediaMetadataCompat?, title: String? = trackTitle(metadata)): String? {
        metadata ?: return null
        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() }

        for (key in PRIMARY_ARTIST_KEYS) {
            metadata.readText(key)?.let { return it }
        }

        metadata.description.subtitle?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { subtitle ->
            if (normalizedTitle == null || !subtitle.equals(normalizedTitle, ignoreCase = true)) {
                return subtitle
            }
        }

        for (key in SECONDARY_ARTIST_KEYS) {
            metadata.readText(key)?.let { candidate ->
                if (normalizedTitle == null || !candidate.equals(normalizedTitle, ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun MediaMetadataCompat.readText(key: String): String? =
            getString(key)?.trim()?.takeIf { it.isNotBlank() }
                    ?: getText(key)?.toString()?.trim()?.takeIf { it.isNotBlank() }

    private val PRIMARY_ARTIST_KEYS =
            listOf(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                    MediaMetadataCompat.METADATA_KEY_AUTHOR,
            )

    private val SECONDARY_ARTIST_KEYS =
            listOf(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
            )
}
