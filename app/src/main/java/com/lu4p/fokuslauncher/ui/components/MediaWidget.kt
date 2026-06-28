package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound

/**
 * Two-line now-playing widget shown below the date/battery row when audio is active.
 * Top line scrolls the track title (and artist when present); the bottom line offers
 * rewind 15s / play-pause / forward 30s controls for the active media session.
 *
 * The seek buttons are dimmed and inert when [canSeek] is false (e.g. live streams).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaWidget(
        title: String,
        artist: String?,
        isPlaying: Boolean,
        canSeek: Boolean,
        modifier: Modifier = Modifier,
        outlined: Boolean = false,
        onOpenApp: () -> Unit = {},
        onRewind: () -> Unit = {},
        onPlayPause: () -> Unit = {},
        onForward: () -> Unit = {},
) {
    val color = MaterialTheme.colorScheme.onBackground
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val nowPlaying = if (artist.isNullOrBlank()) title else "$title — $artist"
    val iconSize = with(LocalDensity.current) { (titleStyle.fontSize * 1.5f).toDp() }

    Column(modifier = modifier.testTag("media_widget")) {
        // Tapping the title opens the playing app (its now-playing screen when offered).
        val titleModifier =
                Modifier.testTag("media_now_playing")
                        .fillMaxWidth()
                        .clickableNoRippleWithSystemSound(onClick = onOpenApp)
                        .basicMarquee()
        if (outlined) {
            OutlinedText(
                    text = nowPlaying,
                    style = titleStyle,
                    color = color,
                    maxLines = 1,
                    modifier = titleModifier,
            )
        } else {
            Text(
                    text = nowPlaying,
                    style = titleStyle,
                    color = color,
                    maxLines = 1,
                    modifier = titleModifier,
            )
        }
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 4.dp),
        ) {
            val seekColor = if (canSeek) color else color.copy(alpha = 0.38f)
            LauncherIcon(
                    imageVector = Icons.Filled.FastRewind,
                    contentDescription = stringResource(R.string.media_rewind_15),
                    iconSize = iconSize,
                    tint = seekColor,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("media_rewind")
                                    .then(
                                            if (canSeek) {
                                                Modifier.clickableNoRippleWithSystemSound(
                                                        onClick = onRewind
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
                                    if (isPlaying) R.string.media_pause else R.string.media_play
                            ),
                    iconSize = iconSize,
                    tint = color,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("media_play_pause")
                                    .clickableNoRippleWithSystemSound(onClick = onPlayPause),
            )
            LauncherIcon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = stringResource(R.string.media_forward_30),
                    iconSize = iconSize,
                    tint = seekColor,
                    outlined = outlined,
                    modifier =
                            Modifier.testTag("media_forward")
                                    .then(
                                            if (canSeek) {
                                                Modifier.clickableNoRippleWithSystemSound(
                                                        onClick = onForward
                                                )
                                            } else {
                                                Modifier
                                            }
                                    ),
            )
        }
    }
}
