package com.antigravity.peqconfigurator

import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.domain.profile.ExternalEqParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalEqParserTest {

    @Test
    fun testParseApoText() {
        val text = """
            Preamp: -4.5 dB
            Filter 1: ON PK Fc 105 Hz Gain -2.5 dB Q 1.20
            Filter 2: ON LSC Fc 80 Hz Gain 3.0 dB Q 0.71
        """.trimIndent()

        val profile = ExternalEqParser.parseApoText(text, "Bass Fix")
        assertEquals(-4.5, profile.preamp, 1e-4)
        assertEquals(2, profile.bands.size)
        assertEquals(105.0, profile.bands[0].frequency, 1e-4)
        assertEquals(-2.5, profile.bands[0].gain, 1e-4)
        assertEquals(1.20, profile.bands[0].q, 1e-4)
        assertEquals(FilterType.PEAK, profile.bands[0].type)
        assertEquals(FilterType.LOW_SHELF, profile.bands[1].type)
    }

    @Test
    fun testExportApoText() {
        val original = ExternalEqParser.parseApoText("Preamp: -2.0 dB\nFilter 1: ON PK Fc 500 Hz Gain 1.5 dB Q 2.00")
        val exportedText = ExternalEqParser.exportApoText(original)
        assertTrue(exportedText.contains("Preamp: -2.0 dB"))
        assertTrue(exportedText.contains("Filter 1: ON PK Fc 500.0 Hz Gain 1.5 dB Q 2.00"))
    }
}
