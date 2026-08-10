package com.auralis.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.app.data.model.PerformanceMode
import com.auralis.app.data.model.ThemeMode
import com.auralis.app.ui.components.SectionHeader
import com.auralis.app.ui.components.SettingsRow
import com.auralis.app.ui.components.SettingsSwitchRow
import com.auralis.app.viewmodel.EqualizerViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings

    val theme by settings.theme.collectAsState(initial = ThemeMode.SYSTEM)
    val performance by settings.performance.collectAsState(initial = PerformanceMode.BALANCED)
    val autoEnable by settings.autoEnable.collectAsState(initial = true)
    val restorePreset by settings.restorePreset.collectAsState(initial = true)
    val applyOnStartup by settings.applyOnStartup.collectAsState(initial = true)
    val eqHaptics by settings.eqHaptics.collectAsState(initial = true)
    val uiHaptics by settings.uiHaptics.collectAsState(initial = true)
    val show3d by settings.show3d.collectAsState(initial = true)
    val showSpectrum by settings.showSpectrum.collectAsState(initial = true)
    val showParticles by settings.showParticles.collectAsState(initial = true)
    val showWaveform by settings.showWaveform.collectAsState(initial = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(20.dp))

        SectionHeader(title = "Appearance")
        SettingsRow(title = "Theme", subtitle = theme.name.lowercase().replaceFirstChar { it.uppercase() }) {
            TextButtonCycle(
                options = ThemeMode.values().map { it.name },
                current = theme.name,
                onSelect = { scope.launch { settings.setTheme(ThemeMode.valueOf(it)) } },
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Visualization")
        SettingsSwitchRow("3D Visualization", show3d, { scope.launch { settings.setShow3d(it) } })
        SettingsSwitchRow("Spectrum", showSpectrum, { scope.launch { settings.setShowSpectrum(it) } })
        SettingsSwitchRow("Particles", showParticles, { scope.launch { settings.setShowParticles(it) } })
        SettingsSwitchRow("Waveform", showWaveform, { scope.launch { settings.setShowWaveform(it) } })

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Audio")
        SettingsSwitchRow("Auto-enable engine", autoEnable, { scope.launch { settings.setAutoEnable(it) } })
        SettingsSwitchRow("Restore last preset", restorePreset, { scope.launch { settings.setRestorePreset(it) } })
        SettingsSwitchRow("Apply on startup", applyOnStartup, { scope.launch { settings.setApplyOnStartup(it) } })

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Haptics")
        SettingsSwitchRow("EQ haptics", eqHaptics, { scope.launch { settings.setEqHaptics(it) } })
        SettingsSwitchRow("Interface haptics", uiHaptics, { scope.launch { settings.setUiHaptics(it) } })

        Spacer(Modifier.height(16.dp))
        SectionHeader(title = "Performance")
        SettingsRow(title = "Visualization quality", subtitle = performance.name.lowercase().replaceFirstChar { it.uppercase() }) {
            TextButtonCycle(
                options = PerformanceMode.values().map { it.name },
                current = performance.name,
                onSelect = { scope.launch { settings.setPerformance(PerformanceMode.valueOf(it)) } },
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader(title = "About")
        SettingsRow(title = "Auralis", subtitle = "Version 1.0.0 · Built with Kotlin") {}
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TextButtonCycle(
    options: List<String>,
    current: String,
    onSelect: (String) -> Unit,
) {
    androidx.compose.material3.TextButton(onClick = {
        val next = options[(options.indexOf(current) + 1) % options.size]
        onSelect(next)
    }) {
        Text(current, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
