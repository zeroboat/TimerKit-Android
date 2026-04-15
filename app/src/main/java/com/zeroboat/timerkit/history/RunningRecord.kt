package com.zeroboat.timerkit.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_records")
data class RunningRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,                     // epoch millis
    val mode: String,                   // "BASIC" or "INTERVAL"
    val durationMillis: Long,
    val distanceMeters: Float,
    val avgPaceSecondsPerKm: Int,       // 0이면 미측정
    val avgHeartRateBpm: Int,           // 0이면 미측정
    val maxHeartRateBpm: Int,           // 0이면 미측정
    val routePoints: String,            // JSON "[{lat,lng},...]"
    val kmPaceRecords: String           // JSON "[{km,paceSeconds},...]"
)
