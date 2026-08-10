package com.auralis.app

import com.auralis.app.data.model.EqualizerBand
import com.auralis.app.data.model.PresetExport
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validation logic mirrored from PresetRepository.import (pure JVM, no Android deps). */
class PresetValidationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun validate(text: String): Boolean {
        return runCatching {
            val parsed = json.decodeFromString(PresetExport.serializer(), text)
            require(parsed.name.isNotBlank())
            val bands = parsed.bands.mapNotNull { (f, g) ->
                val freq = f.toIntOrNull() ?: return@mapNotNull null
                if (!g.isFinite() || g < -15f || g > 15f) return@mapNotNull null
                EqualizerBand(freq, g)
            }
            require(bands.isNotEmpty())
        }.isSuccess
    }

    @Test
    fun `valid preset passes`() {
        val good = """{"name":"Studio","bands":{"31":4.0,"1000":-2.0},"bassBoost":25}"""
        assertTrue(validate(good))
    }

    @Test
    fun `blank name rejected`() {
        val bad = """{"name":"","bands":{"31":4.0}}"""
        assertTrue(!validate(bad))
    }

    @Test
    fun `out-of-range gain rejected`() {
        val bad = """{"name":"X","bands":{"31":99.0}}"""
        assertTrue(!validate(bad))
    }

    @Test
    fun `malformed json rejected`() {
        assertTrue(!validate("not json at all"))
        assertTrue(!validate("{\"name\":\"X\""))
    }

    @Test
    fun `empty bands rejected`() {
        val bad = """{"name":"X","bands":{}}"""
        assertTrue(!validate(bad))
    }
}
