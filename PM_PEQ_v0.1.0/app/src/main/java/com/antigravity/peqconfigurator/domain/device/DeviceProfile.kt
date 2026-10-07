package com.antigravity.peqconfigurator.domain.device

import com.antigravity.peqconfigurator.domain.eq.FilterType

data class DeviceCapabilities(
    val hasPeq: Boolean = true,
    val hasPreamp: Boolean = true,
    val hasDacFilter: Boolean = false,
    val hasBalance: Boolean = false,
    val hasGainMode: Boolean = false,
    val hasWorkMode: Boolean = false,
    val hasMicMonitor: Boolean = false,
    val presetSlotsCount: Int = 1
)

data class DeviceConstraints(
    val bandCount: Int = 10,
    val frequencyMin: Double = 20.0,
    val frequencyMax: Double = 20000.0,
    val gainMin: Double = -12.0,
    val gainMax: Double = 12.0,
    val qMin: Double = 0.1,
    val qMax: Double = 10.0,
    val supportedFilterTypes: List<FilterType> = listOf(
        FilterType.PEAK,
        FilterType.LOW_SHELF,
        FilterType.HIGH_SHELF
    )
)

data class DeviceProfile(
    val id: String,
    val manufacturer: String,
    val model: String,
    val vendorId: Int,
    val productId: Int,
    val protocolName: String,
    val supportLevel: DeviceSupportLevel = DeviceSupportLevel.EXPERIMENTAL,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val constraints: DeviceConstraints = DeviceConstraints()
)
