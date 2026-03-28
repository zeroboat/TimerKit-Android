package com.zeroboat.timerkit.common

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _distanceMeters = MutableStateFlow(0f)
    val distanceMeters: StateFlow<Float> = _distanceMeters

    private val _routePoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val routePoints: StateFlow<List<GpsPoint>> = _routePoints

    private var lastLocation: Location? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _routePoints.value = _routePoints.value + GpsPoint(loc.latitude, loc.longitude)
            lastLocation?.let { prev ->
                val delta = prev.distanceTo(loc)
                if (delta > 1f) _distanceMeters.value += delta
            }
            lastLocation = loc
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun stop() {
        fusedClient.removeLocationUpdates(callback)
    }

    fun reset() {
        stop()
        _distanceMeters.value = 0f
        _routePoints.value = emptyList()
        lastLocation = null
    }
}
