package com.lu4p.fokuslauncher.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.BatteryManager
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.HomeAlignment
import com.lu4p.fokuslauncher.data.model.HomeShortcut
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var weatherRepository: WeatherRepository
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

        // Mock battery intent
        val batteryIntent = mockk<Intent>(relaxed = true)
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 75
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { context.registerReceiver(null, any()) } returns batteryIntent

        // Mock preferences using Fake
        preferencesManager = mockk(relaxed = true)
        every { preferencesManager.favoritesFlow } returns flowOf(testFavorites)
        every { preferencesManager.showWallpaperFlow } returns flowOf(false)
        every { preferencesManager.swipeLeftTargetFlow } returns flowOf(null as ShortcutTarget?)
        every { preferencesManager.swipeRightTargetFlow } returns flowOf(null as ShortcutTarget?)
        every { preferencesManager.rightSideShortcutsFlow } returns flowOf(emptyList<HomeShortcut>())
        every { preferencesManager.preferredWeatherAppFlow } returns flowOf("")
        every { preferencesManager.weatherLocationOptedOutFlow } returns flowOf(false)
        every { preferencesManager.homeAlignmentFlow } returns flowOf(HomeAlignment.LEFT)
        coEvery { preferencesManager.ensureRightSideShortcutsInitialized() } returns Unit
        coEvery { preferencesManager.setFavorites(any()) } returns Unit

        // Mock repository flows used by name resolution
        every { appRepository.getAllRenamedApps() } returns flowOf(emptyList())
        every { appRepository.getInstalledApps() } returns emptyList()
        every { appRepository.getAllShortcutActions() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        context, appRepository, preferencesManager, weatherRepository
    )

    private fun createViewModel(withContext: Context) = HomeViewModel(
        withContext, appRepository, preferencesManager, weatherRepository
    )

    @Test
    fun `initial state has battery percentage`() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        val state = viewModel.uiState.value
        assertEquals(75, state.batteryPercent)
    }

    @Test
    fun `initial state has formatted time`() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.uiState.value
        assertTrue(state.currentTime.isNotEmpty())
    }

    @Test
    fun `refreshBattery handles invalid battery intent gracefully`() {
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED)
        // Missing EXTRAS will cause getIntExtra to return default (-1)
        every { context.registerReceiver(null, any()) } returns batteryIntent

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(0, viewModel.uiState.value.batteryPercent)
    }

    @Test
    fun `checkDefaultLauncher handles exception and sets to false`() {
        every { context.packageManager } throws RuntimeException("Package manager crashed")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `initial state has formatted date`() {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(1100)

        val state = viewModel.uiState.value
        assertTrue(state.currentDate.isNotEmpty())
    }

    @Test
    fun `favorites flow emits from preferences manager`() {
        val viewModel = createViewModel()
        val collected = mutableListOf<List<FavoriteApp>>()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { collected += it }
        }
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()

        val favorites = collected.lastOrNull().orEmpty()
        assertEquals(3, favorites.size)
        assertEquals("Music", favorites[0].categoryLabel)
        assertEquals("com.lu4p.music", favorites[0].packageName)
        collectJob.cancel()
    }

    @Test
    fun `launchApp delegates to repository`() {
        val viewModel = createViewModel()

        viewModel.launchApp("com.lu4p.music")

        verify { appRepository.launchApp("com.lu4p.music") }
    }

    @Test
    fun `refreshBattery updates battery percentage`() {
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
    fun `battery handles missing intent gracefully`() {
        every { context.registerReceiver(null, any()) } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(0, viewModel.uiState.value.batteryPercent)
    }

    @Test
    fun `initial state reads battery from sticky system broadcast`() {
        val realContext = RuntimeEnvironment.getApplication().applicationContext as Context
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 42)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
        }
        @Suppress("DEPRECATION")
        realContext.sendStickyBroadcast(batteryIntent)

        val viewModel = createViewModel(realContext)
        testDispatcher.scheduler.advanceTimeBy(100)

        assertEquals(42, viewModel.uiState.value.batteryPercent)
    }

    @Test
    fun `wallpaper setting is observed from preferences`() {
        every { preferencesManager.showWallpaperFlow } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(200)

        assertTrue(viewModel.uiState.value.showWallpaper)
    }

    @Test
    fun `wallpaper defaults to disabled`() {
        every { preferencesManager.showWallpaperFlow } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(200)

        assertFalse(viewModel.uiState.value.showWallpaper)
    }

    @Test
    fun `isDefaultLauncher is false when not the default home app`() {
        // With relaxed mocks, resolveActivity returns a mock whose packageName
        // won't match our package, so isDefaultLauncher should be false.
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceTimeBy(100)

        assertFalse(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `isDefaultLauncher is true when package manager resolves home to app package`() {
        val realContext = RuntimeEnvironment.getApplication().applicationContext as Context
        val packageManager = mockk<PackageManager>(relaxed = true)
        val wrappedContext = object : ContextWrapper(realContext) {
            override fun getPackageManager(): PackageManager = packageManager
            override fun getPackageName(): String = "io.github.luantak.fokuslauncher"
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "io.github.luantak.fokuslauncher"
                name = "io.github.luantak.fokuslauncher.MainActivity"
            }
        }
        every {
            packageManager.resolveActivity(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns resolveInfo

        val viewModel = createViewModel(wrappedContext)
        testDispatcher.scheduler.advanceTimeBy(100)

        assertTrue(viewModel.uiState.value.isDefaultLauncher)
    }

    @Test
    fun `refreshInstalledApps removes uninstalled favorites`() {
        every { appRepository.getInstalledApps() } returns listOf(
            AppInfo(packageName = "com.lu4p.music", label = "Music", icon = null)
        )
        val viewModel = createViewModel()
        val collectJob = CoroutineScope(testDispatcher).launch {
            viewModel.favorites.collect { }
        }
        testDispatcher.scheduler.runCurrent()

        viewModel.refreshInstalledApps()
        verify(timeout = 2_000) { appRepository.invalidateCache() }
        coVerify(timeout = 2_000) {
            preferencesManager.setFavorites(
                match { favorites ->
                    favorites.size == 1 && favorites[0].packageName == "com.lu4p.music"
                }
            )
        }
        collectJob.cancel()
    }

    @Test
    fun `openClockApp launches clock safely`() {
        val viewModel = createViewModel()
        viewModel.openClockApp()
        
        // Either AlarmClock or DeskClock gets started. Since mock is relaxed, it doesn't crash.
        verify(atLeast = 1) { context.startActivity(any()) }
    }

    @Test
    fun `launchShortcut handles App target`() {
        val viewModel = createViewModel()
        viewModel.launchShortcut(ShortcutTarget.App("com.lu4p.music"))
        
        verify { appRepository.launchApp("com.lu4p.music") }
    }
}
