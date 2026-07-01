package com.lu4p.fokuslauncher.media

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaCustomActionsReaderTest {

    @Test
    fun likeButtonMatchesHeartAction() {
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("com.spotify.heart", "Like", 0)
                        .build()

        val like = MediaCustomActionsReader.likeButton(state)
        assertEquals("com.spotify.heart", like?.actionId)
        assertFalse(like?.active == true)
    }

    @Test
    fun saveButtonMatchesLibraryAction() {
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("ADD_TO_LIBRARY", "Add to library", 0)
                        .build()

        val save = MediaCustomActionsReader.saveButton(state)
        assertEquals("ADD_TO_LIBRARY", save?.actionId)
        assertFalse(save?.active == true)
    }

    @Test
    fun likedSongsActionMapsToLikeNotSave() {
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("add_to_liked_songs", "Add to Liked Songs", 0)
                        .build()

        val like = MediaCustomActionsReader.likeButton(state)
        assertEquals("add_to_liked_songs", like?.actionId)
        assertFalse(like?.active == true)
        assertNull(MediaCustomActionsReader.saveButton(state))
    }

    @Test
    fun removeFromLikedSongsIsActiveLike() {
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("remove_from_liked_songs", "Remove from Liked Songs", 0)
                        .build()

        val like = MediaCustomActionsReader.likeButton(state)
        assertTrue(like?.active == true)
    }

    @Test
    fun activeSaveDetectedFromRemoveLabel() {
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("REMOVE_FROM_LIBRARY", "Remove from library", 0)
                        .build()

        val save = MediaCustomActionsReader.saveButton(state)
        assertTrue(save?.active == true)
    }

    @Test
    fun activeLikeDetectedFromExtras() {
        val extras = Bundle().apply { putBoolean("liked", true) }
        val customAction =
                PlaybackStateCompat.CustomAction.Builder("heart", "Like", android.R.drawable.btn_star)
                        .setExtras(extras)
                        .build()
        val state =
                PlaybackStateCompat.Builder().addCustomAction(customAction).build()

        assertTrue(MediaCustomActionsReader.likeButton(state)?.active == true)
    }

    @Test
    fun likedStateFromMetadataRating() {
        val metadata =
                MediaMetadataCompat.Builder()
                        .putRating(
                                MediaMetadataCompat.METADATA_KEY_USER_RATING,
                                RatingCompat.newHeartRating(true),
                        )
                        .build()
        val state =
                PlaybackStateCompat.Builder()
                        .addCustomAction("heart", "Like", 0)
                        .build()

        assertTrue(MediaCustomActionsReader.likeButton(state, metadata)?.active == true)
    }
}
