package com.zeroboat.timerkit.cooking

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroboat.timerkit.R
import com.zeroboat.timerkit.common.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CookingTimer(
    val id: Int,
    val name: String,
    val durationSeconds: Int,
    val remainingSeconds: Int = durationSeconds,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

data class CookingUiState(
    val timers: List<CookingTimer> = emptyList()
)

class CookingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private var nextId = 0
    private val timerJobs = mutableMapOf<Int, Job>()

    fun addTimer(name: String, durationSeconds: Int) {
        if (durationSeconds <= 0) return
        val timer = CookingTimer(
            id = nextId++,
            name = name.ifBlank { "타이머 $nextId" },
            durationSeconds = durationSeconds
        )
        _uiState.update { it.copy(timers = it.timers + timer) }
    }

    fun removeTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        _uiState.update { it.copy(timers = it.timers.filter { t -> t.id != id }) }
        if (runningCount() == 0) stopService()
    }

    fun startTimer(id: Int) {
        val timer = _uiState.value.timers.find { it.id == id } ?: return
        if (timer.isRunning || timer.isFinished) return
        val wasIdle = runningCount() == 0
        updateTimer(id) { it.copy(isRunning = true) }
        if (wasIdle) {
            startService(buildNotificationText())
        } else {
            updateService(buildNotificationText())
        }
        timerJobs[id] = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val current = _uiState.value.timers.find { it.id == id } ?: break
                if (!current.isRunning) break
                val newRemaining = current.remainingSeconds - 1
                if (newRemaining <= 0) {
                    updateTimer(id) { it.copy(remainingSeconds = 0, isRunning = false, isFinished = true) }
                    timerJobs.remove(id)
                    postDoneNotification(current.name)
                    if (runningCount() == 0) stopService() else updateService(buildNotificationText())
                    break
                } else {
                    updateTimer(id) { it.copy(remainingSeconds = newRemaining) }
                    updateService(buildNotificationText())
                }
            }
        }
    }

    fun pauseTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        updateTimer(id) { it.copy(isRunning = false) }
        if (runningCount() == 0) stopService() else updateService(buildNotificationText())
    }

    fun resetTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        updateTimer(id) { it.copy(remainingSeconds = it.durationSeconds, isRunning = false, isFinished = false) }
        if (runningCount() == 0) stopService() else updateService(buildNotificationText())
    }

    private fun runningCount() = _uiState.value.timers.count { it.isRunning }

    private fun buildNotificationText(): String {
        val running = _uiState.value.timers.filter { it.isRunning }
        return if (running.isEmpty()) ""
        else running.joinToString(" · ") { t -> "${t.name} ${formatSeconds(t.remainingSeconds)}" }
    }

    private fun formatSeconds(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

    private fun updateTimer(id: Int, transform: (CookingTimer) -> CookingTimer) {
        _uiState.update { state ->
            state.copy(timers = state.timers.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun startService(text: String) {
        val ctx = getApplication<Application>()
        ContextCompat.startForegroundService(ctx, Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TEXT, text)
        })
    }

    private fun updateService(text: String) {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE
            putExtra(TimerService.EXTRA_TEXT, text)
        })
    }

    private fun stopService() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        })
    }

    private fun postDoneNotification(timerName: String) {
        val ctx = getApplication<Application>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val channelId = "timerkit_done_channel"
        val manager = ctx.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, "타이머 완료", NotificationManager.IMPORTANCE_HIGH)
                .apply { setSound(null, null) }
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("타이머 완료")
            .setContentText("$timerName 완료!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        manager.notify(timerName.hashCode(), notification)
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
    }
}
