package com.antigravity.peqconfigurator

import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.device.DeviceProfile
import com.antigravity.peqconfigurator.domain.eq.ConstraintSystem
import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.domain.eq.clamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstraintSystemTest {

    private val sampleProfile = DeviceProfile(
        id = "test",
        manufacturer = "Test",
        model = "Test Model",
        vendorId = 0x1234,
        productId = 0x5678,
        protocolName = "TestProto",
        constraints = DeviceConstraints(
            bandCount = 5,
            gainMin = -12.0,
            gainMax = 12.0
        )
    )

    @Test
    fun testValidateBandOutOfBoundsGain() {
        val band = EqBand(bandIndex = 1, gain = 15.0) // > 12 dB limit
        val res = ConstraintSystem.validateBand(band, sampleProfile)
        assertFalse(res.isValid)
        assertTrue(res.errors.any { it.contains("Gain 15.0 dB is outside") })
    }

    @Test
    fun testSanitizeProfile() {
        val rawProfile = EqProfile(
            bands = (1..8).map { EqBand(bandIndex = it, gain = 20.0) } // 8 bands, gain 20
        )
        val sanitized = ConstraintSystem.sanitizeProfile(rawProfile, sampleProfile)
        assertEquals(5, sanitized.bands.size) // truncated to max 5 bands
        assertEquals(12.0, sanitized.bands[0].gain, 1e-4) // clamped to max 12.0 dB
    }

    @Test
    fun testBandClampWithEmptyFilterTypesDoesNotThrow() {
        val constraints = DeviceConstraints(supportedFilterTypes = emptyList())
        val band = EqBand(bandIndex = 1, type = FilterType.LOW_PASS)
        val clamped = constraints.clamp(band)
        assertEquals(FilterType.PEAK, clamped.type)
    }
}
