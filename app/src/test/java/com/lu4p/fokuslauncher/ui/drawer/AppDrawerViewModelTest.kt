package com.lu4p.fokuslauncher.ui.drawer

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.DrawerAppSortMode
import com.lu4p.fokuslauncher.data.model.FavoriteApp
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {

    private lateinit var context: Context
    private lateinit var appRepository: AppRepository
    private lateinit var privateSpaceManager: PrivateSpaceManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: AppDrawerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val hiddenFlow = MutableStateFlow<List<String>>(emptyList())
    private val renamedFlow = MutableStateFlow<List<RenamedAppEntity>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    private val categoryDefinitionsFlow = MutableStateFlow<List<AppCategoryDefinitionEntity>>(emptyList())
    private val favoritesFlow = MutableStateFlow<List<FavoriteApp>>(emptyList())
    private val autoOpenDrawerKeyboardFlow = MutableStateFlow(true)
    private val drawerAppSortModeFlow = MutableStateFlow(DrawerAppSortMode.ALPHABETICAL)
    private val drawerAppOpenCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val privateProfileChanges = MutableSharedFlow<Unit>()
    private val installedAppsVersion = MutableStateFlow(0L)
    private var installedApps: List<AppInfo> = emptyList()

    private val testApps =
            listOf(
                    AppInfo("com.lu4p.atom", "Atom", null),
                    AppInfo("com.lu4p.calculator", "Calculator", null),
                    AppInfo("com.lu4p.calendar", "Calendar", null),
                    AppInfo("com.lu4p.camera", "Camera", null),
                    AppInfo("com.lu4p.chrome", "Chrome", null),
                    AppInfo("com.lu4p.gmail", "Gmail", null, category = "Productivity"),
                    AppInfo("com.lu4p.bank", "Bank", null, category = "Finance"),
                    AppInfo("com.lu4p.maps", "Maps", null),
                    AppInfo("com.lu4p.twitter", "Twitter", null, category = "Social")
            )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        every { context.getSystemService(Context.USER_SERVICE) } returns null
        every { context.getString(R.string.drawer_section_personal) } returns "Personal"
        every { context.getString(R.string.drawer_section_other_profile) } returns "Other profile"
        every { context.getString(R.string.drawer_section_work_profile) } returns "Work profile"
        every { context.getString(R.string.drawer_section_clone_profile) } returns "Parallel apps"
        every { context.getString(R.string.drawer_section_profile_numbered, any()) } answers {
            "Profile ${invocation.args[1]}"
        }
        appRepository = mockk(relaxed = true)
        privateSpaceManager = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        installedAppsVersion.value = 0L
        every { appRepository.getInstalledAppsVersion() } returns installedAppsVersion.asStateFlow()
        every { appRepository.invalidateCache() } answers { installedAppsVersion.value += 1L }
        installedApps = testApps
        every { appRepository.getInstalledApps() } answers { installedApps }
        every { appRepository.getHiddenPackageNames() } returns hiddenFlow
        every { appRepository.getAllRenamedApps() } returns renamedFlow
        every { appRepository.getAllAppCategories() } returns categoriesFlow
        every { appRepository.getAllCategoryDefinitions() } returns categoryDefinitionsFlow
        every { appRepository.launchApp(any()) } returns true
        every { preferencesManager.favoritesFlow } returns favoritesFlow
        every { preferencesManager.autoOpenDrawerKeyboardFlow } returns autoOpenDrawerKeyboardFlow
        every { preferencesManager.drawerAppSortModeFlow } returns drawerAppSortModeFlow
        every { preferencesManager.drawerAppOpenCountsFlow } returns drawerAppOpenCountsFlow
        every { privateSpaceManager.isSupported } returns false
        every { privateSpaceManager.isPrivateSpaceUnlocked() } returns false
        every { privateSpaceManager.launchApp(any(), any()) } returns true
        every { privateSpaceManager.profileStateChanged } returns privateProfileChanges
        viewModel =
                AppDrawerViewModel(
                        context,
                        appRepository,
                        privateSpaceManager,
                        preferencesManager,
                        Dispatchers.Unconfined
                )
        awaitState("apps to load") { it.allApps.isNotEmpty() }
    }

    private fun flatFiltered(state: AppDrawerUiState): List<AppInfo> =
            state.filteredProfileSections.flatMap { it.apps }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun awaitState(
        description: String,
        predicate: (AppDrawerUiState) -> Boolean
    ) {
        val timeoutAt = System.currentTimeMillis() + 1500
        while (System.currentTimeMillis() < timeoutAt) {
            if (predicate(viewModel.uiState.value)) return
            Thread.sleep(10)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    @Test
    fun `initial state loads all apps`() {
        val state = viewModel.uiState.value

        assertEquals(testApps.size, state.allApps.size)
        assertEquals(testApps.size, flatFiltered(state).size)
        assertEquals("", state.searchQuery)
        assertEquals("All apps", state.selectedCategory)
    }

    @Test
    fun `apps are loaded from repository on init`() {
        verify { appRepository.getInstalledApps() }
    }

    @Test
    fun `search query filters apps by label`() {
        viewModel.onSearchQueryChanged("cal")

        val state = viewModel.uiState.value
        assertEquals(2, flatFiltered(state).size)
        assertTrue(flatFiltered(state).any { it.label == "Calculator" })
        assertTrue(flatFiltered(state).any { it.label == "Calendar" })
    }

    @Test
    fun `disabling auto open keyboard keeps search available`() {
        viewModel.onSearchQueryChanged("cal")

        autoOpenDrawerKeyboardFlow.value = false
        awaitState("drawer keyboard auto open to be disabled") { !it.autoOpenKeyboard }

        val state = viewModel.uiState.value
        assertFalse(state.autoOpenKeyboard)
        assertEquals("cal", state.searchQuery)
        assertEquals(2, flatFiltered(state).size)
    }

    @Test
    fun `search is case insensitive`() {
        viewModel.onSearchQueryChanged("CHROME")

        // Single result triggers auto-launch, search resets
        verify { appRepository.launchApp("com.lu4p.chrome") }
    }

    @Test
    fun `single search result auto-launches the app`() {
        viewModel.onSearchQueryChanged("Atom")

        // Atom is the only match, should be auto-launched
        verify { appRepository.launchApp("com.lu4p.atom") }
        // Search should be cleared after auto-launch
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `single search result does not reset query when launch fails`() {
        every { appRepository.launchApp("com.lu4p.atom") } returns false

        viewModel.onSearchQueryChanged("Atom")

        assertEquals("Atom", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `empty search query shows all apps`() {
        viewModel.onSearchQueryChanged("cal")
        viewModel.onSearchQueryChanged("")

        val state = viewModel.uiState.value
        assertEquals(testApps.size, flatFiltered(state).size)
    }

    @Test
    fun `category selection filters apps`() {
        viewModel.onCategorySelected("Social")

        val state = viewModel.uiState.value
        assertEquals("Social", state.selectedCategory)
    }

    @Test
    fun `All apps category shows all apps`() {
        viewModel.onCategorySelected("Social")
        viewModel.onCategorySelected("All apps")

        val state = viewModel.uiState.value
        assertEquals(testApps.size, flatFiltered(state).size)
    }

    @Test
    fun `resetSearchState resets selected category to All apps`() {
        viewModel.onCategorySelected("Social")
        viewModel.onSearchQueryChanged("tw")
        viewModel.resetSearchState()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals("All apps", state.selectedCategory)
        assertEquals(testApps.size, flatFiltered(state).size)
    }

    @Test
    fun `resetSearchStateIfNeeded is no-op when already at default category and blank search`() {
        val before = viewModel.uiState.value
        viewModel.resetSearchStateIfNeeded()
        assertSame(before, viewModel.uiState.value)
    }

    @Test
    fun `resetSearchStateIfNeeded matches resetSearchState when dirty`() {
        viewModel.onCategorySelected("Social")
        viewModel.onSearchQueryChanged("tw")

        viewModel.resetSearchStateIfNeeded()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals("All apps", state.selectedCategory)
        assertEquals(testApps.size, flatFiltered(state).size)
    }

    @Test
    fun `search and category filters work together`() {
        viewModel.onCategorySelected("Productivity")
        viewModel.onSearchQueryChanged("gm")

        // Gmail is only result, auto-launched
        verify { appRepository.launchApp("com.lu4p.gmail") }
    }

    @Test
    fun `launchApp delegates to repository`() {
        viewModel.launchApp("com.lu4p.chrome")

        verify { appRepository.launchApp("com.lu4p.chrome") }
    }

    @Test
    fun `launchTarget main app delegates to repository`() {
        viewModel.launchTarget(LaunchTarget.MainApp("com.lu4p.maps"))

        verify { appRepository.launchApp("com.lu4p.maps") }
    }

    @Test
    fun `launchTarget private app delegates to private space manager`() {
        val component = ComponentName("com.private.app", "MainActivity")
        val userHandle = mockk<UserHandle>(relaxed = true)

        viewModel.launchTarget(
                LaunchTarget.PrivateApp(
                        packageName = "com.private.app",
                        componentName = component,
                        userHandle = userHandle
                )
        )

        verify { privateSpaceManager.launchApp(any(), any()) }
        verify(exactly = 0) { appRepository.launchApp("com.private.app") }
    }

    @Test
    fun `refresh invalidates cache and reloads`() {
        viewModel.refresh()

        verify { appRepository.invalidateCache() }
        verify(atLeast = 2) { appRepository.getInstalledApps() }
    }

    @Test
    fun `refresh updates state with newly installed apps`() {
        installedApps = testApps + AppInfo("com.lu4p.newapp", "New App", null)

        viewModel.refresh()
        awaitState("new app to appear after refresh") { state ->
            state.allApps.any { it.packageName == "com.lu4p.newapp" }
        }

        assertTrue(viewModel.uiState.value.allApps.any { it.packageName == "com.lu4p.newapp" })
    }

    @Test
    fun `categories list contains expected defaults`() {
        val state = viewModel.uiState.value

        assertTrue(state.categories.contains("All apps"))
        assertTrue(state.categories.contains("Productivity"))
        assertTrue(state.categories.contains("Finance"))
        assertTrue(state.categories.contains("Social"))
    }

    @Test
    fun `search with no matches returns empty list`() {
        viewModel.onSearchQueryChanged("zzzznonexistent")

        val state = viewModel.uiState.value
        assertTrue(flatFiltered(state).isEmpty())
    }

    @Test
    fun `tryLaunchFirstSearchResult launches first visible match when several match`() {
        viewModel.onSearchQueryChanged("cal")

        assertEquals(2, flatFiltered(viewModel.uiState.value).size)
        assertTrue(viewModel.tryLaunchFirstSearchResult())
        verify { appRepository.launchApp("com.lu4p.calculator") }
    }

    @Test
    fun `tryLaunchFirstSearchResult is no-op in browse mode`() {
        viewModel.onSearchQueryChanged(" cal")
        assertTrue(flatFiltered(viewModel.uiState.value).isNotEmpty())
        assertFalse(viewModel.tryLaunchFirstSearchResult())
        verify(exactly = 0) { appRepository.launchApp(any()) }
    }

    @Test
    fun `tryLaunchFirstSearchResult is no-op with blank query`() {
        assertFalse(viewModel.tryLaunchFirstSearchResult())
        verify(exactly = 0) { appRepository.launchApp(any()) }
    }

    @Test
    fun `tryLaunchFirstSearchResult returns false when launch fails`() {
        viewModel.onSearchQueryChanged("cal")
        every { appRepository.launchApp(any()) } returns false
        assertFalse(viewModel.tryLaunchFirstSearchResult())
        every { appRepository.launchApp(any()) } returns true
    }

    // --- Long-press / action sheet tests ---

    @Test
    fun `onAppLongPress sets selectedApp`() {
        val app = testApps[0]
        viewModel.onAppLongPress(app)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedApp)
        assertEquals(app.packageName, state.selectedApp!!.packageName)
    }

    @Test
    fun `dismissActionSheet clears selectedApp`() {
        viewModel.onAppLongPress(testApps[0])
        viewModel.dismissActionSheet()

        val state = viewModel.uiState.value
        assertNull(state.selectedApp)
    }

    @Test
    fun `hideApp calls repository`() {
        val app = testApps[0]
        viewModel.hideApp(app)

        coVerify { appRepository.hideApp(app.packageName) }
    }

    @Test
    fun `hidden apps are filtered from visible list`() {
        // Simulate hiding an app via the Flow
        hiddenFlow.value = listOf("com.lu4p.atom")
        awaitState("hidden app to be removed") { state ->
            state.allApps.none { it.packageName == "com.lu4p.atom" } &&
                flatFiltered(state).none { it.packageName == "com.lu4p.atom" }
        }

        val state = viewModel.uiState.value
        assertFalse(state.allApps.any { it.packageName == "com.lu4p.atom" })
        assertFalse(flatFiltered(state).any { it.packageName == "com.lu4p.atom" })
    }

    @Test
    fun `renamed apps show custom names in list`() {
        renamedFlow.value = listOf(RenamedAppEntity("com.lu4p.chrome", "My Browser"))
        awaitState("rename to be reflected") { state ->
            state.allApps.any {
                it.packageName == "com.lu4p.chrome" && it.label == "My Browser"
            }
        }

        val state = viewModel.uiState.value
        val chrome = state.allApps.find { it.packageName == "com.lu4p.chrome" }
        assertNotNull(chrome)
        assertEquals("My Browser", chrome!!.label)
    }

    @Test
    fun `renameApp calls repository`() {
        viewModel.renameApp("com.lu4p.atom", "My Atom")

        coVerify { appRepository.renameApp("com.lu4p.atom", "My Atom") }
    }

    // --- Menu tests ---

    @Test
    fun `toggleMenu toggles showMenu state`() {
        assertFalse(viewModel.uiState.value.showMenu)

        viewModel.toggleMenu()
        assertTrue(viewModel.uiState.value.showMenu)

        viewModel.toggleMenu()
        assertFalse(viewModel.uiState.value.showMenu)
    }

    @Test
    fun `dismissMenu sets showMenu to false`() {
        viewModel.toggleMenu()
        viewModel.dismissMenu()

        assertFalse(viewModel.uiState.value.showMenu)
    }

    // --- Private Space tests ---

    @Test
    fun `private space not supported by default on test JVM`() {
        assertFalse(viewModel.uiState.value.isPrivateSpaceSupported)
    }

    @Test
    fun `refreshPrivateSpaceState reads from manager`() {
        every { privateSpaceManager.isSupported } returns true
        every { privateSpaceManager.isPrivateSpaceUnlocked() } returns true
        every { privateSpaceManager.getPrivateSpaceApps() } returns
                listOf(AppInfo("com.private.app", "Private App", null))

        viewModel.refreshPrivateSpaceState()

        val state = viewModel.uiState.value
        assertTrue(state.isPrivateSpaceSupported)
        assertTrue(state.isPrivateSpaceUnlocked)
        assertEquals(1, state.privateSpaceApps.size)
    }

    @Test
    fun `togglePrivateSpace when unlocked clears private apps`() {
        every { privateSpaceManager.isSupported } returns true
        every { privateSpaceManager.isPrivateSpaceUnlocked() } returns true
        every { privateSpaceManager.getPrivateSpaceApps() } returns
                listOf(AppInfo("com.private.app", "Private App", null))
        viewModel.refreshPrivateSpaceState()

        viewModel.toggleMenu()
        viewModel.togglePrivateSpace()

        val state = viewModel.uiState.value
        assertFalse(state.isPrivateSpaceUnlocked)
        assertTrue(state.privateSpaceApps.isEmpty())
        assertFalse(state.showMenu)
    }
}
