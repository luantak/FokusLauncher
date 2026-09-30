package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerShortcutSettingsTest {
    @Test
    fun `saved shortcuts remain in management settings until explicitly removed`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val action = AppShortcutAction("Chat", "Alice", ShortcutTarget.LauncherShortcut("chat", "alice"))
            val saved = MutableStateFlow(listOf(action))
            val repository = mockk<AppRepository>(relaxed = true)
            every { repository.getDrawerShortcuts() } returns saved
            val viewModel = SettingsViewModel(mockk<Context>(relaxed = true), repository,
                    mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            assertEquals(listOf(action), viewModel.uiState.value.drawerShortcuts)
            viewModel.removeDrawerShortcut(action)
            verify { repository.removeDrawerShortcut(action) }
            saved.value = emptyList()
            assertEquals(emptyList<AppShortcutAction>(), viewModel.uiState.value.drawerShortcuts)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
