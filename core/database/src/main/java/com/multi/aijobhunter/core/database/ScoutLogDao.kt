package com.multi.aijobhunter.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoutLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ScoutLogEntity)

    @Query("SELECT * FROM scout_logs ORDER BY timestamp DESC LIMIT 50")
    fun getLatestLogsFlow(): Flow<List<ScoutLogEntity>>

    @Query("DELETE FROM scout_logs")
    suspend fun clearLogs()
}
