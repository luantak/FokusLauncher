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
        dao.hideApp(HiddenAppEntity("com.lu4p.app1"))

        val isHidden = dao.isAppHidden("com.lu4p.app1")
        assertTrue(isHidden)
    }

    @Test
    fun unhideApp_removesFromDatabase() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1"))
        dao.unhideApp(HiddenAppEntity("com.lu4p.app1"))

        val isHidden = dao.isAppHidden("com.lu4p.app1")
        assertFalse(isHidden)
    }

    @Test
    fun isAppHidden_returnsFalseForUnhiddenApp() = runTest {
        val isHidden = dao.isAppHidden("com.lu4p.nonexistent")
        assertFalse(isHidden)
    }

    @Test
    fun getHiddenPackageNames_emitsUpdates() = runTest {
        dao.getHiddenPackageNames().test {
            // Initially empty
            assertEquals(emptyList<String>(), awaitItem())

            // Add a hidden app
            dao.hideApp(HiddenAppEntity("com.lu4p.app1"))
            assertEquals(listOf("com.lu4p.app1"), awaitItem())

            // Add another
            dao.hideApp(HiddenAppEntity("com.lu4p.app2"))
            val twoHidden = awaitItem()
            assertEquals(2, twoHidden.size)
            assertTrue(twoHidden.contains("com.lu4p.app1"))
            assertTrue(twoHidden.contains("com.lu4p.app2"))

            // Remove one
            dao.unhideApp(HiddenAppEntity("com.lu4p.app1"))
            assertEquals(listOf("com.lu4p.app2"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hideApp_replaceOnConflict() = runTest {
        dao.hideApp(HiddenAppEntity("com.lu4p.app1"))
        dao.hideApp(HiddenAppEntity("com.lu4p.app1"))

        dao.getHiddenPackageNames().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Renamed Apps ---

    @Test
    fun renameApp_storesCustomName() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "My App"))

        val name = dao.getCustomName("com.lu4p.app1")
        assertEquals("My App", name)
    }

    @Test
    fun renameApp_updatesExistingName() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "Old Name"))
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "New Name"))

        val name = dao.getCustomName("com.lu4p.app1")
        assertEquals("New Name", name)
    }

    @Test
    fun removeRename_deletesEntry() = runTest {
        dao.renameApp(RenamedAppEntity("com.lu4p.app1", "Custom"))
        dao.removeRename("com.lu4p.app1")

        val name = dao.getCustomName("com.lu4p.app1")
        assertNull(name)
    }

    @Test
    fun getCustomName_returnsNullForUnrenamedApp() = runTest {
        val name = dao.getCustomName("com.lu4p.nonexistent")
        assertNull(name)
    }

    @Test
    fun getAllRenamedApps_emitsUpdates() = runTest {
        dao.getAllRenamedApps().test {
            assertEquals(emptyList<RenamedAppEntity>(), awaitItem())

            dao.renameApp(RenamedAppEntity("com.lu4p.app1", "App One"))
            val oneRenamed = awaitItem()
            assertEquals(1, oneRenamed.size)
            assertEquals("App One", oneRenamed[0].customName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- App Categories ---

    @Test
    fun setAppCategory_storesCategory() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "Productivity"))

        val category = dao.getAppCategory("com.lu4p.app1")
        assertEquals("Productivity", category)
    }

    @Test
    fun setAppCategory_updatesExistingCategory() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "Social"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "Productivity"))

        val category = dao.getAppCategory("com.lu4p.app1")
        assertEquals("Productivity", category)
    }

    @Test
    fun removeAppCategory_deletesEntry() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "Social"))
        dao.removeAppCategory("com.lu4p.app1")

        val category = dao.getAppCategory("com.lu4p.app1")
        assertNull(category)
    }

    @Test
    fun getAppCategory_returnsNullForUncategorized() = runTest {
        val category = dao.getAppCategory("com.lu4p.nonexistent")
        assertNull(category)
    }

    @Test
    fun getAppsByCategory_filtersCorrectly() = runTest {
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app1", "Social"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app2", "Productivity"))
        dao.setAppCategory(AppCategoryEntity("com.lu4p.app3", "Social"))

        dao.getAppsByCategory("Social").test {
            val socialApps = awaitItem()
            assertEquals(2, socialApps.size)
            assertTrue(socialApps.any { it.packageName == "com.lu4p.app1" })
            assertTrue(socialApps.any { it.packageName == "com.lu4p.app3" })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
