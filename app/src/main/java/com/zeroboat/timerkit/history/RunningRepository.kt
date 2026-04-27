package com.zeroboat.timerkit.history

import android.content.Context
import com.zeroboat.timerkit.common.GpsPoint
import com.zeroboat.timerkit.running.KmPaceRecord
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class RunningRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).runningRecordDao()

    val allRecords: Flow<List<RunningRecord>> = dao.getAllRecords()

    suspend fun save(
        mode: String,
        durationMillis: Long,
        distanceMeters: Float,
        avgHeartRateBpm: Int?,
        maxHeartRateBpm: Int?,
        routePoints: List<GpsPoint>,
        kmPaceRecords: List<KmPaceRecord>
    ): Long {
        val avgPace = if (distanceMeters > 10f && durationMillis > 0L) {
            ((durationMillis / 1000f) / (distanceMeters / 1000f)).toInt()
        } else 0

        val routeJson = JSONArray().apply {
            routePoints.forEach { p ->
                put(JSONObject().put("lat", p.lat).put("lng", p.lon))
            }
        }.toString()

        val kmJson = JSONArray().apply {
            kmPaceRecords.forEach { r ->
                put(JSONObject().put("km", r.km).put("paceSeconds", r.paceSeconds))
            }
        }.toString()

        return dao.insert(
            RunningRecord(
                date = System.currentTimeMillis(),
                mode = mode,
                durationMillis = durationMillis,
                distanceMeters = distanceMeters,
                avgPaceSecondsPerKm = avgPace,
                avgHeartRateBpm = avgHeartRateBpm ?: 0,
                maxHeartRateBpm = maxHeartRateBpm ?: 0,
                routePoints = routeJson,
                kmPaceRecords = kmJson
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long) = dao.getById(id)

    suspend fun totalCount() = dao.count()

    suspend fun totalDistanceMeters() = dao.totalDistanceMeters() ?: 0f

    companion object {
        fun parseRoutePoints(json: String): List<GpsPoint> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                GpsPoint(lat = obj.getDouble("lat"), lon = obj.getDouble("lng"))
            }
        }

        fun parseKmPaceRecords(json: String): List<KmPaceRecord> {
            val arr = JSONArray(json)
            return (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                KmPaceRecord(obj.getInt("km"), obj.getInt("paceSeconds"))
            }
        }
    }
}
