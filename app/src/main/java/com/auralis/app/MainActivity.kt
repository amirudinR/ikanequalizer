package com.auralis.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auralis.app.ui.navigation.AuralisNavHost
import com.auralis.app.ui.theme.AuralisTheme
import com.auralis.app.viewmodel.EqualizerViewModel
import com.auralis.app.viewmodel.VisualizationViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var eqViewModel: EqualizerViewModel? = null

    private val recordAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) eqViewModel?.startVisualizer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val eqVm: EqualizerViewModel = viewModel()
            eqViewModel = eqVm
            val visVm = remember {
                VisualizationViewModel(eqVm.engine.spectrumAnalyzer, eqVm.engine.sessionManager.isMusicActive)
            }
            val theme by eqVm.theme.collectAsState()

            // Reduced-motion: honor the system animator duration scale
            val reducedMotion = remember {
                android.provider.Settings.Global.getFloat(
                    contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            }

            AuralisTheme(themeMode = theme) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onDone = { showSplash = false })
                } else {
                    AuralisNavHost(
                        eqViewModel = eqVm,
                        visViewModel = visVm,
                        reducedMotion = reducedMotion,
                    )
                }
            }
        }
        maybeStartVisualizer()
    }

    /** Start the visualizer only if RECORD_AUDIO is already granted; otherwise ask once. */
    private fun maybeStartVisualizer() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> eqViewModel?.startVisualizer()
            else -> recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        eqViewModel?.stopVisualizer()
        super.onDestroy()
    }
}

@Composable
private fun SplashScreen(onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "splash",
    )
    LaunchedEffect(Unit) {
        visible = true
        delay(800)
        onDone()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha)) {
                Text(
                    "AURALIS",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "AUDIO LABORATORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
