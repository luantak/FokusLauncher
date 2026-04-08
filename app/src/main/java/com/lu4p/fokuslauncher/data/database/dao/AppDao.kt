package com.lu4p.fokuslauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryDefinitionEntity
import com.lu4p.fokuslauncher.data.database.entity.AppCategoryEntity
import com.lu4p.fokuslauncher.data.database.entity.HiddenAppEntity
import com.lu4p.fokuslauncher.data.database.entity.RenamedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Hidden Apps ---

    @Query("SELECT * FROM hidden_apps")
    fun getHiddenApps(): Flow<List<HiddenAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideApp(entity: HiddenAppEntity)

    @Delete
    suspend fun unhideApp(entity: HiddenAppEntity)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM hidden_apps WHERE packageName = :packageName AND profileKey = :profileKey)"
    )
    suspend fun isAppHidden(packageName: String, profileKey: String): Boolean

    // --- Renamed Apps ---

    @Query("SELECT * FROM renamed_apps")
    fun getAllRenamedApps(): Flow<List<RenamedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun renameApp(entity: RenamedAppEntity)

    @Query("DELETE FROM renamed_apps WHERE packageName = :packageName AND profileKey = :profileKey")
    suspend fun removeRename(packageName: String, profileKey: String)

    @Query(
        "SELECT customName FROM renamed_apps WHERE packageName = :packageName AND profileKey = :profileKey"
    )
    suspend fun getCustomName(packageName: String, profileKey: String): String?

    // --- App Categories ---

    @Query("SELECT * FROM app_categories")
    fun getAllAppCategories(): Flow<List<AppCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppCategory(entity: AppCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppCategories(entities: List<AppCategoryEntity>)

    @Query("DELETE FROM app_categories WHERE packageName = :packageName AND profileKey = :profileKey")
    suspend fun removeAppCategory(packageName: String, profileKey: String)

    @Query(
        "SELECT category FROM app_categories WHERE packageName = :packageName AND profileKey = :profileKey"
    )
    suspend fun getAppCategory(packageName: String, profileKey: String): String?

    @Query("SELECT * FROM app_category_definitions ORDER BY position ASC, name COLLATE NOCASE ASC")
    fun getAllCategoryDefinitions(): Flow<List<AppCategoryDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryDefinition(entity: AppCategoryDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryDefinitions(entities: List<AppCategoryDefinitionEntity>)

    @Query("SELECT position FROM app_category_definitions WHERE name = :name LIMIT 1")
    suspend fun getCategoryDefinitionPosition(name: String): Int?

    @Query("SELECT COALESCE(MAX(position), -1) FROM app_category_definitions")
    suspend fun getMaxCategoryDefinitionPosition(): Int

    @Query("DELETE FROM app_categories WHERE category = :category")
    suspend fun removeCategoryAssignments(category: String)

    @Query("UPDATE app_categories SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategoryAssignments(oldCategory: String, newCategory: String)

    @Query("DELETE FROM app_category_definitions WHERE name = :name")
    suspend fun removeCategoryDefinition(name: String)

    /**
     * Clears categories, assignment rows, and the definition in one transaction so Flow collectors
     * (e.g. settings UI counts) refresh once instead of after each per-app write.
     */
    @Transaction
    suspend fun deleteCategoryWithAppResets(
            appsToUncategorize: List<AppCategoryEntity>,
            categoryName: String
    ) {
        if (appsToUncategorize.isNotEmpty()) {
            upsertAppCategories(appsToUncategorize)
        }
        removeCategoryAssignments(categoryName)
        removeCategoryDefinition(categoryName)
    }

    @Transaction
    suspend fun replaceCategoryDefinitions(entities: List<AppCategoryDefinitionEntity>) {
        clearAllCategoryDefinitions()
        if (entities.isNotEmpty()) {
            upsertCategoryDefinitions(entities)
        }
    }

    // --- Reset / Clear All ---

    @Query("DELETE FROM hidden_apps")
    suspend fun clearAllHiddenApps()

    @Query("DELETE FROM renamed_apps")
    suspend fun clearAllRenamedApps()

    @Query("DELETE FROM app_categories")
    suspend fun clearAllAppCategories()

    @Query("DELETE FROM app_category_definitions")
    suspend fun clearAllCategoryDefinitions()

    /**
     * Full data reset in one transaction so category-definition Flow observers never see an
     * intermediate "everything cleared" frame without defaults restored.
     */
    @Transaction
    suspend fun resetAllAppData(defaultCategoryDefinitions: List<AppCategoryDefinitionEntity>) {
        clearAllHiddenApps()
        clearAllRenamedApps()
        clearAllAppCategories()
        clearAllCategoryDefinitions()
        if (defaultCategoryDefinitions.isNotEmpty()) {
            upsertCategoryDefinitions(defaultCategoryDefinitions)
        }
    }
}
