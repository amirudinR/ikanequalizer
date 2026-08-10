package com.auralis.app.audio

import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/** Wraps [android.media.audiofx.LoudnessEnhancer] (API 19+). */
class LoudnessController {

    private var effect: LoudnessEnhancer? = null

    var available = false
        private set
    var enabled = false
        private set
    /** 0..100 */
    var gainPercent = 0
        private set

    fun attach(audioSessionId: Int): Boolean {
        release()
        return try {
            val le = LoudnessEnhancer(audioSessionId)
            effect = le
            available = true
            Log.i(TAG, "LoudnessEnhancer attached")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "LoudnessEnhancer unavailable: ${t.message}")
            available = false
            false
        }
    }

    fun setEnabled(on: Boolean) {
        val le = effect ?: return
        try {
            le.enabled = on
            enabled = le.enabled
        } catch (t: Throwable) { Log.w(TAG, "setEnabled failed: ${t.message}") }
    }

    /** [percent] 0..100 mapped to 0..1000 millibels (0..10 dB). */
    fun setGain(percent: Int) {
        val le = effect ?: return
        val clamped = percent.coerceIn(0, 100)
        try {
            le.setTargetGain(clamped * 10) // millibels
            gainPercent = clamped
        } catch (t: Throwable) { Log.w(TAG, "setGain failed: ${t.message}") }
    }

    fun release() {
        try { effect?.release() } catch (t: Throwable) { Log.w(TAG, "release failed: ${t.message}") }
        effect = null
        available = false
        enabled = false
    }

    companion object { private const val TAG = "LoudnessController" }
}
