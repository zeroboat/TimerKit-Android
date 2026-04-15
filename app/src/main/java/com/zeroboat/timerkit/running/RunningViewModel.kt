package com.zeroboat.timerkit.running

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroboat.timerkit.common.GpsPoint
import com.zeroboat.timerkit.common.HeartRateTracker
import com.zeroboat.timerkit.common.IntervalTimerHelper
import com.zeroboat.timerkit.common.MusicController
import com.zeroboat.timerkit.common.MusicState
import com.zeroboat.timerkit.common.PreferencesKeys
import com.zeroboat.timerkit.common.TimerService
import com.zeroboat.timerkit.common.VibrationHelper
import com.zeroboat.timerkit.common.appDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RunningMode { BASIC, INTERVAL }
enum class RunningPhase { WARMUP, RUN, REST }

data class KmPaceRecord(
    val km: Int,
    val paceSeconds: Int  // 해당 km 구간을 달리는 데 걸린 초
)

data class RunningUiState(
    val mode: RunningMode = RunningMode.BASIC,
    val phase: RunningPhase = RunningPhase.WARMUP,
    val remainingMillis: Long = 0L,
    val totalElapsedMillis: Long = 0L,
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
    val routePoints: List<GpsPoint> = emptyList(),
    // km 페이스 기록
    val kmPaceRecords: List<KmPaceRecord> = emptyList(),
    val lastKmElapsedMillis: Long = 0L,
    // 심박수
    val heartRateBpm: Int? = null,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val heartRateSamples: List<Int> = emptyList(),
    val isHeartRatePermissionGranted: Boolean = false,
    // 음악
    val musicState: MusicState? = null,
    val isMusicPermissionGranted: Boolean = false
)

class RunningViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    private val timer = IntervalTimerHelper(viewModelScope)
    private val heartRateTracker = HeartRateTracker(application)
    private var heartRateJob: Job? = null

    val isHeartRateAvailable: Boolean get() = heartRateTracker.isAvailable()
    val heartRatePermissions get() = heartRateTracker.permissions

    private val musicController = MusicController(application)
    private var musicJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().appDataStore.data.first()
            _uiState.update { it.copy(
                totalIntervals = prefs[PreferencesKeys.RUNNING_TOTAL_INTERVALS] ?: 5,
                warmupSeconds  = prefs[PreferencesKeys.RUNNING_WARMUP_SECONDS]  ?: 300,
                runSeconds     = prefs[PreferencesKeys.RUNNING_RUN_SECONDS]     ?: 60,
                restSeconds    = prefs[PreferencesKeys.RUNNING_REST_SECONDS]    ?: 90
            )}
        }
        checkLocationPermission()
        checkMusicPermission()
        viewModelScope.launch { checkHeartRatePermission() }
        // TimerService companion 흐름 관찰 (서비스 시작 여부와 무관하게 안정적으로 접근)
        viewModelScope.launch {
            TimerService.distanceMeters.collect { meters ->
                _uiState.update { it.copy(distanceMeters = meters) }
            }
        }
        viewModelScope.launch {
            TimerService.routePoints.collect { points ->
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
        if (_uiState.value.isRunning) startLocationTracking()
    }

    fun setMode(mode: RunningMode) {
        if (_uiState.value.isRunning || _uiState.value.isFinished) return
        _uiState.update { it.copy(mode = mode) }
    }

    fun start() {
        val s = _uiState.value
        if (s.isRunning || s.isFinished) return
        when (s.mode) {
            RunningMode.BASIC -> {
                _uiState.update { it.copy(
                    phase = RunningPhase.RUN,
                    isRunning = true
                )}
                startService("러닝 중...")
                if (_uiState.value.isLocationGranted) startLocationTracking()
                timer.start { tickBasic() }
                startHeartRatePolling()
                startMusicPolling()
            }
            RunningMode.INTERVAL -> {
                if (s.remainingMillis == 0L) {
                    _uiState.update { it.copy(
                        phase = RunningPhase.WARMUP,
                        remainingMillis = s.warmupSeconds * 1_000L,
                        currentInterval = 0
                    )}
                }
                _uiState.update { it.copy(isRunning = true) }
                startService("러닝 워밍업 중...")
                if (_uiState.value.isLocationGranted) startLocationTracking()
                timer.start { tickInterval() }
                startHeartRatePolling()
                startMusicPolling()
            }
        }
    }

    fun pause() {
        timer.cancel()
        stopHeartRatePolling()
        stopMusicPolling()
        _uiState.update { it.copy(isRunning = false) }
        stopService()
    }

    fun finish() {
        if (_uiState.value.mode != RunningMode.BASIC) return
        timer.cancel()
        stopHeartRatePolling()
        stopMusicPolling()
        VibrationHelper.doneBuzz(getApplication())
        _uiState.update { it.copy(isRunning = false, isFinished = true) }
        stopService()
    }

    fun reset() {
        timer.cancel()
        stopHeartRatePolling()
        stopMusicPolling()
        TimerService.distanceMeters.value = 0f
        TimerService.routePoints.value = emptyList()
        val s = _uiState.value
        _uiState.value = RunningUiState(
            mode              = s.mode,
            totalIntervals    = s.totalIntervals,
            warmupSeconds     = s.warmupSeconds,
            runSeconds        = s.runSeconds,
            restSeconds       = s.restSeconds,
            isLocationGranted = s.isLocationGranted
        )
        stopService()
    }

    fun onHeartRatePermissionGranted() {
        _uiState.update { it.copy(isHeartRatePermissionGranted = true) }
        if (_uiState.value.isRunning) startHeartRatePolling()
    }

    fun refreshHeartRatePermission() {
        viewModelScope.launch { checkHeartRatePermission() }
    }

    suspend fun checkHeartRatePermission() {
        val granted = heartRateTracker.hasPermission()
        _uiState.update { it.copy(isHeartRatePermissionGranted = granted) }
    }

    private fun startHeartRatePolling() {
        if (!heartRateTracker.isAvailable()) return
        heartRateJob?.cancel()
        heartRateJob = viewModelScope.launch {
            if (!heartRateTracker.hasPermission()) return@launch
            while (true) {
                val bpm = heartRateTracker.getLatestBpm()
                if (bpm != null) {
                    val samples = _uiState.value.heartRateSamples + bpm
                    _uiState.update { it.copy(
                        heartRateBpm     = bpm,
                        heartRateSamples = samples,
                        avgHeartRateBpm  = samples.average().toInt(),
                        maxHeartRateBpm  = samples.max()
                    )}
                }
                delay(5_000L)
            }
        }
    }

    private fun stopHeartRatePolling() {
        heartRateJob?.cancel()
        heartRateJob = null
    }

    fun checkMusicPermission() {
        _uiState.update { it.copy(isMusicPermissionGranted = musicController.isPermissionGranted()) }
    }

    private fun startMusicPolling() {
        if (!musicController.isPermissionGranted()) return
        musicJob?.cancel()
        musicJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(musicState = musicController.getCurrentState()) }
                delay(1_000L)
            }
        }
    }

    private fun stopMusicPolling() {
        musicJob?.cancel()
        musicJob = null
    }

    fun musicTogglePlayPause() = musicController.togglePlayPause()
    fun musicPrevious() = musicController.previous()
    fun musicNext() = musicController.next()

    fun toggleOverlay() {
        val ctx = getApplication<Application>()
        val action = if (TimerService.isOverlayVisible.value)
            TimerService.ACTION_HIDE_OVERLAY
        else
            TimerService.ACTION_SHOW_OVERLAY
        val s = _uiState.value
        val text = when {
            s.mode == RunningMode.BASIC -> "러닝 ${formatElapsedMillis(s.totalElapsedMillis)} · ${distanceText(s.distanceMeters)}"
            else -> when (s.phase) {
                RunningPhase.WARMUP -> "워밍업 ${s.remainingMillis / 1000}초"
                RunningPhase.RUN    -> "러닝 ${s.currentInterval}/${s.totalIntervals} — ${s.remainingMillis / 1000}초"
                RunningPhase.REST   -> "휴식 ${s.currentInterval}/${s.totalIntervals} — ${s.remainingMillis / 1000}초"
            }
        }
        ctx.startService(Intent(ctx, TimerService::class.java).apply {
            this.action = action
            putExtra(TimerService.EXTRA_TEXT, text)
        })
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

    // 기본 러닝 tick: 시간 누적 + km 페이스 기록
    private fun tickBasic() {
        val s = _uiState.value
        val newElapsed = s.totalElapsedMillis + 100L
        val newRunElapsed = s.runElapsedMillis + 100L

        // 다음 km 경계 도달 여부 확인
        val nextKm = s.kmPaceRecords.size + 1
        val nextKmThreshold = nextKm * 1000f
        if (s.distanceMeters >= nextKmThreshold) {
            val timeForThisKm = newElapsed - s.lastKmElapsedMillis
            val paceSeconds = (timeForThisKm / 1000).toInt()
            VibrationHelper.shortBuzz(getApplication())
            _uiState.update { it.copy(
                totalElapsedMillis = newElapsed,
                runElapsedMillis   = newRunElapsed,
                kmPaceRecords      = it.kmPaceRecords + KmPaceRecord(nextKm, paceSeconds),
                lastKmElapsedMillis = newElapsed
            )}
            updateService("${nextKm}km 통과! 페이스 ${formatPaceSeconds(paceSeconds)}")
        } else {
            _uiState.update { it.copy(
                totalElapsedMillis = newElapsed,
                runElapsedMillis   = newRunElapsed
            )}
            if (newElapsed % 1_000L == 0L) {
                updateService("러닝 중 ${formatElapsedMillis(newElapsed)} · ${distanceText(s.distanceMeters)}")
            }
        }
    }

    // 인터벌 러닝 tick (기존 로직)
    private fun tickInterval() {
        val s = _uiState.value
        val newRemaining = s.remainingMillis - 100L
        if (newRemaining > 0) {
            _uiState.update { it.copy(
                remainingMillis = newRemaining,
                runElapsedMillis = if (s.phase == RunningPhase.RUN) s.runElapsedMillis + 100L
                                   else s.runElapsedMillis
            )}
            if (newRemaining % 1_000L == 0L) {
                val remainSec = (newRemaining / 1000).toInt()
                val text = when (s.phase) {
                    RunningPhase.WARMUP -> "워밍업 ${remainSec}초"
                    RunningPhase.RUN    -> "러닝 ${s.currentInterval}/${s.totalIntervals} — ${remainSec}초"
                    RunningPhase.REST   -> "휴식 ${s.currentInterval}/${s.totalIntervals} — ${remainSec}초"
                }
                updateService(text)
            }
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
                    stopService()  // 위치 추적도 함께 종료
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

    private fun startLocationTracking() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_START_LOCATION
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

fun formatPaceSeconds(paceSeconds: Int): String {
    val min = paceSeconds / 60
    val sec = paceSeconds % 60
    return "$min'%02d\"".format(sec)
}

private fun formatElapsedMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun distanceText(meters: Float): String =
    if (meters < 1000) "${"%.0f".format(meters)}m"
    else "${"%.1f".format(meters / 1000)}km"
