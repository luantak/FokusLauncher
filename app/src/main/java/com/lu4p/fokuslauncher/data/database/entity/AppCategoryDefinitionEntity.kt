package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category_definitions")
data class AppCategoryDefinitionEntity(
    @PrimaryKey
    val name: String
)
