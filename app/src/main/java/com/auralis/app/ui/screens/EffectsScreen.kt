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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.components.BalanceControl
import com.auralis.app.ui.components.EffectControl
import com.auralis.app.viewmodel.EqualizerViewModel

@Composable
fun EffectsScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("EFFECTS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Independent audio processors",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        EffectControl(
            title = "Bass Boost",
            supported = ui.availability.bassBoost,
            enabled = ui.bassBoost > 0,
            onEnabledChange = { on -> viewModel.setBassBoost(if (on) 50 else 0) },
            strengthPercent = ui.bassBoost,
            strengthLabel = "Strength",
            onStrengthChange = { viewModel.setBassBoost(it) },
        )
        Spacer(Modifier.height(14.dp))

        EffectControl(
            title = "Virtualizer",
            supported = ui.availability.virtualizer,
            enabled = ui.virtualizer > 0,
            onEnabledChange = { on -> viewModel.setVirtualizer(if (on) 50 else 0) },
            strengthPercent = ui.virtualizer,
            strengthLabel = "Width",
            onStrengthChange = { viewModel.setVirtualizer(it) },
        )
        Spacer(Modifier.height(14.dp))

        EffectControl(
            title = "Loudness",
            supported = ui.availability.loudness,
            enabled = ui.loudness > 0,
            onEnabledChange = { on -> viewModel.setLoudness(if (on) 30 else 0) },
            strengthPercent = ui.loudness,
            strengthLabel = "Gain",
            onStrengthChange = { viewModel.setLoudness(it) },
        )
        Spacer(Modifier.height(14.dp))

        BalanceControl(
            balance = ui.balance,
            onBalanceChange = { viewModel.setBalance(it) },
        )
        Spacer(Modifier.height(24.dp))
    }
}
