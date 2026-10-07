package com.antigravity.peqconfigurator.ui.peq

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.ui.i18n.LocalStrings
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.DarkSurface
import com.antigravity.peqconfigurator.ui.theme.DarkSurfaceVariant
import com.antigravity.peqconfigurator.ui.theme.EmeraldGreen
import androidx.compose.material3.MaterialTheme

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BandEditor(
    bands: List<EqBand>,
    selectedBandIndex: Int?,
    onSelectBand: (Int) -> Unit,
    onBandChange: (EqBand) -> Unit,
    deviceConstraints: DeviceConstraints = DeviceConstraints(),
    modifier: Modifier = Modifier
) {
    val activeIndex = selectedBandIndex ?: 1
    val selectedBand = bands.firstOrNull { it.bandIndex == activeIndex } ?: bands.firstOrNull() ?: return
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Horizontal Band Selector Ribbon (B1 to B10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bands.forEach { band ->
                val isSelected = band.bandIndex == activeIndex
                val pillBorderColor = when {
                    isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                    band.enabled -> androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                    else -> androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                }
                val pillBg = when {
                    isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    band.enabled -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    else -> androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pillBg)
                        .border(if (isSelected) 1.5.dp else 1.dp, pillBorderColor, RoundedCornerShape(999.dp))
                        .clickable { onSelectBand(band.bandIndex) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !band.enabled -> TextMuted
                                    isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    else -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
                                }
                            )
                    )

                    Text(
                        text = "B${band.bandIndex}",
                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else if (band.enabled) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    val freqLabel = if (band.frequency >= 1000) {
                        String.format(Locale.US, "%.1fk", band.frequency / 1000.0).replace(".0k", "k")
                    } else {
                        "${band.frequency.roundToInt()}"
                    }

                    Text(
                        text = freqLabel,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 10.sp
                    )

                    if (kotlin.math.abs(band.gain) > 0.05) {
                        Text(
                            text = String.format(Locale.US, "%+.0f", band.gain),
                            color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Active Band Detail Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header of Active Band Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BAND ${selectedBand.bandIndex}",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "•  ${selectedBand.type.displayName}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(26.dp),
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "PEQ Information Guide",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.0f Hz  •  %+.1f dB  •  Q %.2f", selectedBand.frequency, selectedBand.gain, selectedBand.q),
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Enable / Bypass Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedBand.enabled) "ON" else "BYPASS",
                            color = if (selectedBand.enabled) EmeraldGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = selectedBand.enabled,
                            onCheckedChange = { isChecked ->
                                onBandChange(selectedBand.copy(enabled = isChecked))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkCardBorder
                            )
                        )
                    }
                }

                // Filter Type Selector (M3 Expressive connected button group)
                com.antigravity.peqconfigurator.ui.components.ExpressiveSingleChoiceGroup(
                    options = deviceConstraints.supportedFilterTypes.map { it to it.displayName },
                    selected = selectedBand.type,
                    onSelect = { onBandChange(selectedBand.copy(type = it)) }
                )

                // Frequency Control (Clamped between adjacent bands: BAND 1 < 2 < 3 < ... < 10)
                val prevBand = bands.firstOrNull { it.bandIndex == selectedBand.bandIndex - 1 }
                val nextBand = bands.firstOrNull { it.bandIndex == selectedBand.bandIndex + 1 }
                val rawMinF = (prevBand?.let { it.frequency + 1.0 } ?: deviceConstraints.frequencyMin).coerceIn(deviceConstraints.frequencyMin, deviceConstraints.frequencyMax)
                val rawMaxF = (nextBand?.let { it.frequency - 1.0 } ?: deviceConstraints.frequencyMax).coerceIn(deviceConstraints.frequencyMin, deviceConstraints.frequencyMax)
                val bandFreqMin = rawMinF
                val bandFreqMax = rawMaxF.coerceAtLeast(bandFreqMin + 0.1)

                ExpressiveValueControl(
                    label = "FREQUENCY",
                    unit = "Hz",
                    currentValue = selectedBand.frequency.coerceIn(bandFreqMin, bandFreqMax),
                    min = bandFreqMin,
                    max = bandFreqMax,
                    isLogarithmic = true,
                    onValueChange = { onBandChange(selectedBand.copy(frequency = it.coerceIn(bandFreqMin, bandFreqMax))) }
                )

                // Gain Control
                ExpressiveValueControl(
                    label = "GAIN",
                    unit = "dB",
                    currentValue = selectedBand.gain,
                    min = deviceConstraints.gainMin,
                    max = deviceConstraints.gainMax,
                    isLogarithmic = false,
                    isSigned = true,
                    onValueChange = { onBandChange(selectedBand.copy(gain = it)) }
                )

                // Q-Factor Control
                ExpressiveValueControl(
                    label = "Q-FACTOR",
                    unit = "",
                    currentValue = selectedBand.q,
                    min = deviceConstraints.qMin,
                    max = deviceConstraints.qMax,
                    isLogarithmic = false,
                    stepPrecision = 2,
                    onValueChange = { onBandChange(selectedBand.copy(q = it)) }
                )

                // Quick Q Presets: Auto / 0.7 / 1.4 / 3.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val autoQ = calculateAutoQ(selectedBand, bands, deviceConstraints)
                    val is07 = kotlin.math.abs(selectedBand.q - 0.7) < 0.05
                    val is14 = kotlin.math.abs(selectedBand.q - 1.4) < 0.05
                    val is30 = kotlin.math.abs(selectedBand.q - 3.0) < 0.05
                    val isAuto = !is07 && !is14 && !is30 && kotlin.math.abs(selectedBand.q - autoQ) < 0.05

                    val qPresets = listOf(
                        Triple("Auto", autoQ, isAuto),
                        Triple("0.7", 0.7, is07),
                        Triple("1.4", 1.4, is14),
                        Triple("3.0", 3.0, is30)
                    )

                    val scheme = MaterialTheme.colorScheme
                    qPresets.forEach { (qLabel, qVal, isCurrent) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isCurrent) scheme.primary else DarkSurface)
                                .clickable { onBandChange(selectedBand.copy(q = qVal)) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = qLabel,
                                color = if (isCurrent) scheme.onPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    // PEQ Guide / Information Overlay Dialog
    if (showInfoDialog) {
        val strings = LocalStrings.current
        val isJa = strings.aboutCategory == "アプリ情報"
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = DarkSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isJa) "PEQ 設定・パラメータガイド" else "PEQ Settings & Parameters Guide",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Filter Types Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isJa) "【フィルター種別の設定】" else "Filter Types",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        GuideItem(
                            title = if (isJa) "Peak (ピーキング)" else "Peak",
                            description = if (isJa)
                                "指定した周波数を中心に山または谷の形で増幅・減衰します。特定帯域の突出したピークをカットしたり、狙った音域を持ち上げる際に最も汎用的に使用します。"
                            else
                                "Boosts or cuts in a bell curve around the center frequency. Ideal for targeted tonal adjustments and resonance correction."
                        )
                        GuideItem(
                            title = if (isJa) "Low Shelf (ローシェルフ)" else "Low Shelf",
                            description = if (isJa)
                                "指定した周波数以下の低域全体を均一に持ち上げたり減衰させます。サブベースやキックの量感、低域の重み付けを自然に調整するのに適しています。"
                            else
                                "Boosts or attenuates all frequencies below the cutoff. Perfect for tuning bass weight and fullness."
                        )
                        GuideItem(
                            title = if (isJa) "High Shelf (ハイシェルフ)" else "High Shelf",
                            description = if (isJa)
                                "指定した周波数以上の高域全体を均一に持ち上げたり減衰させます。シンバルやボーカルの空気感・透明感を付加したり、刺さり感を低減するのに適しています。"
                            else
                                "Boosts or attenuates all frequencies above the cutoff. Ideal for airy presence or smoothing harsh highs."
                        )
                    }

                    HorizontalDivider(color = DarkCardBorder)

                    // Parameters Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isJa) "【パラメータの説明】" else "Parameters",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        GuideItem(
                            title = if (isJa) "FREQUENCY (周波数)" else "FREQUENCY",
                            description = if (isJa)
                                "フィルターが作用する中心周波数またはカットオフ周波数を設定します（20 Hz 〜 20,000 Hz）。各バンドは BAND 1 < 2 < ... < 10 の昇順順序が維持されます。"
                            else
                                "The center or cutoff frequency in Hz (20 Hz - 20,000 Hz). Bands maintain strict ascending order (BAND 1 < 2 < ... < 10)."
                        )
                        GuideItem(
                            title = if (isJa) "GAIN (ゲイン)" else "GAIN",
                            description = if (isJa)
                                "対象帯域の増幅・減衰量を設定します（-12.0 dB 〜 +12.0 dB）。0.0 dB を中央（センター）としてブースト/カットします。"
                            else
                                "The boost or cut amount in decibels (-12.0 dB to +12.0 dB), centered at 0.0 dB."
                        )
                        GuideItem(
                            title = if (isJa) "Q-FACTOR (クオリティファクター / Q値)" else "Q-FACTOR",
                            description = if (isJa)
                                "フィルターの帯域幅（鋭さ）を決定します。Q値が大きい（例: 3.0）ほど狭くピンポイントに作用し、Q値が小さい（例: 0.7）ほど緩やかで広い帯域に作用します。"
                            else
                                "Determines the bandwidth or sharpness of the filter curve. Higher Q (e.g. 3.0) gives a narrower curve; lower Q (e.g. 0.7) gives a broader curve."
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = if (isJa) "閉じる" else "Close",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveValueControl(
    label: String,
    unit: String,
    currentValue: Double,
    min: Double,
    max: Double,
    isLogarithmic: Boolean,
    isSigned: Boolean = false,
    stepPrecision: Int = 1,
    onValueChange: (Double) -> Unit
) {
    val displayFormat = if (isSigned) "%+.${stepPrecision}f" else "%.${stepPrecision}f"
    val fieldText = String.format(Locale.US, displayFormat, currentValue)
    var textValue by remember { mutableStateOf(fieldText) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(currentValue, focused) {
        if (!focused) textValue = fieldText
    }

    val safeMax = max.coerceAtLeast(min + 0.1)
    val sliderPosition = if (isLogarithmic) {
        log10(currentValue.coerceIn(min, safeMax)).toFloat()
    } else {
        currentValue.toFloat().coerceIn(min.toFloat(), safeMax.toFloat())
    }

    val sliderRange = if (isLogarithmic) {
        log10(min).toFloat()..log10(safeMax).toFloat()
    } else {
        min.toFloat()..safeMax.toFloat()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(85.dp)
            )

            val sliderColors = SliderDefaults.colors(
                thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                activeTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                inactiveTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
            )

            Slider(
                value = sliderPosition,
                onValueChange = { raw ->
                    val newVal = if (isLogarithmic) {
                        10.0.pow(raw.toDouble()).roundToInt().toDouble()
                    } else {
                        val factor = 10.0.pow(stepPrecision.toDouble())
                        (raw * factor).roundToInt() / factor
                    }
                    onValueChange(newVal.coerceIn(min, max))
                },
                valueRange = sliderRange,
                colors = sliderColors,
                track = { sliderState ->
                    if (isSigned) {
                        SliderDefaults.CenteredTrack(
                            sliderState = sliderState,
                            colors = sliderColors
                        )
                    } else {
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            colors = sliderColors
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Text field with signed decimal support
            OutlinedTextField(
                value = textValue,
                onValueChange = { str ->
                    textValue = str
                    str.toDoubleOrNull()?.let { num ->
                        if (num in min..max) onValueChange(num)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier
                    .width(76.dp)
                    .onFocusChanged { focused = it.isFocused },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )
        }
    }
}

@Composable
private fun GuideItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private fun calculateAutoQ(selectedBand: EqBand, bands: List<EqBand>, constraints: DeviceConstraints): Double {
    val prevBand = bands.firstOrNull { it.bandIndex == selectedBand.bandIndex - 1 }
    val nextBand = bands.firstOrNull { it.bandIndex == selectedBand.bandIndex + 1 }
    val f0 = selectedBand.frequency
    val q = when {
        prevBand != null && nextBand != null -> {
            val fUpper = kotlin.math.sqrt(f0 * nextBand.frequency)
            val fLower = kotlin.math.sqrt(f0 * prevBand.frequency)
            val deltaF = fUpper - fLower
            if (deltaF > 0.1) f0 / deltaF else 1.4
        }
        nextBand != null -> {
            val fUpper = kotlin.math.sqrt(f0 * nextBand.frequency)
            val ratio = fUpper / f0
            val fLower = f0 / ratio
            val deltaF = fUpper - fLower
            if (deltaF > 0.1) f0 / deltaF else 1.4
        }
        prevBand != null -> {
            val fLower = kotlin.math.sqrt(f0 * prevBand.frequency)
            val ratio = f0 / fLower
            val fUpper = f0 * ratio
            val deltaF = fUpper - fLower
            if (deltaF > 0.1) f0 / deltaF else 1.4
        }
        else -> 1.4
    }
    return ((q.coerceIn(constraints.qMin, constraints.qMax)) * 10.0).roundToInt() / 10.0
}
