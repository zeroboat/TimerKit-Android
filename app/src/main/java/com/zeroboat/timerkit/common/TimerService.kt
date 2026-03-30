package com.zeroboat.timerkit.common

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
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
        const val ACTION_SHOW_OVERLAY = "com.zeroboat.timerkit.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.zeroboat.timerkit.ACTION_HIDE_OVERLAY"
        const val EXTRA_TEXT = "extra_text"

        val distanceMeters = MutableStateFlow(0f)
        val routePoints = MutableStateFlow<List<GpsPoint>>(emptyList())
        val isOverlayVisible = MutableStateFlow(false)
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

    private var overlayView: LinearLayout? = null
    private var overlayTextView: TextView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val wm: WindowManager by lazy { getSystemService(WindowManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "타이머 실행 중"
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification(text))
            ACTION_UPDATE -> {
                updateNotification(text)
                updateOverlay(text)
            }
            ACTION_STOP -> {
                hideOverlay()
                fusedClient.removeLocationUpdates(locationCallback)
                stopSelf()
            }
            ACTION_START_LOCATION -> {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .build()
                fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
            ACTION_STOP_LOCATION -> fusedClient.removeLocationUpdates(locationCallback)
            ACTION_SHOW_OVERLAY -> showOverlay(text)
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
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

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimerKit")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay(initialText: String) {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        val dp = resources.displayMetrics.density

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f * dp
                setColor(Color.parseColor("#DD101828"))
            }
        }

        val textView = TextView(this).apply {
            text = initialText
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val closeBtn = TextView(this).apply {
            text = "  ✕"
            setTextColor(Color.parseColor("#AAFFFFFF"))
            textSize = 14f
            setOnClickListener { hideOverlay() }
        }

        container.addView(textView)
        container.addView(closeBtn)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * dp).toInt()
            y = (160 * dp).toInt()
        }
        overlayParams = params
        overlayTextView = textView

        var lastTouchX = 0f
        var lastTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX; lastTouchY = event.rawY; isDragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastTouchX
                    val dy = event.rawY - lastTouchY
                    if (!isDragging && (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x -= dx.toInt()
                        params.y += dy.toInt()
                        lastTouchX = event.rawX; lastTouchY = event.rawY
                        wm.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (!isDragging) v.performClick()
            }
            isDragging
        }

        wm.addView(container, params)
        overlayView = container
        isOverlayVisible.value = true
    }

    private fun hideOverlay() {
        overlayView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        overlayView = null
        overlayTextView = null
        overlayParams = null
        isOverlayVisible.value = false
    }

    private fun updateOverlay(text: String) {
        overlayTextView?.post { overlayTextView?.text = text }
    }
}
