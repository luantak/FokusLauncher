package com.lu4p.fokuslauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        appDao = mockk(relaxed = true)
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.lu4p.fokuslauncher"
        repository = AppRepository(context, appDao)
    }

    // --- App Loading Tests ---

    @Test
    fun `getInstalledApps returns sorted list excluding launcher itself`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.chrome", "Chrome"),
            createResolveInfo("com.lu4p.fokuslauncher", "Fokus Launcher"),
            createResolveInfo("com.lu4p.calculator", "Calculator"),
            createResolveInfo("com.lu4p.atom", "Atom")
        )
        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        val result = repository.getInstalledApps()

        assertEquals(3, result.size)
        assertEquals("Atom", result[0].label)
        assertEquals("Calculator", result[1].label)
        assertEquals("Chrome", result[2].label)
        assertTrue(result.none { it.packageName == "com.lu4p.fokuslauncher" })
    }

    @Test
    fun `getInstalledApps caches results on subsequent calls`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.app1", "App 1")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        repository.getInstalledApps()
        repository.getInstalledApps()

        verify(exactly = 1) {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        }
    }

    @Test
    fun `invalidateCache forces reload on next call`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.app1", "App 1")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        repository.getInstalledApps()
        repository.invalidateCache()
        repository.getInstalledApps()

        verify(exactly = 2) {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        }
    }

    @Test
    fun `searchApps filters by label case-insensitively`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.chrome", "Chrome"),
            createResolveInfo("com.lu4p.calendar", "Calendar"),
            createResolveInfo("com.lu4p.camera", "Camera")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        val result = repository.searchApps("ca")

        assertEquals(2, result.size)
        assertEquals("Calendar", result[0].label)
        assertEquals("Camera", result[1].label)
    }

    @Test
    fun `searchApps with blank query returns all apps`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.app1", "App 1"),
            createResolveInfo("com.lu4p.app2", "App 2")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        val result = repository.searchApps("")

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
        val realRepository = AppRepository(realContext, appDao)

        val result = realRepository.launchApp("com.lu4p.nonexistent")

        assertFalse(result)
    }

    @Test
    fun `filterByCategory returns all apps for All apps category`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.app1", "App 1"),
            createResolveInfo("com.lu4p.app2", "App 2")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        val result = repository.filterByCategory("All apps")

        assertEquals(2, result.size)
    }

    @Test
    fun `getInstalledApps deduplicates by package name`() {
        val resolveInfos = listOf(
            createResolveInfo("com.lu4p.app1", "App 1"),
            createResolveInfo("com.lu4p.app1", "App 1 Duplicate")
        )

        every {
            packageManager.queryIntentActivities(any<Intent>(), any<Int>())
        } returns resolveInfos

        val result = repository.getInstalledApps()

        assertEquals(1, result.size)
    }

    // --- Hidden Apps Tests ---

    @Test
    fun `hideApp delegates to DAO`() = runTest {
        repository.hideApp("com.lu4p.app1")

        coVerify { appDao.hideApp(HiddenAppEntity("com.lu4p.app1")) }
    }

    @Test
    fun `unhideApp delegates to DAO`() = runTest {
        repository.unhideApp("com.lu4p.app1")

        coVerify { appDao.unhideApp(HiddenAppEntity("com.lu4p.app1")) }
    }

    @Test
    fun `isAppHidden delegates to DAO`() = runTest {
        coEvery { appDao.isAppHidden("com.lu4p.app1") } returns true

        val result = repository.isAppHidden("com.lu4p.app1")

        assertTrue(result)
    }

    @Test
    fun `getHiddenPackageNames returns flow from DAO`() {
        every { appDao.getHiddenPackageNames() } returns flowOf(listOf("com.lu4p.hidden"))

        val result = repository.getHiddenPackageNames()

        // Flow is returned directly from DAO
        assertEquals(appDao.getHiddenPackageNames(), result)
    }

    // --- Renamed Apps Tests ---

    @Test
    fun `renameApp delegates to DAO`() = runTest {
        repository.renameApp("com.lu4p.app1", "My Custom Name")

        coVerify { appDao.renameApp(RenamedAppEntity("com.lu4p.app1", "My Custom Name")) }
    }

    @Test
    fun `getCustomName returns name from DAO`() = runTest {
        coEvery { appDao.getCustomName("com.lu4p.app1") } returns "Custom"

        val result = repository.getCustomName("com.lu4p.app1")

        assertEquals("Custom", result)
    }

    @Test
    fun `getCustomName returns null when not renamed`() = runTest {
        coEvery { appDao.getCustomName("com.lu4p.app1") } returns null

        val result = repository.getCustomName("com.lu4p.app1")

        assertNull(result)
    }

    // --- Category Tests ---

    @Test
    fun `setAppCategory delegates to DAO`() = runTest {
        repository.setAppCategory("com.lu4p.app1", "Productivity")

        coVerify {
            appDao.setAppCategory(
                com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity(
                    "com.lu4p.app1",
                    "Productivity"
                )
            )
        }
    }

    @Test
    fun `getAppCategory returns category from DAO`() = runTest {
        coEvery { appDao.getAppCategory("com.lu4p.app1") } returns "Social"

        val result = repository.getAppCategory("com.lu4p.app1")

        assertEquals("Social", result)
    }

    @Test
    fun `addCategoryDefinition ignores reserved category names`() = runTest {
        repository.addCategoryDefinition("All apps")
        repository.addCategoryDefinition("Private")
        repository.addCategoryDefinition("   ")

        coVerify(exactly = 0) { appDao.upsertCategoryDefinition(any()) }
    }

    @Test
    fun `addCategoryDefinition stores normalized category name`() = runTest {
        coEvery { appDao.getCategoryDefinitionPosition("My Category") } returns null
        coEvery { appDao.getMaxCategoryDefinitionPosition() } returns 3

        repository.addCategoryDefinition("  My Category  ")

        coVerify { appDao.upsertCategoryDefinition(AppCategoryDefinitionEntity("My Category", 4)) }
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

        coVerify(exactly = 0) { appDao.removeCategoryAssignments(any()) }
    }

    // --- Helper ---

    private fun createResolveInfo(packageName: String, label: String): ResolveInfo {
        return ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                this.name = "$packageName.MainActivity"
            }
            nonLocalizedLabel = label
        }
    }
}
