package com.zeroboat.timerkit.running

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import com.zeroboat.timerkit.BuildConfig
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningMapScreen(
    state: RunningUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var showContent by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    // 전면 광고 로드 → 표시 → 완료 시 콘텐츠 노출
    LaunchedEffect(Unit) {
        val adUnitId = BuildConfig.INTERSTITIAL_AD_UNIT_ID
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() { showContent = true }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) { showContent = true }
                    }
                    ad.show(activity)
                }
                override fun onAdFailedToLoad(e: LoadAdError) { showContent = true }
            }
        )
        // 광고 로딩 실패 등 예외 상황: 5초 후 강제 진입
        delay(5_000L)
        showContent = true
    }

    if (!showContent) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val routeLatLng = state.routePoints.map { LatLng(it.lat, it.lon) }
    val cameraPositionState = rememberCameraPositionState {
        if (routeLatLng.isNotEmpty()) {
            position = CameraPosition.fromLatLngZoom(routeLatLng.first(), 15f)
        }
    }
    var isMapLoaded by remember { mutableStateOf(false) }

    // 지도가 완전히 로드된 후 경로 전체가 보이도록 카메라 조정
    // onMapLoaded 이전에 newLatLngBounds를 호출하면 뷰 크기 미확정으로 경로가 표시되지 않음
    LaunchedEffect(isMapLoaded, routeLatLng.size) {
        if (isMapLoaded && routeLatLng.size >= 2) {
            val bounds = LatLngBounds.Builder()
                .apply { routeLatLng.forEach { include(it) } }
                .build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 80),
                durationMs = 800
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("러닝 결과") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (routeLatLng.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "위치 데이터가 없습니다\n(위치 권한 허용 후 러닝 시 기록됩니다)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = { isMapLoaded = true }
                ) {
                    Polyline(
                        points = routeLatLng,
                        color = Color(0xFF1976D2),
                        width = 12f
                    )
                    Marker(
                        state = MarkerState(position = routeLatLng.first()),
                        title = "시작",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                    Marker(
                        state = MarkerState(position = routeLatLng.last()),
                        title = "종료",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            // 하단 통계 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStat(label = "거리", value = formatDistance(state.distanceMeters))
                    ResultStat(label = "페이스", value = formatPace(state.distanceMeters, state.runElapsedMillis))
                    if (state.mode == RunningMode.INTERVAL) {
                        ResultStat(label = "인터벌", value = "${state.totalIntervals}회")
                    } else {
                        ResultStat(label = "시간", value = formatElapsed(state.totalElapsedMillis))
                    }
                }

                // 심박수 결과 (데이터 있을 때만)
                if (state.avgHeartRateBpm != null || state.maxHeartRateBpm != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        state.avgHeartRateBpm?.let {
                            ResultStat(label = "평균 심박수", value = "$it BPM")
                        }
                        state.maxHeartRateBpm?.let {
                            ResultStat(label = "최대 심박수", value = "$it BPM")
                        }
                    }
                }

                // km 페이스 기록 (기본 모드)
                if (state.mode == RunningMode.BASIC && state.kmPaceRecords.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "km 기록",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        state.kmPaceRecords.forEach { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${record.km} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatPaceSeconds(record.paceSeconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
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

private fun formatDistance(meters: Float): String =
    if (meters < 1000) "${"%.0f".format(meters)} m"
    else "${"%.2f".format(meters / 1000)} km"

private fun formatPace(meters: Float, runMs: Long): String {
    if (meters < 10f || runMs == 0L) return "--'--\""
    val secPerKm = (runMs / 1000f) / (meters / 1000f)
    return "${(secPerKm / 60).toInt()}'%02d\"".format((secPerKm % 60).toInt())
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
