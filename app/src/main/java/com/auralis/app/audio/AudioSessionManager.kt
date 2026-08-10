package com.auralis.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the active audio session. Uses the global output mix (session 0) so
 * effects apply system-wide where the device/ROM permits, and reports whether
 * any audio is actually playing.
 */
class AudioSessionManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _sessionId = MutableStateFlow(GLOBAL_SESSION)
    val sessionId: StateFlow<Int> = _sessionId.asStateFlow()

    private val _isMusicActive = MutableStateFlow(false)
    val isMusicActive: StateFlow<Boolean> = _isMusicActive.asStateFlow()

    /** Poll-based activity flag; cheap and lifecycle-driven by the engine. */
    fun refreshPlaybackState() {
        _isMusicActive.value = try {
            audioManager.isMusicActive
        } catch (t: Throwable) {
            Log.w(TAG, "isMusicActive failed: ${t.message}")
            false
        }
    }

    companion object {
        private const val TAG = "AudioSessionManager"
        /** Session 0 = global output mix; lets effects apply to other apps' audio. */
        const val GLOBAL_SESSION = 0
    }
}
