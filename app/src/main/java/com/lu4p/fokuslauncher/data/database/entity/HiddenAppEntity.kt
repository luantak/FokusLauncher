package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity

/**
 * Represents an app that has been hidden by the user.
 */
@Entity(tableName = "hidden_apps", primaryKeys = ["packageName", "profileKey"])
data class HiddenAppEntity(
    val packageName: String,
    val profileKey: String
)
