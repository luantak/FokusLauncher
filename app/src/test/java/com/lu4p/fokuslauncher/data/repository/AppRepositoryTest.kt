package com.lu4p.fokuslauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import com.lu4p.fokuslauncher.data.model.AddCategoryResult
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.SystemCategoryKeys
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.containsNormalizedSearch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var appDao: AppDao
    private lateinit var privateSpaceManager: PrivateSpaceManager
    private lateinit var launcherApps: LauncherApps
    private lateinit var userManager: UserManager
    private lateinit var repository: AppRepository

    private val myUser: UserHandle = Process.myUserHandle()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        appDao = mockk(relaxed = true)
        privateSpaceManager = mockk(relaxed = true)
        launcherApps = mockk(relaxed = true)
        userManager = mockk(relaxed = true)

        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.lu4p.fokuslauncher"
        every { packageManager.getUserBadgedLabel(any(), any()) } answers { firstArg<CharSequence>() }
        every { privateSpaceManager.isPrivateSpaceProfile(any()) } returns false
        every { context.getSystemService(Context.LAUNCHER_APPS_SERVICE) } returns launcherApps
        every { context.getSystemService(Context.USER_SERVICE) } returns userManager
        every { userManager.userProfiles } returns listOf(myUser)
        every { launcherApps.getActivityList(null, myUser) } returns emptyList()

        every { context.getString(R.string.inferred_category_utilities) } returns "Utilities"
        every { context.getString(R.string.inferred_category_games) } returns "Games"
        every { context.getString(R.string.inferred_category_productivity) } returns "Productivity"
        every { context.getString(R.string.inferred_category_social) } returns "Social"
        every { context.getString(R.string.inferred_category_media) } returns "Media"
        every { context.getString(R.string.shortcut_generic_label) } returns "Shortcut"

        repository = AppRepository(context, appDao, privateSpaceManager)
    }

    // --- App Loading Tests ---

    @Test
    fun `getInstalledApps returns sorted list excluding launcher itself`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity("com.lu4p.chrome", "Chrome"),
                        createMockLauncherActivity("com.lu4p.fokuslauncher", "Fokus Launcher"),
                        createMockLauncherActivity("com.lu4p.calculator", "Calculator"),
                        createMockLauncherActivity("com.lu4p.atom", "Atom")
                )

        val result = repository.getInstalledApps()

        assertEquals(3, result.size)
        assertEquals("Atom", result[0].label)
        assertEquals("Calculator", result[1].label)
        assertEquals("Chrome", result[2].label)
        assertTrue(result.none { it.packageName == "com.lu4p.fokuslauncher" })
    }

    @Test
    fun `getInstalledApps caches results on subsequent calls`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.app1", "App 1"))

        repository.getInstalledApps()
        repository.getInstalledApps()

        verify(exactly = 1) { launcherApps.getActivityList(null, myUser) }
    }

    @Test
    fun `getInstalledApps does not cache empty results`() {
        every { launcherApps.getActivityList(null, myUser) } returns emptyList()

        repository.getInstalledApps()
        repository.getInstalledApps()

        verify(exactly = 2) { launcherApps.getActivityList(null, myUser) }
    }

    @Test
    fun `invalidateCache forces reload on next call`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.app1", "App 1"))

        repository.getInstalledApps()
        repository.invalidateCache()
        repository.getInstalledApps()

        verify(exactly = 2) { launcherApps.getActivityList(null, myUser) }
    }

    @Test
    fun `installed apps list matches drawer-style normalized label search`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity("com.lu4p.chrome", "Chrome"),
                        createMockLauncherActivity("com.lu4p.calendar", "Calendar"),
                        createMockLauncherActivity("com.lu4p.camera", "Camera")
                )

        val all = repository.getInstalledApps()
        val result =
                all.filter { it.label.containsNormalizedSearch("ca") }.sortedBy {
                    it.label.lowercase()
                }

        assertEquals(2, result.size)
        assertEquals("Calendar", result[0].label)
        assertEquals("Camera", result[1].label)
    }

    @Test
    fun `normalized label search is accent-insensitive on installed apps`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.camera", "Càmera"))

        val result =
                repository.getInstalledApps().filter { it.label.containsNormalizedSearch("cam") }

        assertEquals(1, result.size)
        assertEquals("Càmera", result[0].label)
    }

    @Test
    fun `blank search needle matches all installed apps`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity("com.lu4p.app1", "App 1"),
                        createMockLauncherActivity("com.lu4p.app2", "App 2")
                )

        val all = repository.getInstalledApps()
        val result = all.filter { it.label.containsNormalizedSearch("") }

        assertEquals(2, result.size)
    }

    @Test
    fun `launchApp returns true when intent is found`() {
        val launchIntent = mockk<Intent>(relaxed = true)
        every { packageManager.getLaunchIntentForPackage("com.lu4p.app1") } returns launchIntent

        val result = repository.launchApp("com.lu4p.app1")

        assertTrue(result)
        verify { context.startActivity(launchIntent, null) }
    }

    @Test
    fun `launchApp returns false when no intent found`() {
        val realContext = RuntimeEnvironment.getApplication().applicationContext as Context
        val realRepository = AppRepository(realContext, appDao, PrivateSpaceManager(realContext))

        val result = realRepository.launchApp("com.lu4p.nonexistent")

        assertFalse(result)
    }

    @Test
    fun `getInstalledApps deduplicates by package name on primary profile`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity("com.lu4p.app1", "App 1"),
                        createMockLauncherActivity("com.lu4p.app1", "App 1 Duplicate")
                )

        val result = repository.getInstalledApps()

        assertEquals(1, result.size)
    }

    @Test
    fun `getInstalledApps keeps same package from clone profile as separate entry`() {
        val cloneUser = mockk<UserHandle>(relaxed = true)
        every { userManager.userProfiles } returns listOf(myUser, cloneUser)
        every { privateSpaceManager.isPrivateSpaceProfile(cloneUser) } returns false
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.whatsapp", "WhatsApp"))
        val cloneActivity = createMockLauncherActivity("com.lu4p.whatsapp", "WhatsApp")
        every { launcherApps.getActivityList(null, cloneUser) } returns listOf(cloneActivity)

        val result = repository.getInstalledApps()

        assertEquals(2, result.size)
        assertEquals(1, result.count { it.userHandle == null })
        assertEquals(1, result.count { it.userHandle == cloneUser })
        assertNotNull(result.find { it.userHandle == cloneUser }?.componentName)
        assertEquals("WhatsApp", result.find { it.userHandle == null }?.label)
        assertEquals("WhatsApp", result.find { it.userHandle == cloneUser }?.label)
    }

    @Test
    fun `getInstalledApps aligns managed profile labels with personal copy`() {
        val workUser = mockk<UserHandle>(relaxed = true)
        every { userManager.userProfiles } returns listOf(myUser, workUser)
        every { privateSpaceManager.isPrivateSpaceProfile(workUser) } returns false
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.slack", "Slack"))
        every {
            launcherApps.getActivityList(null, workUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.slack", "Slack"))

        val result = repository.getInstalledApps()

        assertEquals("Slack", result.find { it.userHandle == null }?.label)
        assertEquals("Slack", result.find { it.userHandle == workUser }?.label)
    }

    @Test
    @Suppress("ReplaceCallWithBinaryOperator")
    fun `getInstalledApps strips leading Work prefix when primary has no label for package`() {
        val workUser = mockk<UserHandle>(relaxed = true)
        every { workUser.equals(any()) } answers { firstArg<Any?>() === workUser }
        every { userManager.userProfiles } returns listOf(workUser)
        every { privateSpaceManager.isPrivateSpaceProfile(workUser) } returns false
        every {
            launcherApps.getActivityList(null, workUser)
        } returns listOf(createMockLauncherActivity("com.whatsapp", "Work WhatsApp"))

        val result = repository.getInstalledApps()

        assertEquals(1, result.size)
        assertEquals("WhatsApp", result.single().label)
        assertEquals(workUser, result.single().userHandle)
    }

    @Test
    fun `getInstalledApps does not add clone suffix when multiple secondary profiles exist`() {
        val userA = mockk<UserHandle>(relaxed = true)
        val userB = mockk<UserHandle>(relaxed = true)
        every { userManager.userProfiles } returns listOf(myUser, userA, userB)
        every { privateSpaceManager.isPrivateSpaceProfile(any()) } returns false
        every {
            launcherApps.getActivityList(null, myUser)
        } returns listOf(createMockLauncherActivity("com.lu4p.app", "App"))
        every {
            launcherApps.getActivityList(null, userA)
        } returns listOf(createMockLauncherActivity("com.lu4p.onlya", "Only A"))
        every {
            launcherApps.getActivityList(null, userB)
        } returns listOf(createMockLauncherActivity("com.lu4p.onlyb", "Only B"))

        val result = repository.getInstalledApps()

        assertEquals("Only A", result.find { it.packageName == "com.lu4p.onlya" }!!.label)
        assertEquals("Only B", result.find { it.packageName == "com.lu4p.onlyb" }!!.label)
    }

    @Test
    fun `getInstalledApps uses Android app category for games`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity(
                                packageName = "com.lu4p.runner",
                                label = "Runner",
                                category = ApplicationInfo.CATEGORY_GAME
                        )
                )

        val result = repository.getInstalledApps()

        assertEquals("Games", result.single().category)
    }

    @Test
    fun `getInstalledApps falls back to Utilities when no system category exists`() {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity(
                                packageName = "com.supercell.clashofclans",
                                label = "Clash of Clans"
                        )
                )

        val result = repository.getInstalledApps()

        assertEquals("Utilities", result.single().category)
    }

    @Test
    fun `getInstalledApps falls back to legacy query when LauncherApps missing`() {
        every { context.getSystemService(Context.LAUNCHER_APPS_SERVICE) } returns null
        val legacyRepo = AppRepository(context, appDao, privateSpaceManager)

        val resolveInfos =
                listOf(
                        createResolveInfo("com.lu4p.chrome", "Chrome"),
                        createResolveInfo("com.lu4p.fokuslauncher", "Fokus Launcher")
                )
        every { packageManager.queryIntentActivities(any<Intent>(), any<Int>()) } returns
                resolveInfos

        val result = legacyRepo.getInstalledApps()

        assertEquals(1, result.size)
        assertEquals("Chrome", result.single().label)
    }

    // --- Hidden Apps Tests ---

    @Test
    fun `hideApp delegates to DAO`() = runTest {
        repository.hideApp("com.lu4p.app1", "0")

        coVerify { appDao.hideApp(HiddenAppEntity("com.lu4p.app1", "0")) }
    }

    @Test
    fun `unhideApp delegates to DAO`() = runTest {
        repository.unhideApp("com.lu4p.app1", "0")

        coVerify { appDao.unhideApp(HiddenAppEntity("com.lu4p.app1", "0")) }
    }

    @Test
    fun `getHiddenApps returns flow from DAO`() {
        every { appDao.getHiddenApps() } returns
            flowOf(listOf(HiddenAppEntity("com.lu4p.hidden", "0")))

        val result = repository.getHiddenApps()

        // Flow is returned directly from DAO
        assertEquals(appDao.getHiddenApps(), result)
    }

    // --- Renamed Apps Tests ---

    @Test
    fun `renameApp delegates to DAO`() = runTest {
        repository.renameApp("com.lu4p.app1", "0", "My Custom Name")

        coVerify {
            appDao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "My Custom Name"))
        }
    }

    // --- Category Tests ---

    @Test
    fun `setAppCategory delegates to DAO`() = runTest {
        repository.setAppCategory("com.lu4p.app1", "0", "Productivity")

        coVerify {
            appDao.setAppCategory(
                    AppCategoryEntity(
                            "com.lu4p.app1",
                            "0",
                            "Productivity"
                    )
            )
        }
    }

    @Test
    fun `setAppCategory normalizes localized inferred category names`() = runTest {
        val realContext = RuntimeEnvironment.getApplication().applicationContext as Context
        val realRepository = AppRepository(realContext, appDao, PrivateSpaceManager(realContext))

        realRepository.setAppCategory("com.lu4p.app1", "0", "Produktivität")

        coVerify {
            appDao.setAppCategory(
                    AppCategoryEntity(
                            "com.lu4p.app1",
                            "0",
                            "Productivity"
                    )
            )
        }
    }

    @Test
    fun `getAllAppCategories normalizes legacy localized inferred categories`() = runTest {
        val realContext = RuntimeEnvironment.getApplication().applicationContext as Context
        every { appDao.getAllAppCategories() } returns
                flowOf(listOf(AppCategoryEntity("com.lu4p.app1", "0", "Spiele")))
        val realRepository = AppRepository(realContext, appDao, PrivateSpaceManager(realContext))

        val result = realRepository.getAllAppCategories().first()

        assertEquals("Games", result.single().category)
    }

    @Test
    fun `addCategoryDefinition rejects reserved category names`() = runTest {
        assertEquals(
                AddCategoryResult.Failure.ReservedAllApps,
                repository.addCategoryDefinition("All apps")
        )
        assertEquals(
                AddCategoryResult.Failure.ReservedPrivate,
                repository.addCategoryDefinition("Private")
        )
        assertEquals(AddCategoryResult.Failure.Blank, repository.addCategoryDefinition("   "))

        coVerify(exactly = 0) { appDao.upsertCategoryDefinition(any()) }
    }

    @Test
    fun `clearAllAppData clears tables and restores default category definitions`() = runTest {
        val expectedDefaults =
                SystemCategoryKeys.defaultOrderedCategoryNames().mapIndexed { index, name ->
                    AppCategoryDefinitionEntity(name, index)
                }

        repository.clearAllAppData()

        coVerify { appDao.resetAllAppData(expectedDefaults) }
    }

    @Test
    fun `addCategoryDefinition stores normalized category name`() = runTest {
        every { appDao.getAllCategoryDefinitions() } returns flowOf(emptyList())
        coEvery { appDao.getMaxCategoryDefinitionPosition() } returns 3

        val result = repository.addCategoryDefinition("  My Category  ")

        assertEquals(AddCategoryResult.Success, result)
        coVerify { appDao.upsertCategoryDefinition(AppCategoryDefinitionEntity("My Category", 4)) }
    }

    @Test
    fun `addCategoryDefinition returns duplicate when name exists`() = runTest {
        every { appDao.getAllCategoryDefinitions() } returns
                flowOf(listOf(AppCategoryDefinitionEntity("Games", 0)))
        coEvery { appDao.getMaxCategoryDefinitionPosition() } returns 0

        assertEquals(
                AddCategoryResult.Failure.Duplicate("Games"),
                repository.addCategoryDefinition("Games")
        )
        coVerify(exactly = 0) { appDao.upsertCategoryDefinition(any()) }
    }

    @Test
    fun `renameCategory ignores reserved category names`() = runTest {
        repository.renameCategory("All apps", "Work")
        repository.renameCategory("Work", "Private")

        coVerify(exactly = 0) { appDao.renameCategoryAssignments(any(), any()) }
    }

    @Test
    fun `deleteCategory ignores reserved category names`() = runTest {
        repository.deleteCategory("All apps")
        repository.deleteCategory("Private")

        coVerify(exactly = 0) { appDao.deleteCategoryWithAppResets(any(), any()) }
    }

    @Test
    fun `deleteCategory clears apps that only have system inferred category`() = runTest {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity(
                                "com.lu4p.game",
                                "Game",
                                ApplicationInfo.CATEGORY_GAME
                        )
                )
        every { appDao.getAllAppCategories() } returns flowOf(emptyList())

        repository.invalidateCache()
        repository.deleteCategory("Games")

        coVerify {
            appDao.deleteCategoryWithAppResets(
                    listOf(AppCategoryEntity("com.lu4p.game", "0", "")),
                    "Games"
            )
        }
    }

    @Test
    fun `deleteCategory clears explicit Room assignments matching name`() = runTest {
        every {
            launcherApps.getActivityList(null, myUser)
        } returns
                listOf(
                        createMockLauncherActivity(
                                "com.lu4p.app1",
                                "App",
                                ApplicationInfo.CATEGORY_UNDEFINED
                        )
                )
        every { appDao.getAllAppCategories() } returns
                flowOf(listOf(AppCategoryEntity("com.lu4p.app1", "0", "Games")))

        repository.invalidateCache()
        repository.deleteCategory("Games")

        coVerify {
            appDao.deleteCategoryWithAppResets(
                    listOf(AppCategoryEntity("com.lu4p.app1", "0", "")),
                    "Games"
            )
        }
    }

    // --- Helpers ---

    private fun createMockLauncherActivity(
            packageName: String,
            label: String,
            category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
            flags: Int = 0
    ): android.content.pm.LauncherActivityInfo {
        val mockLa = mockk<android.content.pm.LauncherActivityInfo>(relaxed = true)
        val appInfo =
                ApplicationInfo().apply {
                    this.packageName = packageName
                    this.category = category
                    this.flags = flags
                }
        every { mockLa.applicationInfo } returns appInfo
        every { mockLa.label } returns label
        every { mockLa.componentName } returns
                ComponentName(packageName, "$packageName.MainActivity")
        every { mockLa.getBadgedIcon(0) } returns null
        return mockLa
    }

    private fun createResolveInfo(
            packageName: String,
            label: String,
            category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
            flags: Int = 0
    ): android.content.pm.ResolveInfo {
        return android.content.pm.ResolveInfo().apply {
            activityInfo =
                    android.content.pm.ActivityInfo().apply {
                        this.packageName = packageName
                        this.name = "$packageName.MainActivity"
                        applicationInfo =
                                ApplicationInfo().apply {
                                    this.packageName = packageName
                                    this.category = category
                                    this.flags = flags
                                }
                    }
            nonLocalizedLabel = label
        }
    }

    @Test
    fun `appSupportsWebSearch true when package resolves WEB_SEARCH`() {
        val app = AppInfo("com.browser", "Browser", null)
        every {
            packageManager.queryIntentActivities(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns listOf(createResolveInfo("com.browser", "Browser"))
        assertTrue(repository.appSupportsWebSearch(app))
    }

    @Test
    fun `appSupportsWebSearch false when query returns empty`() {
        val app = AppInfo("com.nope", "Nope", null)
        every {
            packageManager.queryIntentActivities(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } returns emptyList()
        assertFalse(repository.appSupportsWebSearch(app))
    }

    @Test
    fun `appSupportsWebSearch true when only ACTION_SEARCH resolves`() {
        val app = AppInfo("com.searchapp", "SearchApp", null)
        every {
            packageManager.queryIntentActivities(any(), PackageManager.MATCH_DEFAULT_ONLY)
        } answers {
            val intent = invocation.args[0] as Intent
            when (intent.action) {
                Intent.ACTION_WEB_SEARCH -> emptyList()
                Intent.ACTION_SEARCH ->
                        listOf(createResolveInfo("com.searchapp", "SearchApp"))
                else -> emptyList()
            }
        }
        assertTrue(repository.appSupportsWebSearch(app))
    }
}
