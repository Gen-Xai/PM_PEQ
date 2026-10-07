package com.antigravity.peqconfigurator.ui.main

import com.antigravity.peqconfigurator.ui.theme.TextPrimary
import com.antigravity.peqconfigurator.ui.theme.TextMuted

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.peqconfigurator.device.session.ConnectionState
import com.antigravity.peqconfigurator.ui.device.DeviceScreen
import com.antigravity.peqconfigurator.ui.i18n.LocalStrings
import com.antigravity.peqconfigurator.ui.peq.PeqScreen
import com.antigravity.peqconfigurator.ui.profiles.ProfilesScreen
import com.antigravity.peqconfigurator.ui.settings.SettingsScreen
import com.antigravity.peqconfigurator.ui.theme.DarkCardBorder
import com.antigravity.peqconfigurator.ui.theme.EmeraldGreen



enum class NavigationTab(val icon: ImageVector) {
    PEQ(Icons.Filled.Equalizer),
    DEVICE(Icons.Filled.Usb),
    PROFILES(Icons.Filled.Folder),
    SETTINGS(Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentProfile by viewModel.currentProfile.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val selectedSlot by viewModel.selectedSlot.collectAsState()
    val selectedBandIndex by viewModel.selectedBandIndex.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val dacSettings by viewModel.dacSettings.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val slotAssignments by viewModel.slotAssignments.collectAsState()
    val isPinkNoisePlaying by viewModel.isPinkNoisePlaying.collectAsState()
    val pinkNoiseVolume by viewModel.pinkNoiseVolume.collectAsState()

    val tabTitles = listOf(
        strings.navPeq,
        strings.navDevice,
        strings.navProfiles,
        strings.navSettings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PM_PEQ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        // Connection Status Pill
                        StatusPill(connectionState = connectionState)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ShortNavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                NavigationTab.entries.forEachIndexed { index, tab ->
                    val title = tabTitles.getOrElse(index) { "" }
                    ShortNavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = title) },
                        label = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        colors = ShortNavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> PeqScreen(
                profile = currentProfile,
                connectionState = connectionState,
                isDirty = isDirty,
                errorMessage = lastError,
                onDismissError = { viewModel.clearError() },
                selectedSlot = selectedSlot,
                selectedBandIndex = selectedBandIndex,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSelectSlot = { viewModel.selectSlot(it) },
                onSelectBand = { viewModel.selectBand(it) },
                onBandChange = { viewModel.updateBand(it) },
                onPreampChange = { viewModel.updatePreamp(it) },
                onResetProfile = { viewModel.resetCurrentProfile() },
                channelBalance = (dacSettings["balance"] as? Number)?.toInt() ?: 0,
                onBalanceChange = { viewModel.setDacSetting("balance", it) },
                isPinkNoisePlaying = isPinkNoisePlaying,
                pinkNoiseVolume = pinkNoiseVolume,
                onTogglePinkNoise = { viewModel.togglePinkNoise() },
                onPinkNoiseVolumeChange = { viewModel.setPinkNoiseVolume(it) },
                onPullFromDevice = { viewModel.pullFromDevice() },
                onPushToDevice = { viewModel.pushToDevice() },
                onCommitToFlash = { viewModel.commitToFlash() },
                onDragStart = { viewModel.onDragStart() },
                onDragEnd = { viewModel.onDragEnd() },
                modifier = modifier
            )
            1 -> DeviceScreen(
                connectionState = connectionState,
                onConnectRequested = { viewModel.autoDetectUsbDevice() },
                onDisconnectRequested = { viewModel.disconnectUsbDevice() },
                modifier = modifier
            )
            2 -> ProfilesScreen(
                profiles = profiles,
                activeProfileId = currentProfile.id,
                activeProfileName = currentProfile.name,
                slotAssignments = slotAssignments,
                onAssignSlot = { slot, profId -> viewModel.assignProfileToSlot(slot, profId) },
                onLoadProfile = { viewModel.loadProfile(it) },
                onSaveProfile = { viewModel.saveCurrentProfile(it) },
                onDuplicateProfile = { viewModel.duplicateProfile(it) },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onImportApoProfile = { viewModel.importApoProfile(it) },
                onRenameProfile = { p, n -> viewModel.renameProfile(p, n) },
                modifier = modifier
            )
            3 -> SettingsScreen(
                appSettings = appSettings,
                onThemeModeChanged = { viewModel.setThemeMode(it) },
                onColorPaletteChanged = { viewModel.setColorPalette(it) },
                onLanguageChanged = { viewModel.setLanguage(it) },
                isDebugEnabled = appSettings.isDebugEnabled,
                logs = logs,
                onToggleDebug = { viewModel.toggleDebug(it) },
                onClearLogs = { viewModel.clearLogs() },
                modifier = modifier
            )
        }
    }
}

@Composable
fun StatusPill(connectionState: ConnectionState) {
    val strings = LocalStrings.current
    val isConnected = connectionState is ConnectionState.Connected
    val pillBg = if (isConnected) EmeraldGreen.copy(alpha = 0.15f) else DarkCardBorder
    val dotColor = if (isConnected) EmeraldGreen else TextMuted
    val statusText = when (connectionState) {
        is ConnectionState.Connected -> connectionState.deviceProfile.model
        is ConnectionState.Connecting -> strings.statusConnecting
        is ConnectionState.Reading -> connectionState.message
        is ConnectionState.Writing -> connectionState.message
        is ConnectionState.Verifying -> connectionState.message
        is ConnectionState.Error -> strings.statusError
        ConnectionState.Disconnected -> strings.statusDisconnected
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(statusText, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
