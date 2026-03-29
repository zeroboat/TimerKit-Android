package com.zeroboat.timerkit.running

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RunningScreen(
    modifier: Modifier = Modifier,
    vm: RunningViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    var showMap by rememberSaveable { mutableStateOf(false) }

    if (showMap) {
        RunningMapScreen(state = state, onBack = { showMap = false })
        return
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.onLocationPermissionGranted() }

    LaunchedEffect(Unit) { vm.checkLocationPermission() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 모드 토글 (정지 상태일 때만)
        if (!state.isRunning && !state.isFinished) {
            ModeToggle(
                selected = state.mode,
                onSelect = vm::setMode
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 페이즈 라벨
        val (phaseLabel, phaseColor) = when {
            state.isFinished -> "DONE" to MaterialTheme.colorScheme.primary
            state.mode == RunningMode.BASIC && state.isRunning -> "RUN" to MaterialTheme.colorScheme.error
            state.mode == RunningMode.BASIC -> "기본 러닝" to MaterialTheme.colorScheme.secondary
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

        // 시간 표시: 기본 모드는 경과 시간(올라감), 인터벌은 카운트다운
        val timeText = when {
            state.mode == RunningMode.BASIC -> formatElapsed(state.totalElapsedMillis)
            else -> formatCountdown(state.remainingMillis)
        }
        Text(
            text = timeText,
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

        // 인터벌 진행 표시 (인터벌 모드만)
        if (state.mode == RunningMode.INTERVAL) {
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 거리/페이스
        if (state.isLocationGranted) {
            LocationStatsRow(
                distanceMeters   = state.distanceMeters,
                runElapsedMillis = state.runElapsedMillis
            )
        } else {
            LocationPermissionBanner(
                onRequest = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 컨트롤 버튼
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = vm::reset,
                modifier = Modifier.weight(1f)
            ) { Text("Reset") }
            Button(
                onClick = { if (state.isRunning) vm.pause() else vm.start() },
                enabled = !state.isFinished,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isRunning) "Pause" else "Start") }
        }

        // 완료 시 지도 결과 보기
        if (state.isFinished) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showMap = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("지도에서 결과 보기")
            }
        }

        // km 페이스 기록 (기본 모드, 달리는 중 또는 완료)
        if (state.mode == RunningMode.BASIC && state.kmPaceRecords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "km 기록",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.kmPaceRecords.forEach { record ->
                KmPaceRow(record)
            }
        }

        // 설정 (인터벌 모드, 정지 상태일 때만)
        if (state.mode == RunningMode.INTERVAL && !state.isRunning && !state.isFinished) {
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
            SettingRow("인터벌 수", state.totalIntervals, "회", 1,
                onDecrement = {
                    vm.updateSettings((state.totalIntervals - 1).coerceAtLeast(1),
                        state.warmupSeconds, state.runSeconds, state.restSeconds)
                },
                onIncrement = {
                    vm.updateSettings(state.totalIntervals + 1,
                        state.warmupSeconds, state.runSeconds, state.restSeconds)
                }
            )
            SettingRow("워밍업", state.warmupSeconds / 60, "분", 0,
                onDecrement = {
                    vm.updateSettings(state.totalIntervals,
                        ((state.warmupSeconds / 60) - 1).coerceAtLeast(0) * 60,
                        state.runSeconds, state.restSeconds)
                },
                onIncrement = {
                    vm.updateSettings(state.totalIntervals,
                        ((state.warmupSeconds / 60) + 1) * 60,
                        state.runSeconds, state.restSeconds)
                }
            )
            SettingRow("러닝 시간", state.runSeconds, "초", 10,
                onDecrement = {
                    vm.updateSettings(state.totalIntervals, state.warmupSeconds,
                        (state.runSeconds - 10).coerceAtLeast(10), state.restSeconds)
                },
                onIncrement = {
                    vm.updateSettings(state.totalIntervals, state.warmupSeconds,
                        state.runSeconds + 10, state.restSeconds)
                }
            )
            SettingRow("휴식 시간", state.restSeconds, "초", 10,
                onDecrement = {
                    vm.updateSettings(state.totalIntervals, state.warmupSeconds,
                        state.runSeconds, (state.restSeconds - 10).coerceAtLeast(10))
                },
                onIncrement = {
                    vm.updateSettings(state.totalIntervals, state.warmupSeconds,
                        state.runSeconds, state.restSeconds + 10)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModeToggle(selected: RunningMode, onSelect: (RunningMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(RunningMode.BASIC to "기본 러닝", RunningMode.INTERVAL to "인터벌").forEach { (mode, label) ->
            val isSelected = selected == mode
            if (isSelected) {
                Button(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f)
                ) { Text(label) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f)
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun KmPaceRow(record: KmPaceRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${record.km} km",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatPaceSeconds(record.paceSeconds),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LocationStatsRow(distanceMeters: Float, runElapsedMillis: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatBox(label = "거리", value = formatDistance(distanceMeters))
        StatBox(label = "페이스", value = formatPace(distanceMeters, runElapsedMillis))
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationPermissionBanner(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "거리 측정",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "위치 권한을 허용하면 달린 거리를 기록합니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = onRequest) { Text("허용") }
        }
    }
}

@Composable
private fun SettingRow(
    label: String, value: Int, unit: String, min: Int,
    onDecrement: () -> Unit, onIncrement: () -> Unit
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

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000) "${"%.0f".format(meters)} m"
    else "${"%.2f".format(meters / 1000)} km"
}

private fun formatPace(meters: Float, runMs: Long): String {
    if (meters < 10f || runMs == 0L) return "--'--\""
    val secPerKm = (runMs / 1000f) / (meters / 1000f)
    val min = (secPerKm / 60).toInt()
    val sec = (secPerKm % 60).toInt()
    return "$min'%02d\"".format(sec)
}
