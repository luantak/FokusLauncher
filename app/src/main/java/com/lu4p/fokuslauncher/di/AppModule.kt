package com.lu4p.fokuslauncher.di

import android.content.Context
import androidx.room.Room
import com.lu4p.fokuslauncher.data.database.AppDatabase
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.data.repository.WeatherRepository
import com.lu4p.fokuslauncher.utils.PrivateSpaceManager
import com.lu4p.fokuslauncher.utils.WallpaperHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "fokus_launcher_db"
    ).build()

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao = database.appDao()

    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context,
        appDao: AppDao
    ): AppRepository = AppRepository(context, appDao)

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
    fun provideWallpaperHelper(
        @ApplicationContext context: Context
    ): WallpaperHelper = WallpaperHelper(context)

    @Provides
    @Singleton
    fun providePrivateSpaceManager(
        @ApplicationContext context: Context
    ): PrivateSpaceManager = PrivateSpaceManager(context)
}
