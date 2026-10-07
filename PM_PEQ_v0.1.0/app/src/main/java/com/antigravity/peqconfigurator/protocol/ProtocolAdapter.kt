package com.antigravity.peqconfigurator.protocol

import com.antigravity.peqconfigurator.domain.device.DeviceProfile
import com.antigravity.peqconfigurator.domain.eq.EqProfile

interface ProtocolAdapter {
    val deviceProfile: DeviceProfile

    suspend fun pullEqProfile(slot: Int = 1): Result<EqProfile>
    suspend fun pushEqProfile(profile: EqProfile, slot: Int = 1): Result<Boolean>
    suspend fun readbackVerify(expectedProfile: EqProfile, slot: Int = 1): Result<Boolean>
    suspend fun commitSave(slot: Int = 1): Result<Boolean>
    suspend fun getDacSpecificSettings(): Result<Map<String, Any>>
    suspend fun setDacSpecificSetting(key: String, value: Any): Result<Boolean>
}
