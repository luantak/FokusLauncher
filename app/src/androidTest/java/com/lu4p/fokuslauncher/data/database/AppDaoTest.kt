package com.lu4p.fokuslauncher.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.appDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Hidden Apps ---

    @Test
    fun hideApp_addsToDatabase() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))

        val isHidden = dao.isAppHidden("com.lu4p.app1", "0")
        assertTrue(isHidden)
    }

    @Test
    fun unhideApp_removesFromDatabase() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))
        dao.unhideApp(HiddenAppEntity("com.lu4p.app1", "0"))

        val isHidden = dao.isAppHidden("com.lu4p.app1", "0")
        assertFalse(isHidden)
    }

    @Test
    fun isAppHidden_returnsFalseForUnhiddenApp() = runTest {
        val isHidden = dao.isAppHidden("com.lu4p.nonexistent", "0")
        assertFalse(isHidden)
    }

    @Test
    fun getHiddenPackageNames_emitsUpdates() = runTest {
        dao.getHiddenApps().test {
            // Initially empty
            assertEquals(emptyList<HiddenAppEntity>(), awaitItem())

            // Add a hidden app
            dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))
            assertEquals(listOf(HiddenAppEntity("com.lu4p.app1", "0")), awaitItem())

            // Add another
            dao.hideApp(HiddenAppEntity("com.lu4p.app2", "0"))
            val twoHidden = awaitItem()
            assertEquals(2, twoHidden.size)
            assertTrue(twoHidden.contains(HiddenAppEntity("com.lu4p.app1", "0")))
            assertTrue(twoHidden.contains(HiddenAppEntity("com.lu4p.app2", "0")))

            // Remove one
            dao.unhideApp(HiddenAppEntity("com.lu4p.app1", "0"))
            assertEquals(listOf(HiddenAppEntity("com.lu4p.app2", "0")), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hideApp_replaceOnConflict() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))

        dao.getHiddenApps().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hiddenApps_areProfileScoped() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "0"))
        dao.hideApp(HiddenAppEntity("com.lu4p.app1", "10"))

        assertTrue(dao.isAppHidden("com.lu4p.app1", "0"))
        assertTrue(dao.isAppHidden("com.lu4p.app1", "10"))

        dao.unhideApp(HiddenAppEntity("com.lu4p.app1", "10"))

        assertTrue(dao.isAppHidden("com.lu4p.app1", "0"))
        assertFalse(dao.isAppHidden("com.lu4p.app1", "10"))
    }

    // --- Renamed Apps ---

    @Test
    fun renameApp_storesCustomName() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "My App"))

        val name = dao.getCustomName("com.lu4p.app1", "0")
        assertEquals("My App", name)
    }

    @Test
    fun renameApp_updatesExistingName() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "Old Name"))
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "New Name"))

        val name = dao.getCustomName("com.lu4p.app1", "0")
        assertEquals("New Name", name)
    }

    @Test
    fun removeRename_deletesEntry() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "Custom"))
        dao.removeRename("com.lu4p.app1", "0")

        val name = dao.getCustomName("com.lu4p.app1", "0")
        assertNull(name)
    }

    @Test
    fun getCustomName_returnsNullForUnrenamedApp() = runTest {
        val name = dao.getCustomName("com.lu4p.nonexistent", "0")
        assertNull(name)
    }

    @Test
    fun getAllRenamedApps_emitsUpdates() = runTest {
        dao.getAllRenamedApps().test {
            assertEquals(emptyList<RenamedAppEntity>(), awaitItem())

            dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "App One"))
            val oneRenamed = awaitItem()
            assertEquals(1, oneRenamed.size)
            assertEquals("App One", oneRenamed[0].customName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun renamedApps_areProfileScoped() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "0", "Personal"))
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "10", "Work"))

        assertEquals("Personal", dao.getCustomName("com.lu4p.app1", "0"))
        assertEquals("Work", dao.getCustomName("com.lu4p.app1", "10"))

        dao.removeRename("com.lu4p.app1", "10")

        assertEquals("Personal", dao.getCustomName("com.lu4p.app1", "0"))
        assertNull(dao.getCustomName("com.lu4p.app1", "10"))
    }

    // --- App Categories ---

    @Test
    fun setAppCategory_storesCategory() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Productivity"))

        val category = dao.getAppCategory("com.lu4p.app1", "0")
        assertEquals("Productivity", category)
    }

    @Test
    fun setAppCategory_updatesExistingCategory() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Social"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Productivity"))

        val category = dao.getAppCategory("com.lu4p.app1", "0")
        assertEquals("Productivity", category)
    }

    @Test
    fun removeAppCategory_deletesEntry() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Social"))
        dao.removeAppCategory("com.lu4p.app1", "0")

        val category = dao.getAppCategory("com.lu4p.app1", "0")
        assertNull(category)
    }

    @Test
    fun getAppCategory_returnsNullForUncategorized() = runTest {
        val category = dao.getAppCategory("com.lu4p.nonexistent", "0")
        assertNull(category)
    }

    @Test
    fun getAppsByCategory_filtersCorrectly() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Social"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app2", "0", "Productivity"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app3", "0", "Social"))

        dao.getAppsByCategory("Social").test {
            val socialApps = awaitItem()
            assertEquals(2, socialApps.size)
            assertTrue(socialApps.any { it.packageName == "com.lu4p.app1" })
            assertTrue(socialApps.any { it.packageName == "com.lu4p.app3" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun appCategories_areProfileScoped() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "0", "Social"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "10", "Productivity"))

        assertEquals("Social", dao.getAppCategory("com.lu4p.app1", "0"))
        assertEquals("Productivity", dao.getAppCategory("com.lu4p.app1", "10"))

        dao.removeAppCategory("com.lu4p.app1", "10")

        assertEquals("Social", dao.getAppCategory("com.lu4p.app1", "0"))
        assertNull(dao.getAppCategory("com.lu4p.app1", "10"))
    }
}
