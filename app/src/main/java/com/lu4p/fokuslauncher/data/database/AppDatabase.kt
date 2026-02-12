package com.lu4p.fokuslauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lu4p.fokuslauncher.data.database.dao.AppDao
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity

@Database(
    entities = [
        HiddenAppEntity::class,
        RenamedAppEntity::class,
        AppCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
