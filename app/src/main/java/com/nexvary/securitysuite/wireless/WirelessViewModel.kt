package com.nexvary.securitysuite.wireless

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WirelessUiState(
    val scanning: Boolean = false,
    val devices: List<DisplayDevice> = emptyList(),
    val watchTerms: List<String> = emptyList(),
    val customSignatures: List<CustomSignature> = emptyList(),
    val status: String = "Ready",
)

class WirelessViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = SignatureEngine()
    private val store = WatchStore(application)
    private val notifier = NotificationHelper(application)
    private val ble = BleScanner(application, ::onObservation)
    private val wifi = WifiScanner(application, ::onObservation)

    private val _state = MutableStateFlow(
        WirelessUiState(
            watchTerms = store.watchTerms(),
            customSignatures = store.customSignatures(),
        ),
    )
    val state: StateFlow<WirelessUiState> = _state.asStateFlow()

    fun toggleScan() {
        if (_state.value.scanning) stop() else start()
    }

    fun start() {
        if (!hasPermissions()) {
            _state.update { it.copy(status = "Grant Nearby devices / Location permissions first") }
            return
        }
        val bleStarted = ble.start()
        val wifiStarted = wifi.start()
        _state.update {
            it.copy(
                scanning = bleStarted || wifiStarted,
                status = "BLE " + (if (bleStarted) "ON" else "OFF") +
                    " · Wi-Fi " + (if (wifiStarted) "ON" else "OFF"),
            )
        }
    }

    fun stop() {
        ble.stop()
        wifi.stop()
        _state.update { it.copy(scanning = false, status = "Stopped") }
    }

    fun addWatchTerm(raw: String) {
        val term = raw.trim()
        if (term.isBlank()) return
        val values = (_state.value.watchTerms + term).distinctBy { it.lowercase() }.sorted()
        store.saveWatchTerms(values)
        _state.update { it.copy(watchTerms = values) }
    }

    fun removeWatchTerm(term: String) {
        val values = _state.value.watchTerms.filterNot { it.equals(term, true) }
        store.saveWatchTerms(values)
        _state.update { it.copy(watchTerms = values) }
    }

    fun addCustomSignature(labelRaw: String, keywordRaw: String) {
        val label = labelRaw.trim()
        val keyword = keywordRaw.trim()
        if (label.isBlank() || keyword.isBlank()) return
        val values = (_state.value.customSignatures + CustomSignature(label, keyword))
            .distinctBy { it.label.lowercase() + "|" + it.keyword.lowercase() }
        store.saveCustomSignatures(values)
        _state.update { old ->
            old.copy(
                customSignatures = values,
                devices = old.devices.map { item ->
                    item.copy(match = engine.classify(item.observation, values))
                },
            )
        }
    }

    fun removeCustomSignature(signature: CustomSignature) {
        val values = _state.value.customSignatures.filterNot { it == signature }
        store.saveCustomSignatures(values)
        _state.update { old ->
            old.copy(
                customSignatures = values,
                devices = old.devices.map { item ->
                    item.copy(match = engine.classify(item.observation, values))
                },
            )
        }
    }

    private fun onObservation(observation: RadioObservation) {
        val custom = _state.value.customSignatures
        val display = DisplayDevice(observation, engine.classify(observation, custom))
        _state.update { current ->
            val map = current.devices.associateBy { it.observation.stableId }.toMutableMap()
            map[observation.stableId] = display
            current.copy(devices = map.values.sortedByDescending { it.observation.lastSeenMs }.take(300))
        }

        val haystack = listOfNotNull(
            observation.name,
            observation.address,
            display.match?.label,
            display.match?.reason,
        ).joinToString(" ").lowercase()

        _state.value.watchTerms.firstOrNull { haystack.contains(it.lowercase()) }
            ?.let { notifier.alert(it, display) }
    }

    private fun hasPermissions(): Boolean {
        val app = getApplication<Application>()
        val location = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val bluetooth = Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.BLUETOOTH_SCAN,
            ) == PackageManager.PERMISSION_GRANTED
        val wifiNearby = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return location && bluetooth && wifiNearby
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
