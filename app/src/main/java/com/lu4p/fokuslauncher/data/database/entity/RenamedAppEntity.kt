package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an app that has been renamed by the user.
 */
@Entity(tableName = "renamed_apps")
data class RenamedAppEntity(
    @PrimaryKey
    val packageName: String,
    val customName: String
)
