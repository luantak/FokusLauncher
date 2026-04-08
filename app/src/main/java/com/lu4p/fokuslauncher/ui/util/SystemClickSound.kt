package com.lu4p.fokuslauncher.ui.util

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/**
 * Plays the standard UI click sound when the user has touch sounds enabled in system settings.
 * Compose [clickable] / Material tap targets do not do this by default.
 */
fun View.playSystemClickSound() {
    playSoundEffect(SoundEffectConstants.CLICK)
}

/** Invoked for taps that should respect system touch sounds. Provided by [ProvideSystemClickSound]. */
val LocalSystemClickSound = compositionLocalOf { { } }

@Composable
fun ProvideSystemClickSound(content: @Composable () -> Unit) {
    val view = LocalView.current
    val play = remember(view) { { view.playSystemClickSound() } }
    CompositionLocalProvider(LocalSystemClickSound provides play) { content() }
}

/** Wraps [onClick] so system touch sound runs first (for SegmentedButton, [DropdownMenuItem], etc.). */
@Composable
fun rememberClickWithSystemSound(onClick: () -> Unit): () -> Unit {
    val play = LocalSystemClickSound.current
    return remember(onClick, play) {
        {
            play()
            onClick()
        }
    }
}

/** For [androidx.compose.material3.Switch], [androidx.compose.material3.Checkbox], exposed menu open/close, etc. */
@Composable
fun rememberBooleanChangeWithSystemSound(onChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val play = LocalSystemClickSound.current
    return remember(onChange, play) {
        { value ->
            play()
            onChange(value)
        }
    }
}

@Composable
fun Modifier.clickableWithSystemSound(
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onClick: () -> Unit,
): Modifier {
    val play = LocalSystemClickSound.current
    val interactionSource = remember { MutableInteractionSource() }
    return then(
            Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onClick = {
                        play()
                        onClick()
                    },
            )
    )
}

@Composable
fun Modifier.clickableWithSystemSound(
        interactionSource: MutableInteractionSource,
        indication: Indication?,
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onClick: () -> Unit,
): Modifier {
    val play = LocalSystemClickSound.current
    return then(
            Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onClick = {
                        play()
                        onClick()
                    },
            )
    )
}

/** System click sound without press indication (e.g. text / widget taps on home). */
@Composable
fun Modifier.clickableNoRippleWithSystemSound(onClick: () -> Unit): Modifier =
        clickableWithSystemSound(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
        )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.combinedClickableWithSystemSound(
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onLongClickLabel: String? = null,
        onLongClick: (() -> Unit)? = null,
        onDoubleClick: (() -> Unit)? = null,
        onClick: () -> Unit,
): Modifier {
    val play = LocalSystemClickSound.current
    val interactionSource = remember { MutableInteractionSource() }
    return then(
            Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onDoubleClick = onDoubleClick,
                    onClick = {
                        play()
                        onClick()
                    },
            )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.combinedClickableWithSystemSound(
        interactionSource: MutableInteractionSource,
        indication: Indication?,
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onLongClickLabel: String? = null,
        onLongClick: (() -> Unit)? = null,
        onDoubleClick: (() -> Unit)? = null,
        onClick: () -> Unit,
): Modifier {
    val play = LocalSystemClickSound.current
    return then(
            Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = role,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onDoubleClick = onDoubleClick,
                    onClick = {
                        play()
                        onClick()
                    },
            )
    )
}
