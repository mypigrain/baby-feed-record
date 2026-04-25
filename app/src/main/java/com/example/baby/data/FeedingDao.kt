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

    @Query("DELETE FROM feeding_records WHERE sync_id = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query("SELECT sync_id FROM feeding_records WHERE sync_id IS NOT NULL")
    suspend fun getAllSyncIds(): List<String>

    @Query("SELECT * FROM feeding_records")
    suspend fun getAllRecords(): List<FeedingRecord>

    @Query("UPDATE feeding_records SET sync_id = :syncId WHERE id = :id")
    suspend fun updateSyncId(id: Long, syncId: String)

    @Query("SELECT * FROM feeding_records WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getRecordsSince(since: Long): List<FeedingRecord>

    // Deleted sync ID tracking
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeletedSyncId(deletedSyncId: DeletedSyncId)

    @Query("SELECT syncId FROM deleted_sync_ids")
    suspend fun getAllDeletedSyncIds(): List<String>

    @Query("DELETE FROM deleted_sync_ids WHERE syncId IN (:syncIds)")
    suspend fun clearDeletedSyncIds(syncIds: List<String>)
}
