package com.multi.aijobhunter.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scout_logs")
data class ScoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val scannedCount: Int,
    val matchedCount: Int,
    val status: String,
    val message: String
)
