package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity

/**
 * Represents an app that has been renamed by the user.
 */
@Entity(tableName = "renamed_apps", primaryKeys = ["packageName", "profileKey"])
data class RenamedAppEntity(
    val packageName: String,
    val profileKey: String,
    val customName: String
)
