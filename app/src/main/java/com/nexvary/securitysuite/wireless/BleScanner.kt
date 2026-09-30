package com.nexvary.securitysuite.wireless

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.Locale

class BleScanner(
    context: Context,
    private val onObservation: (RadioObservation) -> Unit,
) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = emit(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::emit)
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val activeScanner = scanner ?: return false
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        return runCatching {
            activeScanner.startScan(null, settings, callback)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        runCatching { scanner?.stopScan(callback) }
    }

    @SuppressLint("MissingPermission")
    private fun emit(result: ScanResult) {
        val record = result.scanRecord
        val name = runCatching { record?.deviceName ?: result.device.name }.getOrNull()
        val address = runCatching { result.device.address }.getOrNull()
        val serviceData = buildMap<String, ByteArray> {
            record?.serviceData?.forEach { entry ->
                shortUuid(entry.key)?.let { put(it, entry.value) }
            }
        }
        val serviceUuids = buildSet {
            record?.serviceUuids?.forEach { uuid -> shortUuid(uuid)?.let(::add) }
            addAll(serviceData.keys)
        }
        val stable = "BLE:" + (address ?: name ?: result.hashCode().toString())
        onObservation(
            RadioObservation(
                stableId = stable,
                source = RadioSource.BLE,
                address = address,
                name = name,
                rssi = result.rssi,
                lastSeenMs = System.currentTimeMillis(),
                serviceUuids = serviceUuids,
                serviceData = serviceData,
                metadata = mapOf(
                    "connectable" to if (android.os.Build.VERSION.SDK_INT >= 26) result.isConnectable.toString() else "unknown",
                ),
            ),
        )
    }

    private fun shortUuid(uuid: ParcelUuid): String? {
        val value = uuid.uuid.toString().uppercase(Locale.US)
        val suffix = "-0000-1000-8000-00805F9B34FB"
        if (!value.endsWith(suffix)) return value
        val head = value.substringBefore("-")
        return if (head.startsWith("0000")) head.takeLast(4) else head
    }
}
