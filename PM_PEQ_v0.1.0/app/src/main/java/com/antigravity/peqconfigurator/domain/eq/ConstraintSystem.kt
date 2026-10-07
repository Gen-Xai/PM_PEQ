package com.antigravity.peqconfigurator.domain.eq

import com.antigravity.peqconfigurator.domain.device.DeviceProfile

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

object ConstraintSystem {
    fun validateBand(band: EqBand, deviceProfile: DeviceProfile?): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val minFreq = deviceProfile?.constraints?.frequencyMin ?: 20.0
        val maxFreq = deviceProfile?.constraints?.frequencyMax ?: 20000.0
        val minGain = deviceProfile?.constraints?.gainMin ?: -24.0
        val maxGain = deviceProfile?.constraints?.gainMax ?: 18.0
        val minQ = deviceProfile?.constraints?.qMin ?: 0.1
        val maxQ = deviceProfile?.constraints?.qMax ?: 20.0
        val supportedTypes = deviceProfile?.constraints?.supportedFilterTypes ?: FilterType.entries

        if (band.frequency < minFreq || band.frequency > maxFreq) {
            errors.add("Band ${band.bandIndex}: Frequency ${band.frequency} Hz is outside device range ($minFreq - $maxFreq Hz)")
        }
        if (band.gain < minGain || band.gain > maxGain) {
            errors.add("Band ${band.bandIndex}: Gain ${band.gain} dB is outside device range ($minGain - $maxGain dB)")
        }
        if (band.q < minQ || band.q > maxQ) {
            errors.add("Band ${band.bandIndex}: Q ${band.q} is outside device range ($minQ - $maxQ)")
        }
        if (band.type !in supportedTypes) {
            errors.add("Band ${band.bandIndex}: Filter type ${band.type.displayName} is not supported by device")
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors, warnings = warnings)
    }

    fun validateProfile(profile: EqProfile, deviceProfile: DeviceProfile?): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val maxBands = deviceProfile?.constraints?.bandCount ?: 10
        if (profile.bands.size > maxBands) {
            errors.add("Profile has ${profile.bands.size} bands, but device supports maximum of $maxBands bands")
        }

        profile.bands.forEach { band ->
            val res = validateBand(band, deviceProfile)
            errors.addAll(res.errors)
            warnings.addAll(res.warnings)
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors, warnings = warnings)
    }

    fun sanitizeProfile(profile: EqProfile, deviceProfile: DeviceProfile?): EqProfile {
        val maxBands = deviceProfile?.constraints?.bandCount ?: 10
        val defaultBands = EqProfile.default10Bands()
        val sortedInput = profile.bands.sortedBy { it.frequency }
        val existingMap = sortedInput.mapIndexed { i, b -> (i + 1) to b }.toMap()

        val minFreq = deviceProfile?.constraints?.frequencyMin ?: 20.0
        val maxFreq = deviceProfile?.constraints?.frequencyMax ?: 20000.0
        val minGain = deviceProfile?.constraints?.gainMin ?: -24.0
        val maxGain = deviceProfile?.constraints?.gainMax ?: 18.0
        val minQ = deviceProfile?.constraints?.qMin ?: 0.1
        val maxQ = deviceProfile?.constraints?.qMax ?: 20.0
        val supportedTypes = deviceProfile?.constraints?.supportedFilterTypes ?: FilterType.entries

        var lastFreq = minFreq - 1.0
        val sanitizedBands = (1..maxBands).map { idx ->
            val band = existingMap[idx] ?: defaultBands.getOrElse(idx - 1) {
                EqBand(bandIndex = idx, enabled = false, frequency = (lastFreq + 100.0).coerceAtMost(maxFreq), gain = 0.0, q = 1.414)
            }

            val targetMinFreq = (lastFreq + 1.0).coerceAtLeast(minFreq)
            val safeFreq = if (targetMinFreq <= maxFreq) band.frequency.coerceIn(targetMinFreq, maxFreq) else targetMinFreq
            lastFreq = safeFreq

            band.copy(
                bandIndex = idx,
                frequency = safeFreq,
                gain = band.gain.coerceIn(minGain, maxGain),
                q = band.q.coerceIn(minQ, maxQ),
                type = if (band.type in supportedTypes) band.type else FilterType.PEAK
            )
        }
        return profile.copy(bands = sanitizedBands)
    }
}
