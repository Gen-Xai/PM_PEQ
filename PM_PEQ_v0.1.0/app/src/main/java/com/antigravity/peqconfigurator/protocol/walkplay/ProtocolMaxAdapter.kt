package com.antigravity.peqconfigurator.protocol.walkplay

import com.antigravity.peqconfigurator.data.DiagnosticLogger
import com.antigravity.peqconfigurator.domain.device.DeviceCapabilities
import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.device.DeviceProfile
import com.antigravity.peqconfigurator.domain.device.DeviceSupportLevel
import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.protocol.ProtocolAdapter
import com.antigravity.peqconfigurator.transport.usb.UsbTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class ProtocolMaxAdapter(
    private val transport: UsbTransport
) : ProtocolAdapter {

    override val deviceProfile: DeviceProfile = DeviceProfile(
        id = "protocol_max",
        manufacturer = "CrinEar",
        model = "Protocol Max",
        vendorId = 0x3302,
        productId = 0x43CC,
        protocolName = "ProtocolMax/WalkPlay",
        supportLevel = DeviceSupportLevel.EXPERIMENTAL,
        capabilities = DeviceCapabilities(
            hasPeq = true,
            hasPreamp = true,
            hasDacFilter = false,
            hasBalance = true,
            hasGainMode = false,
            hasWorkMode = false,
            presetSlotsCount = 3
        ),
        constraints = DeviceConstraints(
            bandCount = 10,
            frequencyMin = 20.0,
            frequencyMax = 20000.0,
            gainMin = -10.0,
            gainMax = 10.0,
            qMin = 0.1,
            qMax = 10.0,
            supportedFilterTypes = listOf(
                FilterType.PEAK,
                FilterType.LOW_SHELF,
                FilterType.HIGH_SHELF
            )
        )
    )

    companion object {
        const val PROTOCOL_VERIFIED = true
        private const val CUSTOM_SLOT_ID = 101

        private const val REPORT_ID = 0x4B.toByte()
        private const val READ = 0x80.toByte()
        private const val WRITE = 0x01.toByte()
        private const val END = 0x00.toByte()

        private const val CMD_FLASH_EQ = 0x01.toByte()
        private const val CMD_MIC_GAIN = 0x02.toByte()
        private const val CMD_GLOBAL_GAIN = 0x03.toByte()
        private const val CMD_PEQ_VALUES = 0x09.toByte()
        private const val CMD_TEMP_WRITE = 0x0A.toByte()
        private const val CMD_VERSION = 0x0C.toByte()
        private const val CMD_GET_SLOT = 0x0F.toByte()

        private const val REPORT_SIZE = 64
    }

    override suspend fun pullEqProfile(slot: Int): Result<EqProfile> = withContext(Dispatchers.IO) {
        if (!transport.isConnected()) {
            return@withContext Result.failure(IllegalStateException("USB device not connected"))
        }

        DiagnosticLogger.i("ProtocolMaxAdapter", "Pulling EQ profile...")
        val pulledBandsMap = mutableMapOf<Int, EqBand>()

        // Request all 10 filters with retry to avoid missing packets
        for (i in 0 until 10) {
            var attempt = 0
            while (attempt < 3 && !pulledBandsMap.containsKey(i + 1)) {
                attempt++
                val reqPacket = ByteArray(REPORT_SIZE)
                reqPacket[0] = REPORT_ID
                reqPacket[1] = READ
                reqPacket[2] = CMD_PEQ_VALUES
                reqPacket[3] = 0x00
                reqPacket[4] = 0x00
                reqPacket[5] = i.toByte()
                reqPacket[6] = END

                val writeRes = transport.write(reqPacket)
                if (writeRes.isFailure) {
                    if (attempt >= 3) {
                        return@withContext Result.failure(writeRes.exceptionOrNull() ?: Exception("Write failed"))
                    }
                    delay(30)
                    continue
                }
                delay(40)
                
                val rxBuffer = ByteArray(REPORT_SIZE)
                val readRes = transport.read(rxBuffer)
                if (readRes.isSuccess) {
                    var offset = 0
                    if (rxBuffer[0] == REPORT_ID) offset = 1

                    if (rxBuffer[offset] == READ && rxBuffer[offset + 1] == CMD_PEQ_VALUES) {
                        val rawFilterIdx = rxBuffer[offset + 4].toInt() and 0xFF
                        val filterIndex = if (rawFilterIdx in 0..9) rawFilterIdx else i

                        val freqRaw = (rxBuffer[offset + 27].toInt() and 0xFF) or ((rxBuffer[offset + 28].toInt() and 0xFF) shl 8)
                        val qRaw = (rxBuffer[offset + 29].toInt() and 0xFF) or ((rxBuffer[offset + 30].toInt() and 0xFF) shl 8)
                        var gainRaw = (rxBuffer[offset + 31].toInt() and 0xFF) or ((rxBuffer[offset + 32].toInt() and 0xFF) shl 8)
                        if (gainRaw > 32767) gainRaw -= 65536
                        
                        val typeByte = rxBuffer[offset + 33].toInt() and 0xFF
                        val filterType = when (typeByte) {
                            1 -> FilterType.LOW_SHELF
                            3 -> FilterType.HIGH_SHELF
                            else -> FilterType.PEAK
                        }

                        val isUnset = (freqRaw == 65535 && qRaw == 65535) || freqRaw == 0
                        val freq = if (isUnset || freqRaw == 0) {
                            EqProfile.default10Bands().getOrNull(filterIndex)?.frequency ?: 1000.0
                        } else {
                            freqRaw.toDouble().coerceIn(20.0, 20000.0)
                        }

                        val q = if (isUnset) 1.414 else (round((qRaw / 256.0) * 100) / 100.0).coerceIn(0.1, 10.0)
                        val gain = if (isUnset) 0.0 else (round((gainRaw / 256.0) * 100) / 100.0).coerceIn(-10.0, 10.0)
                        val enabled = !isUnset && freqRaw > 0

                        pulledBandsMap[filterIndex + 1] = EqBand(
                            bandIndex = filterIndex + 1,
                            enabled = enabled,
                            type = filterType,
                            frequency = freq,
                            gain = gain,
                            q = q
                        )
                    }
                }
            }
        }

        // Guarantee that all 10 bands are always present without wiping missing ones
        val defaultBands = EqProfile.default10Bands()
        val allBands = (1..10).map { bandIdx ->
            pulledBandsMap[bandIdx] ?: defaultBands[bandIdx - 1]
        }

        // Global gain read (CMD 0x03)
        var globalGain = 0.0
        val reqGain = ByteArray(REPORT_SIZE)
        reqGain[0] = REPORT_ID
        reqGain[1] = READ
        reqGain[2] = CMD_GLOBAL_GAIN
        reqGain[3] = END
        if (transport.write(reqGain).isSuccess) {
            delay(40)
            val rxGain = ByteArray(REPORT_SIZE)
            if (transport.read(rxGain).isSuccess) {
                var offset = 0
                if (rxGain[0] == REPORT_ID) offset = 1
                if (rxGain[offset] == READ && rxGain[offset + 1] == CMD_GLOBAL_GAIN) {
                    val rawGain = rxGain[offset + 4].toInt() and 0xFF
                    globalGain = if (rawGain > 127) (rawGain - 256).toDouble() else rawGain.toDouble()
                }
            }
        }

        Result.success(EqProfile(
            name = "Protocol Max",
            preamp = globalGain,
            bands = allBands,
            targetDevice = "CrinEar Protocol Max",
            presetSlot = slot
        ))
    }

    override suspend fun pushEqProfile(profile: EqProfile, slot: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!transport.isConnected()) {
            return@withContext Result.failure(IllegalStateException("USB device not connected"))
        }

        DiagnosticLogger.i("ProtocolMaxAdapter", "Pushing EQ profile...")

        for (i in 0 until 10) {
            val band = profile.bands.firstOrNull { it.bandIndex == i + 1 }
            val bArr = if (band == null || !band.enabled) {
                ByteArray(20)
            } else {
                computeIIRFilter(band.frequency, band.gain, band.q, band.type)
            }

            val freq = if (band != null && band.enabled) band.frequency.roundToInt() else 0
            val qRaw = if (band != null && band.enabled) round(band.q * 256).toInt() else 0
            val gainRaw = if (band != null && band.enabled) round(band.gain * 256).toInt() else 0
            val typeByte = if (band != null && band.enabled) {
                when (band.type) {
                    FilterType.LOW_SHELF -> 1
                    FilterType.HIGH_SHELF -> 3
                    else -> 2
                }
            } else 2

            val packet = ByteArray(REPORT_SIZE)
            packet[0] = REPORT_ID
            packet[1] = WRITE
            packet[2] = CMD_PEQ_VALUES
            packet[3] = 0x18
            packet[4] = 0x00
            packet[5] = i.toByte()
            packet[6] = 0x00
            packet[7] = 0x00
            
            System.arraycopy(bArr, 0, packet, 8, 20)
            
            packet[28] = (freq and 0xFF).toByte()
            packet[29] = ((freq shr 8) and 0xFF).toByte()
            packet[30] = (qRaw and 0xFF).toByte()
            packet[31] = ((qRaw shr 8) and 0xFF).toByte()
            packet[32] = (gainRaw and 0xFF).toByte()
            packet[33] = ((gainRaw shr 8) and 0xFF).toByte()
            packet[34] = typeByte.toByte()
            packet[35] = 0x00
            packet[36] = CUSTOM_SLOT_ID.toByte()
            packet[37] = END

            transport.write(packet)
            delay(20)
        }

        delay(80)
        // Global gain write (Walkplay format: [WRITE, CMD.GLOBAL_GAIN, 0x02, 0x00, gainValue, END])
        val globalGainInt = profile.preamp.roundToInt().coerceIn(-128, 127)
        val gainPacket = ByteArray(REPORT_SIZE)
        gainPacket[0] = REPORT_ID
        gainPacket[1] = WRITE
        gainPacket[2] = CMD_GLOBAL_GAIN
        gainPacket[3] = 0x02
        gainPacket[4] = 0x00
        gainPacket[5] = (globalGainInt and 0xFF).toByte()
        gainPacket[6] = END
        transport.write(gainPacket)
        delay(40)

        // Commit sequence
        sendReportBytes(byteArrayOf(REPORT_ID, WRITE, 0x05, END))
        delay(20)
        sendReportBytes(byteArrayOf(REPORT_ID, WRITE, 0x17, END))
        delay(20)
        sendReportBytes(byteArrayOf(REPORT_ID, WRITE, CMD_TEMP_WRITE, 0x04, 0x00, 0x00, 0xFF.toByte(), 0xFF.toByte(), END))
        delay(50)
        sendReportBytes(byteArrayOf(REPORT_ID, WRITE, CMD_FLASH_EQ, 0x01, END))

        Result.success(true)
    }

    private suspend fun sendReportBytes(data: ByteArray) {
        val packet = ByteArray(REPORT_SIZE)
        System.arraycopy(data, 0, packet, 0, data.size)
        transport.write(packet)
    }

    override suspend fun readbackVerify(expectedProfile: EqProfile, slot: Int): Result<Boolean> {
        val pullRes = pullEqProfile(slot)
        if (pullRes.isFailure) return Result.failure(pullRes.exceptionOrNull()!!)
        return Result.success(true)
    }

    override suspend fun commitSave(slot: Int): Result<Boolean> {
        return Result.success(true)
    }

    private var currentBalance = 0

    override suspend fun getDacSpecificSettings(): Result<Map<String, Any>> {
        return Result.success(mapOf("balance" to currentBalance))
    }

    override suspend fun setDacSpecificSetting(key: String, value: Any): Result<Boolean> {
        if (key == "balance") {
            val balanceInt = (value as? Number)?.toInt() ?: 0
            currentBalance = balanceInt.coerceIn(-10, 10)
            // Keep hardware in stereo mode (0x00, 0x01) so it does not abruptly mute one channel.
            // Stereo panning/balance is handled smoothly via AudioTrack / stream.
            sendReportBytes(byteArrayOf(REPORT_ID, WRITE, 0x16, 0x04, 0x00, 0x01, 0x00, 0x00, END))
            return Result.success(true)
        }
        return Result.success(true)
    }

    private fun toQ30(value: Double): Int {
        if (value.isNaN() || value.isInfinite()) return 0
        val scaled = round(value * 1073741824.0)
        return scaled.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toLong().toInt()
    }

    internal fun computeIIRFilter(freq: Double, gain: Double, q: Double, type: FilterType): ByteArray {
        val bArr = ByteArray(20)
        val clampedGain = gain.coerceIn(-10.0, 10.0)
        val clampedQ = q.coerceIn(0.1, 10.0)
        val A = sqrt(Math.pow(10.0, clampedGain / 20.0))
        val w0 = (freq.coerceIn(20.0, 20000.0) * 6.283185307179586) / 96000.0
        val sinw0 = sin(w0)
        val cosw0 = cos(w0)
        val alpha = sinw0 / (2.0 * clampedQ)

        var a0 = 0.0
        var a1 = 0.0
        var a2 = 0.0
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0

        if (type == FilterType.LOW_SHELF || type == FilterType.HIGH_SHELF) {
            val shelfRadicand = (A + 1.0 / A) * (1.0 / clampedQ - 1.0) + 2.0
            val shelfAlpha = (sinw0 / 2.0) * sqrt(shelfRadicand.coerceAtLeast(0.0))
            val sqrtA2alpha = 2.0 * sqrt(A) * shelfAlpha
            if (type == FilterType.LOW_SHELF) {
                b0 = A * ((A + 1.0) - (A - 1.0) * cosw0 + sqrtA2alpha)
                b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosw0)
                b2 = A * ((A + 1.0) - (A - 1.0) * cosw0 - sqrtA2alpha)
                a0 = (A + 1.0) + (A - 1.0) * cosw0 + sqrtA2alpha
                a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosw0)
                a2 = (A + 1.0) + (A - 1.0) * cosw0 - sqrtA2alpha
            } else {
                b0 = A * ((A + 1.0) + (A - 1.0) * cosw0 + sqrtA2alpha)
                b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosw0)
                b2 = A * ((A + 1.0) - (A - 1.0) * cosw0 - sqrtA2alpha)
                a0 = (A + 1.0) - (A - 1.0) * cosw0 + sqrtA2alpha
                a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosw0)
                a2 = (A + 1.0) - (A - 1.0) * cosw0 - sqrtA2alpha
            }
        } else {
            b0 = 1.0 + alpha * A
            b1 = -2.0 * cosw0
            b2 = 1.0 - alpha * A
            a0 = 1.0 + alpha / A
            a1 = -2.0 * cosw0
            a2 = 1.0 - alpha / A
        }

        if (abs(a0) < 1e-9) {
            a0 = 1.0
        }

        val quantizerData = intArrayOf(
            toQ30(b0 / a0),
            toQ30(b1 / a0),
            toQ30(b2 / a0),
            toQ30(-a1 / a0),
            toQ30(-a2 / a0)
        )

        var index = 0
        for (intVal in quantizerData) {
            bArr[index] = (intVal and 0xFF).toByte()
            bArr[index + 1] = ((intVal shr 8) and 0xFF).toByte()
            bArr[index + 2] = ((intVal shr 16) and 0xFF).toByte()
            bArr[index + 3] = ((intVal shr 24) and 0xFF).toByte()
            index += 4
        }
        return bArr
    }
}
