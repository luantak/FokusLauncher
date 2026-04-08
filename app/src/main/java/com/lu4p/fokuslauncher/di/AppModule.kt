package com.lu4p.fokuslauncher.di

import android.content.Context.LAUNCHER_APPS_SERVICE
import android.content.Context.USER_SERVICE
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lu4p.fokuslauncher.data.database.AppDatabase
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.appProfileKey
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_category_definitions` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))"
                )
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `app_category_definitions` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

    fun migration3To4(
        context: Context,
        profileKeyResolver: (Context, String) -> Set<String> = ::resolveInstalledProfileKeys
    ) =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hidden_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `renamed_apps_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `customName` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_categories_new` (`packageName` TEXT NOT NULL, `profileKey` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`packageName`, `profileKey`))"
                )

                db.query("SELECT packageName FROM hidden_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        for (profileKey in profileKeyResolver(context, packageName)) {
                            db.execSQL(
                                "INSERT OR REPLACE INTO `hidden_apps_new` (`packageName`, `profileKey`) VALUES (?, ?)",
                                arrayOf(packageName, profileKey)
                            )
                        }
                    }
                }

                db.query("SELECT packageName, customName FROM renamed_apps").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val customNameIndex = cursor.getColumnIndexOrThrow("customName")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        val customName = cursor.getString(customNameIndex)
                        for (profileKey in profileKeyResolver(context, packageName)) {
                            db.execSQL(
                                "INSERT OR REPLACE INTO `renamed_apps_new` (`packageName`, `profileKey`, `customName`) VALUES (?, ?, ?)",
                                arrayOf(packageName, profileKey, customName)
                            )
                        }
                    }
                }

                db.query("SELECT packageName, category FROM app_categories").use { cursor ->
                    val packageIndex = cursor.getColumnIndexOrThrow("packageName")
                    val categoryIndex = cursor.getColumnIndexOrThrow("category")
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)
                        val category = cursor.getString(categoryIndex)
                        for (profileKey in profileKeyResolver(context, packageName)) {
                            db.execSQL(
                                "INSERT OR REPLACE INTO `app_categories_new` (`packageName`, `profileKey`, `category`) VALUES (?, ?, ?)",
                                arrayOf(packageName, profileKey, category)
                            )
                        }
                    }
                }

                db.execSQL("DROP TABLE `hidden_apps`")
                db.execSQL("ALTER TABLE `hidden_apps_new` RENAME TO `hidden_apps`")
                db.execSQL("DROP TABLE `renamed_apps`")
                db.execSQL("ALTER TABLE `renamed_apps_new` RENAME TO `renamed_apps`")
                db.execSQL("DROP TABLE `app_categories`")
                db.execSQL("ALTER TABLE `app_categories_new` RENAME TO `app_categories`")
            }
        }

    private fun resolveInstalledProfileKeys(context: Context, packageName: String): Set<String> {
        val privateSpaceManager = PrivateSpaceManager(context)
        val launcherApps =
            try {
                context.getSystemService(LAUNCHER_APPS_SERVICE) as? LauncherApps
            } catch (_: Exception) {
                null
            }
        val userManager =
            try {
                context.getSystemService(USER_SERVICE) as? UserManager
            } catch (_: Exception) {
                null
            }

        val discovered = linkedSetOf<String>()
        if (launcherApps != null && userManager != null) {
            for (user in userManager.userProfiles) {
                if (privateSpaceManager.isPrivateSpaceProfile(user)) continue
                val hasActivities =
                    try {
                        launcherApps.getActivityList(packageName, user).isNotEmpty()
                    } catch (_: Exception) {
                        false
                    }
                if (hasActivities) {
                    val profileKey =
                        if (user == Process.myUserHandle()) "0" else appProfileKey(user)
                    discovered += profileKey
                }
            }
        }

        if (discovered.isNotEmpty()) return discovered

        return setOf("0")
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "fokus_launcher_db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3To4(context)).build()

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun provideWeatherRepository(): WeatherRepository = WeatherRepository()

    @Provides
    @Singleton
    fun providePrivateSpaceManager(
        @ApplicationContext context: Context
    ): PrivateSpaceManager = PrivateSpaceManager(context)

    /** CPU-bound drawer work; tests may replace with [Dispatchers.Unconfined]. */
    @Provides
    @Singleton
    @Named("DrawerComputation")
    fun provideDrawerComputationDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
