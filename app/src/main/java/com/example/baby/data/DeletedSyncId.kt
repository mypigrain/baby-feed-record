package com.example.baby.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_sync_ids")
data class DeletedSyncId(
    @PrimaryKey
    val syncId: String
)
