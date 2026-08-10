package com.auralis.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auralis.app.data.model.BuiltInPresets
import com.auralis.app.data.model.EqualizerPreset
import com.auralis.app.ui.components.SectionHeader
import com.auralis.app.viewmodel.EqualizerViewModel

@Composable
fun PresetsScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("PRESETS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Built-in and custom equalizer curves",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { showSaveDialog = true }) { Text("SAVE CURRENT") }
            TextButton(onClick = { showImportDialog = true }) { Text("IMPORT") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader(title = "Built-in") }
            items(BuiltInPresets.all, key = { it.id }) { preset ->
                PresetRow(
                    preset = preset,
                    selected = preset.id == ui.activePresetId,
                    onApply = { viewModel.applyPreset(preset) },
                    onExport = { viewModel.exportPreset(preset.id) },
                    onDelete = null,
                    onDuplicate = { viewModel.duplicatePreset(preset.id, "${preset.name} copy") },
                )
            }
            item { Spacer(Modifier.height(8.dp)); SectionHeader(title = "Custom") }
            if (ui.customPresets.isEmpty()) {
                item {
                    Text(
                        "No custom presets yet. Save the current EQ to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ui.customPresets, key = { it.id }) { preset ->
                PresetRow(
                    preset = preset,
                    selected = preset.id == ui.activePresetId,
                    onApply = { viewModel.applyPreset(preset) },
                    onExport = { viewModel.exportPreset(preset.id) },
                    onDelete = { viewModel.deletePreset(preset.id) },
                    onDuplicate = { viewModel.duplicatePreset(preset.id, "${preset.name} copy") },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPresetName.isNotBlank()) {
                        viewModel.saveCurrentAsPreset(newPresetName.trim())
                        newPresetName = ""
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import preset") },
            text = {
                Column {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it; importError = false },
                        label = { Text("Preset JSON") },
                        isError = importError,
                    )
                    if (importError) {
                        Text(
                            "Invalid preset data",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importPreset(importText) { ok ->
                        if (ok) {
                            importText = ""
                            showImportDialog = false
                        } else {
                            importError = true
                        }
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PresetRow(
    preset: EqualizerPreset,
    selected: Boolean,
    onApply: () -> Unit,
    onExport: () -> String?,
    onDelete: (() -> Unit)?,
    onDuplicate: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Preset ${preset.name}${if (selected) ", active" else ""}" },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${preset.bands.size} bands · bass ${preset.bassBoost}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onApply) { Text(if (selected) "ACTIVE" else "APPLY") }
            TextButton(onClick = onDuplicate) { Text("COPY") }
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("DEL", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
