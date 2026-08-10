package com.auralis.app.data.model

/** Built-in presets with real EQ curves. Gains are in dB for the canonical
 *  10-band layout [31,62,125,250,500,1k,2k,4k,8k,16k]. Values are mapped onto
 *  the device's actual bands at apply-time by nearest-frequency matching. */
object BuiltInPresets {

    val canonicalFrequencies = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    private fun preset(name: String, gains: List<Float>, bass: Int = 0, virt: Int = 0, loud: Int = 0) =
        EqualizerPreset(
            id = "builtin_" + name.lowercase().replace(' ', '_'),
            name = name,
            bands = canonicalFrequencies.zip(gains).map { (f, g) -> EqualizerBand(f, g) },
            bassBoost = bass,
            virtualizer = virt,
            loudness = loud,
            isBuiltIn = true,
        )

    val all: List<EqualizerPreset> = listOf(
        preset("Flat",       listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        preset("Bass Boost", listOf(6f, 5f, 4f, 2f, 1f, 0f, 0f, 0f, 0f, 0f), bass = 40),
        preset("Deep Bass",  listOf(8f, 6f, 4f, 2f, 0f, 0f, -1f, -1f, 0f, 1f), bass = 60),
        preset("Vocal",      listOf(-2f, -3f, -2f, 0f, 2f, 4f, 4f, 3f, 1f, 0f)),
        preset("Podcast",    listOf(-3f, -2f, 0f, 2f, 4f, 5f, 3f, 1f, 0f, -1f)),
        preset("Classical",  listOf(2f, 1f, 0f, 0f, 0f, -1f, -1f, 0f, 1f, 2f)),
        preset("Jazz",       listOf(3f, 2f, 1f, 1f, -1f, -1f, 0f, 1f, 2f, 3f)),
        preset("Rock",       listOf(4f, 3f, 1f, -1f, -2f, -1f, 1f, 3f, 4f, 4f)),
        preset("Electronic", listOf(5f, 4f, 1f, 0f, -2f, 1f, 2f, 3f, 4f, 5f), bass = 30),
        preset("Acoustic",   listOf(3f, 2f, 1f, 1f, 2f, 2f, 3f, 3f, 2f, 1f)),
        preset("Gaming",     listOf(4f, 3f, 2f, 0f, -1f, 0f, 2f, 4f, 5f, 4f), virt = 50),
        preset("Movie",      listOf(3f, 4f, 3f, 1f, 0f, 1f, 2f, 3f, 3f, 2f), virt = 60),
        preset("Night",      listOf(-2f, -1f, 0f, 0f, 1f, 2f, 1f, 0f, -2f, -4f), loud = 20),
        preset("Studio",     listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f)),
        preset("Balanced",   listOf(2f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 2f, 2f)),
    )

    fun byId(id: String): EqualizerPreset? = all.firstOrNull { it.id == id }
}
