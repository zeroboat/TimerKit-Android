package com.zeroboat.timerkit.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {
    /** 짧게 — REST 페이즈 전환 */
    fun shortBuzz(context: Context) = vibrate(context, longArrayOf(0, 200))

    /** 길게 — WORK/RUN 페이즈 전환 (GO!) */
    fun longBuzz(context: Context) = vibrate(context, longArrayOf(0, 500))

    /** 완료 — 세 번 진동 */
    fun doneBuzz(context: Context) = vibrate(context, longArrayOf(0, 100, 80, 100, 80, 300))

    private fun vibrate(context: Context, pattern: LongArray) {
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)
                ?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)?.vibrate(effect)
        }
    }
}
