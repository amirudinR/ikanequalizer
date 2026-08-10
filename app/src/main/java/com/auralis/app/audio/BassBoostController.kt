package com.auralis.app.audio

import android.media.audiofx.BassBoost
import android.util.Log

/** Wraps [android.media.audiofx.BassBoost]. */
class BassBoostController {

    private var effect: BassBoost? = null

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
            val bb = BassBoost(0, audioSessionId)
            effect = bb
            available = true
            Log.i(TAG, "BassBoost attached, strengthSupported=${bb.strengthSupported}")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost unavailable: ${t.message}")
            available = false
            false
        }
    }

    fun setEnabled(on: Boolean) {
        val bb = effect ?: return
        try {
            bb.enabled = on
            enabled = bb.enabled
        } catch (t: Throwable) { Log.w(TAG, "setEnabled failed: ${t.message}") }
    }

    /** [percent] 0..100. */
    fun setStrength(percent: Int) {
        val bb = effect ?: return
        val clamped = percent.coerceIn(0, 100)
        try {
            if (bb.strengthSupported) {
                bb.setStrength((clamped * 10).toShort()) // API uses 0..1000
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

    companion object { private const val TAG = "BassBoostController" }
}
