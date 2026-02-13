package com.lu4p.fokuslauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Hidden Apps ---

    @Query("SELECT * FROM hidden_apps")
    fun getAllHiddenApps(): Flow<List<HiddenAppEntity>>

    @Query("SELECT packageName FROM hidden_apps")
    fun getHiddenPackageNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideApp(entity: HiddenAppEntity)

    @Delete
    suspend fun unhideApp(entity: HiddenAppEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_apps WHERE packageName = :packageName)")
    suspend fun isAppHidden(packageName: String): Boolean

    // --- Renamed Apps ---

    @Query("SELECT * FROM renamed_apps")
    fun getAllRenamedApps(): Flow<List<RenamedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun renameApp(entity: RenamedAppEntity)

    @Query("DELETE FROM renamed_apps WHERE packageName = :packageName")
    suspend fun removeRename(packageName: String)

    @Query("SELECT customName FROM renamed_apps WHERE packageName = :packageName")
    suspend fun getCustomName(packageName: String): String?

    // --- App Categories ---

    @Query("SELECT * FROM app_categories")
    fun getAllAppCategories(): Flow<List<AppCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppCategory(entity: AppCategoryEntity)

    @Query("DELETE FROM app_categories WHERE packageName = :packageName")
    suspend fun removeAppCategory(packageName: String)

    @Query("SELECT category FROM app_categories WHERE packageName = :packageName")
    suspend fun getAppCategory(packageName: String): String?

    @Query("SELECT * FROM app_categories WHERE category = :category")
    fun getAppsByCategory(category: String): Flow<List<AppCategoryEntity>>

    @Query("SELECT * FROM app_category_definitions")
    fun getAllCategoryDefinitions(): Flow<List<AppCategoryDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCategoryDefinition(entity: AppCategoryDefinitionEntity)

    @Query("DELETE FROM app_categories WHERE category = :category")
    suspend fun removeCategoryAssignments(category: String)

    @Query("UPDATE app_categories SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategoryAssignments(oldCategory: String, newCategory: String)

    @Query("DELETE FROM app_category_definitions WHERE name = :name")
    suspend fun removeCategoryDefinition(name: String)

    // --- Reset / Clear All ---

    @Query("DELETE FROM hidden_apps")
    suspend fun clearAllHiddenApps()

    @Query("DELETE FROM renamed_apps")
    suspend fun clearAllRenamedApps()

    @Query("DELETE FROM app_categories")
    suspend fun clearAllAppCategories()

    @Query("DELETE FROM app_category_definitions")
    suspend fun clearAllCategoryDefinitions()
}
