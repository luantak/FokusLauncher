package com.lu4p.fokuslauncher.ui.settings

import com.lu4p.fokuslauncher.data.local.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordInvitationViewModelTest {
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val viewModel = DiscordInvitationViewModel(preferences)

    @Test fun `settings entry checks eligibility only when explicitly opened`() = runBlocking {
        coVerify(exactly = 0) { preferences.claimDiscordInvitation(any()) }
        coEvery { preferences.claimDiscordInvitation(123L) } returns true
        assertTrue(viewModel.onSettingsOpened(123L))
        coEvery { preferences.claimDiscordInvitation(456L) } returns false
        assertFalse(viewModel.onSettingsOpened(456L))
    }

    @Test fun `join callback runs only after the opened outcome has persisted`() = runBlocking {
        var persisted = false
        var opened = false
        coEvery { preferences.recordDiscordLinkOpened() } answers { persisted = true }
        viewModel.openDiscordLink {
            assertTrue(persisted)
            opened = true
        }
        assertTrue(opened)
    }

    @Test fun `failed persistence never launches an external link`() = runBlocking {
        var opened = false
        coEvery { preferences.recordDiscordLinkOpened() } throws java.io.IOException("disk full")
        try {
            viewModel.openDiscordLink { opened = true }
            org.junit.Assert.fail("Expected persistence failure")
        } catch (_: java.io.IOException) {
            assertFalse(opened)
        }
    }

    @Test fun `dismissal persists its outcome before finishing`() = runBlocking {
        viewModel.onDismissed()
        coVerify(exactly = 1) { preferences.recordDiscordInvitationDismissed() }
    }
}
