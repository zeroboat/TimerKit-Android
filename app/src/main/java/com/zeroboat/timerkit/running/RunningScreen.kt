package com.zeroboat.timerkit.running

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RunningScreen(
    modifier: Modifier = Modifier,
    vm: RunningViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 페이즈 라벨
        val (phaseLabel, phaseColor) = when {
            state.isFinished -> "DONE" to MaterialTheme.colorScheme.primary
            else -> when (state.phase) {
                RunningPhase.WARMUP -> "WARMUP" to MaterialTheme.colorScheme.secondary
                RunningPhase.RUN    -> "RUN"    to MaterialTheme.colorScheme.error
                RunningPhase.REST   -> "REST"   to Color(0xFF4CAF50)
            }
        }
        Surface(
            color = phaseColor.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = phaseLabel,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = phaseColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 남은 시간
        Text(
            text = formatCountdown(state.remainingMillis),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 인터벌 진행
        when {
            state.isFinished -> Text(
                text = "완료!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            state.currentInterval > 0 -> Text(
                text = "Interval ${state.currentInterval} / ${state.totalIntervals}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 컨트롤 버튼
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = vm::reset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
            Button(
                onClick = { if (state.isRunning) vm.pause() else vm.start() },
                enabled = !state.isFinished,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isRunning) "Pause" else "Start")
            }
        }

        // 설정 (정지 상태일 때만)
        if (!state.isRunning && !state.isFinished) {
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "설정",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingRow(
                label = "인터벌 수",
                value = state.totalIntervals,
                unit = "회",
                min = 1,
                onDecrement = {
                    vm.updateSettings(
                        (state.totalIntervals - 1).coerceAtLeast(1),
                        state.warmupSeconds, state.runSeconds, state.restSeconds
                    )
                },
                onIncrement = {
                    vm.updateSettings(
                        state.totalIntervals + 1,
                        state.warmupSeconds, state.runSeconds, state.restSeconds
                    )
                }
            )
            SettingRow(
                label = "워밍업",
                value = state.warmupSeconds / 60,
                unit = "분",
                min = 0,
                onDecrement = {
                    vm.updateSettings(
                        state.totalIntervals,
                        ((state.warmupSeconds / 60) - 1).coerceAtLeast(0) * 60,
                        state.runSeconds, state.restSeconds
                    )
                },
                onIncrement = {
                    vm.updateSettings(
                        state.totalIntervals,
                        ((state.warmupSeconds / 60) + 1) * 60,
                        state.runSeconds, state.restSeconds
                    )
                }
            )
            SettingRow(
                label = "러닝 시간",
                value = state.runSeconds,
                unit = "초",
                min = 10,
                onDecrement = {
                    vm.updateSettings(
                        state.totalIntervals, state.warmupSeconds,
                        (state.runSeconds - 10).coerceAtLeast(10),
                        state.restSeconds
                    )
                },
                onIncrement = {
                    vm.updateSettings(
                        state.totalIntervals, state.warmupSeconds,
                        state.runSeconds + 10,
                        state.restSeconds
                    )
                }
            )
            SettingRow(
                label = "휴식 시간",
                value = state.restSeconds,
                unit = "초",
                min = 10,
                onDecrement = {
                    vm.updateSettings(
                        state.totalIntervals, state.warmupSeconds,
                        state.runSeconds,
                        (state.restSeconds - 10).coerceAtLeast(10)
                    )
                },
                onIncrement = {
                    vm.updateSettings(
                        state.totalIntervals, state.warmupSeconds,
                        state.runSeconds,
                        state.restSeconds + 10
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onDecrement, enabled = value > min) { Text("-") }
            Text(
                text = "$value $unit",
                modifier = Modifier.width(72.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            OutlinedButton(onClick = onIncrement) { Text("+") }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
