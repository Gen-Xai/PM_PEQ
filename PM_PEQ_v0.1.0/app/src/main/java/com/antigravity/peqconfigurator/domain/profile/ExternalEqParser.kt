package com.antigravity.peqconfigurator.domain.profile

import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.FilterType
import java.util.Locale

object ExternalEqParser {

    fun parseApoText(text: String, profileName: String = "Imported Profile"): EqProfile {
        var preamp = 0.0
        val bands = mutableListOf<EqBand>()
        var autoBandIndex = 1

        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

        for (line in lines) {
            val lowerLine = line.lowercase(Locale.ROOT)
            if (lowerLine.startsWith("preamp:")) {
                val preampValStr = line.substringAfter(":").replace("db", "", ignoreCase = true).trim()
                preampValStr.toDoubleOrNull()?.let { preamp = it }
                continue
            }

            if (lowerLine.startsWith("filter")) {
                // e.g. "Filter 1: ON PK Fc 100 Hz Gain -2.0 dB Q 1.00" or "Filter: ON PK Fc 100 Hz Gain -2.0 dB Q 1.00"
                val enabled = !lowerLine.contains("off")
                
                var filterType = FilterType.PEAK
                if (lowerLine.contains(" pk ") || lowerLine.contains(" peq ")) filterType = FilterType.PEAK
                else if (lowerLine.contains(" ls ") || lowerLine.contains(" lsc ")) filterType = FilterType.LOW_SHELF
                else if (lowerLine.contains(" hs ") || lowerLine.contains(" hsc ")) filterType = FilterType.HIGH_SHELF
                else if (lowerLine.contains(" lp ")) filterType = FilterType.LOW_PASS
                else if (lowerLine.contains(" hp ")) filterType = FilterType.HIGH_PASS

                val fcMatch = Regex("""(?:fc|freq|frequency)\s*[:=]?\s*([\d.]+)(?:\s*hz)?""", RegexOption.IGNORE_CASE).find(line)
                val freq = fcMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1000.0

                val gainMatch = Regex("""gain\s*[:=]?\s*([-\d.]+)(?:\s*db)?""", RegexOption.IGNORE_CASE).find(line)
                val gain = gainMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

                val qMatch = Regex("""(?:q|q-factor)\s*[:=]?\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(line)
                val q = qMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.414

                val bandNumberMatch = Regex("""filter\s+(\d+)""", RegexOption.IGNORE_CASE).find(line)
                val bandIdx = bandNumberMatch?.groupValues?.get(1)?.toIntOrNull() ?: autoBandIndex

                bands.add(
                    EqBand(
                        bandIndex = bandIdx,
                        enabled = enabled,
                        type = filterType,
                        frequency = freq,
                        gain = gain,
                        q = q
                    )
                )
                autoBandIndex++
            }
        }

        val finalBands = if (bands.isEmpty()) EqProfile.default10Bands() else bands
        return EqProfile(
            name = profileName,
            preamp = preamp,
            bands = finalBands
        )
    }

    fun exportApoText(profile: EqProfile): String {
        val sb = StringBuilder()
        sb.appendLine("# Equalizer APO configuration exported from Android PEQ Configurator")
        sb.appendLine("# Profile: ${profile.name}")
        sb.appendLine(String.format(Locale.US, "Preamp: %.1f dB", profile.preamp))

        profile.bands.forEachIndexed { index, band ->
            val statusStr = if (band.enabled) "ON" else "OFF"
            val typeStr = when (band.type) {
                FilterType.PEAK -> "PK"
                FilterType.LOW_SHELF -> "LSC"
                FilterType.HIGH_SHELF -> "HSC"
                FilterType.LOW_PASS -> "LP"
                FilterType.HIGH_PASS -> "HP"
                FilterType.NOTCH -> "NO"
                else -> "PK"
            }
            sb.appendLine(
                String.format(
                    Locale.US,
                    "Filter %d: %s %s Fc %.1f Hz Gain %.1f dB Q %.2f",
                    band.bandIndex,
                    statusStr,
                    typeStr,
                    band.frequency,
                    band.gain,
                    band.q
                )
            )
        }
        return sb.toString()
    }
}
