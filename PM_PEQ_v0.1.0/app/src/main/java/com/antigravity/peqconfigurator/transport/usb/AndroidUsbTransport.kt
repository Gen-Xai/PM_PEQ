package com.antigravity.peqconfigurator.transport.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.antigravity.peqconfigurator.data.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidUsbTransport(
    private val usbManager: UsbManager
) : UsbTransport {

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null
    private var activeDevice: UsbDevice? = null

    override suspend fun connect(device: UsbDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            disconnect()

            if (!usbManager.hasPermission(device)) {
                DiagnosticLogger.e("AndroidUsbTransport", "Permission denied for device ${device.deviceName}")
                return@withContext Result.failure(SecurityException("USB Permission Denied"))
            }

            val conn = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Failed to open USB device connection"))

            // Find valid HID or vendor-specific interface
            val candidates = (0 until device.interfaceCount).map { device.getInterface(it) }
            var targetInterface: UsbInterface? =
                candidates.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_HID }
                    ?: candidates.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC }
            if (targetInterface == null && device.interfaceCount > 0) {
                targetInterface = device.getInterface(0)
            }

            if (targetInterface == null) {
                conn.close()
                return@withContext Result.failure(Exception("No usable USB interface found on device"))
            }

            if (!conn.claimInterface(targetInterface, true)) {
                conn.close()
                return@withContext Result.failure(Exception("Failed to claim USB interface ${targetInterface.id}"))
            }

            var epOut: UsbEndpoint? = null
            var epIn: UsbEndpoint? = null

            for (i in 0 until targetInterface.endpointCount) {
                val ep = targetInterface.getEndpoint(i)
                if (ep.direction == UsbConstants.USB_DIR_OUT && epOut == null) {
                    epOut = ep
                } else if (ep.direction == UsbConstants.USB_DIR_IN && epIn == null) {
                    epIn = ep
                }
            }

            connection = conn
            usbInterface = targetInterface
            endpointOut = epOut
            endpointIn = epIn
            activeDevice = device

            DiagnosticLogger.i("AndroidUsbTransport", "Successfully connected to ${device.deviceName} (VID: 0x${device.vendorId.toString(16)}, PID: 0x${device.productId.toString(16)})")
            Result.success(true)
        } catch (e: Exception) {
            DiagnosticLogger.e("AndroidUsbTransport", "Connect error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Unit {
        withContext(Dispatchers.IO) {
        try {
            connection?.let { conn ->
                usbInterface?.let { iface -> conn.releaseInterface(iface) }
                conn.close()
            }
        } catch (e: Exception) {
            DiagnosticLogger.w("AndroidUsbTransport", "Disconnect error: ${e.message}")
        } finally {
            connection = null
            usbInterface = null
            endpointOut = null
            endpointIn = null
            activeDevice = null
            DiagnosticLogger.i("AndroidUsbTransport", "Disconnected USB Transport")
            }
    }
    }

    override fun isConnected(): Boolean = connection != null && activeDevice != null

    override suspend fun write(data: ByteArray, timeoutMs: Int): Result<Int> = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext Result.failure(Exception("USB not connected"))

        try {
            // First attempt bulk/interrupt endpoint if available
            val epOut = endpointOut
            if (epOut != null) {
                val transferred = conn.bulkTransfer(epOut, data, data.size, timeoutMs)
                if (transferred >= 0) {
                    DiagnosticLogger.d("AndroidUsbTransport", "Wrote $transferred bytes via EP Out")
                    return@withContext Result.success(transferred)
                }
            }

            // Fallback to HID Set Report via Control Transfer
            // requestType = 0x21 (Host to Device, Class, Interface)
            // request = 0x09 (SET_REPORT)
            // value = 0x0200 (Report Type Output << 8 | Report ID 0)
            val index = usbInterface?.id ?: 0
            val transferred = conn.controlTransfer(
                0x21, // USB_TYPE_CLASS | USB_RECIP_INTERFACE
                0x09, // SET_REPORT
                0x0200, // Report Type: Output (0x02), Report ID: 0x00
                index,
                data,
                data.size,
                timeoutMs
            )

            if (transferred >= 0) {
                DiagnosticLogger.d("AndroidUsbTransport", "Wrote $transferred bytes via Control Transfer")
                Result.success(transferred)
            } else {
                Result.failure(Exception("USB write failed with code $transferred"))
            }
        } catch (e: Exception) {
            DiagnosticLogger.e("AndroidUsbTransport", "Write error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun read(buffer: ByteArray, timeoutMs: Int): Result<Int> = withContext(Dispatchers.IO) {
        val conn = connection ?: return@withContext Result.failure(Exception("USB not connected"))

        try {
            val epIn = endpointIn
            if (epIn != null) {
                val readBytes = conn.bulkTransfer(epIn, buffer, buffer.size, timeoutMs)
                if (readBytes >= 0) {
                    DiagnosticLogger.d("AndroidUsbTransport", "Read $readBytes bytes via EP In")
                    return@withContext Result.success(readBytes)
                }
            }

            // Fallback to HID Get Report via Control Transfer
            // requestType = 0xA1 (Device to Host, Class, Interface)
            // request = 0x01 (GET_REPORT)
            val index = usbInterface?.id ?: 0
            val readBytes = conn.controlTransfer(
                0xA1,
                0x01, // GET_REPORT
                0x0100, // Report Type: Input (0x01), Report ID: 0x00
                index,
                buffer,
                buffer.size,
                timeoutMs
            )

            if (readBytes >= 0) {
                DiagnosticLogger.d("AndroidUsbTransport", "Read $readBytes bytes via Control Transfer")
                Result.success(readBytes)
            } else {
                Result.failure(Exception("USB read failed with code $readBytes"))
            }
        } catch (e: Exception) {
            DiagnosticLogger.e("AndroidUsbTransport", "Read error: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getDeviceName(): String? = activeDevice?.productName ?: activeDevice?.deviceName
}
