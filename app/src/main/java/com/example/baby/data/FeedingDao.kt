package com.example.baby.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedingDao {

    @Insert
    suspend fun insert(record: FeedingRecord): Long

    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC")
    fun getAllDesc(): Flow<List<FeedingRecord>>

    @Query("SELECT * FROM feeding_records WHERE timestamp >= :dayStart AND timestamp < :dayEnd ORDER BY timestamp DESC")
    fun getRecordsForDay(dayStart: Long, dayEnd: Long): Flow<List<FeedingRecord>>

    @Query("SELECT COUNT(*) FROM feeding_records WHERE timestamp >= :dayStart AND timestamp < :dayEnd")
    suspend fun getCountForDay(dayStart: Long, dayEnd: Long): Int

    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastRecord(): FeedingRecord?

    @Query("SELECT * FROM feeding_records WHERE timestamp >= :weekStart AND timestamp < :weekEnd ORDER BY timestamp DESC")
    fun getRecordsForWeek(weekStart: Long, weekEnd: Long): Flow<List<FeedingRecord>>

    @Query("""
        SELECT SUM(amount_ml) FROM feeding_records
        WHERE timestamp >= :dayStart AND timestamp < :dayEnd AND amount_ml IS NOT NULL
    """)
    suspend fun getTotalAmountForDay(dayStart: Long, dayEnd: Long): Int?

    @Query("DELETE FROM feeding_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
