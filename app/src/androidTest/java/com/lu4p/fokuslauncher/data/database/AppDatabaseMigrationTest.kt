package com.lu4p.fokuslauncher.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lu4p.fokuslauncher.di.AppModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory()
        )

    @Test
    fun migrate3To4_expandsPackageScopedRowsToAllResolvedProfiles() {
        val dbName = "migration-test-multi-profile"
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS `hidden_apps` (`packageName` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `renamed_apps` (`packageName` TEXT NOT NULL, `customName` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `app_categories` (`packageName` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `app_category_definitions` (`name` TEXT NOT NULL, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`name`))"
            )
            execSQL("INSERT INTO `hidden_apps` (`packageName`) VALUES ('com.example.chrome')")
            execSQL(
                "INSERT INTO `renamed_apps` (`packageName`, `customName`) VALUES ('com.example.chrome', 'Work Chrome')"
            )
            execSQL(
                "INSERT INTO `app_categories` (`packageName`, `category`) VALUES ('com.example.chrome', 'Productivity')"
            )
            close()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val migratedDb =
            helper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            AppModule.migration3To4(context) { _, packageName ->
                if (packageName == "com.example.chrome") setOf("0", "10") else setOf("0")
            }
        )

        migratedDb.use { db ->
            db.query("SELECT packageName, profileKey FROM hidden_apps ORDER BY profileKey").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals("com.example.chrome", cursor.getString(0))
                assertEquals("0", cursor.getString(1))
                cursor.moveToNext()
                assertEquals("10", cursor.getString(1))
            }
            db.query(
                "SELECT packageName, profileKey, customName FROM renamed_apps ORDER BY profileKey"
            ).use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals("Work Chrome", cursor.getString(2))
                cursor.moveToNext()
                assertEquals("Work Chrome", cursor.getString(2))
            }
            db.query(
                "SELECT packageName, profileKey, category FROM app_categories ORDER BY profileKey"
            ).use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals("Productivity", cursor.getString(2))
                cursor.moveToNext()
                assertEquals("Productivity", cursor.getString(2))
            }
        }
    }

    @Test
    fun migrate3To4_fallsBackToOwnerProfileWhenResolverHasNoProfiles() {
        val dbName = "migration-test-owner-fallback"
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS `hidden_apps` (`packageName` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `renamed_apps` (`packageName` TEXT NOT NULL, `customName` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `app_categories` (`packageName` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`packageName`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `app_category_definitions` (`name` TEXT NOT NULL, `position` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`name`))"
            )
            execSQL("INSERT INTO `hidden_apps` (`packageName`) VALUES ('com.example.owner')")
            execSQL(
                "INSERT INTO `renamed_apps` (`packageName`, `customName`) VALUES ('com.example.owner', 'Owner App')"
            )
            execSQL(
                "INSERT INTO `app_categories` (`packageName`, `category`) VALUES ('com.example.owner', 'Social')"
            )
            close()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val migratedDb =
            helper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            AppModule.migration3To4(context) { _, _ -> emptySet() }
        )

        migratedDb.use { db ->
            db.query("SELECT profileKey FROM hidden_apps").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("0", cursor.getString(0))
            }
            db.query("SELECT profileKey FROM renamed_apps").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("0", cursor.getString(0))
            }
            db.query("SELECT profileKey FROM app_categories").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("0", cursor.getString(0))
            }
        }
    }
}
