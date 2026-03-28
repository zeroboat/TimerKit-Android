package com.zeroboat.timerkit.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class StopwatchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun start() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(10L)
                _uiState.update { it.copy(elapsedMillis = it.elapsedMillis + 10L) }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = StopwatchUiState()
    }

    fun lap() {
        val current = _uiState.value.elapsedMillis
        _uiState.update { it.copy(laps = it.laps + current) }
    }
}
