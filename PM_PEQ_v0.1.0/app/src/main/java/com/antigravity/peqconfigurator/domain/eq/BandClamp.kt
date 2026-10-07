package com.antigravity.peqconfigurator.domain.eq

import com.antigravity.peqconfigurator.domain.device.DeviceConstraints

/** Clamps a band to the device constraints (UI-layer validation; adapters validate again). */
fun DeviceConstraints.clamp(band: EqBand): EqBand = band.copy(
    frequency = band.frequency.coerceIn(frequencyMin, frequencyMax),
    gain = band.gain.coerceIn(gainMin, gainMax),
    q = band.q.coerceIn(qMin, qMax),
    type = if (band.type in supportedFilterTypes) band.type else (supportedFilterTypes.firstOrNull() ?: FilterType.PEAK)
)
