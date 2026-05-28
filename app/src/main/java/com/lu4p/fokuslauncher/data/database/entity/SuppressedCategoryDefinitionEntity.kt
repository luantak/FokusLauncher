package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppressed_category_definitions")
data class SuppressedCategoryDefinitionEntity(@PrimaryKey val name: String)
