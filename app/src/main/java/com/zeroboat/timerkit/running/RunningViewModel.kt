package com.zeroboat.timerkit.running

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroboat.timerkit.common.IntervalTimerHelper
import com.zeroboat.timerkit.common.LocationTracker
import com.zeroboat.timerkit.common.PreferencesKeys
import com.zeroboat.timerkit.common.TimerService
import com.zeroboat.timerkit.common.VibrationHelper
import com.zeroboat.timerkit.common.appDataStore
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
    val isFinished: Boolean = false,
    // 위치
    val isLocationGranted: Boolean = false,
    val distanceMeters: Float = 0f,
    val runElapsedMillis: Long = 0L,
    val routePoints: List<com.zeroboat.timerkit.common.GpsPoint> = emptyList()
)

class RunningViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    private val timer = IntervalTimerHelper(viewModelScope)
    private val locationTracker = LocationTracker(application)

    init {
        // 저장된 설정 복원
        viewModelScope.launch {
            val prefs = getApplication<Application>().appDataStore.data.first()
            _uiState.update { it.copy(
                totalIntervals = prefs[PreferencesKeys.RUNNING_TOTAL_INTERVALS] ?: 5,
                warmupSeconds  = prefs[PreferencesKeys.RUNNING_WARMUP_SECONDS]  ?: 300,
                runSeconds     = prefs[PreferencesKeys.RUNNING_RUN_SECONDS]     ?: 60,
                restSeconds    = prefs[PreferencesKeys.RUNNING_REST_SECONDS]    ?: 90
            )}
        }
        // 위치 권한 초기 확인
        checkLocationPermission()
        // 거리 + 경로 업데이트 수신
        viewModelScope.launch {
            locationTracker.distanceMeters.collect { meters ->
                _uiState.update { it.copy(distanceMeters = meters) }
            }
        }
        viewModelScope.launch {
            locationTracker.routePoints.collect { points ->
                _uiState.update { it.copy(routePoints = points) }
            }
        }
    }

    fun checkLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(isLocationGranted = granted) }
    }

    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(isLocationGranted = true) }
        if (_uiState.value.isRunning) locationTracker.start()
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
        if (_uiState.value.isLocationGranted) locationTracker.start()
        startService("러닝 워밍업 중...")
        timer.start { tick() }
    }

    fun pause() {
        timer.cancel()
        locationTracker.stop()
        _uiState.update { it.copy(isRunning = false) }
        stopService()
    }

    fun reset() {
        timer.cancel()
        locationTracker.reset()
        val s = _uiState.value
        _uiState.value = RunningUiState(
            totalIntervals    = s.totalIntervals,
            warmupSeconds     = s.warmupSeconds,
            runSeconds        = s.runSeconds,
            restSeconds       = s.restSeconds,
            isLocationGranted = s.isLocationGranted
        )
        stopService()
    }

    fun updateSettings(totalIntervals: Int, warmupSeconds: Int, runSeconds: Int, restSeconds: Int) {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(
            totalIntervals = totalIntervals,
            warmupSeconds  = warmupSeconds,
            runSeconds     = runSeconds,
            restSeconds    = restSeconds
        )}
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
            _uiState.update { it.copy(
                remainingMillis = newRemaining,
                // RUN 페이즈에서만 경과 시간 누적
                runElapsedMillis = if (s.phase == RunningPhase.RUN) s.runElapsedMillis + 100L
                                   else s.runElapsedMillis
            )}
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
                    locationTracker.stop()
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
        ctx.stopService(Intent(ctx, TimerService::class.java))
    }
}
