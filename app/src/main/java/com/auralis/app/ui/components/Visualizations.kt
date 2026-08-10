package com.auralis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.Accent
import com.auralis.app.ui.theme.AudioActive
import com.auralis.app.ui.theme.TrackColor

/** Thin, low-opacity spectrum bars with smooth values (already smoothed upstream). */
@Composable
fun SpectrumAnalyzerView(
    spectrum: FloatArray,
    modifier: Modifier = Modifier,
    barColor: Color = Accent,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = "Live audio spectrum" },
    ) {
        val n = spectrum.size
        if (n == 0) return@Canvas
        val slot = size.width / n
        val barWidth = slot * 0.45f
        for (i in 0 until n) {
            val v = spectrum[i].coerceIn(0f, 1f)
            val h = v * size.height
            val x = i * slot + (slot - barWidth) / 2
            // baseline track
            drawRect(
                color = TrackColor.copy(alpha = 0.4f),
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height),
            )
            drawRect(
                color = barColor.copy(alpha = 0.35f + v * 0.55f),
                topLeft = Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
            )
        }
    }
}

/** Minimal waveform trace. */
@Composable
fun WaveformView(
    waveform: FloatArray,
    modifier: Modifier = Modifier,
    lineColor: Color = AudioActive,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = "Audio waveform" },
    ) {
        val n = waveform.size
        if (n < 2) return@Canvas
        val mid = size.height / 2
        val stepX = size.width / (n - 1)
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until n) {
            val x = i * stepX
            val y = mid - waveform[i].coerceIn(-1f, 1f) * mid * 0.9f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor.copy(alpha = 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}
