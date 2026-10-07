package com.antigravity.peqconfigurator.domain.eq

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType(val displayName: String, val isBasic: Boolean) {
    PEAK("Peak", true),
    LOW_SHELF("Low Shelf", true),
    HIGH_SHELF("High Shelf", true),
    SPLINE("Spline", true),
    LOW_PASS("Low Pass", false),
    HIGH_PASS("High Pass", false),
    NOTCH("Notch", false),
    BAND_STOP("Band Stop", false),
    ALL_PASS("All Pass", false),
    CONSTANT_Q("Constant Q", false);

    companion object {
        fun fromDisplayName(name: String): FilterType {
            return entries.firstOrNull { 
                it.displayName.equals(name, ignoreCase = true) ||
                it.name.equals(name, ignoreCase = true) ||
                (it == PEAK && (name.equals("PK", ignoreCase = true) || name.equals("PEQ", ignoreCase = true))) ||
                (it == LOW_SHELF && (name.equals("LS", ignoreCase = true) || name.equals("LSC", ignoreCase = true))) ||
                (it == HIGH_SHELF && (name.equals("HS", ignoreCase = true) || name.equals("HSC", ignoreCase = true)))
            } ?: PEAK
        }
    }
}
