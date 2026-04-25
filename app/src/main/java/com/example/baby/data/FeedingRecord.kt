package com.example.baby.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "amount_ml")
    val amountMl: Int? = null,

    @ColumnInfo(name = "type")
    val type: String = "breast",

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
