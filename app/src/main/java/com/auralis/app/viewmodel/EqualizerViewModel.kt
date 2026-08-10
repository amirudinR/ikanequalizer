package com.auralis.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.audio.AudioEngine
import com.auralis.app.data.local.SettingsRepository
import com.auralis.app.data.model.BuiltInPresets
import com.auralis.app.data.model.EffectAvailability
import com.auralis.app.data.model.EqualizerBand
import com.auralis.app.data.model.EqualizerCapabilities
import com.auralis.app.data.model.EqualizerPreset
import com.auralis.app.data.model.PerformanceMode
import com.auralis.app.data.model.QuickMode
import com.auralis.app.data.model.ThemeMode
import com.auralis.app.data.repository.PresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqualizerUiState(
    val engineReady: Boolean = false,
    val engineEnabled: Boolean = false,
    val availability: EffectAvailability = EffectAvailability(false, false, false, false),
    val capabilities: EqualizerCapabilities? = null,
    /** Current per-device-band gains in dB, sized to capabilities.bandCount. */
    val bandGains: List<Float> = emptyList(),
    val activePresetId: String = "",
    val activePresetName: String = "Flat",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudness: Int = 0,
    val masterGain: Float = 0f,
    val treble: Int = 50,
    val balance: Float = 0f,
    val isMusicActive: Boolean = false,
    val visualizerAvailable: Boolean = false,
    val customPresets: List<EqualizerPreset> = emptyList(),
    val statusMessage: String? = null,
)

class EqualizerViewModel(app: Application) : AndroidViewModel(app) {

    val engine = AudioEngine(app.applicationContext)
    val settings = SettingsRepository(app.applicationContext)
    val presets = PresetRepository(app.applicationContext)

    private val _ui = MutableStateFlow(EqualizerUiState())
    val ui: StateFlow<EqualizerUiState> = _ui.asStateFlow()

    val theme: StateFlow<ThemeMode> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val performance: StateFlow<PerformanceMode> = settings.performance
        .stateIn(viewModelScope, SharingStarted.Eagerly, PerformanceMode.BALANCED)

    init {
        viewModelScope.launch {
            presets.loadCustom()
            engine.initialize()
            val caps = engine.capabilities.value
            _ui.value = _ui.value.copy(
                engineReady = true,
                availability = engine.availability.value,
                capabilities = caps,
                bandGains = if (caps != null) List(caps.bandCount) { 0f } else emptyList(),
                customPresets = presets.customPresets.value,
            )
            observeFlows()
            restoreState()
        }
    }

    private fun observeFlows() {
        viewModelScope.launch {
            engine.sessionManager.isMusicActive.collect { active ->
                _ui.value = _ui.value.copy(isMusicActive = active)
            }
        }
        viewModelScope.launch {
            engine.spectrumAnalyzer.available.collect { avail ->
                _ui.value = _ui.value.copy(visualizerAvailable = avail)
            }
        }
        viewModelScope.launch {
            presets.customPresets.collect { list ->
                _ui.value = _ui.value.copy(customPresets = list)
            }
        }
    }

    private suspend fun restoreState() {
        // Collect persisted one-shot values
        var restore = true
        var applyOnStartup = true
        var autoEnable = true
        viewModelScope.launch { settings.restorePreset.collect { restore = it } }
        viewModelScope.launch { settings.applyOnStartup.collect { applyOnStartup = it } }
        viewModelScope.launch { settings.autoEnable.collect { autoEnable = it } }
        viewModelScope.launch { settings.masterGain.collect { _ui.value = _ui.value.copy(masterGain = it) } }
        viewModelScope.launch { settings.balance.collect { _ui.value = _ui.value.copy(balance = it) } }
        viewModelScope.launch { settings.treble.collect { _ui.value = _ui.value.copy(treble = it) } }

        // Give the collectors a beat to emit, then restore preset
        kotlinx.coroutines.delay(50)
        if (restore) {
            var lastId: String? = null
            val job = viewModelScope.launch { settings.lastPresetId.collect { lastId = it } }
            kotlinx.coroutines.delay(50)
            job.cancel()
            val preset = lastId?.let { presets.findById(it) } ?: BuiltInPresets.all.first()
            applyPreset(preset, persist = false)
        } else {
            applyPreset(BuiltInPresets.all.first(), persist = false)
        }
        if (applyOnStartup && autoEnable) setEngineEnabled(true)
    }

    fun setEngineEnabled(enabled: Boolean) {
        engine.setEngineEnabled(enabled)
        _ui.value = _ui.value.copy(engineEnabled = enabled)
    }

    fun setBandGain(deviceBandIndex: Int, gainDb: Float) {
        engine.setBandGain(deviceBandIndex, gainDb)
        val gains = _ui.value.bandGains.toMutableList()
        if (deviceBandIndex in gains.indices) {
            gains[deviceBandIndex] = gainDb
            _ui.value = _ui.value.copy(bandGains = gains, activePresetId = "", activePresetName = "Custom")
        }
    }

    fun applyPreset(preset: EqualizerPreset, persist: Boolean = true) {
        engine.applyPreset(preset)
        // Reflect preset gains onto device bands for the UI
        val caps = _ui.value.capabilities
        val gains = if (caps != null) {
            caps.bandFrequenciesHz.map { deviceFreq ->
                // find nearest preset band
                preset.bands.minByOrNull { kotlin.math.abs(it.frequency - deviceFreq) }?.gainDb ?: 0f
            }
        } else emptyList()
        _ui.value = _ui.value.copy(
            bandGains = gains,
            activePresetId = preset.id,
            activePresetName = preset.name,
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            loudness = preset.loudness,
        )
        if (persist) viewModelScope.launch { settings.setLastPresetId(preset.id) }
    }

    fun resetBands() {
        val caps = _ui.value.capabilities ?: return
        repeat(caps.bandCount) { engine.setBandGain(it, 0f) }
        _ui.value = _ui.value.copy(
            bandGains = List(caps.bandCount) { 0f },
            activePresetId = BuiltInPresets.all.first().id,
            activePresetName = "Flat",
        )
    }

    fun setBassBoost(percent: Int) {
        engine.setBassBoost(percent)
        _ui.value = _ui.value.copy(bassBoost = percent)
    }

    fun setVirtualizer(percent: Int) {
        engine.setVirtualizer(percent)
        _ui.value = _ui.value.copy(virtualizer = percent)
    }

    fun setLoudness(percent: Int) {
        engine.setLoudness(percent)
        _ui.value = _ui.value.copy(loudness = percent)
    }

    fun setMasterGain(db: Float) {
        _ui.value = _ui.value.copy(masterGain = db)
        viewModelScope.launch { settings.setMasterGain(db) }
    }

    fun setTreble(percent: Int) {
        _ui.value = _ui.value.copy(treble = percent)
        viewModelScope.launch { settings.setTreble(percent) }
        // Map treble onto the highest device band for a real audible effect
        val caps = _ui.value.capabilities ?: return
        val topBand = caps.bandCount - 1
        val db = ((percent - 50) / 50f) * 6f // ±6 dB on the top band
        engine.setBandGain(topBand, db)
    }

    fun setBalance(v: Float) {
        _ui.value = _ui.value.copy(balance = v)
        viewModelScope.launch { settings.setBalance(v) }
    }

    fun applyQuickMode(mode: QuickMode) {
        when (mode) {
            QuickMode.PURE -> { resetBands(); setBassBoost(0); setVirtualizer(0); setLoudness(0) }
            QuickMode.BASS -> applyPreset(BuiltInPresets.byId("builtin_deep_bass") ?: return)
            QuickMode.VOCAL -> applyPreset(BuiltInPresets.byId("builtin_vocal") ?: return)
            QuickMode.SPACE -> { applyPreset(BuiltInPresets.byId("builtin_movie") ?: return); setVirtualizer(70) }
            QuickMode.NIGHT -> applyPreset(BuiltInPresets.byId("builtin_night") ?: return)
        }
    }

    // ---- Custom preset ops ----
    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val caps = _ui.value.capabilities ?: return@launch
            val bands = caps.bandFrequenciesHz.mapIndexed { i, f ->
                EqualizerBand(f, _ui.value.bandGains.getOrElse(i) { 0f })
            }
            val preset = EqualizerPreset(
                id = "",
                name = name,
                bands = bands,
                bassBoost = _ui.value.bassBoost,
                virtualizer = _ui.value.virtualizer,
                loudness = _ui.value.loudness,
            )
            val saved = presets.save(preset)
            _ui.value = _ui.value.copy(activePresetId = saved.id, activePresetName = saved.name)
        }
    }

    fun deletePreset(id: String) = viewModelScope.launch { presets.delete(id) }
    fun duplicatePreset(id: String, newName: String) = viewModelScope.launch { presets.duplicate(id, newName) }
    fun renamePreset(id: String, newName: String) = viewModelScope.launch {
        presets.findById(id)?.takeIf { !it.isBuiltIn }?.let { presets.save(it.copy(name = newName)) }
    }
    fun exportPreset(id: String): String? = presets.findById(id)?.let { presets.export(it) }
    fun importPreset(jsonText: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(presets.import(jsonText) != null)
    }

    fun startVisualizer() { engine.startVisualizer() }
    fun stopVisualizer() { engine.stopVisualizer() }

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }
}
