package com.auralis.app

import com.auralis.app.data.model.BuiltInPresets
import com.auralis.app.data.model.EqualizerBand
import com.auralis.app.data.model.EqualizerPreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `built-in presets are present and non-empty`() {
        assertTrue(BuiltInPresets.all.size >= 15)
        BuiltInPresets.all.forEach { preset ->
            assertTrue(preset.name.isNotBlank())
            assertEquals(10, preset.bands.size)
        }
    }

    @Test
    fun `preset serializes and deserializes losslessly`() {
        val preset = EqualizerPreset(
            id = "custom_test",
            name = "My Studio",
            bands = listOf(EqualizerBand(31, 4f), EqualizerBand(1000, -2f)),
            bassBoost = 25,
            virtualizer = 10,
            loudness = 5,
        )
        val text = json.encodeToString(EqualizerPreset.serializer(), preset)
        val back = json.decodeFromString(EqualizerPreset.serializer(), text)
        assertEquals(preset, back)
    }

    @Test
    fun `deep bass preset has expected curve`() {
        val deepBass = BuiltInPresets.byId("builtin_deep_bass")
        assertNotNull(deepBass)
        assertEquals(8f, deepBass!!.bands[0].gainDb) // 31 Hz
        assertEquals(1f, deepBass.bands[9].gainDb)   // 16 kHz
        assertEquals(60, deepBass.bassBoost)
    }

    @Test
    fun `flat preset is all zeros`() {
        val flat = BuiltInPresets.byId("builtin_flat")
        assertNotNull(flat)
        assertTrue(flat!!.bands.all { it.gainDb == 0f })
    }
}
