package com.antigravity.peqconfigurator.device.session

import com.antigravity.peqconfigurator.domain.device.DeviceProfile

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val deviceProfile: DeviceProfile, val currentSlot: Int = 1) : ConnectionState
    data class Reading(val message: String = "Pulling settings from DAC...") : ConnectionState
    data class Writing(val message: String = "Pushing settings to DAC...") : ConnectionState
    data class Verifying(val message: String = "Verifying readback parameters...") : ConnectionState
    data class Error(val title: String, val detail: String) : ConnectionState
}
