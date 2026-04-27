package com.zeroboat.timerkit.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningRecordDao {
    @Insert
    suspend fun insert(record: RunningRecord): Long

    @Query("SELECT * FROM running_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<RunningRecord>>

    @Query("SELECT * FROM running_records WHERE id = :id")
    suspend fun getById(id: Long): RunningRecord?

    @Query("DELETE FROM running_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM running_records")
    suspend fun count(): Int

    @Query("SELECT SUM(distanceMeters) FROM running_records")
    suspend fun totalDistanceMeters(): Float?
}
