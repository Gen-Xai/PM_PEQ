package com.antigravity.peqconfigurator.transport.usb

import android.hardware.usb.UsbDevice

interface UsbTransport {
    suspend fun connect(device: UsbDevice): Result<Boolean>
    suspend fun disconnect()
    fun isConnected(): Boolean
    suspend fun write(data: ByteArray, timeoutMs: Int = 1000): Result<Int>
    suspend fun read(buffer: ByteArray, timeoutMs: Int = 1000): Result<Int>
    fun getDeviceName(): String?
}
