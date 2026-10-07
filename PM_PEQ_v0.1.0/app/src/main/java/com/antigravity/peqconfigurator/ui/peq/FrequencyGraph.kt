package com.antigravity.peqconfigurator.ui.peq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.domain.eq.EqEngine
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.ui.theme.GridLineColor
import com.antigravity.peqconfigurator.ui.theme.GridZeroLine

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun FrequencyGraph(
    profile: EqProfile,
    selectedBandIndex: Int?,
    onSelectBand: (Int) -> Unit,
    onBandChange: (Int, Double, Double) -> Unit, // (bandIndex, newFreq, newGain)
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    minDb: Double = -18.0,
    maxDb: Double = +12.0,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(260.dp)
) {
    val textMeasurer = rememberTextMeasurer()
    val frequencies = remember { EqEngine.generateLogFrequencies(points = 250) }

    val minLogFreq = remember { log10(20.0) }
    val maxLogFreq = remember { log10(20000.0) }

    fun freqToX(freq: Double, width: Float): Float {
        val logF = log10(freq.coerceIn(20.0, 20000.0))
        return ((logF - minLogFreq) / (maxLogFreq - minLogFreq) * width).toFloat()
    }

    fun xToFreq(x: Float, width: Float): Double {
        val ratio = (x / width).coerceIn(0f, 1f)
        val logF = minLogFreq + ratio * (maxLogFreq - minLogFreq)
        return 10.0.pow(logF.toDouble())
    }

    fun gainToY(gainDb: Double, height: Float): Float {
        val ratio = (gainDb - minDb) / (maxDb - minDb)
        return ((1.0 - ratio) * height).toFloat()
    }

    fun yToGain(y: Float, height: Float): Double {
        val ratio = 1.0 - (y / height).toDouble()
        return (minDb + ratio * (maxDb - minDb)).coerceIn(minDb, maxDb)
    }

    val profileState = androidx.compose.runtime.rememberUpdatedState(profile)
    val selectedState = androidx.compose.runtime.rememberUpdatedState(selectedBandIndex)
    val onSelectState = androidx.compose.runtime.rememberUpdatedState(onSelectBand)
    val onChangeState = androidx.compose.runtime.rememberUpdatedState(onBandChange)
    val onDragStartState = androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val onDragEndState = androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val secondaryColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
    val mutedColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
    val surfaceVariantColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        var closestIndex: Int? = null
                        var minDistance = Float.MAX_VALUE

                        profileState.value.bands.forEach { band ->
                            val handleX = freqToX(band.frequency, width)
                            val handleY = gainToY(profileState.value.preamp + band.gain, height)
                            val dist = Math.hypot((tapOffset.x - handleX).toDouble(), (tapOffset.y - handleY).toDouble()).toFloat()
                            if (dist < 80f && dist < minDistance) {
                                minDistance = dist
                                closestIndex = band.bandIndex
                            }
                        }
                        closestIndex?.let { onSelectState.value(it) }
                    }
                }
                .pointerInput(Unit) {
                    var activeIndex: Int? = null
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            var minDistance = Float.MAX_VALUE
                            activeIndex = null
                            profileState.value.bands.forEach { band ->
                                val handleX = freqToX(band.frequency, width)
                                val handleY = gainToY(profileState.value.preamp + band.gain, height)
                                val dist = Math.hypot((startOffset.x - handleX).toDouble(), (startOffset.y - handleY).toDouble()).toFloat()
                                if (dist < 100f && dist < minDistance) {
                                    minDistance = dist
                                    activeIndex = band.bandIndex
                                }
                            }
                            // Only drag a band that was actually grabbed
                            activeIndex?.let {
                                onSelectState.value(it)
                                onDragStartState.value()
                            }
                        },
                        onDrag = { change, _ ->
                            val currentBandIdx = activeIndex
                            if (currentBandIdx != null) {
                                change.consume()
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                val rawFreq = xToFreq(change.position.x, width)
                                val newGain = (yToGain(change.position.y, height) - profileState.value.preamp).coerceIn(minDb, maxDb)

                                // Enforce strict frequency order: BAND 1 < BAND 2 < ... < BAND 10
                                val allBands = profileState.value.bands
                                val prevBand = allBands.firstOrNull { it.bandIndex == currentBandIdx - 1 }
                                val nextBand = allBands.firstOrNull { it.bandIndex == currentBandIdx + 1 }
                                val minF = (prevBand?.let { it.frequency + 1.0 } ?: 20.0).coerceAtLeast(20.0)
                                val maxF = (nextBand?.let { it.frequency - 1.0 } ?: 20000.0).coerceAtMost(20000.0)
                                val clampedFreq = if (minF < maxF) rawFreq.coerceIn(minF, maxF) else minF

                                onChangeState.value(currentBandIdx, clampedFreq, newGain)
                            }
                        },
                        onDragEnd = {
                            if (activeIndex != null) onDragEndState.value()
                            activeIndex = null
                        },
                        onDragCancel = {
                            if (activeIndex != null) onDragEndState.value()
                            activeIndex = null
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw Grid Lines and Labels
            val freqGrid = listOf(
                20.0 to "20", 50.0 to "50", 100.0 to "100", 200.0 to "200",
                500.0 to "500", 1000.0 to "1k", 2000.0 to "2k", 5000.0 to "5k",
                10000.0 to "10k", 20000.0 to "20k"
            )
            freqGrid.forEach { (freq, label) ->
                val x = freqToX(freq, width)
                drawLine(
                    color = GridLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                val measured = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = mutedColor, fontSize = 9.sp)
                )
                val labelX = (x + 2f).coerceIn(0f, (width - measured.size.width - 2f).coerceAtLeast(0f))
                val labelY = (height - 16.dp.toPx()).coerceIn(0f, (height - measured.size.height).coerceAtLeast(0f))
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(labelX, labelY)
                )
            }

            val gainGrid = listOf(-18.0, -12.0, -6.0, 0.0, 6.0, 12.0)
            gainGrid.forEach { gain ->
                val y = gainToY(gain, height)
                val lineCol = if (abs(gain) < 1e-4) GridZeroLine else GridLineColor
                val strokeW = if (abs(gain) < 1e-4) 2f else 1f

                drawLine(
                    color = lineCol,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = strokeW
                )
                val gainLabel = if (gain > 0) "+${gain.toInt()}" else "${gain.toInt()}"
                val measuredGain = textMeasurer.measure(
                    text = "$gainLabel dB",
                    style = TextStyle(color = mutedColor, fontSize = 9.sp)
                )
                val labelY = (y - 12.dp.toPx()).coerceIn(0f, (height - measuredGain.size.height).coerceAtLeast(0f))
                drawText(
                    textLayoutResult = measuredGain,
                    topLeft = Offset(4.dp.toPx(), labelY)
                )
            }

            // 2. Draw Preamp Offset Line
            if (abs(profile.preamp) > 1e-4) {
                val preampY = gainToY(profile.preamp, height)
                drawLine(
                    color = Color(0xFFFFB300),
                    start = Offset(0f, preampY),
                    end = Offset(width, preampY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            // 3. Draw Individual Band Filter Response Curves (Thin subtle curves)
            profile.bands.filter { it.enabled }.forEach { band ->
                val bandPoints = EqEngine.calculateBandResponse(band, frequencies)
                val bandPath = Path()
                bandPoints.forEachIndexed { i, pt ->
                    val x = freqToX(pt.frequency, width)
                    val y = gainToY(pt.magnitudeDb, height)
                    if (i == 0) bandPath.moveTo(x, y) else bandPath.lineTo(x, y)
                }
                drawPath(
                    path = bandPath,
                    color = secondaryColor.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Draw Total Combined EQ Curve with Gradient Fill
            val totalPoints = EqEngine.calculateTotalResponse(profile, frequencies)
            val totalPath = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, height)

            totalPoints.forEachIndexed { i, pt ->
                val x = freqToX(pt.frequency, width)
                val y = gainToY(pt.magnitudeDb, height)
                if (i == 0) {
                    totalPath.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    totalPath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()

            // Fill area with subtle glowing gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw glowing line
            drawPath(
                path = totalPath,
                color = primaryColor,
                style = Stroke(width = 3f)
            )

            // 5. Draw Band Control Point Handles
            profile.bands.forEach { band ->
                val hX = freqToX(band.frequency, width)
                val hY = gainToY(profile.preamp + band.gain, height)
                val isSelected = band.bandIndex == selectedBandIndex

                val handleRadius = if (isSelected) 10.dp.toPx() else 7.dp.toPx()
                val handleColor = if (!band.enabled) mutedColor else if (isSelected) primaryColor else secondaryColor

                // Selected Outer Ring
                if (isSelected) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = handleRadius * 1.8f,
                        center = Offset(hX, hY)
                    )
                }

                // Inner Circle
                drawCircle(
                    color = handleColor,
                    radius = handleRadius,
                    center = Offset(hX, hY)
                )
                drawCircle(
                    color = surfaceVariantColor,
                    radius = handleRadius * 0.4f,
                    center = Offset(hX, hY)
                )

                // Band Index Text
                val measuredBand = textMeasurer.measure(
                    text = "${band.bandIndex}",
                    style = TextStyle(color = Color.White, fontSize = 9.sp)
                )
                val bandTextX = (hX - measuredBand.size.width / 2f).coerceIn(0f, (width - measuredBand.size.width).coerceAtLeast(0f))
                val bandTextY = (hY - handleRadius - 14.dp.toPx()).coerceIn(0f, (height - measuredBand.size.height).coerceAtLeast(0f))
                drawText(
                    textLayoutResult = measuredBand,
                    topLeft = Offset(bandTextX, bandTextY)
                )
            }
        }
    }
}
