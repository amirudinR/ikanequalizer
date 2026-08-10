package com.auralis.app.data.repository

import android.content.Context
import android.util.Log
import com.auralis.app.data.model.BuiltInPresets
import com.auralis.app.data.model.EqualizerBand
import com.auralis.app.data.model.EqualizerPreset
import com.auralis.app.data.model.PresetExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Stores custom presets as JSON files in internal storage and provides
 * validated import/export. Built-in presets are always present.
 */
class PresetRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File by lazy { File(context.filesDir, "presets").apply { mkdirs() } }

    private val _customPresets = MutableStateFlow<List<EqualizerPreset>>(emptyList())
    val customPresets: StateFlow<List<EqualizerPreset>> = _customPresets.asStateFlow()

    val allPresets: List<EqualizerPreset>
        get() = BuiltInPresets.all + _customPresets.value

    suspend fun loadCustom() = withContext(Dispatchers.IO) {
        val loaded = dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { file ->
                runCatching {
                    json.decodeFromString<EqualizerPreset>(file.readText())
                }.onFailure {
                    Log.w(TAG, "Skipping malformed preset ${file.name}: ${it.message}")
                }.getOrNull()
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        _customPresets.value = loaded
        Log.i(TAG, "Loaded ${loaded.size} custom presets")
    }

    suspend fun save(preset: EqualizerPreset): EqualizerPreset = withContext(Dispatchers.IO) {
        val toSave = preset.copy(id = if (preset.id.isBlank() || preset.isBuiltIn) "custom_" + UUID.randomUUID() else preset.id, isBuiltIn = false)
        File(dir, "${toSave.id}.json").writeText(json.encodeToString(EqualizerPreset.serializer(), toSave))
        loadCustom()
        toSave
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        loadCustom()
    }

    suspend fun duplicate(id: String, newName: String): EqualizerPreset? {
        val source = findById(id) ?: return null
        return save(source.copy(id = "", name = newName, isBuiltIn = false))
    }

    fun findById(id: String): EqualizerPreset? = allPresets.firstOrNull { it.id == id }

    /** Export a preset to the portable JSON format. */
    fun export(preset: EqualizerPreset): String {
        val export = PresetExport(
            name = preset.name,
            bands = preset.bands.associate { it.frequency.toString() to it.gainDb },
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudness = preset.loudness,
        )
        return json.encodeToString(PresetExport.serializer(), export)
    }

    /** Parse and validate imported JSON. Returns null on malformed data. */
    suspend fun import(jsonText: String): EqualizerPreset? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = json.decodeFromString<PresetExport>(jsonText)
            // Validate: name non-blank, gains finite and within ±15 dB
            require(parsed.name.isNotBlank()) { "blank name" }
            val bands = parsed.bands.mapNotNull { (freqStr, gain) ->
                val freq = freqStr.toIntOrNull() ?: return@mapNotNull null
                if (!gain.isFinite() || gain < -15f || gain > 15f) return@mapNotNull null
                EqualizerBand(freq, gain)
            }
            require(bands.isNotEmpty()) { "no valid bands" }
            save(
                EqualizerPreset(
                    id = "",
                    name = parsed.name,
                    bands = bands.sortedBy { it.frequency },
                    bassBoost = parsed.bassBoost.coerceIn(0, 100),
                    virtualizer = parsed.virtualizer.coerceIn(0, 100),
                    loudness = parsed.loudness.coerceIn(0, 100),
                )
            )
        }.onFailure {
            Log.w(TAG, "Import failed: ${it.message}")
        }.getOrNull()
    }

    companion object { private const val TAG = "PresetRepository" }
}
