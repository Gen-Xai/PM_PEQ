package com.antigravity.peqconfigurator

import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.protocol.walkplay.ProtocolMaxAdapter
import com.antigravity.peqconfigurator.transport.usb.UsbTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotProfileLinkTest {

    private val dummyTransport = object : UsbTransport {
        override suspend fun connect(device: android.hardware.usb.UsbDevice): Result<Boolean> = Result.success(true)
        override suspend fun disconnect() {}
        override fun isConnected(): Boolean = true
        override suspend fun write(data: ByteArray, timeoutMs: Int): Result<Int> = Result.success(data.size)
        override suspend fun read(buffer: ByteArray, timeoutMs: Int): Result<Int> = Result.success(buffer.size)
        override fun getDeviceName(): String? = "Protocol Max"
    }

    @Test
    fun testProtocolMaxConstraints_GainMinMax() {
        val adapter = ProtocolMaxAdapter(dummyTransport)
        assertEquals(-10.0, adapter.deviceProfile.constraints.gainMin, 0.001)
        assertEquals(10.0, adapter.deviceProfile.constraints.gainMax, 0.001)
        assertEquals(10, adapter.deviceProfile.constraints.bandCount)
    }

    @Test
    fun testSlotProfilesMappingDefaults() {
        val defaultProfile = EqProfile(id = EqProfile.DEFAULT_PROFILE_ID, name = EqProfile.DEFAULT_PROFILE_NAME, presetSlot = 1)
        val slot2Profile = EqProfile(id = "slot-2-default", name = "Slot 2", presetSlot = 2)
        val slot3Profile = EqProfile(id = "slot-3-default", name = "Slot 3", presetSlot = 3)

        assertEquals(1, defaultProfile.presetSlot)
        assertEquals(2, slot2Profile.presetSlot)
        assertEquals(3, slot3Profile.presetSlot)

        val slots = listOf(defaultProfile, slot2Profile, slot3Profile)
        assertEquals(3, slots.size)
        assertTrue(slots.any { it.presetSlot == 1 && it.name == "Slot 1" })
        assertTrue(slots.any { it.presetSlot == 2 && it.name == "Slot 2" })
        assertTrue(slots.any { it.presetSlot == 3 && it.name == "Slot 3" })
    }

    @Test
    fun testDefault10BandsFrequencies() {
        val bands = EqProfile.default10Bands()
        assertEquals(10, bands.size)
        val expectedFreqs = listOf(50.0, 200.0, 500.0, 1000.0, 4000.0, 7500.0, 10000.0, 13000.0, 15000.0, 19000.0)
        assertEquals(expectedFreqs, bands.map { it.frequency })
    }
}
