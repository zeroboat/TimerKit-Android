package com.zeroboat.timerkit.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tabata / Running 처럼 일정 간격으로 tick 콜백을 호출하는 공통 타이머 헬퍼.
 * ViewModel의 viewModelScope를 주입받아 사용한다.
 */
class IntervalTimerHelper(private val scope: CoroutineScope) {

    private var job: Job? = null

    val isActive: Boolean get() = job?.isActive == true

    fun start(tickMs: Long = 100L, onTick: () -> Unit) {
        if (isActive) return
        job = scope.launch {
            while (true) {
                delay(tickMs)
                onTick()
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
