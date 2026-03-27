package com.zeroboat.timerkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.zeroboat.timerkit.ui.theme.TimerKitAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimerKitAndroidTheme {
                TimerKitAndroidApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun TimerKitAndroidApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.StopWatch -> StopWatchScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.Tabata -> TabataScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to TimerKit Android!\n\nChoose a timer from the navigation.",
        modifier = modifier
    )
}

@Composable
fun StopWatchScreen(modifier: Modifier = Modifier) {
    Text(
        text = "StopWatch Timer\n\nFeature coming soon...",
        modifier = modifier
    )
}

@Composable
fun TabataScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Tabata Timer\n\nFeature coming soon...",
        modifier = modifier
    )
}

@Composable
fun CookingScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Cooking Timer\n\nFeature coming soon...",
        modifier = modifier
    )
}

@Composable
fun RunningScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Running Timer\n\nFeature coming soon...",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TimerKitAndroidTheme {
        Greeting("Android")
    }
}