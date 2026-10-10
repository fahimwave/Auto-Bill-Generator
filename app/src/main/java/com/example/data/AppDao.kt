package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: UserProfile)
}

@Dao
interface DutyLogDao {
    // Auto-Sorting: All entries sorted in Ascending Order by 'Date' and 'start_time'
    @Query("SELECT * FROM duty_logs ORDER BY date ASC, start_time ASC")
    fun getAllLogsAscending(): Flow<List<DutyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DutyLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DutyLog>)

    @Query("UPDATE duty_logs SET start_time = :startTime, end_time = :endTime, duration = :duration WHERE id = :id")
    suspend fun updateLog(id: Int, startTime: String, endTime: String, duration: Double)

    @Query("DELETE FROM duty_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM duty_logs WHERE date = :date")
    suspend fun deleteLogsForDate(date: String)

    @Query("DELETE FROM duty_logs")
    suspend fun clearAllLogs()
}
