package com.lu4p.fokuslauncher.media

import android.media.MediaMetadata
import android.media.Rating
import android.media.session.PlaybackState
import android.os.Bundle

/** A like or save control advertised by the active app through [PlaybackState] custom actions. */
data class MediaCustomActionButton(
        val actionId: String,
        val label: String,
        val active: Boolean,
        val extras: Bundle?,
)

/** Maps session custom actions to like/save buttons using action id and label heuristics. */
object MediaCustomActionsReader {

    fun likeButton(
            state: PlaybackState?,
            metadata: MediaMetadata? = null,
    ): MediaCustomActionButton? =
            bestMatch(state?.customActions.orEmpty(), ActionKind.LIKE, metadata)

    fun saveButton(
            state: PlaybackState?,
            metadata: MediaMetadata? = null,
    ): MediaCustomActionButton? =
            bestMatch(state?.customActions.orEmpty(), ActionKind.SAVE, metadata)

    private enum class ActionKind {
        LIKE,
        SAVE,
    }

    private fun bestMatch(
            actions: List<PlaybackState.CustomAction>,
            kind: ActionKind,
            metadata: MediaMetadata?,
    ): MediaCustomActionButton? =
            actions
                    .mapNotNull { action -> score(action, kind, metadata) }
                    .maxByOrNull { it.second }
                    ?.first

    private fun score(
            action: PlaybackState.CustomAction,
            kind: ActionKind,
            metadata: MediaMetadata?,
    ): Pair<MediaCustomActionButton, Int>? {
        val actionId = action.action.orEmpty()
        val label = action.name?.toString().orEmpty()
        val haystack = "$actionId $label".lowercase()
        if (haystack.isBlank()) return null

        val otherKind = if (kind == ActionKind.LIKE) ActionKind.SAVE else ActionKind.LIKE
        val thisScore = keywordScore(haystack, keywords(kind))
        if (thisScore <= 0) return null
        val otherScore = keywordScore(haystack, keywords(otherKind))
        if (otherScore >= thisScore) return null

        val active = resolveActive(haystack, action.extras, metadata, kind)
        return MediaCustomActionButton(
                        actionId = actionId,
                        label = label.ifBlank { defaultLabel(kind, active) },
                        active = active,
                        extras = action.extras,
                ) to thisScore
    }

    private fun resolveActive(
            haystack: String,
            extras: Bundle?,
            metadata: MediaMetadata?,
            kind: ActionKind,
    ): Boolean {
        extrasIndicateActive(extras)?.let { return it }
        if (kind == ActionKind.LIKE) {
            metadataIndicatesLiked(metadata)?.let { return it }
        }
        return isActiveFromLabel(haystack)
    }

    private fun extrasIndicateActive(extras: Bundle?): Boolean? {
        extras ?: return null
        for (key in extras.keySet()) {
            val normalizedKey = key.lowercase()
            if (
                    normalizedKey !in ACTIVE_EXTRA_KEYS &&
                            normalizedKey !in INACTIVE_EXTRA_KEYS
            ) {
                continue
            }
            @Suppress("DEPRECATION")
            when (val value = extras.get(key)) {
                is Boolean ->
                        return if (normalizedKey in INACTIVE_EXTRA_KEYS) !value else value
                is Int ->
                        return when (value) {
                            1 -> normalizedKey !in INACTIVE_EXTRA_KEYS
                            0 -> false
                            else -> null
                        }
                is String -> {
                    val normalized = value.lowercase()
                    if (normalized in ACTIVE_EXTRA_VALUES) return true
                    if (normalized in INACTIVE_EXTRA_VALUES) return false
                }
            }
        }
        return null
    }

    private fun metadataIndicatesLiked(metadata: MediaMetadata?): Boolean? {
        val rating = metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING) ?: return null
        if (!rating.isRated) return false
        return when (rating.ratingStyle) {
            Rating.RATING_HEART -> rating.hasHeart()
            Rating.RATING_THUMB_UP_DOWN -> rating.isThumbUp
            else -> null
        }
    }

    /** Liked/saved items usually expose a remove action; new items expose add/save. */
    private fun isActiveFromLabel(haystack: String): Boolean {
        if (INACTIVE_LABEL_PATTERNS.any { it in haystack }) return false
        return ACTIVE_LABEL_PATTERNS.any { it in haystack }
    }

    private fun keywords(kind: ActionKind): List<String> =
            when (kind) {
                ActionKind.LIKE ->
                        listOf(
                                "like",
                                "thumb",
                                "favorite",
                                "favourite",
                                "heart",
                                "love",
                                "add_to_liked",
                                "liked_songs",
                        )
                ActionKind.SAVE ->
                        listOf(
                                "save",
                                "library",
                                "collection",
                                "bookmark",
                                "add_to_library",
                                "add_to_collection",
                                "add_song",
                                "your_episodes",
                        )
            }

    private fun keywordScore(haystack: String, keywords: List<String>): Int {
        var score = 0
        for (keyword in keywords) {
            if (keyword in haystack) score += 10 + keyword.length
        }
        return score
    }

    private fun defaultLabel(kind: ActionKind, active: Boolean): String =
            when (kind) {
                ActionKind.LIKE -> if (active) "Liked" else "Like"
                ActionKind.SAVE -> if (active) "Saved" else "Save"
            }

    private val ACTIVE_LABEL_PATTERNS =
            listOf(
                    "remove from",
                    "remove_from",
                    "delete from",
                    "delete_from",
                    "unlike",
                    "unsave",
                    "remove",
            )

    private val INACTIVE_LABEL_PATTERNS =
            listOf(
                    "add to",
                    "add_to",
                    "save to",
                    "save_to",
                    "download",
            )

    private val ACTIVE_EXTRA_KEYS =
            setOf(
                    "active",
                    "enabled",
                    "liked",
                    "saved",
                    "is_liked",
                    "is_saved",
                    "favorite",
                    "favourite",
                    "in_library",
                    "checked",
                    "selected",
            )

    private val INACTIVE_EXTRA_KEYS =
            setOf(
                    "inactive",
                    "disabled",
                    "unchecked",
                    "unselected",
            )

    private val ACTIVE_EXTRA_VALUES =
            setOf("true", "1", "on", "yes", "liked", "saved", "active", "checked", "selected")

    private val INACTIVE_EXTRA_VALUES =
            setOf("false", "0", "off", "no", "inactive", "unchecked", "unselected")
}
