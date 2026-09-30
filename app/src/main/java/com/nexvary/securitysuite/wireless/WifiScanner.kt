package com.nexvary.securitysuite.wireless

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper

class WifiScanner(
    private val context: Context,
    private val onObservation: (RadioObservation) -> Unit,
) {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = publish()
    }

    private val repeat = object : Runnable {
        override fun run() {
            requestScan()
            handler.postDelayed(this, 30_000)
        }
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (wifi == null) return false
        if (!registered) {
            context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            registered = true
        }
        handler.removeCallbacks(repeat)
        handler.post(repeat)
        publish()
        return true
    }

    fun stop() {
        handler.removeCallbacks(repeat)
        if (registered) {
            runCatching { context.unregisterReceiver(receiver) }
            registered = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestScan() {
        runCatching { wifi?.startScan() }
    }

    @SuppressLint("MissingPermission")
    private fun publish() {
        val results = runCatching { wifi?.scanResults.orEmpty() }.getOrDefault(emptyList())
        results.forEach { result ->
            val ssid = ssid(result)
            val bssid = result.BSSID
            onObservation(
                RadioObservation(
                    stableId = "WIFI:" + (bssid ?: ssid ?: result.hashCode().toString()),
                    source = RadioSource.WIFI,
                    address = bssid,
                    name = ssid,
                    rssi = result.level,
                    lastSeenMs = System.currentTimeMillis(),
                    metadata = buildMap {
                        put("frequencyMHz", result.frequency.toString())
                        put("capabilities", result.capabilities.orEmpty())
                        if (android.os.Build.VERSION.SDK_INT >= 30) put("standard", wifiStandard(result))
                    },
                ),
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun ssid(result: ScanResult): String? =
        result.SSID?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }

    private fun wifiStandard(result: ScanResult): String {
        if (android.os.Build.VERSION.SDK_INT < 30) return "unknown"
        return when (result.wifiStandard) {
            ScanResult.WIFI_STANDARD_LEGACY -> "802.11a/b/g"
            ScanResult.WIFI_STANDARD_11N -> "802.11n"
            ScanResult.WIFI_STANDARD_11AC -> "802.11ac"
            ScanResult.WIFI_STANDARD_11AX -> "802.11ax"
            ScanResult.WIFI_STANDARD_11AD -> "802.11ad"
            else -> "unknown"
        }
    }
}
