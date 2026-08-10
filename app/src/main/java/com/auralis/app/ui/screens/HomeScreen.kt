package com.auralis.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.app.data.model.BuiltInPresets
import com.auralis.app.data.model.PerformanceMode
import com.auralis.app.data.model.QuickMode
import com.auralis.app.ui.components.AudioSessionCard
import com.auralis.app.ui.components.AuralisTopBar
import com.auralis.app.ui.components.EqualizerBandControl
import com.auralis.app.ui.components.PresetSelector
import com.auralis.app.ui.components.SectionHeader
import com.auralis.app.ui.components.SpectrumAnalyzerView
import com.auralis.app.ui.components.ThreeDAudioVisualizer
import com.auralis.app.ui.components.WaveformView
import com.auralis.app.visualization.Audio3DRenderer
import com.auralis.app.viewmodel.EqualizerViewModel
import com.auralis.app.viewmodel.VisualizationViewModel

@Composable
fun HomeScreen(
    eqViewModel: EqualizerViewModel,
    visViewModel: VisualizationViewModel,
    reducedMotion: Boolean,
    performance: PerformanceMode,
    modifier: Modifier = Modifier,
) {
    val ui by eqViewModel.ui.collectAsState()
    val vis by visViewModel.ui.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        AuralisTopBar()

        // Session card
        Box(Modifier.padding(horizontal = 24.dp)) {
            AudioSessionCard(isMusicActive = ui.isMusicActive)
        }

        Spacer(Modifier.height(20.dp))

        // 3D audio field
        SectionHeader(
            title = "3D Audio Field",
            trailing = if (vis.available) "LIVE" else "IDLE",
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        val quality = when (performance) {
            PerformanceMode.LOW -> Audio3DRenderer.Quality.LOW
            PerformanceMode.BALANCED -> Audio3DRenderer.Quality.BALANCED
            PerformanceMode.HIGH -> Audio3DRenderer.Quality.HIGH
        }
        ThreeDAudioVisualizer(
            bass = vis.bass,
            rms = vis.rms,
            highEnergy = vis.spectrum.takeLast(8).average().toFloat().coerceIn(0f, 1f),
            playing = vis.playing,
            reducedMotion = reducedMotion,
            quality = quality,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Spectrum + waveform
        SectionHeader(
            title = "Spectrum",
            trailing = if (vis.available) "LIVE" else "UNAVAILABLE",
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(horizontal = 24.dp)) {
            SpectrumAnalyzerView(spectrum = vis.spectrum)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(horizontal = 24.dp)) {
            WaveformView(waveform = vis.waveform)
        }

        Spacer(Modifier.height(20.dp))

        // Presets
        SectionHeader(
            title = "Preset",
            trailing = ui.activePresetName,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        PresetSelector(
            presets = BuiltInPresets.all + ui.customPresets,
            selectedId = ui.activePresetId,
            onSelect = { eqViewModel.applyPreset(it) },
        )

        Spacer(Modifier.height(20.dp))

        // Quick modes
        SectionHeader(title = "Mode", modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickMode.values().forEach { mode ->
                QuickModeChip(label = mode.name, onClick = { eqViewModel.applyQuickMode(mode) })
            }
        }

        Spacer(Modifier.height(24.dp))

        // Equalizer bands
        SectionHeader(
            title = "Equalizer",
            trailing = if (ui.availability.equalizer) "${ui.capabilities?.bandCount ?: 0} BAND" else "UNAVAILABLE",
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        if (ui.availability.equalizer && ui.capabilities != null) {
            val caps = ui.capabilities!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                caps.bandFrequenciesHz.forEachIndexed { index, freq ->
                    EqualizerBandControl(
                        frequencyHz = freq,
                        gainDb = ui.bandGains.getOrElse(index) { 0f },
                        minDb = caps.minLevelDb,
                        maxDb = caps.maxLevelDb,
                        onGainChange = { eqViewModel.setBandGain(index, it) },
                    )
                }
            }
        } else {
            Text(
                text = "Equalizer unavailable on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Master controls
        SectionHeader(title = "Master", trailing = "${ui.masterGain} dB", modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        MasterGainRow(
            masterGain = ui.masterGain,
            onMasterGain = { eqViewModel.setMasterGain(it) },
            treble = ui.treble,
            onTreble = { eqViewModel.setTreble(it) },
            bass = ui.bassBoost,
            onBass = { eqViewModel.setBassBoost(it) },
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun QuickModeChip(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MasterGainRow(
    masterGain: Float,
    onMasterGain: (Float) -> Unit,
    treble: Int,
    onTreble: (Int) -> Unit,
    bass: Int,
    onBass: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MasterKnob(label = "MASTER", value = "${masterGain.toInt()} dB") { delta ->
            onMasterGain((masterGain + delta).coerceIn(-12f, 12f))
        }
        MasterKnob(label = "BASS", value = "$bass%") { delta ->
            onBass((bass + delta.toInt()).coerceIn(0, 100))
        }
        MasterKnob(label = "TREBLE", value = "$treble%") { delta ->
            onTreble((treble + delta.toInt()).coerceIn(0, 100))
        }
    }
}

@Composable
private fun MasterKnob(label: String, value: String, onAdjust: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Row {
            androidx.compose.material3.TextButton(onClick = { onAdjust(-1f) }) { Text("−") }
            androidx.compose.material3.TextButton(onClick = { onAdjust(1f) }) { Text("+") }
        }
    }
}
