package com.zeroboat.timerkit.common

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.zeroboat.timerkit.R
import kotlinx.coroutines.flow.MutableStateFlow

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timerkit_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.zeroboat.timerkit.ACTION_START"
        const val ACTION_UPDATE = "com.zeroboat.timerkit.ACTION_UPDATE"
        const val ACTION_STOP = "com.zeroboat.timerkit.ACTION_STOP"
        const val ACTION_START_LOCATION = "com.zeroboat.timerkit.ACTION_START_LOCATION"
        const val ACTION_STOP_LOCATION = "com.zeroboat.timerkit.ACTION_STOP_LOCATION"
        const val EXTRA_TEXT = "extra_text"

        // ViewModel이 init 시점부터 관찰할 수 있는 안정적인 흐름
        val distanceMeters = MutableStateFlow(0f)
        val routePoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    }

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var lastLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            routePoints.value = routePoints.value + GpsPoint(loc.latitude, loc.longitude)
            lastLocation?.let { prev ->
                val delta = prev.distanceTo(loc)
                if (delta > 1f) distanceMeters.value += delta
            }
            lastLocation = loc
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "타이머 실행 중"
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification(text))
            ACTION_UPDATE -> updateNotification(text)
            ACTION_STOP -> {
                fusedClient.removeLocationUpdates(locationCallback)
                stopSelf()
            }
            ACTION_START_LOCATION -> {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .build()
                fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
            ACTION_STOP_LOCATION -> {
                fusedClient.removeLocationUpdates(locationCallback)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TimerKit 타이머",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "타이머 실행 중 표시되는 알림입니다"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimerKit")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
