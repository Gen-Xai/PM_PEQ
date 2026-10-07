package com.antigravity.peqconfigurator.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.device.session.ConnectionState
import com.antigravity.peqconfigurator.domain.device.DeviceSupportLevel
import com.antigravity.peqconfigurator.ui.components.BoxCard
import com.antigravity.peqconfigurator.ui.i18n.LocalStrings
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.EmeraldGreen
import com.antigravity.peqconfigurator.ui.theme.TextMuted
import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceScreen(
    connectionState: ConnectionState,
    onConnectRequested: () -> Unit,
    onDisconnectRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings.deviceInfoTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Connection Card
        BoxCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isConnected = connectionState is ConnectionState.Connected
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Usb else Icons.Filled.UsbOff,
                        contentDescription = null,
                        tint = if (isConnected) EmeraldGreen else TextMuted,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        val title = when (connectionState) {
                            is ConnectionState.Connected -> connectionState.deviceProfile.model
                            is ConnectionState.Connecting -> "${strings.statusConnecting} (${connectionState.deviceName})"
                            is ConnectionState.Reading -> connectionState.message
                            is ConnectionState.Writing -> connectionState.message
                            is ConnectionState.Verifying -> connectionState.message
                            is ConnectionState.Error -> "${strings.statusError}: ${connectionState.title}"
                            ConnectionState.Disconnected -> strings.noDacConnected
                        }
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)

                        val sub = when (connectionState) {
                            is ConnectionState.Connected -> "VID: 0x${connectionState.deviceProfile.vendorId.toString(16).uppercase()}  PID: 0x${connectionState.deviceProfile.productId.toString(16).uppercase()} (${connectionState.deviceProfile.protocolName})"
                            is ConnectionState.Error -> connectionState.detail
                            else -> strings.plugDacPrompt
                        }
                        Text(sub, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (connectionState is ConnectionState.Connected) {
                    Button(
                        onClick = onDisconnectRequested,
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder, contentColor = TextPrimary)
                    ) {
                        Text(strings.disconnect)
                    }
                } else {
                    Button(
                        onClick = onConnectRequested,
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(strings.scanAndConnect)
                    }
                }
            }
        }

        // Capabilities & Support Level Card
        if (connectionState is ConnectionState.Connected) {
            val prof = connectionState.deviceProfile
            BoxCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.deviceCapabilitiesTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        
                        val levelBadgeColor = when (prof.supportLevel) {
                            DeviceSupportLevel.CONFIRMED -> EmeraldGreen
                            DeviceSupportLevel.EXPERIMENTAL -> MaterialTheme.colorScheme.primary
                            DeviceSupportLevel.UNSUPPORTED -> Color(0xFFFF5252)
                        }
                        val levelText = when (prof.supportLevel) {
                            DeviceSupportLevel.CONFIRMED -> strings.supportConfirmed
                            DeviceSupportLevel.EXPERIMENTAL -> strings.supportExperimental
                            DeviceSupportLevel.UNSUPPORTED -> strings.supportUnsupported
                        }
                        Text(
                            text = levelText,
                            color = levelBadgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(levelBadgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CapabilityItem(strings.cap10BandPeq, prof.capabilities.hasPeq, strings = strings)
                    CapabilityItem(strings.capPreampGain, prof.capabilities.hasPreamp, strings = strings)
                    CapabilityItem(strings.capDacFilter, prof.capabilities.hasDacFilter, strings = strings)
                    CapabilityItem(strings.capGainMode, prof.capabilities.hasGainMode, strings = strings)
                    CapabilityItem(strings.capBalance, prof.capabilities.hasBalance, strings = strings)
                    CapabilityItem(strings.capPresetSlots, true, strings.presetSlotsCount(prof.capabilities.presetSlotsCount), strings = strings)
                }
            }
        }
    }
}

@Composable
fun CapabilityItem(
    title: String,
    supported: Boolean,
    detail: String? = null,
    strings: com.antigravity.peqconfigurator.ui.i18n.Strings = LocalStrings.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextPrimary, fontSize = 13.sp)
        Text(
            text = detail ?: if (supported) strings.supported else strings.notSupported,
            color = if (supported) EmeraldGreen else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

