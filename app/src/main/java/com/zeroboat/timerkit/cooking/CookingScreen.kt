package com.zeroboat.timerkit.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeroboat.timerkit.common.OverlayToggleButton
import com.zeroboat.timerkit.common.TimerService

@Composable
fun CookingScreen(
    modifier: Modifier = Modifier,
    vm: CookingViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val isOverlayVisible by TimerService.isOverlayVisible.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val anyRunning = state.timers.any { it.isRunning }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.timers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "타이머를 추가하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "오른쪽 아래 + 버튼을 눌러 시작",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 80.dp), // FAB 공간 확보
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.timers, key = { it.id }) { timer ->
                    TimerCard(
                        timer = timer,
                        onStart = { vm.startTimer(timer.id) },
                        onPause = { vm.pauseTimer(timer.id) },
                        onReset = { vm.resetTimer(timer.id) },
                        onDelete = { vm.removeTimer(timer.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "타이머 추가")
        }

        if (anyRunning) {
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                OverlayToggleButton(
                    isOverlayVisible = isOverlayVisible,
                    onToggle = vm::toggleOverlay
                )
            }
        }
    }

    if (showDialog) {
        AddTimerDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, minutes, seconds ->
                vm.addTimer(name, minutes * 60 + seconds)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TimerCard(
    timer: CookingTimer,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingColor = when {
        timer.isFinished -> MaterialTheme.colorScheme.error
        timer.remainingSeconds <= 10 && !timer.isFinished -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (timer.isFinished)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (timer.isFinished) "완료!" else formatTime(timer.remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = remainingColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset) { Text("Reset") }
                if (!timer.isFinished) {
                    OutlinedButton(
                        onClick = { if (timer.isRunning) onPause() else onStart() }
                    ) {
                        Text(if (timer.isRunning) "Pause" else "Start")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, minutes: Int, seconds: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("5") }
    var secondsText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("타이머 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("분") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter { c -> c.isDigit() } },
                        label = { Text("초") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minutes = minutesText.toIntOrNull() ?: 0
                val seconds = secondsText.toIntOrNull() ?: 0
                if (minutes > 0 || seconds > 0) {
                    onConfirm(name, minutes, seconds.coerceIn(0, 59))
                }
            }) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
