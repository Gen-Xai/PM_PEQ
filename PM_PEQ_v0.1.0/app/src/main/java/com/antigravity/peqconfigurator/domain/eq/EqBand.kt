package com.antigravity.peqconfigurator.domain.eq

import kotlinx.serialization.Serializable

@Serializable
data class EqBand(
    val bandIndex: Int = 1,
    val enabled: Boolean = true,
    val type: FilterType = FilterType.PEAK,
    val frequency: Double = 1000.0,
    val gain: Double = 0.0,
    val q: Double = 1.414
) {
    fun copyWithValidation(
        enabled: Boolean = this.enabled,
        type: FilterType = this.type,
        frequency: Double = this.frequency,
        gain: Double = this.gain,
        q: Double = this.q
    ): EqBand {
        return copy(
            enabled = enabled,
            type = type,
            frequency = frequency.coerceIn(20.0, 20000.0),
            gain = gain.coerceIn(-24.0, 18.0),
            q = q.coerceIn(0.1, 20.0)
        )
    }
}
