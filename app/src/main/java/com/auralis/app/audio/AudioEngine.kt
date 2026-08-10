package com.auralis.app.audio

import android.content.Context
import android.util.Log
import com.auralis.app.data.model.EffectAvailability
import com.auralis.app.data.model.EqualizerBand
import com.auralis.app.data.model.EqualizerCapabilities
import com.auralis.app.data.model.EqualizerPreset
import com.auralis.app.visualization.SpectrumAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Central audio engine. Owns the effect controllers and the spectrum analyzer,
 * exposes immutable state, and survives navigation (scoped to the app, not a screen).
 */
class AudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    val sessionManager = AudioSessionManager(context)
    val equalizer = EqualizerController()
    val bassBoost = BassBoostController()
    val virtualizer = VirtualizerController()
    val loudness = LoudnessController()
    val spectrumAnalyzer = SpectrumAnalyzer()

    private val _availability = MutableStateFlow(EffectAvailability(false, false, false, false))
    val availability: StateFlow<EffectAvailability> = _availability.asStateFlow()

    private val _capabilities = MutableStateFlow<EqualizerCapabilities?>(null)
    val capabilities: StateFlow<EqualizerCapabilities?> = _capabilities.asStateFlow()

    private val _engineEnabled = MutableStateFlow(false)
    val engineEnabled: StateFlow<Boolean> = _engineEnabled.asStateFlow()

    private var monitorJob: Job? = null
    private var initialized = false

    /** Attach all effects to the global session. Idempotent. */
    @Synchronized
    fun initialize() {
        if (initialized) return
        val session = sessionManager.sessionId.value
        val eqOk = equalizer.attach(session)
        val bbOk = bassBoost.attach(session)
        val virtOk = virtualizer.attach(session)
        val loudOk = loudness.attach(session)

        _availability.value = EffectAvailability(
            equalizer = eqOk,
            bassBoost = bbOk,
            virtualizer = virtOk,
            loudness = loudOk,
        )
        if (eqOk) {
            val s = equalizer.state
            _capabilities.value = EqualizerCapabilities(
                bandCount = s.bandCount,
                bandFrequenciesHz = s.centerFrequenciesHz,
                minLevelDb = s.minDb,
                maxLevelDb = s.maxDb,
            )
        }
        initialized = true
        Log.i(TAG, "Engine initialized: eq=$eqOk bass=$bbOk virt=$virtOk loud=$loudOk")
        startMonitoring()
    }

    fun setEngineEnabled(enabled: Boolean) {
        _engineEnabled.value = enabled
        equalizer.setEnabled(enabled)
        bassBoost.setEnabled(enabled && bassBoost.strength > 0)
        virtualizer.setEnabled(enabled && virtualizer.strength > 0)
        loudness.setEnabled(enabled && loudness.gainPercent > 0)
    }

    /** Apply a preset's bands by nearest-frequency matching onto device bands. */
    fun applyPreset(preset: EqualizerPreset) {
        val caps = _capabilities.value ?: return
        preset.bands.forEach { band ->
            val deviceIndex = nearestBandIndex(band.frequency, caps.bandFrequenciesHz)
            if (deviceIndex >= 0) equalizer.setBandLevel(deviceIndex, band.gainDb)
        }
        setBassBoost(preset.bassBoost)
        setVirtualizer(preset.virtualizer)
        setLoudness(preset.loudness)
    }

    fun setBandGain(deviceBandIndex: Int, gainDb: Float) =
        equalizer.setBandLevel(deviceBandIndex, gainDb)

    fun setBassBoost(percent: Int) {
        bassBoost.setStrength(percent)
        bassBoost.setEnabled(_engineEnabled.value && percent > 0)
    }

    fun setVirtualizer(percent: Int) {
        virtualizer.setStrength(percent)
        virtualizer.setEnabled(_engineEnabled.value && percent > 0)
    }

    fun setLoudness(percent: Int) {
        loudness.setGain(percent)
        loudness.setEnabled(_engineEnabled.value && percent > 0)
    }

    fun startVisualizer(): Boolean = spectrumAnalyzer.start(sessionManager.sessionId.value)
    fun stopVisualizer() = spectrumAnalyzer.stop()

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                sessionManager.refreshPlaybackState()
                delay(1000)
            }
        }
    }

    private fun nearestBandIndex(targetHz: Int, deviceFreqs: List<Int>): Int {
        if (deviceFreqs.isEmpty()) return -1
        var best = 0
        var bestDiff = Int.MAX_VALUE
        deviceFreqs.forEachIndexed { i, f ->
            val diff = kotlin.math.abs(f - targetHz)
            if (diff < bestDiff) { bestDiff = diff; best = i }
        }
        return best
    }

    fun release() {
        monitorJob?.cancel()
        stopVisualizer()
        equalizer.release()
        bassBoost.release()
        virtualizer.release()
        loudness.release()
        initialized = false
        Log.i(TAG, "Engine released")
    }

    companion object { private const val TAG = "AudioEngine" }
}
