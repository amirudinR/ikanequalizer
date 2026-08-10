package com.auralis.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auralis.app.data.model.PerformanceMode
import com.auralis.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auralis_settings")

/** Lightweight user preferences backed by DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PERFORMANCE = stringPreferencesKey("performance")
        val LAST_PRESET_ID = stringPreferencesKey("last_preset_id")
        val AUTO_ENABLE = booleanPreferencesKey("auto_enable")
        val RESTORE_PRESET = booleanPreferencesKey("restore_preset")
        val APPLY_ON_STARTUP = booleanPreferencesKey("apply_on_startup")
        val EQ_HAPTICS = booleanPreferencesKey("eq_haptics")
        val UI_HAPTICS = booleanPreferencesKey("ui_haptics")
        val SHOW_3D = booleanPreferencesKey("show_3d")
        val SHOW_SPECTRUM = booleanPreferencesKey("show_spectrum")
        val SHOW_PARTICLES = booleanPreferencesKey("show_particles")
        val SHOW_WAVEFORM = booleanPreferencesKey("show_waveform")
        val ANIMATION_INTENSITY = floatPreferencesKey("animation_intensity")
        val MASTER_GAIN = floatPreferencesKey("master_gain")
        val BALANCE = floatPreferencesKey("balance")
        val TREBLE = intPreferencesKey("treble")
    }

    val theme: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }
    val performance: Flow<PerformanceMode> = context.dataStore.data.map {
        runCatching { PerformanceMode.valueOf(it[Keys.PERFORMANCE] ?: "BALANCED") }.getOrDefault(PerformanceMode.BALANCED)
    }
    val lastPresetId: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_PRESET_ID] }
    val autoEnable: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_ENABLE] ?: true }
    val restorePreset: Flow<Boolean> = context.dataStore.data.map { it[Keys.RESTORE_PRESET] ?: true }
    val applyOnStartup: Flow<Boolean> = context.dataStore.data.map { it[Keys.APPLY_ON_STARTUP] ?: true }
    val eqHaptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.EQ_HAPTICS] ?: true }
    val uiHaptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.UI_HAPTICS] ?: true }
    val show3d: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_3D] ?: true }
    val showSpectrum: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_SPECTRUM] ?: true }
    val showParticles: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_PARTICLES] ?: true }
    val showWaveform: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_WAVEFORM] ?: true }
    val animationIntensity: Flow<Float> = context.dataStore.data.map { it[Keys.ANIMATION_INTENSITY] ?: 1f }
    val masterGain: Flow<Float> = context.dataStore.data.map { it[Keys.MASTER_GAIN] ?: 0f }
    val balance: Flow<Float> = context.dataStore.data.map { it[Keys.BALANCE] ?: 0f }
    val treble: Flow<Int> = context.dataStore.data.map { it[Keys.TREBLE] ?: 50 }

    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = v.name }
    suspend fun setPerformance(v: PerformanceMode) = context.dataStore.edit { it[Keys.PERFORMANCE] = v.name }
    suspend fun setLastPresetId(v: String) = context.dataStore.edit { it[Keys.LAST_PRESET_ID] = v }
    suspend fun setAutoEnable(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_ENABLE] = v }
    suspend fun setRestorePreset(v: Boolean) = context.dataStore.edit { it[Keys.RESTORE_PRESET] = v }
    suspend fun setApplyOnStartup(v: Boolean) = context.dataStore.edit { it[Keys.APPLY_ON_STARTUP] = v }
    suspend fun setEqHaptics(v: Boolean) = context.dataStore.edit { it[Keys.EQ_HAPTICS] = v }
    suspend fun setUiHaptics(v: Boolean) = context.dataStore.edit { it[Keys.UI_HAPTICS] = v }
    suspend fun setShow3d(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_3D] = v }
    suspend fun setShowSpectrum(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_SPECTRUM] = v }
    suspend fun setShowParticles(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_PARTICLES] = v }
    suspend fun setShowWaveform(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_WAVEFORM] = v }
    suspend fun setAnimationIntensity(v: Float) = context.dataStore.edit { it[Keys.ANIMATION_INTENSITY] = v }
    suspend fun setMasterGain(v: Float) = context.dataStore.edit { it[Keys.MASTER_GAIN] = v }
    suspend fun setBalance(v: Float) = context.dataStore.edit { it[Keys.BALANCE] = v }
    suspend fun setTreble(v: Int) = context.dataStore.edit { it[Keys.TREBLE] = v }
}
