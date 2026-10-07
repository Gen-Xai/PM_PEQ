package com.antigravity.peqconfigurator.domain.eq

import kotlinx.serialization.Serializable

@Serializable
data class EqProfile(
    val id: String = DEFAULT_PROFILE_ID,
    val name: String = DEFAULT_PROFILE_NAME,
    val preamp: Double = 0.0,
    val bands: List<EqBand> = default10Bands(),
    val targetDevice: String? = null,
    val deviceModel: String? = null,
    val presetSlot: Int = 1,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val DEFAULT_PROFILE_ID = "default-10-band-peq"
        const val DEFAULT_PROFILE_NAME = "Slot 1"
        fun default10Bands(): List<EqBand> {
            val defaultFreqs = listOf(50.0, 200.0, 500.0, 1000.0, 4000.0, 7500.0, 10000.0, 13000.0, 15000.0, 19000.0)
            return defaultFreqs.mapIndexed { index, freq ->
                EqBand(
                    bandIndex = index + 1,
                    enabled = true,
                    type = when (index) {
                        0 -> FilterType.LOW_SHELF
                        9 -> FilterType.HIGH_SHELF
                        else -> FilterType.PEAK
                    },
                    frequency = freq,
                    gain = 0.0,
                    q = 1.414
                )
            }
        }
    }
}
