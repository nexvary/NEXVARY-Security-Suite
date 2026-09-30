package com.nexvary.securitysuite.wireless

data class SessionDevice(
    val stableId: String,
    val source: RadioSource,
    val name: String?,
    val address: String?,
    val category: DeviceCategory?,
    val label: String?,
    val severity: AlertSeverity,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val strongestRssi: Int,
    val observations: Int,
)

data class SessionSnapshot(
    val id: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val devices: List<SessionDevice>,
) {
    val durationMs: Long get() = (endedAtMs - startedAtMs).coerceAtLeast(0)
    val matchedCount: Int get() = devices.count { it.label != null }
    val trackerCount: Int get() = devices.count { it.category == DeviceCategory.TRACKER }
    val droneCount: Int get() = devices.count { it.category == DeviceCategory.DRONE }
    val highAttentionCount: Int get() = devices.count {
        it.severity == AlertSeverity.HIGH || it.severity == AlertSeverity.CRITICAL
    }
}

data class SessionDiff(
    val firstId: String,
    val secondId: String,
    val added: List<SessionDevice>,
    val removed: List<SessionDevice>,
    val persisted: List<SessionDevice>,
)

class SessionRecorder {
    private data class MutableDevice(
        var observation: RadioObservation,
        var match: SignatureMatch?,
        val firstSeenMs: Long,
        var lastSeenMs: Long,
        var strongestRssi: Int,
        var observations: Int,
    )

    private var startedAtMs: Long? = null
    private val devices = linkedMapOf<String, MutableDevice>()

    val recording: Boolean get() = startedAtMs != null
    val startedAt: Long? get() = startedAtMs

    fun start(now: Long = System.currentTimeMillis()) {
        startedAtMs = now
        devices.clear()
    }

    fun observe(device: DisplayDevice) {
        if (!recording) return
        val now = device.observation.lastSeenMs
        val current = devices[device.observation.stableId]
        if (current == null) {
            devices[device.observation.stableId] = MutableDevice(
                observation = device.observation,
                match = device.match,
                firstSeenMs = now,
                lastSeenMs = now,
                strongestRssi = device.observation.rssi,
                observations = 1,
            )
        } else {
            current.observation = device.observation
            current.match = device.match
            current.lastSeenMs = now
            current.strongestRssi = maxOf(current.strongestRssi, device.observation.rssi)
            current.observations += 1
        }
    }

    fun stop(now: Long = System.currentTimeMillis()): SessionSnapshot? {
        val start = startedAtMs ?: return null
        val snapshot = SessionSnapshot(
            id = "session-" + start,
            startedAtMs = start,
            endedAtMs = now,
            devices = devices.values.map { value ->
                SessionDevice(
                    stableId = value.observation.stableId,
                    source = value.observation.source,
                    name = value.observation.name,
                    address = value.observation.address,
                    category = value.match?.category,
                    label = value.match?.label,
                    severity = value.match?.severity ?: AlertSeverity.INFO,
                    firstSeenMs = value.firstSeenMs,
                    lastSeenMs = value.lastSeenMs,
                    strongestRssi = value.strongestRssi,
                    observations = value.observations,
                )
            }.sortedByDescending { it.strongestRssi },
        )
        startedAtMs = null
        devices.clear()
        return snapshot
    }
}
