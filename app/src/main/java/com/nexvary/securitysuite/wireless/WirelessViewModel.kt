package com.nexvary.securitysuite.wireless

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveStats(
    val total: Int = 0,
    val ble: Int = 0,
    val wifi: Int = 0,
    val matched: Int = 0,
    val trackers: Int = 0,
    val drones: Int = 0,
    val highAttention: Int = 0,
)

data class WirelessUiState(
    val scanning: Boolean = false,
    val devices: List<DisplayDevice> = emptyList(),
    val watchTerms: List<String> = emptyList(),
    val customSignatures: List<CustomSignature> = emptyList(),
    val filter: DeviceFilterState = DeviceFilterState(),
    val sessions: List<SessionSnapshot> = emptyList(),
    val recording: Boolean = false,
    val recordingStartedAt: Long? = null,
    val lastDiff: SessionDiff? = null,
    val stats: LiveStats = LiveStats(),
    val status: String = "Ready",
)

class WirelessViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = SignatureEngine()
    private val watchStore = WatchStore(application)
    private val sessionStore = SessionStore(application)
    private val notifier = NotificationHelper(application)
    private val recorder = SessionRecorder()

    private val _state = MutableStateFlow(
        WirelessUiState(
            watchTerms = watchStore.watchTerms(),
            customSignatures = watchStore.customSignatures(),
            sessions = sessionStore.load(),
        ),
    )
    val state: StateFlow<WirelessUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ObservationBus.events.collect { onObservation(it) }
        }
        viewModelScope.launch {
            ObservationBus.active.collect { active ->
                _state.update {
                    it.copy(
                        scanning = active,
                        status = if (active) "Wireless Watch active" else if (it.scanning) "Starting radios…" else it.status,
                    )
                }
            }
        }
    }

    fun toggleScan() {
        if (_state.value.scanning) stop() else start()
    }

    fun start() {
        if (!hasPermissions()) {
            _state.update { it.copy(status = "Grant Nearby devices / Location permissions first") }
            return
        }
        val app = getApplication<Application>()
        ContextCompat.startForegroundService(app, Intent(app, WirelessMonitorService::class.java))
        _state.update { it.copy(scanning = true, status = "Starting radios…") }
    }

    fun stop() {
        val app = getApplication<Application>()
        app.stopService(Intent(app, WirelessMonitorService::class.java))
        _state.update { it.copy(scanning = false, status = "Stopped") }
    }

    fun toggleRecording() {
        if (recorder.recording) stopRecording() else startRecording()
    }

    fun startRecording() {
        recorder.start()
        _state.update {
            it.copy(
                recording = true,
                recordingStartedAt = recorder.startedAt,
                status = "Session recording",
            )
        }
    }

    fun stopRecording() {
        val snapshot = recorder.stop()
        if (snapshot == null) {
            _state.update { it.copy(recording = false, recordingStartedAt = null) }
            return
        }
        val sessions = sessionStore.save(snapshot)
        _state.update {
            it.copy(
                sessions = sessions,
                recording = false,
                recordingStartedAt = null,
                status = "Session saved · " + snapshot.devices.size + " devices",
            )
        }
    }

    fun compareLastTwo() {
        val sessions = _state.value.sessions
        val diff = if (sessions.size >= 2) SessionDiffEngine.compare(sessions[1], sessions[0]) else null
        _state.update {
            it.copy(
                lastDiff = diff,
                status = if (diff == null) "Need two saved sessions" else "Session comparison ready",
            )
        }
    }

    fun deleteSession(id: String) {
        _state.update { it.copy(sessions = sessionStore.delete(id), lastDiff = null) }
    }

    fun clearSessions() {
        _state.update { it.copy(sessions = sessionStore.clear(), lastDiff = null) }
    }

    fun clearLive() {
        _state.update { it.copy(devices = emptyList(), stats = LiveStats()) }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(filter = it.filter.copy(query = value)) }
    }

    fun setMinRssi(value: Int) {
        _state.update { it.copy(filter = it.filter.copy(minRssi = value.coerceIn(-100, -20))) }
    }

    fun setSourceFilter(value: RadioSource?) {
        _state.update { it.copy(filter = it.filter.copy(source = value)) }
    }

    fun setCategoryFilter(value: DeviceCategory?) {
        _state.update { it.copy(filter = it.filter.copy(category = value)) }
    }

    fun toggleMatchedOnly() {
        _state.update { it.copy(filter = it.filter.copy(matchedOnly = !it.filter.matchedOnly)) }
    }

    fun addWatchTerm(raw: String) {
        val term = raw.trim()
        if (term.isBlank()) return
        val values = (_state.value.watchTerms + term).distinctBy { it.lowercase() }.sorted()
        watchStore.saveWatchTerms(values)
        _state.update { it.copy(watchTerms = values) }
    }

    fun removeWatchTerm(term: String) {
        val values = _state.value.watchTerms.filterNot { it.equals(term, true) }
        watchStore.saveWatchTerms(values)
        _state.update { it.copy(watchTerms = values) }
    }

    fun addCustomSignature(labelRaw: String, keywordRaw: String) {
        val label = labelRaw.trim()
        val keyword = keywordRaw.trim()
        if (label.isBlank() || keyword.isBlank()) return
        val values = (_state.value.customSignatures + CustomSignature(label, keyword))
            .distinctBy { it.label.lowercase() + "|" + it.keyword.lowercase() }
        watchStore.saveCustomSignatures(values)
        _state.update { old ->
            val rematched = old.devices.map { item ->
                item.copy(match = engine.classify(item.observation, values))
            }
            old.copy(
                customSignatures = values,
                devices = rematched,
                stats = calculateStats(rematched),
            )
        }
    }

    fun removeCustomSignature(signature: CustomSignature) {
        val values = _state.value.customSignatures.filterNot { it == signature }
        watchStore.saveCustomSignatures(values)
        _state.update { old ->
            val rematched = old.devices.map { item ->
                item.copy(match = engine.classify(item.observation, values))
            }
            old.copy(
                customSignatures = values,
                devices = rematched,
                stats = calculateStats(rematched),
            )
        }
    }

    private fun onObservation(observation: RadioObservation) {
        val custom = _state.value.customSignatures
        val display = DisplayDevice(observation, engine.classify(observation, custom))
        recorder.observe(display)

        _state.update { current ->
            val map = current.devices.associateBy { it.observation.stableId }.toMutableMap()
            map[observation.stableId] = display
            val devices = map.values
                .filter { System.currentTimeMillis() - it.observation.lastSeenMs <= 60 * 60_000L }
                .sortedByDescending { it.observation.lastSeenMs }
                .take(600)
            current.copy(
                devices = devices,
                stats = calculateStats(devices),
            )
        }

        val haystack = listOfNotNull(
            observation.name,
            observation.address,
            display.match?.label,
            display.match?.reason,
        ).joinToString(" ").lowercase()

        _state.value.watchTerms.firstOrNull { haystack.contains(it.lowercase()) }
            ?.let { notifier.alert(it, display) }

        notifier.alertAttention(display)
    }

    private fun calculateStats(devices: List<DisplayDevice>): LiveStats = LiveStats(
        total = devices.size,
        ble = devices.count { it.observation.source == RadioSource.BLE },
        wifi = devices.count { it.observation.source == RadioSource.WIFI },
        matched = devices.count { it.match != null },
        trackers = devices.count { it.match?.category == DeviceCategory.TRACKER },
        drones = devices.count { it.match?.category == DeviceCategory.DRONE },
        highAttention = devices.count {
            (it.match?.severity?.weight ?: 0) >= AlertSeverity.HIGH.weight
        },
    )

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
}
