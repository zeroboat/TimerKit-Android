package com.zeroboat.timerkit.stopwatch

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroboat.timerkit.common.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopwatchUiState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun start() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        startService("스톱워치 실행 중")
        timerJob = viewModelScope.launch {
            while (true) {
                delay(10L)
                val newElapsed = _uiState.value.elapsedMillis + 10L
                _uiState.update { it.copy(elapsedMillis = newElapsed) }
                if (newElapsed % 1_000L == 0L) {
                    updateService("스톱워치 ${formatTime(newElapsed)}")
                }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
        stopService()
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = StopwatchUiState()
        stopService()
    }

    fun lap() {
        val current = _uiState.value.elapsedMillis
        _uiState.update { it.copy(laps = it.laps + current) }
    }

    fun toggleOverlay() {
        val ctx = getApplication<Application>()
        val action = if (TimerService.isOverlayVisible.value)
            TimerService.ACTION_HIDE_OVERLAY
        else
            TimerService.ACTION_SHOW_OVERLAY
        ctx.startService(Intent(ctx, TimerService::class.java).apply {
            this.action = action
            putExtra(TimerService.EXTRA_TEXT, "스톱워치 ${formatTime(_uiState.value.elapsedMillis)}")
        })
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
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
