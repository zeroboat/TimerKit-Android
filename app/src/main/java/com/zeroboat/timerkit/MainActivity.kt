package com.zeroboat.timerkit

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.zeroboat.timerkit.BuildConfig
import com.zeroboat.timerkit.common.AdHelper
import com.zeroboat.timerkit.cooking.CookingScreen
import com.zeroboat.timerkit.running.RunningScreen
import com.zeroboat.timerkit.stopwatch.StopwatchScreen
import com.zeroboat.timerkit.tabata.TabataScreen
import com.zeroboat.timerkit.ui.theme.TimerKitAndroidTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 허용/거부 결과 무시 - 서비스 실행엔 영향 없음 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdHelper.initialize(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            TimerKitAndroidTheme {
                TimerKitAndroidApp()
            }
        }
    }
}

@Composable
fun TimerKitAndroidApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.StopWatch -> StopwatchScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.Tabata -> TabataScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigate = { currentDestination = it }
                )
                AppDestinations.Cooking -> CookingScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.Running -> RunningScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    StopWatch("StopWatch", Icons.Filled.Timer),
    Tabata("Tabata", Icons.Filled.FitnessCenter),
    HOME("Home", Icons.Default.Home),
    Cooking("Cooking", Icons.Filled.OutdoorGrill),
    Running("Running", Icons.AutoMirrored.Filled.DirectionsRun),
}

private data class TimerShortcut(
    val destination: AppDestinations,
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val shortcuts = listOf(
    TimerShortcut(AppDestinations.StopWatch, Icons.Filled.Timer, "스톱워치", "시간 측정 + 랩 기록"),
    TimerShortcut(AppDestinations.Tabata, Icons.Filled.FitnessCenter, "Tabata", "인터벌 운동 타이머"),
    TimerShortcut(AppDestinations.Running, Icons.AutoMirrored.Filled.DirectionsRun, "러닝", "워밍업 + 인터벌 러닝"),
    TimerShortcut(AppDestinations.Cooking, Icons.Filled.OutdoorGrill, "쿠킹", "다중 타이머 동시 실행"),
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (AppDestinations) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "TimerKit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "원하는 타이머를 선택하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(shortcuts) { shortcut ->
                ShortcutCard(shortcut = shortcut, onClick = { onNavigate(shortcut.destination) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        BannerAd(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    val adUnitId = BuildConfig.BANNER_AD_UNIT_ID
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
private fun ShortcutCard(shortcut: TimerShortcut, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = shortcut.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}
