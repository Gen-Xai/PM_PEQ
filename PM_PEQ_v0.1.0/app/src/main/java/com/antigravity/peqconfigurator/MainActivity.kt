package com.antigravity.peqconfigurator

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.antigravity.peqconfigurator.ui.main.MainNavigation
import com.antigravity.peqconfigurator.ui.main.MainViewModel
import com.antigravity.peqconfigurator.ui.theme.AndroidPEQConfiguratorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val detachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                @Suppress("DEPRECATION")
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                device?.let { viewModel.onUsbDetached(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, detachReceiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(detachReceiver)
        viewModel.pinkNoisePlayer.pause()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleUsbIntent(intent)

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val strings = androidx.compose.runtime.remember(appSettings.language) {
                com.antigravity.peqconfigurator.ui.i18n.getStringsForLanguage(appSettings.language)
            }

            androidx.compose.runtime.CompositionLocalProvider(
                com.antigravity.peqconfigurator.ui.i18n.LocalStrings provides strings
            ) {
                AndroidPEQConfiguratorTheme(appSettings = appSettings) {
                    MainNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            @Suppress("DEPRECATION")
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            device?.let {
                viewModel.connectUsbDevice(it)
            }
        }
    }
}
