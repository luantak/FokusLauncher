package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiscordInvitationViewModel @Inject constructor(
        private val preferences: PreferencesManager,
) : ViewModel() {
    suspend fun onSettingsOpened(nowEpochMs: Long = System.currentTimeMillis()): Boolean =
            preferences.claimDiscordInvitation(nowEpochMs)

    suspend fun onDismissed() = preferences.recordDiscordInvitationDismissed()

    suspend fun openDiscordLink(openLink: () -> Unit) {
        preferences.recordDiscordLinkOpened()
        openLink()
    }
}
