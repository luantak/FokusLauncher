package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-assigned category for an app.
 */
@Entity(tableName = "app_categories")
data class AppCategoryEntity(
    @PrimaryKey
    val packageName: String,
    val category: String
)
