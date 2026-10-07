package com.antigravity.peqconfigurator.domain.eq

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class FrequencyPoint(
    val frequency: Double,
    val magnitudeDb: Double
)

class BiquadCoefficients(
    val b0: Double, val b1: Double, val b2: Double,
    val a0: Double, val a1: Double, val a2: Double
) {
    fun evaluateMagnitudeDb(frequency: Double, sampleRate: Double = 48000.0): Double {
        val w0 = 2.0 * Math.PI * frequency / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val cos2W = cos(2.0 * w0)
        val sin2W = sin(2.0 * w0)

        val numReal = b0 + b1 * cosW + b2 * cos2W
        val numImag = -b1 * sinW - b2 * sin2W

        val denReal = a0 + a1 * cosW + a2 * cos2W
        val denImag = -a1 * sinW - a2 * sin2W

        val numMagSq = numReal * numReal + numImag * numImag
        val denMagSq = denReal * denReal + denImag * denImag

        if (denMagSq < 1e-12) return 0.0
        val mag = sqrt(numMagSq / denMagSq)
        return 20.0 * log10(mag.coerceAtLeast(1e-6))
    }
}

object EqEngine {
    const val DEFAULT_SAMPLE_RATE = 48000.0

    fun generateLogFrequencies(
        points: Int = 300,
        minFreq: Double = 20.0,
        maxFreq: Double = 20000.0
    ): List<Double> {
        val minLog = log10(minFreq)
        val maxLog = log10(maxFreq)
        val step = (maxLog - minLog) / (points - 1)
        return (0 until points).map { i ->
            10.0.pow(minLog + i * step)
        }
    }

    fun calculateCoefficients(band: EqBand, sampleRate: Double = DEFAULT_SAMPLE_RATE): BiquadCoefficients {
        if (!band.enabled || abs(band.gain) < 1e-6 && band.type == FilterType.PEAK) {
            return BiquadCoefficients(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        }

        val f0 = band.frequency.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val gainDb = band.gain
        val q = band.q.coerceAtLeast(0.01)
        val A = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * f0 / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        var b0 = 1.0
        var b1 = 0.0
        var b2 = 0.0
        var a0 = 1.0
        var a1 = 0.0
        var a2 = 0.0

        when (band.type) {
            FilterType.PEAK, FilterType.CONSTANT_Q, FilterType.SPLINE -> {
                b0 = 1.0 + alpha * A
                b1 = -2.0 * cosW0
                b2 = 1.0 - alpha * A
                a0 = 1.0 + alpha / A
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha / A
            }
            FilterType.LOW_SHELF -> {
                val sqrtA2Alpha = 2.0 * sqrt(A) * alpha
                b0 = A * ((A + 1.0) - (A - 1.0) * cosW0 + sqrtA2Alpha)
                b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW0)
                b2 = A * ((A + 1.0) - (A - 1.0) * cosW0 - sqrtA2Alpha)
                a0 = (A + 1.0) + (A - 1.0) * cosW0 + sqrtA2Alpha
                a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosW0)
                a2 = (A + 1.0) + (A - 1.0) * cosW0 - sqrtA2Alpha
            }
            FilterType.HIGH_SHELF -> {
                val sqrtA2Alpha = 2.0 * sqrt(A) * alpha
                b0 = A * ((A + 1.0) + (A - 1.0) * cosW0 + sqrtA2Alpha)
                b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosW0)
                b2 = A * ((A + 1.0) + (A - 1.0) * cosW0 - sqrtA2Alpha)
                a0 = (A + 1.0) - (A - 1.0) * cosW0 + sqrtA2Alpha
                a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosW0)
                a2 = (A + 1.0) - (A - 1.0) * cosW0 - sqrtA2Alpha
            }
            FilterType.LOW_PASS -> {
                b0 = (1.0 - cosW0) / 2.0
                b1 = 1.0 - cosW0
                b2 = (1.0 - cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1.0 + cosW0) / 2.0
                b1 = -(1.0 + cosW0)
                b2 = (1.0 + cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.NOTCH, FilterType.BAND_STOP -> {
                b0 = 1.0
                b1 = -2.0 * cosW0
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.ALL_PASS -> {
                b0 = 1.0 - alpha
                b1 = -2.0 * cosW0
                b2 = 1.0 + alpha
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
        }

        return BiquadCoefficients(b0, b1, b2, a0, a1, a2)
    }

    fun calculateBandResponse(
        band: EqBand,
        frequencies: List<Double>,
        sampleRate: Double = DEFAULT_SAMPLE_RATE
    ): List<FrequencyPoint> {
        val coeffs = calculateCoefficients(band, sampleRate)
        return frequencies.map { freq ->
            FrequencyPoint(freq, coeffs.evaluateMagnitudeDb(freq, sampleRate))
        }
    }

    fun calculateTotalResponse(
        profile: EqProfile,
        frequencies: List<Double>,
        sampleRate: Double = DEFAULT_SAMPLE_RATE
    ): List<FrequencyPoint> {
        val enabledBands = profile.bands.filter { it.enabled }.sortedBy { it.frequency }
        if (enabledBands.isEmpty()) {
            return frequencies.map { FrequencyPoint(it, profile.preamp) }
        }

        // Control points: (log10(freq), preamp + gain)
        val pts = enabledBands.map { band ->
            Pair(log10(band.frequency), profile.preamp + band.gain)
        }
        val n = pts.size

        if (n == 1) {
            val single = pts[0]
            val band0 = enabledBands[0]
            return frequencies.map { freq ->
                val lf = log10(freq)
                val diff = lf - single.first
                val qWidth = 0.3 / band0.q.coerceAtLeast(0.1)
                val gainVal = (single.second - profile.preamp) / (1.0 + (diff / qWidth).pow(2))
                FrequencyPoint(freq, profile.preamp + gainVal)
            }
        }

        // Monotone Cubic Hermite Interpolation (Fritsch-Carlson / PCHIP)
        // Guaranteed to strictly pass through ALL control points without overshoot or oscillation
        val h = DoubleArray(n - 1) { i -> (pts[i + 1].first - pts[i].first).coerceAtLeast(1e-6) }
        val delta = DoubleArray(n - 1) { i -> (pts[i + 1].second - pts[i].second) / h[i] }

        val m = DoubleArray(n)
        for (i in 1 until n - 1) {
            if (delta[i - 1] * delta[i] <= 0.0) {
                m[i] = 0.0 // Local peak or valley: zero derivative ensures curve peaks exactly at the control point
            } else {
                // Harmonic mean of adjacent slopes (PCHIP)
                m[i] = (2.0 * delta[i - 1] * delta[i]) / (delta[i - 1] + delta[i])
            }
        }
        m[0] = 0.0
        m[n - 1] = 0.0

        val firstBand = enabledBands.first()
        val lastBand = enabledBands.last()
        val minLog = log10(20.0)
        val maxLog = log10(20000.0)

        return frequencies.map { freq ->
            val lf = log10(freq)
            val y = if (lf <= pts[0].first) {
                if (firstBand.type == FilterType.LOW_SHELF) {
                    pts[0].second
                } else {
                    val t = if (pts[0].first > minLog) ((lf - minLog) / (pts[0].first - minLog)).coerceIn(0.0, 1.0) else 1.0
                    val smoothT = 3.0 * t * t - 2.0 * t * t * t
                    profile.preamp + (pts[0].second - profile.preamp) * smoothT
                }
            } else if (lf >= pts[n - 1].first) {
                if (lastBand.type == FilterType.HIGH_SHELF) {
                    pts[n - 1].second
                } else {
                    val t = if (maxLog > pts[n - 1].first) ((maxLog - lf) / (maxLog - pts[n - 1].first)).coerceIn(0.0, 1.0) else 1.0
                    val smoothT = 3.0 * t * t - 2.0 * t * t * t
                    profile.preamp + (pts[n - 1].second - profile.preamp) * smoothT
                }
            } else {
                var segIdx = 0
                for (i in 0 until n - 1) {
                    if (lf <= pts[i + 1].first) {
                        segIdx = i
                        break
                    }
                }
                val t = ((lf - pts[segIdx].first) / h[segIdx]).coerceIn(0.0, 1.0)
                val h00 = 2.0 * t * t * t - 3.0 * t * t + 1.0
                val h10 = t * t * t - 2.0 * t * t + t
                val h01 = -2.0 * t * t * t + 3.0 * t * t
                val h11 = t * t * t - t * t
                h00 * pts[segIdx].second + h10 * h[segIdx] * m[segIdx] +
                    h01 * pts[segIdx + 1].second + h11 * h[segIdx] * m[segIdx + 1]
            }
            FrequencyPoint(freq, y)
        }
    }
}
