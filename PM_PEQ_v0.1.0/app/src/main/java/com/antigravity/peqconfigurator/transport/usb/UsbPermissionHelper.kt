package com.antigravity.peqconfigurator.transport.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object UsbPermissionHelper {
    private const val ACTION = "com.antigravity.peqconfigurator.USB_PERMISSION"

    suspend fun ensurePermission(context: Context, usbManager: UsbManager, device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) return true
        return suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action != ACTION) return
                    runCatching { context.unregisterReceiver(this) }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (cont.isActive) cont.resume(granted)
                }
            }
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            val intent = Intent(ACTION).setPackage(context.packageName)
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
            usbManager.requestPermission(device, pi)
        }
    }
}
