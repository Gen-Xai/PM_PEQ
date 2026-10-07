package com.antigravity.peqconfigurator.ui.main

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.peqconfigurator.data.DiagnosticLogger
import com.antigravity.peqconfigurator.data.LogEntry
import com.antigravity.peqconfigurator.data.ProfileStore
import com.antigravity.peqconfigurator.device.registry.DeviceRegistry
import com.antigravity.peqconfigurator.device.session.ConnectionState
import com.antigravity.peqconfigurator.device.session.DeviceSession
import com.antigravity.peqconfigurator.domain.device.DeviceConstraints
import com.antigravity.peqconfigurator.domain.eq.ConstraintSystem
import com.antigravity.peqconfigurator.domain.eq.EqBand
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.domain.eq.clamp
import com.antigravity.peqconfigurator.transport.usb.AndroidUsbTransport
import com.antigravity.peqconfigurator.transport.usb.UsbPermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val usbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
    private val usbTransport = AndroidUsbTransport(usbManager)
    val deviceSession = DeviceSession(usbTransport)
    private val profileStore = ProfileStore(application)
    private val settingsStore = com.antigravity.peqconfigurator.data.SettingsStore(application)

    val appSettings: StateFlow<com.antigravity.peqconfigurator.data.AppSettings> = settingsStore.settings

    val connectionState: StateFlow<ConnectionState> = deviceSession.connectionState
    val lastError: StateFlow<String?> = deviceSession.lastError

    private val _currentProfile = MutableStateFlow(EqProfile())
    val currentProfile: StateFlow<EqProfile> = _currentProfile.asStateFlow()

    /** Last state loaded from disk / DAC / saved; used for unsaved-change detection (spec §30). */
    private val baseline = MutableStateFlow(_currentProfile.value)
    val isDirty: StateFlow<Boolean> = combine(_currentProfile, baseline) { c, b ->
        c.bands != b.bands || c.preamp != b.preamp
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _profiles = MutableStateFlow<List<EqProfile>>(emptyList())
    val profiles: StateFlow<List<EqProfile>> = _profiles.asStateFlow()

    private val _slotAssignments = MutableStateFlow<Map<Int, String>>(emptyMap())
    val slotAssignments: StateFlow<Map<Int, String>> = _slotAssignments.asStateFlow()

    private val _selectedSlot = MutableStateFlow(1)
    val selectedSlot: StateFlow<Int> = _selectedSlot.asStateFlow()

    private val _selectedBandIndex = MutableStateFlow<Int?>(1)
    val selectedBandIndex: StateFlow<Int?> = _selectedBandIndex.asStateFlow()

    private val _dacSettings = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dacSettings: StateFlow<Map<String, Any>> = _dacSettings.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = DiagnosticLogger.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Pink Noise Generator
    val pinkNoisePlayer = com.antigravity.peqconfigurator.domain.audio.PinkNoisePlayer()
    val isPinkNoisePlaying: StateFlow<Boolean> = pinkNoisePlayer.isPlaying
    val pinkNoiseVolume: StateFlow<Float> = pinkNoisePlayer.volume

    fun togglePinkNoise() = pinkNoisePlayer.togglePlay()
    fun setPinkNoiseVolume(vol: Float) = pinkNoisePlayer.setVolume(vol)

    // Real-time Push to RAM Debounce Job
    private var realTimePushJob: Job? = null

    private fun scheduleRealtimePush() {
        if (!deviceSession.isWritable()) return
        realTimePushJob?.cancel()
        realTimePushJob = viewModelScope.launch {
            delay(120)
            val sent = _currentProfile.value
            deviceSession.pushToDevice(sent, _selectedSlot.value, verify = false)
        }
    }

    // Undo / Redo
    private val undoStack = mutableListOf<EqProfile>()
    private val redoStack = mutableListOf<EqProfile>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Edit coalescing: a continuous drag / slider / typing burst is ONE undo step (spec §32)
    private var dragging = false
    private var lastEditKey: String? = null
    private var lastEditTime = 0L

    init {
        DiagnosticLogger.isDebugEnabled = settingsStore.settings.value.isDebugEnabled
        _slotAssignments.value = profileStore.getSlotAssignments()
        loadProfilesFromDisk(selectFirst = true)
        autoDetectUsbDevice()
    }

    private fun constraints(): DeviceConstraints =
        (connectionState.value as? ConnectionState.Connected)?.deviceProfile?.constraints
            ?: DeviceConstraints()

    private fun pushHistoryState() {
        undoStack.add(_currentProfile.value)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun recordCoalesced(key: String) {
        if (dragging) return
        val now = SystemClock.elapsedRealtime()
        if (key != lastEditKey || now - lastEditTime > 800) pushHistoryState()
        lastEditKey = key
        lastEditTime = now
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun resetHistory() {
        undoStack.clear(); redoStack.clear(); lastEditKey = null
        updateHistoryFlags()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_currentProfile.value)
            _currentProfile.value = last
            lastEditKey = null
            updateHistoryFlags()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_currentProfile.value)
            _currentProfile.value = next
            lastEditKey = null
            updateHistoryFlags()
        }
    }

    fun selectBand(bandIndex: Int) {
        _selectedBandIndex.value = bandIndex
    }

    fun selectSlot(slot: Int) {
        if (_selectedSlot.value == slot) return
        val current = _currentProfile.value
        // Persist edits before switching slots
        if (isDirty.value) {
            viewModelScope.launch {
                profileStore.saveProfile(current)
            }
            _profiles.value = _profiles.value.map { if (it.id == current.id) current else it }
            baseline.value = current
        }
        _selectedSlot.value = slot
        val assignedId = _slotAssignments.value[slot]
        val target = _profiles.value.firstOrNull { it.id == assignedId }
            ?: _profiles.value.firstOrNull { it.presetSlot == slot }
            ?: EqProfile(id = "slot-$slot-default", name = "Slot $slot", presetSlot = slot).also { newProf ->
                viewModelScope.launch {
                    profileStore.saveProfile(newProf)
                    profileStore.setSlotAssignment(slot, newProf.id)
                }
                _profiles.value = _profiles.value + newProf
                _slotAssignments.value = _slotAssignments.value + (slot to newProf.id)
            }

        adoptProfile(target)
    }

    fun onDragStart() {
        pushHistoryState()
        dragging = true
    }

    fun onDragEnd() {
        dragging = false
        lastEditKey = null
        if (connectionState.value is ConnectionState.Connected) {
            realTimePushJob?.cancel()
            viewModelScope.launch {
                val sent = _currentProfile.value
                deviceSession.pushToDevice(sent, _selectedSlot.value)
            }
        }
    }

    fun updateBand(requested: EqBand) {
        val current = _currentProfile.value
        val old = current.bands.firstOrNull { it.bandIndex == requested.bandIndex } ?: return
        
        // Enforce strict frequency order: BAND 1 < BAND 2 < ... < BAND 10
        val prevBand = current.bands.firstOrNull { it.bandIndex == requested.bandIndex - 1 }
        val nextBand = current.bands.firstOrNull { it.bandIndex == requested.bandIndex + 1 }
        val minFreq = (prevBand?.frequency?.let { it + 1.0 } ?: constraints().frequencyMin).coerceAtLeast(constraints().frequencyMin)
        val maxFreq = (nextBand?.frequency?.let { it - 1.0 } ?: constraints().frequencyMax).coerceAtMost(constraints().frequencyMax)
        val validFreq = if (minFreq < maxFreq) requested.frequency.coerceIn(minFreq, maxFreq) else minFreq

        val updated = constraints().clamp(requested.copy(frequency = validFreq))
        if (updated == old) return

        if (!dragging) {
            if (updated.enabled != old.enabled || updated.type != old.type) {
                pushHistoryState(); lastEditKey = null
            } else {
                recordCoalesced("band${updated.bandIndex}")
            }
        }
        _currentProfile.value = current.copy(
            bands = current.bands.map { if (it.bandIndex == updated.bandIndex) updated else it }
        )
        scheduleRealtimePush()
    }

    fun updatePreamp(preampDb: Double) {
        if (preampDb == _currentProfile.value.preamp) return
        recordCoalesced("preamp")
        _currentProfile.value = _currentProfile.value.copy(preamp = preampDb)
        scheduleRealtimePush()
    }

    fun resetCurrentProfile() {
        pushHistoryState()
        val reset = _currentProfile.value.copy(
            preamp = 0.0,
            bands = EqProfile.default10Bands()
        )
        _currentProfile.value = reset
        scheduleRealtimePush()
    }

    fun autoDetectUsbDevice() {
        val deviceList = usbManager.deviceList
        DiagnosticLogger.i("MainViewModel", "Scanning USB host bus, found ${deviceList.size} devices")
        // Never open arbitrary USB devices (spec §27): only registry-known ones
        val target = deviceList.values.firstOrNull { DeviceRegistry.isKnown(it) }
        if (target != null) {
            connectUsbDevice(target)
        } else if (deviceList.isNotEmpty()) {
            deviceSession.reportError("Unsupported Device: no supported DAC found among ${deviceList.size} USB device(s)")
        }
    }

    fun connectUsbDevice(device: UsbDevice) {
        if (!DeviceRegistry.isKnown(device)) {
            deviceSession.reportError("Unsupported Device: VID 0x${device.vendorId.toString(16)} PID 0x${device.productId.toString(16)}")
            return
        }
        viewModelScope.launch {
            val granted = UsbPermissionHelper.ensurePermission(getApplication(), usbManager, device)
            if (!granted) {
                deviceSession.reportError("USB Permission Denied: access to the DAC was not granted")
                return@launch
            }
            val res = deviceSession.connectDevice(device)
            if (res.isSuccess) {
                deviceSession.dacProfile.value?.let { adoptProfile(it) }
                fetchDacSettings()
            }
        }
    }

    fun onUsbDetached(device: UsbDevice) {
        viewModelScope.launch { deviceSession.onDeviceDetached(device) }
    }

    fun disconnectUsbDevice() {
        viewModelScope.launch { deviceSession.disconnectDevice() }
    }

    fun clearError() = deviceSession.clearError()

    /** Replace the editor content (pull / load / import); resets baseline and history. */
    private fun adoptProfile(profile: EqProfile, resetHistoryState: Boolean = true) {
        val sanitized = ConstraintSystem.sanitizeProfile(profile, null)
        _currentProfile.value = sanitized
        baseline.value = sanitized
        if (resetHistoryState) {
            resetHistory()
        } else {
            pushHistoryState()
        }
    }

    fun pullFromDevice() {
        viewModelScope.launch {
            val res = deviceSession.pullFromDevice(_selectedSlot.value)
            if (res.isSuccess) adoptProfile(res.getOrThrow())
        }
    }

    fun pushToDevice() {
        viewModelScope.launch {
            val sent = _currentProfile.value
            val res = deviceSession.pushToDevice(sent, _selectedSlot.value)
            if (res.isSuccess) baseline.value = sent
        }
    }

    fun commitToFlash() {
        viewModelScope.launch { deviceSession.commitSave(_selectedSlot.value) }
    }

    private fun fetchDacSettings() {
        viewModelScope.launch {
            val res = deviceSession.getActiveAdapter()?.getDacSpecificSettings()
            if (res != null && res.isSuccess) _dacSettings.value = res.getOrThrow()
        }
    }

    fun setDacSetting(key: String, value: Any) {
        if (key == "balance") {
            val bal = (value as? Number)?.toInt() ?: 0
            pinkNoisePlayer.setBalance(bal)
        }
        viewModelScope.launch {
            val res = deviceSession.setDacSetting(key, value)
            if (res.isSuccess) {
                _dacSettings.value = _dacSettings.value.toMutableMap().apply { put(key, value) }
            }
        }
    }

    private fun loadProfilesFromDisk(selectFirst: Boolean = false) {
        viewModelScope.launch {
            refreshProfilesFromDisk(selectFirst)
        }
    }

    private suspend fun refreshProfilesFromDisk(selectFirst: Boolean = false) {
        val list = profileStore.getAllProfiles()
        _profiles.value = list
        val slotMap = profileStore.getSlotAssignments()
        _slotAssignments.value = slotMap

        if (selectFirst) {
            val currentSlot = _selectedSlot.value
            val assignedId = slotMap[currentSlot]
            val initial = list.firstOrNull { it.id == assignedId }
                ?: list.firstOrNull { it.presetSlot == currentSlot }
                ?: list.firstOrNull()
                ?: EqProfile()
            adoptProfile(initial)
        }
    }

    fun loadProfile(profile: EqProfile) {
        val slotEntry = _slotAssignments.value.entries.firstOrNull { it.value == profile.id }
        if (slotEntry != null) {
            _selectedSlot.value = slotEntry.key
        } else {
            assignProfileToSlot(_selectedSlot.value, profile.id)
        }
        adoptProfile(profile)
    }

    fun assignProfileToSlot(slot: Int, profileId: String) {
        profileStore.setSlotAssignment(slot, profileId)
        val updatedMap = profileStore.getSlotAssignments()
        _slotAssignments.value = updatedMap
        if (_selectedSlot.value == slot) {
            val prof = _profiles.value.firstOrNull { it.id == profileId }
            if (prof != null) {
                adoptProfile(prof)
            }
        }
    }

    /** Overwrites the profile with the same name, otherwise creates a new one linked to current slot. */
    fun saveCurrentProfile(name: String) {
        viewModelScope.launch {
            val existing = _profiles.value.firstOrNull { it.name == name }
            val toSave = _currentProfile.value.copy(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                name = name,
                presetSlot = _selectedSlot.value
            )
            profileStore.saveProfile(toSave)
            profileStore.setSlotAssignment(_selectedSlot.value, toSave.id)
            _currentProfile.value = toSave
            baseline.value = toSave
            loadProfilesFromDisk()
        }
    }

    fun renameProfile(profile: EqProfile, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val renamed = profile.copy(name = newName.trim())
            profileStore.saveProfile(renamed)
            if (_currentProfile.value.id == profile.id) {
                _currentProfile.value = _currentProfile.value.copy(name = renamed.name)
            }
            loadProfilesFromDisk()
        }
    }

    fun duplicateProfile(profile: EqProfile) {
        viewModelScope.launch {
            profileStore.duplicateProfile(profile)
            loadProfilesFromDisk()
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            profileStore.deleteProfile(id)
            val currentSlots = profileStore.getSlotAssignments()
            for ((slot, profId) in currentSlots) {
                if (profId == id) {
                    val defaultId = "slot-$slot-default"
                    val fallbackProfile = EqProfile(
                        id = defaultId,
                        name = "Slot $slot",
                        presetSlot = slot
                    )
                    profileStore.saveProfile(fallbackProfile)
                    profileStore.setSlotAssignment(slot, defaultId)
                }
            }
            refreshProfilesFromDisk(selectFirst = false)
            if (_currentProfile.value.id == id) {
                val updatedMap = profileStore.getSlotAssignments()
                val newProf = _profiles.value.firstOrNull { it.id == updatedMap[_selectedSlot.value] }
                    ?: EqProfile()
                adoptProfile(newProf)
            }
        }
    }

    fun importApoProfile(profile: EqProfile) {
        viewModelScope.launch {
            // Sort by frequency and reindex to guarantee BAND 1 < 2 < ... < 10
            val sortedBands = profile.bands.sortedBy { it.frequency }
            val bands = sortedBands.take(10).mapIndexed { i, b -> b.copy(bandIndex = i + 1) }
            val normalized = ConstraintSystem.sanitizeProfile(profile.copy(bands = bands, presetSlot = _selectedSlot.value), null)
            profileStore.saveProfile(normalized)
            profileStore.setSlotAssignment(_selectedSlot.value, normalized.id)
            adoptProfile(normalized)
            loadProfilesFromDisk()
        }
    }

    fun toggleDebug(enabled: Boolean) {
        settingsStore.setDebugEnabled(enabled)
        DiagnosticLogger.isDebugEnabled = enabled
    }

    fun setThemeMode(mode: com.antigravity.peqconfigurator.data.ThemeMode) {
        settingsStore.setThemeMode(mode)
    }

    fun setColorPalette(palette: com.antigravity.peqconfigurator.data.ColorPalette) {
        settingsStore.setColorPalette(palette)
    }

    fun setLanguage(lang: com.antigravity.peqconfigurator.data.AppLanguage) {
        settingsStore.setLanguage(lang)
    }

    fun clearLogs() {
        DiagnosticLogger.clear()
    }

    override fun onCleared() {
        super.onCleared()
        pinkNoisePlayer.release()
    }
}
