package com.lu4p.fokuslauncher.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun DiscordInvitationHost(viewModel: DiscordInvitationViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    // Intentionally not saveable: a recreated screen must recheck the durable consumed state.
    var visible by remember { mutableStateOf(false) }
    var handling by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        // One check per Settings opening; crossing the delay while already here does nothing.
        visible = viewModel.onSettingsOpened()
    }

    if (visible) {
        val dismiss: () -> Unit = {
            if (!handling) {
                handling = true
                scope.launch {
                    viewModel.onDismissed()
                    visible = false
                }
            }
        }
        FokusAlertDialog(
                modifier = Modifier.testTag("discord_invitation"),
                onDismissRequest = dismiss,
                title = {
                    Text(stringResource(R.string.discord_invitation_title), color = MaterialTheme.colorScheme.onBackground)
                },
                text = {
                    Text(stringResource(R.string.discord_invitation_body), color = MaterialTheme.colorScheme.onBackground)
                },
                confirmButton = {
                    FokusTextButton(
                            enabled = !handling,
                            onClick = {
                                if (!handling) {
                                    handling = true
                                    scope.launch {
                                        viewModel.openDiscordLink {
                                            visible = false
                                            context.openDiscordCommunity()
                                        }
                                    }
                                }
                            },
                    ) {
                        Text(stringResource(R.string.discord_invitation_join), color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    FokusTextButton(enabled = !handling, onClick = dismiss) {
                        Text(stringResource(R.string.discord_invitation_decline), color = MaterialTheme.colorScheme.primary)
                    }
                },
        )
    }
}

internal fun Context.openDiscordCommunity() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, DISCORD_COMMUNITY_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: ActivityNotFoundException) {
        // An unavailable browser must not crash the launcher or replay a consumed invitation.
    }
}
