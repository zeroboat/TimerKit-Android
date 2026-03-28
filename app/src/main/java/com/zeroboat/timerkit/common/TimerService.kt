package com.zeroboat.timerkit.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zeroboat.timerkit.R

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timerkit_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.zeroboat.timerkit.ACTION_START"
        const val ACTION_UPDATE = "com.zeroboat.timerkit.ACTION_UPDATE"
        const val ACTION_STOP = "com.zeroboat.timerkit.ACTION_STOP"
        const val EXTRA_TEXT = "extra_text"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "타이머 실행 중"
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification(text))
            ACTION_UPDATE -> updateNotification(text)
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
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
