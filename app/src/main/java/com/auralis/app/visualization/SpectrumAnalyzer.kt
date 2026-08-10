package com.auralis.app.visualization

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.log10

/**
 * Captures FFT + waveform from the output mix via [Visualizer] on session 0.
 * Requires RECORD_AUDIO permission; if capture is unsupported the analyzer
 * reports [available] = false and the UI falls back to an idle visualization.
 */
class SpectrumAnalyzer {

    private var visualizer: Visualizer? = null

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    /** Normalized 0..1 magnitudes across [BAND_COUNT] log-spaced bands. */
    private val _spectrum = MutableStateFlow(FloatArray(BAND_COUNT))
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    /** Normalized 0..1 waveform samples. */
    private val _waveform = MutableStateFlow(FloatArray(WAVEFORM_SAMPLES))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    /** 0..1 overall loudness (RMS). */
    private val _rms = MutableStateFlow(0f)
    val rms: StateFlow<Float> = _rms.asStateFlow()

    /** 0..1 low-frequency energy (drives the 3D object's scale). */
    private val _bass = MutableStateFlow(0f)
    val bass: StateFlow<Float> = _bass.asStateFlow()

    private val smoothed = FloatArray(BAND_COUNT)

    fun start(audioSessionId: Int): Boolean {
        stop()
        return try {
            val v = Visualizer(audioSessionId)
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
            v.captureSize = captureSize
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer?, bytes: ByteArray?, rate: Int) {
                    bytes ?: return
                    processWaveform(bytes)
                }
                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, rate: Int) {
                    fft ?: return
                    processFft(fft, rate)
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            v.enabled = true
            visualizer = v
            _available.value = true
            Log.i(TAG, "Visualizer started, captureSize=$captureSize")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Visualizer unavailable: ${t.message}")
            _available.value = false
            false
        }
    }

    private fun processWaveform(bytes: ByteArray) {
        val out = FloatArray(WAVEFORM_SAMPLES)
        val step = (bytes.size / WAVEFORM_SAMPLES).coerceAtLeast(1)
        var sum = 0.0
        for (i in 0 until WAVEFORM_SAMPLES) {
            val idx = (i * step).coerceIn(0, bytes.size - 1)
            // bytes are unsigned 8-bit PCM centered at 128
            val v = (bytes[idx].toInt() and 0xFF) - 128
            out[i] = v / 128f
            sum += v * v
        }
        _waveform.value = out
        val rmsValue = kotlin.math.sqrt(sum / WAVEFORM_SAMPLES) / 128.0
        _rms.value = rmsValue.toFloat().coerceIn(0f, 1f)
    }

    private fun processFft(fft: ByteArray, samplingRate: Int) {
        // fft is interleaved [real0, imag0, real1, imag1, ...]; n = fft.size/2 bins
        val n = fft.size / 2
        if (n < 2) return
        val magnitudes = DoubleArray(n)
        for (i in 0 until n) {
            val re = fft[i * 2].toDouble()
            val im = fft[i * 2 + 1].toDouble()
            magnitudes[i] = hypot(re, im)
        }
        val binHz = samplingRate / 2.0 / n
        val out = FloatArray(BAND_COUNT)
        var bassSum = 0.0
        var bassCount = 0
        for (b in 0 until BAND_COUNT) {
            // log-spaced target frequency between 20 Hz and 20 kHz
            val f = 20.0 * Math.pow(1000.0, b / (BAND_COUNT - 1.0))
            val bin = (f / binHz).toInt().coerceIn(1, n - 1)
            val mag = magnitudes[bin]
            // convert to dB and normalize roughly 0..1
            val db = 20 * log10(mag + 1e-6)
            val norm = ((db + 60) / 60).coerceIn(0.0, 1.0).toFloat()
            // exponential smoothing to avoid jitter
            smoothed[b] = smoothed[b] + (norm - smoothed[b]) * 0.35f
            out[b] = smoothed[b]
            if (f <= 250) { bassSum += norm; bassCount++ }
        }
        _spectrum.value = out
        _bass.value = if (bassCount > 0) (bassSum / bassCount).toFloat().coerceIn(0f, 1f) else 0f
    }

    fun stop() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "stop failed: ${t.message}")
        }
        visualizer = null
        _available.value = false
    }

    companion object {
        private const val TAG = "SpectrumAnalyzer"
        const val BAND_COUNT = 32
        const val WAVEFORM_SAMPLES = 64
    }
}
