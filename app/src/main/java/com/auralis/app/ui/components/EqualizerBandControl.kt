package com.auralis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.Accent
import com.auralis.app.ui.theme.TextPrimary
import com.auralis.app.ui.theme.TrackColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Custom vertical EQ band control. Drag to adjust, double-tap to reset to 0 dB,
 * long-press to read the exact value. Haptics fire only at meaningful thresholds
 * (0 dB crossing, min/max) — never continuously while dragging.
 */
@Composable
fun EqualizerBandControl(
    frequencyHz: Int,
    gainDb: Float,
    minDb: Float,
    maxDb: Float,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    var showValue by remember { mutableStateOf(false) }

    val freqLabel = if (frequencyHz >= 1000) "${frequencyHz / 1000}K" else "$frequencyHz"
    val dbLabel = "${if (gainDb >= 0) "+" else ""}${gainDb.roundToInt()}"

    Column(
        modifier = modifier
            .width(44.dp)
            .semantics {
                contentDescription = "$frequencyHz Hertz equalizer band, ${gainDb.roundToInt()} decibels"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Value label (always visible while dragging / on long-press)
        Text(
            text = dbLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (dragging || showValue) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .height(160.dp)
                .width(28.dp)
                .pointerInput(minDb, maxDb) {
                    detectTapGestures(
                        onDoubleTap = {
                            onGainChange(0f)
                            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onLongPress = { showValue = true },
                        onTap = { showValue = !showValue },
                    )
                }
                .pointerInput(minDb, maxDb, gainDb) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            showValue = true
                        },
                        onDragEnd = {
                            dragging = false
                            showValue = false
                        },
                        onDragCancel = {
                            dragging = false
                            showValue = false
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val range = maxDb - minDb
                        val dbPerPx = range / 160f // approx track height in px-ish
                        val prev = gainDb
                        val next = (gainDb - dragAmount.y * dbPerPx).coerceIn(minDb, maxDb)
                        // Haptic at 0 dB crossing and at bounds
                        if (hapticsEnabled) {
                            val crossedZero = (prev < 0 && next >= 0) || (prev > 0 && next <= 0)
                            val hitBound = (next == minDb && prev != minDb) || (next == maxDb && prev != maxDb)
                            if (crossedZero || hitBound) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        onGainChange(next)
                    }
                },
        ) {
            val cx = size.width / 2
            val trackWidth = 3.dp.toPx()
            // Track
            drawRect(
                color = TrackColor,
                topLeft = Offset(cx - trackWidth / 2, 0f),
                size = androidx.compose.ui.geometry.Size(trackWidth, size.height),
            )
            // Zero line
            val zeroFrac = maxDb / (maxDb - minDb) // fraction from top where 0 dB sits
            val zeroY = size.height * zeroFrac
            drawLine(
                color = TrackColor,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
            )
            // Thumb
            val frac = (maxDb - gainDb) / (maxDb - minDb)
            val thumbY = size.height * frac
            drawCircle(
                color = if (dragging) Accent else TextPrimary,
                radius = if (dragging) 9.dp.toPx() else 7.dp.toPx(),
                center = Offset(cx, thumbY),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = freqLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
