package com.auralis.app.audio

import android.media.audiofx.Virtualizer
import android.util.Log

/** Wraps [android.media.audiofx.Virtualizer] (spatial / surround widening). */
class VirtualizerController {

    private var effect: Virtualizer? = null

    var available = false
        private set
    var enabled = false
        private set
    /** 0..100 */
    var strength = 0
        private set

    fun attach(audioSessionId: Int): Boolean {
        release()
        return try {
            val v = Virtualizer(0, audioSessionId)
            effect = v
            available = true
            Log.i(TAG, "Virtualizer attached, strengthSupported=${v.strengthSupported}")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Virtualizer unavailable: ${t.message}")
            available = false
            false
        }
    }

    fun setEnabled(on: Boolean) {
        val v = effect ?: return
        try {
            v.enabled = on
            enabled = v.enabled
        } catch (t: Throwable) { Log.w(TAG, "setEnabled failed: ${t.message}") }
    }

    /** [percent] 0..100. */
    fun setStrength(percent: Int) {
        val v = effect ?: return
        val clamped = percent.coerceIn(0, 100)
        try {
            if (v.strengthSupported) {
                v.setStrength((clamped * 10).toShort())
            }
            strength = clamped
        } catch (t: Throwable) { Log.w(TAG, "setStrength failed: ${t.message}") }
    }

    fun release() {
        try { effect?.release() } catch (t: Throwable) { Log.w(TAG, "release failed: ${t.message}") }
        effect = null
        available = false
        enabled = false
    }

    companion object { private const val TAG = "VirtualizerController" }
}
