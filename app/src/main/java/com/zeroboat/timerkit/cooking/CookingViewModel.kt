package com.zeroboat.timerkit.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class CookingViewModel : ViewModel() {

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
    }

    fun startTimer(id: Int) {
        val timer = _uiState.value.timers.find { it.id == id } ?: return
        if (timer.isRunning || timer.isFinished) return
        updateTimer(id) { it.copy(isRunning = true) }
        timerJobs[id] = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val current = _uiState.value.timers.find { it.id == id } ?: break
                if (!current.isRunning) break
                val newRemaining = current.remainingSeconds - 1
                if (newRemaining <= 0) {
                    updateTimer(id) { it.copy(remainingSeconds = 0, isRunning = false, isFinished = true) }
                    timerJobs.remove(id)
                    break
                } else {
                    updateTimer(id) { it.copy(remainingSeconds = newRemaining) }
                }
            }
        }
    }

    fun pauseTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        updateTimer(id) { it.copy(isRunning = false) }
    }

    fun resetTimer(id: Int) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        updateTimer(id) { it.copy(remainingSeconds = it.durationSeconds, isRunning = false, isFinished = false) }
    }

    private fun updateTimer(id: Int, transform: (CookingTimer) -> CookingTimer) {
        _uiState.update { state ->
            state.copy(timers = state.timers.map { if (it.id == id) transform(it) else it })
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
    }
}
