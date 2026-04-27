package com.zeroboat.timerkit.common

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HeartRateTracker(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return try {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLatestBpm(): Int? {
        if (!isAvailable()) return null
        return try {
            val end = Instant.now()
            val start = end.minusSeconds(300) // 삼성 헬스 → Health Connect 동기화 딜레이 고려 (최대 5분)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun createPermissionLauncher(activity: androidx.activity.ComponentActivity, onResult: (Boolean) -> Unit) =
        activity.registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted -> onResult(granted.containsAll(permissions)) }
}
