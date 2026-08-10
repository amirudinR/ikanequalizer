package com.auralis.app.data.model

import kotlinx.serialization.Serializable

/** A single equalizer band: [frequency] in Hz, [gainDb] in decibels. */
@Serializable
data class EqualizerBand(
    val frequency: Int,
    val gainDb: Float,
)

/** A named equalizer preset. [id] is stable; built-ins use "builtin_*". */
@Serializable
data class EqualizerPreset(
    val id: String,
    val name: String,
    val bands: List<EqualizerBand>,
    val bassBoost: Int = 0,      // 0..100
    val virtualizer: Int = 0,    // 0..100
    val loudness: Int = 0,       // 0..100
    val isBuiltIn: Boolean = false,
)

/** Portable JSON format for import/export. */
@Serializable
data class PresetExport(
    val name: String,
    val bands: Map<String, Float>,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
)

/** Device-reported equalizer capabilities. */
data class EqualizerCapabilities(
    val bandCount: Int,
    val bandFrequenciesHz: List<Int>,
    val minLevelDb: Float,
    val maxLevelDb: Float,
)

/** Which hardware effects are usable on this device/session. */
data class EffectAvailability(
    val equalizer: Boolean,
    val bassBoost: Boolean,
    val virtualizer: Boolean,
    val loudness: Boolean,
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class PerformanceMode { LOW, BALANCED, HIGH }

enum class QuickMode { PURE, BASS, VOCAL, SPACE, NIGHT }
