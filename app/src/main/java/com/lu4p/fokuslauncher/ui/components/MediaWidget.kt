package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.media.MediaCustomActionButton
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound

/**
 * Now-playing widget shown below the date/battery row when audio is active.
 * The title scrolls on the first line, the artist on a second line when present, and
 * optional like / save plus previous / play-pause / next controls sit below.
 *
 * Buttons are hidden when the active app does not advertise the matching session action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaWidget(
        title: String,
        artist: String?,
        isPlaying: Boolean,
        isBuffering: Boolean = false,
        canSkipToPrevious: Boolean,
        canSkipToNext: Boolean,
        like: MediaCustomActionButton? = null,
        save: MediaCustomActionButton? = null,
        modifier: Modifier = Modifier,
        outlined: Boolean = false,
        onOpenApp: () -> Unit = {},
        onLike: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onPlayPause: () -> Unit = {},
        onNext: () -> Unit = {},
        onSave: () -> Unit = {},
) {
    val color = MaterialTheme.colorScheme.onBackground
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val artistStyle = MaterialTheme.typography.bodyMedium
    val showArtist = !artist.isNullOrBlank() && !artist.equals(title, ignoreCase = true)
    val iconSize = with(LocalDensity.current) { (titleStyle.fontSize * 1.5f).toDp() }

    Column(modifier = modifier.testTag("media_widget")) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickableNoRippleWithSystemSound(onClick = onOpenApp),
        ) {
            MediaWidgetMarqueeText(
                    text = title,
                    style = titleStyle,
                    color = color,
                    outlined = outlined,
                    modifier = Modifier.testTag("media_title"),
            )
            if (showArtist) {
                MediaWidgetMarqueeText(
                        text = artist,
                        style = artistStyle,
                        color = color.copy(alpha = 0.78f),
                        outlined = outlined,
                        modifier =
                                Modifier.testTag("media_artist").padding(top = 2.dp),
                )
            }
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val previousColor = if (canSkipToPrevious) color else color.copy(alpha = 0.38f)
                LauncherIcon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.media_previous_track),
                        iconSize = iconSize,
                        tint = previousColor,
                        outlined = outlined,
                        modifier =
                                Modifier.testTag("media_previous")
                                        .then(
                                                if (canSkipToPrevious) {
                                                    Modifier.clickableNoRippleWithSystemSound(
                                                            onClick = onPrevious
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                        ),
                )
                LauncherIcon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription =
                                stringResource(
                                        when {
                                            isBuffering -> R.string.media_buffering
                                            isPlaying -> R.string.media_pause
                                            else -> R.string.media_play
                                        }
                                ),
                        iconSize = iconSize,
                        tint = color,
                        outlined = outlined,
                        modifier =
                                Modifier.testTag("media_play_pause")
                                        .clickableNoRippleWithSystemSound(onClick = onPlayPause),
                )
                val nextColor = if (canSkipToNext) color else color.copy(alpha = 0.38f)
                LauncherIcon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.media_next_track),
                        iconSize = iconSize,
                        tint = nextColor,
                        outlined = outlined,
                        modifier =
                                Modifier.testTag("media_next")
                                        .then(
                                                if (canSkipToNext) {
                                                    Modifier.clickableNoRippleWithSystemSound(
                                                            onClick = onNext
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                        ),
                )
            }
            if (like != null || save != null) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f, fill = true),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (like != null) {
                        val likeTint =
                                if (like.active) color else color.copy(alpha = 0.5f)
                        LauncherIcon(
                                imageVector =
                                        if (like.active) Icons.Filled.Favorite
                                        else Icons.Outlined.FavoriteBorder,
                                contentDescription =
                                        like.label.ifBlank {
                                            stringResource(
                                                    if (like.active) R.string.media_unlike
                                                    else R.string.media_like
                                            )
                                        },
                                iconSize = iconSize,
                                tint = likeTint,
                                outlined = outlined,
                                modifier =
                                        Modifier.testTag("media_like")
                                                .clickableNoRippleWithSystemSound(onClick = onLike),
                        )
                    }
                    if (save != null) {
                        val saveTint =
                                if (save.active) MaterialTheme.colorScheme.primary
                                else color.copy(alpha = 0.5f)
                        LauncherIcon(
                                imageVector =
                                        if (save.active) Icons.Filled.Bookmark
                                        else Icons.Outlined.BookmarkBorder,
                                contentDescription =
                                        save.label.ifBlank {
                                            stringResource(
                                                    if (save.active) R.string.media_unsave
                                                    else R.string.media_save
                                            )
                                        },
                                iconSize = iconSize,
                                tint = saveTint,
                                outlined = outlined,
                                modifier =
                                        Modifier.testTag("media_save")
                                                .clickableNoRippleWithSystemSound(onClick = onSave),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaWidgetMarqueeText(
        text: String,
        style: androidx.compose.ui.text.TextStyle,
        color: androidx.compose.ui.graphics.Color,
        outlined: Boolean,
        modifier: Modifier = Modifier,
) {
    val marqueeModifier = modifier.fillMaxWidth().basicMarquee()
    if (outlined) {
        OutlinedText(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = marqueeModifier,
        )
    } else {
        Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = marqueeModifier,
        )
    }
}
