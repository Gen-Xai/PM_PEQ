package com.antigravity.peqconfigurator.ui.peq

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.device.session.ConnectionState
import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.device.DeviceSupportLevel
import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.ui.components.ExpressiveRibbon
import com.antigravity.peqconfigurator.ui.theme.AmberWarning
import com.antigravity.peqconfigurator.ui.theme.CoralError
import com.antigravity.peqconfigurator.ui.theme.DarkBackground
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.DarkSurfaceVariant
import com.antigravity.peqconfigurator.ui.theme.EmeraldGreen



import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PeqScreen(
    profile: EqProfile,
    connectionState: ConnectionState,
    isDirty: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    selectedSlot: Int,
    selectedBandIndex: Int?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onSelectBand: (Int) -> Unit,
    onBandChange: (EqBand) -> Unit,
    onPreampChange: (Double) -> Unit,
    onResetProfile: () -> Unit,
    channelBalance: Int = 0,
    onBalanceChange: (Int) -> Unit = {},
    isPinkNoisePlaying: Boolean = false,
    pinkNoiseVolume: Float = 0.4f,
    onTogglePinkNoise: () -> Unit = {},
    onPinkNoiseVolumeChange: (Float) -> Unit = {},
    onPullFromDevice: () -> Unit,
    onPushToDevice: () -> Unit = {},
    onCommitToFlash: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.antigravity.peqconfigurator.ui.i18n.LocalStrings.current
    val connected = connectionState as? ConnectionState.Connected
    val busy = connectionState is ConnectionState.Reading ||
        connectionState is ConnectionState.Writing ||
        connectionState is ConnectionState.Verifying
    val isConnected = connected != null
    val supportLevel = connected?.deviceProfile?.supportLevel
    val canWrite = isConnected && supportLevel != DeviceSupportLevel.UNSUPPORTED
    val deviceName = connected?.deviceProfile?.model ?: "DAC"
    val slotsCount = connected?.deviceProfile?.capabilities?.presetSlotsCount ?: 3
    val constraints = connected?.deviceProfile?.constraints ?: DeviceConstraints()

    var confirmReset by remember { mutableStateOf(false) }
    var confirmPull by remember { mutableStateOf(false) }
    var confirmCommit by remember { mutableStateOf(false) }

    // Ribbon Expansion States
    var graphExpanded by remember { mutableStateOf(true) }
    var preampExpanded by remember { mutableStateOf(true) }
    var bandsExpanded by remember { mutableStateOf(true) }
    var othersExpanded by remember { mutableStateOf(true) }
    var syncExpanded by remember { mutableStateOf(true) }

    val activeBandsCount = profile.bands.count { it.enabled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Error Banner
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoralError.copy(alpha = 0.15f))
                    .border(1.dp, CoralError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = errorMessage,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissError, shapes = ButtonDefaults.shapes()) {
                    Text(strings.ok, color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Toolbar: Slot Selector & Undo/Redo & Dirty Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Undo / Redo Group
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = strings.undo,
                        tint = if (canUndo) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = strings.redo,
                        tint = if (canRedo) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isDirty) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(AmberWarning.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(strings.statusEdited, color = AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Target Slot (M3 Expressive connected button group)
            com.antigravity.peqconfigurator.ui.components.ExpressiveSingleChoiceGroup(
                options = (1..slotsCount).map { it to strings.slotName(it) },
                selected = selectedSlot,
                onSelect = onSelectSlot
            )
        }

        // Active Profile Indicator linked to selected slot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Equalizer,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                strings.activeProfileLabel(selectedSlot, profile.name),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        // Ribbon 1: Preamp Gain Control
        ExpressiveRibbon(
            title = strings.preampTitle,
            icon = Icons.Filled.Tune,
            badgeText = String.format(Locale.US, "%+.1f dB", profile.preamp),
            badgeColor = if (profile.preamp < 0) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextSecondary,
            subtitle = null,
            isExpanded = preampExpanded,
            onToggleExpand = { preampExpanded = !preampExpanded }
        ) {
            PreampControl(
                preampDb = profile.preamp,
                onPreampChange = onPreampChange,
                minGain = constraints.gainMin,
                maxGain = constraints.gainMax
            )
        }

        // Ribbon 2: Frequency Response Graph
        ExpressiveRibbon(
            title = strings.freqResponseTitle,
            icon = Icons.Filled.Equalizer,
            badgeText = strings.activeBandsCount(activeBandsCount, profile.bands.size),
            badgeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            subtitle = null,
            isExpanded = graphExpanded,
            onToggleExpand = { graphExpanded = !graphExpanded }
        ) {
            FrequencyGraph(
                profile = profile,
                selectedBandIndex = selectedBandIndex,
                onSelectBand = onSelectBand,
                onBandChange = { bandIdx, freq, gain ->
                    val current = profile.bands.firstOrNull { it.bandIndex == bandIdx } ?: return@FrequencyGraph
                    val prevBand = profile.bands.firstOrNull { it.bandIndex == bandIdx - 1 }
                    val nextBand = profile.bands.firstOrNull { it.bandIndex == bandIdx + 1 }
                    val minF = (prevBand?.let { it.frequency + 1.0 } ?: constraints.frequencyMin).coerceAtLeast(constraints.frequencyMin)
                    val maxF = (nextBand?.let { it.frequency - 1.0 } ?: constraints.frequencyMax).coerceAtMost(constraints.frequencyMax)
                    val clampedFreq = if (minF < maxF) freq.coerceIn(minF, maxF) else minF
                    onBandChange(current.copy(frequency = clampedFreq, gain = gain))
                },
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(207.dp)
            )
        }

        // Ribbon 3: 10-Band Parametric EQ Controls
        ExpressiveRibbon(
            title = strings.peq10BandTitle,
            icon = Icons.Filled.Equalizer,
            badgeText = strings.bandSelected(selectedBandIndex ?: 1),
            badgeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            subtitle = null,
            isExpanded = bandsExpanded,
            onToggleExpand = { bandsExpanded = !bandsExpanded }
        ) {
            BandEditor(
                bands = profile.bands,
                selectedBandIndex = selectedBandIndex,
                onSelectBand = onSelectBand,
                onBandChange = onBandChange,
                deviceConstraints = constraints
            )
        }

        // Ribbon 4: OTHERS (左右バランス & ピンクノイズ再生)
        ExpressiveRibbon(
            title = strings.othersTitle,
            icon = Icons.Filled.GraphicEq,
            badgeText = if (isPinkNoisePlaying) strings.pinkNoisePlaying else null,
            badgeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            subtitle = null,
            isExpanded = othersExpanded,
            onToggleExpand = { othersExpanded = !othersExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. 左右バランス (Channel Balance)
                val balanceLabel = when {
                    channelBalance == 0 -> "${strings.center} (L 100% : R 100%)"
                    channelBalance < 0 -> "L側 (L 100% : R ${(10 + channelBalance) * 10}%)"
                    else -> "R側 (L ${(10 - channelBalance) * 10}% : R 100%)"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.channelBalanceTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        balanceLabel,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val balanceColors = SliderDefaults.colors(
                    thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    activeTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                )
                Slider(
                    value = channelBalance.toFloat(),
                    onValueChange = { onBalanceChange(it.toInt()) },
                    valueRange = -10f..10f,
                    steps = 19,
                    colors = balanceColors,
                    track = { sliderState ->
                        SliderDefaults.CenteredTrack(
                            sliderState = sliderState,
                            colors = balanceColors
                        )
                    }
                )

                HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 2. ピンクノイズ再生 (Pink Noise Generator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            strings.pinkNoiseTitle,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isPinkNoisePlaying) strings.pinkNoisePlaying else strings.pinkNoiseStopped,
                            color = if (isPinkNoisePlaying) androidx.compose.material3.MaterialTheme.colorScheme.primary else TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onTogglePinkNoise,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPinkNoisePlaying) AmberWarning else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            contentColor = DarkBackground
                        )
                    ) {
                        Icon(
                            if (isPinkNoisePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isPinkNoisePlaying) "STOP" else "PLAY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Volume slider for Pink Noise
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.volumeLabel,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = pinkNoiseVolume,
                        onValueChange = onPinkNoiseVolumeChange,
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            activeTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${(pinkNoiseVolume * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }

        // Ribbon 5: Device Hardware Sync (Reset / Pull_DAC / Save_Flash)
        ExpressiveRibbon(
            title = strings.syncTitle,
            icon = Icons.Filled.Usb,
            badgeText = if (isConnected) "${strings.statusConnected}: $deviceName" else strings.statusDisconnected,
            badgeColor = if (isConnected) EmeraldGreen else TextMuted,
            subtitle = null,
            isExpanded = syncExpanded,
            onToggleExpand = { syncExpanded = !syncExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (supportLevel == DeviceSupportLevel.EXPERIMENTAL) {
                    Text(
                        text = strings.experimentalWarning,
                        color = AmberWarning,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Reset
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.weight(1f),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CoralError,
                            disabledContentColor = TextMuted
                        ),
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            Icons.Filled.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(strings.resetBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 2. Pull_DAC
                    OutlinedButton(
                        onClick = { if (isDirty) confirmPull = true else onPullFromDevice() },
                        enabled = canWrite && !busy,
                        modifier = Modifier.weight(1f),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            disabledContentColor = TextMuted
                        ),
                        border = BorderStroke(1.dp, if (canWrite && !busy) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(strings.pullDacBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 3. Save_Flash
                    Button(
                        onClick = { confirmCommit = true },
                        enabled = canWrite && !busy,
                        modifier = Modifier.weight(1f),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = DarkBackground,
                            disabledContainerColor = DarkCardBorder,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(strings.saveFlashBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text(strings.resetConfirmTitle, color = TextPrimary) },
            text = { Text(strings.resetConfirmMsg, color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onResetProfile()
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(strings.resetBtn, color = CoralError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmReset = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(strings.cancel, color = TextMuted)
                }
            }
        )
    }
    if (confirmPull) {
        AlertDialog(
            onDismissRequest = { confirmPull = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            title = { Text(strings.discardTitle, color = TextPrimary) },
            text = { Text(strings.discardMsg(selectedSlot, deviceName), color = TextSecondary) },
            confirmButton = { TextButton(onClick = { confirmPull = false; onPullFromDevice() }, shapes = ButtonDefaults.shapes()) { Text(strings.pullDacBtn, color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmPull = false }, shapes = ButtonDefaults.shapes()) { Text(strings.cancel, color = TextMuted) } }
        )
    }
    if (confirmCommit) {
        AlertDialog(
            onDismissRequest = { confirmCommit = false },
            containerColor = DarkSurfaceVariant,
            title = { Text(strings.saveTitle(selectedSlot), color = TextPrimary) },
            text = { Text(strings.saveMsg(selectedSlot, deviceName), color = TextSecondary) },
            confirmButton = { TextButton(onClick = { confirmCommit = false; onCommitToFlash() }, shapes = ButtonDefaults.shapes()) { Text(strings.saveFlashBtn, color = EmeraldGreen, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmCommit = false }, shapes = ButtonDefaults.shapes()) { Text(strings.cancel, color = TextMuted) } }
        )
    }
}
