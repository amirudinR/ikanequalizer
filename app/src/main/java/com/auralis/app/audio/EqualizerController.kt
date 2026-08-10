package com.auralis.app.audio

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Wraps [android.media.audiofx.Equalizer]. Adapts to the device's actual band
 * count and level range instead of assuming a fixed 10-band layout.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null

    data class State(
        val available: Boolean = false,
        val bandCount: Int = 0,
        val centerFrequenciesHz: List<Int> = emptyList(),
        val minDb: Float = -15f,
        val maxDb: Float = 15f,
        val enabled: Boolean = false,
    )

    var state = State()
        private set

    /** Attach to [audioSessionId]. Returns true on success. Safe to call repeatedly. */
    fun attach(audioSessionId: Int): Boolean {
        release()
        return try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange // shortArrayOf(min, max) in millibels
            val freqs = (0 until bands).map { band ->
                // center freq is returned in milliHertz
                eq.getCenterFreq(band.toShort()) / 1000
            }
            state = State(
                available = true,
                bandCount = bands,
                centerFrequenciesHz = freqs,
                minDb = range[0] / 100f,
                maxDb = range[1] / 100f,
                enabled = false,
            )
            Log.i(TAG, "Equalizer attached: $bands bands, range ${range[0]}..${range[1]} mb, freqs=$freqs")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer unavailable: ${t.message}")
            state = State()
            false
        }
    }

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        try {
            eq.enabled = enabled
            state = state.copy(enabled = eq.enabled)
        } catch (t: Throwable) {
            Log.w(TAG, "setEnabled failed: ${t.message}")
        }
    }

    /** Set band [index] to [gainDb], clamped to the device range. */
    fun setBandLevel(index: Int, gainDb: Float) {
        val eq = equalizer ?: return
        if (index !in 0 until state.bandCount) return
        val clamped = gainDb.coerceIn(state.minDb, state.maxDb)
        try {
            eq.setBandLevel(index.toShort(), (clamped * 100).toInt().toShort())
        } catch (t: Throwable) {
            Log.w(TAG, "setBandLevel($index) failed: ${t.message}")
        }
    }

    fun getBandLevel(index: Int): Float {
        val eq = equalizer ?: return 0f
        return try {
            eq.getBandLevel(index.toShort()) / 100f
        } catch (t: Throwable) {
            0f
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "release failed: ${t.message}")
        }
        equalizer = null
        state = State()
    }

    companion object { private const val TAG = "EqualizerController" }
}
