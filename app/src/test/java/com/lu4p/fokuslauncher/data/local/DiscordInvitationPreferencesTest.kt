package com.lu4p.fokuslauncher.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiscordInvitationPreferencesTest {
    @get:Rule val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("invite.preferences_pb") }
    }
    private val preferences by lazy {
        PreferencesManager(RuntimeEnvironment.getApplication(), mockk<NoteDocuments>(relaxed = true), store)
    }

    @After fun tearDown() { scope.cancel() }

    @Test fun `fresh completion waits three full days and only one visit can claim`() = runBlocking {
        val completedAt = 1_000_000L
        val threeDays = java.util.concurrent.TimeUnit.DAYS.toMillis(3)
        preferences.setHasCompletedOnboarding(true, completedAt)
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(completedAt - 1))
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(completedAt + threeDays - 1))
        org.junit.Assert.assertTrue(preferences.claimDiscordInvitation(completedAt + threeDays))
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(completedAt + threeDays + 1))
    }

    @Test fun `legacy completed install is immediately eligible and migration survives another visit`() = runBlocking {
        store.edit { it[androidx.datastore.preferences.core.booleanPreferencesKey("has_completed_onboarding")] = true }
        org.junit.Assert.assertTrue(preferences.claimDiscordInvitation(1_000_000L))
        assertNotNull(store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
    }

    @Test fun `settings reset must not replay a consumed invitation`() = runBlocking {
        preferences.setHasCompletedOnboarding(true, 1L)
        org.junit.Assert.assertTrue(preferences.claimDiscordInvitation(Long.MAX_VALUE))
        preferences.clearAll()
        preferences.setHasCompletedOnboarding(true, 2L)
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
        org.junit.Assert.assertEquals(1L, store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
    }

    @Test fun `settings reset must not bypass the original new install delay`() = runBlocking {
        val threeDays = java.util.concurrent.TimeUnit.DAYS.toMillis(3)
        preferences.setHasCompletedOnboarding(true, 1_000L)
        preferences.clearAll()
        preferences.setHasCompletedOnboarding(true, 2_000L)
        org.junit.Assert.assertEquals(1_000L, store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(1_000L + threeDays - 1))
        org.junit.Assert.assertTrue(preferences.claimDiscordInvitation(1_000L + threeDays))
    }

    @Test fun `explicit dismissal is recorded separately from an unanswered shown invitation`() = runBlocking {
        preferences.setHasCompletedOnboarding(true, 1L)
        preferences.claimDiscordInvitation(Long.MAX_VALUE)
        org.junit.Assert.assertEquals(DiscordInvitationOutcome.SHOWN, preferences.discordInvitationOutcomeFlow.first())
        preferences.recordDiscordInvitationDismissed()
        org.junit.Assert.assertEquals(DiscordInvitationOutcome.DISMISSED, preferences.discordInvitationOutcomeFlow.first())
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
    }

    @Test fun `opening the permanent community link consumes the invitation and dismissal cannot downgrade it`() = runBlocking {
        org.junit.Assert.assertEquals(DiscordInvitationOutcome.NOT_SHOWN, preferences.discordInvitationOutcomeFlow.first())
        preferences.recordDiscordLinkOpened()
        preferences.setHasCompletedOnboarding(true, 1L)
        preferences.recordDiscordInvitationDismissed()
        org.junit.Assert.assertEquals(DiscordInvitationOutcome.LINK_OPENED, preferences.discordInvitationOutcomeFlow.first())
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
    }

    @Test fun `onboarding at epoch zero is a new completion not a legacy sentinel`() = runBlocking {
        preferences.setHasCompletedOnboarding(true, 0L)
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(0L))
        org.junit.Assert.assertTrue(preferences.claimDiscordInvitation(java.util.concurrent.TimeUnit.DAYS.toMillis(3)))
    }

    @Test fun `incomplete onboarding never migrates or consumes an invitation`() = runBlocking {
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
        org.junit.Assert.assertEquals(null, store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
        preferences.setHasCompletedOnboarding(false)
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(Long.MAX_VALUE))
        org.junit.Assert.assertEquals(DiscordInvitationOutcome.NOT_SHOWN, preferences.discordInvitationOutcomeFlow.first())
        preferences.setHasCompletedOnboarding(true, 1_000L)
        org.junit.Assert.assertFalse(preferences.claimDiscordInvitation(1_000L))
    }

    @Test fun `duplicate completion cannot restart the delay or make a legacy install fresh`() = runBlocking {
        preferences.setHasCompletedOnboarding(true, 1_000L)
        preferences.setHasCompletedOnboarding(true, 2_000L)
        org.junit.Assert.assertEquals(1_000L, store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
    }

    @Test fun `concurrent settings openings can consume the invitation only once`() = runBlocking {
        preferences.setHasCompletedOnboarding(true, 1L)
        val claims = List(8) { async { preferences.claimDiscordInvitation(Long.MAX_VALUE) } }
        org.junit.Assert.assertEquals(1, claims.map { it.await() }.count { it })
    }

    @Test fun `delay and all interaction outcomes survive reopening the actual datastore file`() = runBlocking {
        for (expected in DiscordInvitationOutcome.entries) {
            val file = folder.newFile("restart-${expected.name}.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val firstStore = PreferenceDataStoreFactory.create(scope = firstScope) { file }
            val first = PreferencesManager(RuntimeEnvironment.getApplication(), mockk<NoteDocuments>(relaxed = true), firstStore)
            try {
                first.setHasCompletedOnboarding(true, 1_000L)
                if (expected != DiscordInvitationOutcome.NOT_SHOWN) first.claimDiscordInvitation(Long.MAX_VALUE)
                if (expected == DiscordInvitationOutcome.DISMISSED) first.recordDiscordInvitationDismissed()
                if (expected == DiscordInvitationOutcome.LINK_OPENED) first.recordDiscordLinkOpened()
            } finally {
                firstScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
            }
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened = PreferenceDataStoreFactory.create(scope = secondScope) { file }
                val second = PreferencesManager(RuntimeEnvironment.getApplication(), mockk<NoteDocuments>(relaxed = true), reopened)
                org.junit.Assert.assertEquals(expected, second.discordInvitationOutcomeFlow.first())
                org.junit.Assert.assertFalse(second.claimDiscordInvitation(1_000L))
                org.junit.Assert.assertEquals(expected == DiscordInvitationOutcome.NOT_SHOWN, second.claimDiscordInvitation(Long.MAX_VALUE))
            } finally {
                secondScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
            }
        }
    }

    @Test fun `onboarding completion durably records the start of the invitation delay`() = runBlocking {
        preferences.setHasCompletedOnboarding(true)
        assertNotNull(store.data.first()[longPreferencesKey("discord_invitation_onboarding_completed_at_ms")])
    }
}
