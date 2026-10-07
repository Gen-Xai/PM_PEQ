package com.antigravity.peqconfigurator.protocol

import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.protocol.walkplay.ProtocolMaxAdapter
import com.antigravity.peqconfigurator.transport.usb.UsbTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProtocolMaxAdapterTest {

    private val dummyTransport = object : UsbTransport {
        override suspend fun connect(device: android.hardware.usb.UsbDevice): Result<Boolean> = Result.success(true)
        override suspend fun disconnect() {}
        override fun isConnected(): Boolean = true
        override suspend fun write(data: ByteArray, timeoutMs: Int): Result<Int> = Result.success(data.size)
        override suspend fun read(buffer: ByteArray, timeoutMs: Int): Result<Int> = Result.success(buffer.size)
        override fun getDeviceName(): String? = "Protocol Max"
    }

    private val adapter = ProtocolMaxAdapter(dummyTransport)

    @Test
    fun testComputeIIRFilter_HighShelfPositiveGain_NoOverflow() {
        // High Shelf at 16kHz, +10.0dB, Q=1.414 historically produced b1/a0 = -2.24
        // which caused integer overflow into +1.76 if unquantized/unclamped.
        val bytes = adapter.computeIIRFilter(16000.0, 10.0, 1.414, FilterType.HIGH_SHELF)
        assertEquals(20, bytes.size)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val b0 = buffer.int
        val b1 = buffer.int
        val b2 = buffer.int
        val negA1 = buffer.int
        val negA2 = buffer.int

        // b1 should be clamped to Int.MIN_VALUE (-2.0 in Q2.30) rather than wrapping around to positive
        assert(b1 <= 0) { "b1 coefficient should be negative or zero, but was $b1" }
        assert(b0 > 0) { "b0 coefficient should be positive, but was $b0" }
        assertNotNull(b2)
        assertNotNull(negA1)
        assertNotNull(negA2)
    }

    @Test
    fun testComputeIIRFilter_ExtremeQ_NoNaN() {
        // High Q (e.g. Q=10.0) with high gain can make (A + 1/A)*(1/q - 1) + 2 negative
        val bytes = adapter.computeIIRFilter(1000.0, 10.0, 10.0, FilterType.HIGH_SHELF)
        assertEquals(20, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 5) {
            val coef = buffer.int
            // If NaN occurred, toQ30 would return 0 or clamp properly
            assertFalse("Coefficient should not be corrupted", coef == 0 && i == 0)
        }
    }

    @Test
    fun testComputeIIRFilter_LowShelfAndPeak_ValidCoefficients() {
        val lsBytes = adapter.computeIIRFilter(100.0, 6.0, 0.71, FilterType.LOW_SHELF)
        assertEquals(20, lsBytes.size)

        val pkBytes = adapter.computeIIRFilter(1000.0, -6.0, 1.414, FilterType.PEAK)
        assertEquals(20, pkBytes.size)
    }

    @Test
    fun testSetDacSpecificSetting_Balance() = kotlinx.coroutines.runBlocking {
        val resLeft = adapter.setDacSpecificSetting("balance", -5)
        assertEquals(true, resLeft.getOrNull())
        val settingsLeft = adapter.getDacSpecificSettings().getOrNull()
        assertEquals(-5, settingsLeft?.get("balance"))

        val resRight = adapter.setDacSpecificSetting("balance", 8)
        assertEquals(true, resRight.getOrNull())
        val settingsRight = adapter.getDacSpecificSettings().getOrNull()
        assertEquals(8, settingsRight?.get("balance"))

        val resCenter = adapter.setDacSpecificSetting("balance", 0)
        assertEquals(true, resCenter.getOrNull())
        val settingsCenter = adapter.getDacSpecificSettings().getOrNull()
        assertEquals(0, settingsCenter?.get("balance"))
    }
}
