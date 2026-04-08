package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity

/**
 * Represents a user-assigned category for an app.
 */
@Entity(tableName = "app_categories", primaryKeys = ["packageName", "profileKey"])
data class AppCategoryEntity(
    val packageName: String,
    val profileKey: String,
    val category: String
)
