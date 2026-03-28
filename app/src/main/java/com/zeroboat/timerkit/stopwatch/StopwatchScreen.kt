package com.zeroboat.timerkit.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StopwatchScreen(
    modifier: Modifier = Modifier,
    vm: StopwatchViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 시간 표시
        Text(
            text = formatTime(state.elapsedMillis),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 컨트롤 버튼
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 리셋 버튼
            OutlinedButton(
                onClick = vm::reset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }

            // 시작/일시정지 버튼
            Button(
                onClick = { if (state.isRunning) vm.pause() else vm.start() },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isRunning) "Pause" else "Start")
            }

            // 랩 버튼
            OutlinedButton(
                onClick = vm::lap,
                enabled = state.isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text("Lap")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 랩 목록
        if (state.laps.isNotEmpty()) {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lap",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn {
                itemsIndexed(state.laps.reversed()) { reversedIndex, lapMillis ->
                    val lapNumber = state.laps.size - reversedIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lap $lapNumber",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatTime(lapMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = millis / 60_000
    val seconds = (millis % 60_000) / 1_000
    val centiseconds = (millis % 1_000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}
