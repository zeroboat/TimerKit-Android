package com.zeroboat.timerkit.tabata

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeroboat.timerkit.common.appDataStore
import com.zeroboat.timerkit.common.IntervalTimerHelper
import com.zeroboat.timerkit.common.PreferencesKeys
import com.zeroboat.timerkit.common.TimerService
import com.zeroboat.timerkit.common.VibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TabataPhase { PREPARE, WORK, REST }

data class TabataUiState(
    val phase: TabataPhase = TabataPhase.PREPARE,
    val remainingMillis: Long = 0L,
    val currentSet: Int = 0,
    val totalSets: Int = 8,
    val prepareSeconds: Int = 10,
    val workSeconds: Int = 20,
    val restSeconds: Int = 10,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class TabataViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TabataUiState())
    val uiState: StateFlow<TabataUiState> = _uiState.asStateFlow()

    private val timer = IntervalTimerHelper(viewModelScope)

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().appDataStore.data.first()
            _uiState.value = TabataUiState(
                totalSets      = prefs[PreferencesKeys.TABATA_TOTAL_SETS]      ?: 8,
                prepareSeconds = prefs[PreferencesKeys.TABATA_PREPARE_SECONDS] ?: 10,
                workSeconds    = prefs[PreferencesKeys.TABATA_WORK_SECONDS]    ?: 20,
                restSeconds    = prefs[PreferencesKeys.TABATA_REST_SECONDS]    ?: 10
            )
        }
    }

    fun start() {
        val s = _uiState.value
        if (s.isRunning || s.isFinished) return
        if (s.remainingMillis == 0L) {
            _uiState.update { it.copy(
                phase = TabataPhase.PREPARE,
                remainingMillis = s.prepareSeconds * 1_000L,
                currentSet = 0
            )}
        }
        _uiState.update { it.copy(isRunning = true) }
        startService("Tabata 준비 중...")
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
        _uiState.value = TabataUiState(
            totalSets = s.totalSets,
            prepareSeconds = s.prepareSeconds,
            workSeconds = s.workSeconds,
            restSeconds = s.restSeconds
        )
        stopService()
    }

    fun updateSettings(totalSets: Int, prepareSeconds: Int, workSeconds: Int, restSeconds: Int) {
        if (_uiState.value.isRunning) return
        _uiState.value = TabataUiState(
            totalSets = totalSets,
            prepareSeconds = prepareSeconds,
            workSeconds = workSeconds,
            restSeconds = restSeconds
        )
        viewModelScope.launch {
            getApplication<Application>().appDataStore.edit { prefs ->
                prefs[PreferencesKeys.TABATA_TOTAL_SETS]      = totalSets
                prefs[PreferencesKeys.TABATA_PREPARE_SECONDS] = prepareSeconds
                prefs[PreferencesKeys.TABATA_WORK_SECONDS]    = workSeconds
                prefs[PreferencesKeys.TABATA_REST_SECONDS]    = restSeconds
            }
        }
    }

    private fun tick() {
        val s = _uiState.value
        val newRemaining = s.remainingMillis - 100L
        if (newRemaining > 0) {
            _uiState.update { it.copy(remainingMillis = newRemaining) }
            if (newRemaining % 1_000L == 0L) {
                val remainSec = (newRemaining / 1000).toInt()
                val text = when (s.phase) {
                    TabataPhase.PREPARE -> "준비 ${remainSec}초"
                    TabataPhase.WORK    -> "운동 ${s.currentSet}/${s.totalSets} — ${remainSec}초"
                    TabataPhase.REST    -> "휴식 — ${remainSec}초"
                }
                updateService(text)
            }
            return
        }
        when (s.phase) {
            TabataPhase.PREPARE -> {
                VibrationHelper.longBuzz(getApplication())
                _uiState.update { it.copy(
                    phase = TabataPhase.WORK,
                    remainingMillis = s.workSeconds * 1_000L,
                    currentSet = 1
                )}
                updateService("Tabata 운동 — 세트 1/${s.totalSets}")
            }
            TabataPhase.WORK -> {
                if (s.currentSet >= s.totalSets) {
                    VibrationHelper.doneBuzz(getApplication())
                    timer.cancel()
                    _uiState.update { it.copy(isRunning = false, isFinished = true, remainingMillis = 0L) }
                    stopService()
                } else {
                    VibrationHelper.shortBuzz(getApplication())
                    _uiState.update { it.copy(
                        phase = TabataPhase.REST,
                        remainingMillis = s.restSeconds * 1_000L
                    )}
                    updateService("Tabata 휴식 — 세트 ${s.currentSet}/${s.totalSets}")
                }
            }
            TabataPhase.REST -> {
                val nextSet = s.currentSet + 1
                VibrationHelper.longBuzz(getApplication())
                _uiState.update { it.copy(
                    phase = TabataPhase.WORK,
                    remainingMillis = s.workSeconds * 1_000L,
                    currentSet = nextSet
                )}
                updateService("Tabata 운동 — 세트 $nextSet/${s.totalSets}")
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
