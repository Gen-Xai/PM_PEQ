package com.antigravity.peqconfigurator

import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqEngine
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqEngineTest {

    @Test
    fun testGenerateLogFrequencies() {
        val freqs = EqEngine.generateLogFrequencies(points = 100, minFreq = 20.0, maxFreq = 20000.0)
        assertEquals(100, freqs.size)
        assertEquals(20.0, freqs.first(), 1e-4)
        assertEquals(20000.0, freqs.last(), 1e-4)
    }

    @Test
    fun testPeakFilterMagnitudeAtCenterFreq() {
        val band = EqBand(
            bandIndex = 1,
            enabled = true,
            type = FilterType.PEAK,
            frequency = 1000.0,
            gain = 6.0,
            q = 1.414
        )
        val coeffs = EqEngine.calculateCoefficients(band)
        val magDbAtFc = coeffs.evaluateMagnitudeDb(1000.0)
        assertEquals(6.0, magDbAtFc, 0.2) // Peak filter magnitude at center frequency should match gain (~6 dB)
    }

    @Test
    fun testPreampAdditionToTotalResponse() {
        val profile = EqProfile(
            preamp = -3.0,
            bands = listOf(
                EqBand(bandIndex = 1, enabled = false, frequency = 1000.0, gain = 5.0)
            )
        )
        val freqs = listOf(1000.0)
        val totalResp = EqEngine.calculateTotalResponse(profile, freqs)
        assertEquals(-3.0, totalResp.first().magnitudeDb, 1e-4)
    }

    @Test
    fun testTotalResponseWithIdenticalFrequenciesDoesNotThrowOrNaN() {
        val profile = EqProfile(
            preamp = 0.0,
            bands = listOf(
                EqBand(bandIndex = 1, enabled = true, frequency = 1000.0, gain = 3.0),
                EqBand(bandIndex = 2, enabled = true, frequency = 1000.0, gain = 5.0)
            )
        )
        val freqs = listOf(500.0, 1000.0, 2000.0)
        val totalResp = EqEngine.calculateTotalResponse(profile, freqs)
        assertEquals(3, totalResp.size)
        totalResp.forEach {
            assertTrue("Magnitude should not be NaN", !it.magnitudeDb.isNaN())
            assertTrue("Magnitude should not be Infinite", !it.magnitudeDb.isInfinite())
        }
    }

    @Test
    fun testCurveStrictlyPassesThroughControlPoints() {
        val bands = listOf(
            EqBand(bandIndex = 1, enabled = true, frequency = 100.0, gain = 4.0),
            EqBand(bandIndex = 2, enabled = true, frequency = 1000.0, gain = -2.5),
            EqBand(bandIndex = 3, enabled = true, frequency = 10000.0, gain = 5.0)
        )
        val profile = EqProfile(preamp = 1.0, bands = bands)
        val testFreqs = listOf(100.0, 1000.0, 10000.0)
        val resp = EqEngine.calculateTotalResponse(profile, testFreqs)
        assertEquals(1.0 + 4.0, resp[0].magnitudeDb, 1e-3)
        assertEquals(1.0 - 2.5, resp[1].magnitudeDb, 1e-3)
        assertEquals(1.0 + 5.0, resp[2].magnitudeDb, 1e-3)
    }
}
