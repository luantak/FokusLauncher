package com.lu4p.fokuslauncher.ui.home

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import app.cash.turbine.test
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import com.lu4p.fokuslauncher.utils.WallpaperHelper
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var wallpaperHelper: WallpaperHelper
    private val testDispatcher = StandardTestDispatcher()

    private val testFavorites = listOf(
        FavoriteApp(label = "Music", packageName = "com.lu4p.music", iconName = "music"),
        FavoriteApp(label = "Work", packageName = "com.lu4p.work", iconName = "work"),
        FavoriteApp(label = "Social", packageName = "com.lu4p.social", iconName = "chat")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        weatherRepository = mockk(relaxed = true)
        wallpaperHelper = mockk(relaxed = true)

        // Mock battery intent
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 75
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { context.registerReceiver(null, any()) } returns batteryIntent

        // Mock preferences
        every { preferencesManager.favoritesFlow } returns flowOf(testFavorites)
        every { preferencesManager.showWallpaperFlow } returns flowOf(false)
        every { preferencesManager.swipeLeftTargetFlow } returns flowOf<ShortcutTarget?>(null)
        every { preferencesManager.swipeRightTargetFlow } returns flowOf<ShortcutTarget?>(null)
        every { preferencesManager.rightSideShortcutsFlow } returns flowOf(emptyList<HomeShortcut>())
        coEvery { preferencesManager.ensureRightSideShortcutsInitialized() } returns Unit

        // Mock repository flows used by name resolution
        every { appRepository.getAllRenamedApps() } returns flowOf(emptyList<RenamedAppEntity>())
        every { appRepository.getInstalledApps() } returns emptyList()
        every { appRepository.getAllShortcutActions() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        context, appRepository, preferencesManager, weatherRepository, wallpaperHelper
    )

    @Test
    fun `initial state has battery percentage`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertEquals(75, state.batteryPercent)
    }

    @Test
    fun `initial state has formatted time`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.uiState.value
        assertTrue(state.currentTime.isNotEmpty())
    }

    @Test
    fun `initial state has formatted date`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.uiState.value
        assertTrue(state.currentDate.isNotEmpty())
    }

    @Test
    fun `favorites flow emits from preferences manager`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.favorites.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val favorites = expectMostRecentItem()
            assertEquals(3, favorites.size)
            assertEquals("Music", favorites[0].categoryLabel)
            assertEquals("com.lu4p.music", favorites[0].packageName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchApp delegates to repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.launchApp("com.lu4p.music")

        verify { appRepository.launchApp("com.lu4p.music") }
    }

    @Test
    fun `refreshBattery updates battery percentage`() = runTest {
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 50
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { context.registerReceiver(null, any()) } returns batteryIntent

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 30
        viewModel.refreshBattery()

        assertEquals(30, viewModel.uiState.value.batteryPercent)
    }

    @Test
    fun `battery handles missing intent gracefully`() = runTest {
        every { context.registerReceiver(null, any()) } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(0, viewModel.uiState.value.batteryPercent)
    }

    @Test
    fun `wallpaper setting is observed from preferences`() = runTest {
        every { preferencesManager.showWallpaperFlow } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showWallpaper)
    }

    @Test
    fun `wallpaper defaults to disabled`() = runTest {
        every { preferencesManager.showWallpaperFlow } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showWallpaper)
    }

    @Test
    fun `isDefaultLauncher is false when not the default home app`() = runTest {
        // With relaxed mocks, resolveActivity returns a mock whose packageName
        // won't match our package, so isDefaultLauncher should be false.
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.isDefaultLauncher)
    }
}
