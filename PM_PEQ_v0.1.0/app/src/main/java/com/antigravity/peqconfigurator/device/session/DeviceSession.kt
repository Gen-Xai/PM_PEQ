package com.antigravity.peqconfigurator.device.session

import android.hardware.usb.UsbDevice
import com.antigravity.peqconfigurator.data.DiagnosticLogger
import com.antigravity.peqconfigurator.device.registry.DeviceRegistry
import com.antigravity.peqconfigurator.domain.device.DeviceSupportLevel
import com.antigravity.peqconfigurator.domain.eq.EqProfile
import com.antigravity.peqconfigurator.protocol.DacException
import com.antigravity.peqconfigurator.protocol.ProtocolAdapter
import com.antigravity.peqconfigurator.transport.usb.UsbTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DeviceSession(
    private val transport: UsbTransport
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Editor/DAC/LastSent separation (spec §31). */
    private val _dacProfile = MutableStateFlow<EqProfile?>(null)
    val dacProfile: StateFlow<EqProfile?> = _dacProfile.asStateFlow()

    private val _lastSentProfile = MutableStateFlow<EqProfile?>(null)
    val lastSentProfile: StateFlow<EqProfile?> = _lastSentProfile.asStateFlow()

    /**
     * Operation-level, non-fatal error shown to the user. Failed operations no longer leave the
     * session stuck in an Error state (which previously disabled Pull/Push permanently).
     */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val opLock = Mutex()
    private var activeAdapter: ProtocolAdapter? = null
    private var activeDevice: UsbDevice? = null

    var currentSlot: Int = 1
        private set

    fun reportError(message: String) {
        _lastError.value = message
    }

    fun clearError() {
        _lastError.value = null
    }

    /** True when write operations may be offered in the UI (spec §38). */
    fun isWritable(): Boolean {
        val adapter = activeAdapter ?: return false
        return adapter.deviceProfile.supportLevel != DeviceSupportLevel.UNSUPPORTED
    }

    suspend fun connectDevice(device: UsbDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting(device.productName ?: "USB Device")

        val connRes = transport.connect(device)
        if (connRes.isFailure) {
            val err = connRes.exceptionOrNull()?.message ?: "USB Connection failed"
            _connectionState.value = ConnectionState.Error("Connection Failed", err)
            _lastError.value = "Connection Failed: $err"
            return@withContext Result.failure(connRes.exceptionOrNull() ?: Exception(err))
        }
        activeDevice = device

        val adapter = DeviceRegistry.matchAdapter(device, transport)
        if (adapter == null) {
            val fallbackProfile = DeviceRegistry.getGenericFallbackProfile(device)
            _connectionState.value = ConnectionState.Connected(fallbackProfile)
            _lastError.value = "Unsupported Device: no protocol adapter for ${fallbackProfile.model}"
            DiagnosticLogger.w("DeviceSession", "Connected device ${device.deviceName} has no matching protocol adapter (UNSUPPORTED)")
            return@withContext Result.success(true)
        }

        activeAdapter = adapter
        _connectionState.value = ConnectionState.Connected(adapter.deviceProfile, currentSlot)
        DiagnosticLogger.i("DeviceSession", "Connected and bound protocol adapter: ${adapter.deviceProfile.protocolName}")

        // Initial pull; failure is reported but the session stays usable
        pullFromDevice(currentSlot)
        Result.success(true)
    }

    suspend fun disconnectDevice() = withContext(Dispatchers.IO) {
        transport.disconnect()
        activeAdapter = null
        activeDevice = null
        _dacProfile.value = null
        _lastSentProfile.value = null
        _connectionState.value = ConnectionState.Disconnected
        DiagnosticLogger.i("DeviceSession", "Session disconnected")
    }

    /** Called when USB_DEVICE_DETACHED is received (spec §26 Device Disconnected). */
    suspend fun onDeviceDetached(device: UsbDevice) {
        val active = activeDevice ?: return
        if (active.deviceName == device.deviceName) {
            disconnectDevice()
            _lastError.value = "Device Disconnected: the DAC was unplugged"
        }
    }

    suspend fun pullFromDevice(slot: Int = currentSlot): Result<EqProfile> = opLock.withLock {
        withContext(Dispatchers.IO) {
            val adapter = activeAdapter
                ?: return@withContext Result.failure(DacException.UnsupportedDevice("No protocol adapter active"))

            currentSlot = slot
            val model = adapter.deviceProfile.model
            _connectionState.value = ConnectionState.Reading("Pulling EQ settings from Slot $slot...")

            val res = adapter.pullEqProfile(slot)
            _connectionState.value = ConnectionState.Connected(adapter.deviceProfile, slot)
            if (res.isSuccess) {
                _dacProfile.value = res.getOrThrow()
                _lastError.value = null
                DiagnosticLogger.i("DeviceSession", "Pulled EQ state from $model slot $slot")
            } else {
                val cause = res.exceptionOrNull()?.message ?: "unknown error"
                _lastError.value = "Read Failed: could not read slot $slot from $model ($cause)"
            }
            res
        }
    }

    /** Result value: true = pushed AND readback verified (or skipped); false = pushed but verification mismatch. */
    suspend fun pushToDevice(profile: EqProfile, slot: Int = currentSlot, verify: Boolean = true): Result<Boolean> = opLock.withLock {
        withContext(Dispatchers.IO) {
            val adapter = activeAdapter
                ?: return@withContext Result.failure(DacException.UnsupportedDevice("No protocol adapter active"))
            if (adapter.deviceProfile.supportLevel == DeviceSupportLevel.UNSUPPORTED) {
                return@withContext Result.failure(DacException.UnsupportedDevice("Device is UNSUPPORTED; writes are disabled"))
            }

            currentSlot = slot
            val model = adapter.deviceProfile.model
            if (verify) {
                _connectionState.value = ConnectionState.Writing("Pushing EQ settings to Slot $slot...")
            }

            val pushRes = adapter.pushEqProfile(profile, slot)
            if (pushRes.isFailure) {
                _connectionState.value = ConnectionState.Connected(adapter.deviceProfile, slot)
                val cause = pushRes.exceptionOrNull()?.message ?: "unknown error"
                _lastError.value = "Write Failed: could not write slot $slot to $model ($cause)"
                return@withContext Result.failure(pushRes.exceptionOrNull() ?: Exception(cause))
            }
            _lastSentProfile.value = profile

            if (!verify) {
                _dacProfile.value = profile
                DiagnosticLogger.d("DeviceSession", "Real-time push to RAM complete")
                return@withContext Result.success(true)
            }

            _connectionState.value = ConnectionState.Verifying("Verifying parameters via readback...")
            val verifyRes = adapter.readbackVerify(profile, slot)
            _connectionState.value = ConnectionState.Connected(adapter.deviceProfile, slot)

            when {
                verifyRes.isFailure -> {
                    _lastError.value = "Verification Failed: readback from $model failed (${verifyRes.exceptionOrNull()?.message})"
                    Result.success(false)
                }
                verifyRes.getOrNull() == true -> {
                    _dacProfile.value = profile
                    _lastError.value = null
                    DiagnosticLogger.i("DeviceSession", "Push & verification OK")
                    Result.success(true)
                }
                else -> {
                    _lastError.value = "Verification Failed: values read back from $model differ from what was sent (rounded or rejected by the DAC)"
                    DiagnosticLogger.w("DeviceSession", "Readback mismatch")
                    Result.success(false)
                }
            }
        }
    }

    suspend fun commitSave(slot: Int = currentSlot): Result<Boolean> = opLock.withLock {
        withContext(Dispatchers.IO) {
            val adapter = activeAdapter
                ?: return@withContext Result.failure(DacException.UnsupportedDevice("No protocol adapter active"))
            if (adapter.deviceProfile.supportLevel == DeviceSupportLevel.UNSUPPORTED) {
                return@withContext Result.failure(DacException.UnsupportedDevice("Device is UNSUPPORTED; writes are disabled"))
            }

            _connectionState.value = ConnectionState.Writing("Committing parameters to DAC flash...")
            val res = adapter.commitSave(slot)
            _connectionState.value = ConnectionState.Connected(adapter.deviceProfile, slot)
            if (res.isFailure) {
                _lastError.value = "Write Failed: could not save slot $slot to flash on ${adapter.deviceProfile.model} (${res.exceptionOrNull()?.message})"
            }
            res
        }
    }

    suspend fun setDacSetting(key: String, value: Any): Result<Boolean> = opLock.withLock {
        withContext(Dispatchers.IO) {
            val adapter = activeAdapter
                ?: return@withContext Result.failure(DacException.UnsupportedDevice("No protocol adapter active"))
            val res = adapter.setDacSpecificSetting(key, value)
            if (res.isFailure) _lastError.value = "Write Failed: could not change $key (${res.exceptionOrNull()?.message})"
            res
        }
    }

    fun getActiveAdapter(): ProtocolAdapter? = activeAdapter
}
