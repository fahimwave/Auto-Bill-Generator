package com.example.data

import kotlinx.coroutines.flow.Flow

class DutyRepository(
    private val userProfileDao: UserProfileDao,
    private val dutyLogDao: DutyLogDao
) {
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val allLogs: Flow<List<DutyLog>> = dutyLogDao.getAllLogsAscending()

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.saveProfile(profile)
    }

    suspend fun insertLog(log: DutyLog): Long {
        return dutyLogDao.insertLog(log)
    }

    suspend fun insertLogs(logs: List<DutyLog>) {
        dutyLogDao.insertLogs(logs)
    }

    suspend fun updateLog(id: Int, startTime: String, endTime: String, duration: Double) {
        dutyLogDao.updateLog(id, startTime, endTime, duration)
    }

    suspend fun deleteLog(id: Int) {
        dutyLogDao.deleteLogById(id)
    }

    suspend fun deleteLogsForDate(date: String) {
        dutyLogDao.deleteLogsForDate(date)
    }

    suspend fun clearAll() {
        dutyLogDao.clearAllLogs()
    }
}
