package com.auralis.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auralis.app.data.model.EqualizerPreset

/** Horizontal preset selector — text-first, minimal, with a subtle accent underline. */
@Composable
fun PresetSelector(
    presets: List<EqualizerPreset>,
    selectedId: String,
    onSelect: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            val selected = preset.id == selectedId
            val textColor = if (selected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant
            val accent = MaterialTheme.colorScheme.primary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(preset) }
                    .semantics { contentDescription = "Preset ${preset.name}${if (selected) ", selected" else ""}" }
                    .drawBehind {
                        if (selected) {
                            val y = size.height - 2.dp.toPx()
                            drawLine(
                                color = accent,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = preset.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
            }
        }
    }
}
