package com.zeroboat.timerkit.running

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.zeroboat.timerkit.common.appDataStore
import com.zeroboat.timerkit.common.IntervalTimerHelper
import com.zeroboat.timerkit.common.PreferencesKeys
import com.zeroboat.timerkit.common.TimerService
import com.zeroboat.timerkit.common.VibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RunningPhase { WARMUP, RUN, REST }

data class RunningUiState(
    val phase: RunningPhase = RunningPhase.WARMUP,
    val remainingMillis: Long = 0L,
    val currentInterval: Int = 0,
    val totalIntervals: Int = 5,
    val warmupSeconds: Int = 300,
    val runSeconds: Int = 60,
    val restSeconds: Int = 90,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class RunningViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    private val timer = IntervalTimerHelper(viewModelScope)

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().appDataStore.data.first()
            _uiState.value = RunningUiState(
                totalIntervals = prefs[PreferencesKeys.RUNNING_TOTAL_INTERVALS] ?: 5,
                warmupSeconds  = prefs[PreferencesKeys.RUNNING_WARMUP_SECONDS]  ?: 300,
                runSeconds     = prefs[PreferencesKeys.RUNNING_RUN_SECONDS]     ?: 60,
                restSeconds    = prefs[PreferencesKeys.RUNNING_REST_SECONDS]    ?: 90
            )
        }
    }

    fun start() {
        val s = _uiState.value
        if (s.isRunning || s.isFinished) return
        if (s.remainingMillis == 0L) {
            _uiState.update { it.copy(
                phase = RunningPhase.WARMUP,
                remainingMillis = s.warmupSeconds * 1_000L,
                currentInterval = 0
            )}
        }
        _uiState.update { it.copy(isRunning = true) }
        startService("러닝 워밍업 중...")
        timer.start { tick() }
    }

    fun pause() {
        timer.cancel()
        _uiState.update { it.copy(isRunning = false) }
        stopService()
    }

    fun reset() {
        timer.cancel()
        val s = _uiState.value
        _uiState.value = RunningUiState(
            totalIntervals = s.totalIntervals,
            warmupSeconds = s.warmupSeconds,
            runSeconds = s.runSeconds,
            restSeconds = s.restSeconds
        )
        stopService()
    }

    fun updateSettings(totalIntervals: Int, warmupSeconds: Int, runSeconds: Int, restSeconds: Int) {
        if (_uiState.value.isRunning) return
        _uiState.value = RunningUiState(
            totalIntervals = totalIntervals,
            warmupSeconds = warmupSeconds,
            runSeconds = runSeconds,
            restSeconds = restSeconds
        )
        viewModelScope.launch {
            getApplication<Application>().appDataStore.edit { prefs ->
                prefs[PreferencesKeys.RUNNING_TOTAL_INTERVALS] = totalIntervals
                prefs[PreferencesKeys.RUNNING_WARMUP_SECONDS]  = warmupSeconds
                prefs[PreferencesKeys.RUNNING_RUN_SECONDS]     = runSeconds
                prefs[PreferencesKeys.RUNNING_REST_SECONDS]    = restSeconds
            }
        }
    }

    private fun tick() {
        val s = _uiState.value
        val newRemaining = s.remainingMillis - 100L
        if (newRemaining > 0) {
            _uiState.update { it.copy(remainingMillis = newRemaining) }
            return
        }
        when (s.phase) {
            RunningPhase.WARMUP -> {
                VibrationHelper.longBuzz(getApplication())
                _uiState.update { it.copy(
                    phase = RunningPhase.RUN,
                    remainingMillis = s.runSeconds * 1_000L,
                    currentInterval = 1
                )}
                updateService("러닝 — 인터벌 1/${s.totalIntervals}")
            }
            RunningPhase.RUN -> {
                if (s.currentInterval >= s.totalIntervals) {
                    VibrationHelper.doneBuzz(getApplication())
                    timer.cancel()
                    _uiState.update { it.copy(isRunning = false, isFinished = true, remainingMillis = 0L) }
                    stopService()
                } else {
                    VibrationHelper.shortBuzz(getApplication())
                    _uiState.update { it.copy(
                        phase = RunningPhase.REST,
                        remainingMillis = s.restSeconds * 1_000L
                    )}
                    updateService("러닝 휴식 — 인터벌 ${s.currentInterval}/${s.totalIntervals}")
                }
            }
            RunningPhase.REST -> {
                val nextInterval = s.currentInterval + 1
                VibrationHelper.longBuzz(getApplication())
                _uiState.update { it.copy(
                    phase = RunningPhase.RUN,
                    remainingMillis = s.runSeconds * 1_000L,
                    currentInterval = nextInterval
                )}
                updateService("러닝 — 인터벌 $nextInterval/${s.totalIntervals}")
            }
        }
    }

    private fun startService(text: String) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TEXT, text)
        }
        ContextCompat.startForegroundService(ctx, intent)
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
        ctx.stopService(Intent(ctx, TimerService::class.java))
    }
}
