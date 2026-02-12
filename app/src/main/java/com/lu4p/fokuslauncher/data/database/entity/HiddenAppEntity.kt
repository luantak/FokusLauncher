package com.lu4p.fokuslauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an app that has been hidden by the user.
 */
@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey
    val packageName: String
)
