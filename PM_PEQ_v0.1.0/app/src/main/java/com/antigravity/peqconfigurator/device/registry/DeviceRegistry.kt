package com.antigravity.peqconfigurator.device.registry

import android.hardware.usb.UsbDevice
import com.antigravity.peqconfigurator.domain.device.DeviceCapabilities
import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.device.DeviceProfile
import com.antigravity.peqconfigurator.domain.device.DeviceSupportLevel
import com.antigravity.peqconfigurator.domain.eq.FilterType
import com.antigravity.peqconfigurator.protocol.ProtocolAdapter
import com.antigravity.peqconfigurator.protocol.walkplay.ProtocolMaxAdapter
import com.antigravity.peqconfigurator.transport.usb.UsbTransport

object DeviceRegistry {

    fun isKnown(device: UsbDevice): Boolean =
        device.vendorId == 0x3302 && device.productId == 0x43CC

    fun matchAdapter(device: UsbDevice, transport: UsbTransport): ProtocolAdapter? {
        val vid = device.vendorId
        val pid = device.productId

        return when {
            vid == 0x3302 && pid == 0x43CC -> {
                ProtocolMaxAdapter(transport)
            }
            else -> {
                // Fallback for unknown / generic experimental USB audio DACs
                null
            }
        }
    }

    fun getGenericFallbackProfile(device: UsbDevice): DeviceProfile {
        return DeviceProfile(
            id = "generic_${device.vendorId}_${device.productId}",
            manufacturer = device.manufacturerName ?: "Generic",
            model = device.productName ?: "USB DAC (VID: 0x${device.vendorId.toString(16)}, PID: 0x${device.productId.toString(16)})",
            vendorId = device.vendorId,
            productId = device.productId,
            protocolName = "Generic USB HID",
            supportLevel = DeviceSupportLevel.UNSUPPORTED,
            capabilities = DeviceCapabilities(
                hasPeq = false,
                hasPreamp = false
            ),
            constraints = DeviceConstraints(
                bandCount = 10,
                supportedFilterTypes = listOf(
                    FilterType.PEAK,
                    FilterType.LOW_SHELF,
                    FilterType.HIGH_SHELF
                )
            )
        )
    }
}
