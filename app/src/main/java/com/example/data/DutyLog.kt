package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duty_logs")
data class DutyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: String,             // ISO "YYYY-MM-DD"
    @ColumnInfo(name = "day") val day: String,               // "Monday", "Tuesday", etc.
    @ColumnInfo(name = "start_time") val startTime: String,  // "10:00"
    @ColumnInfo(name = "end_time") val endTime: String,      // "13:00"
    @ColumnInfo(name = "duration") val duration: Double,     // 0.5 hr intervals (e.g. 1.0, 1.5, 2.0)
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
