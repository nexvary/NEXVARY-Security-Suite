package com.nexvary.securitysuite.wireless

enum class RadioSource { BLE, WIFI }

enum class DeviceCategory {
    TRACKER,
    DRONE,
    CAMERA,
    BODY_CAMERA,
    VEHICLE,
    NETWORK,
    MESH,
    SENSOR,
    WEARABLE,
    ACCESS_CONTROL,
    OTHER,
}

enum class AlertSeverity(val weight: Int) {
    INFO(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4),
}

data class RadioObservation(
    val stableId: String,
    val source: RadioSource,
    val address: String?,
    val name: String?,
    val rssi: Int,
    val lastSeenMs: Long,
    val serviceUuids: Set<String> = emptySet(),
    val serviceData: Map<String, ByteArray> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
)

data class SignatureMatch(
    val label: String,
    val category: DeviceCategory,
    val confidence: String,
    val reason: String,
    val severity: AlertSeverity = AlertSeverity.INFO,
)

data class DisplayDevice(
    val observation: RadioObservation,
    val match: SignatureMatch?,
)

data class CustomSignature(
    val label: String,
    val keyword: String,
    val source: RadioSource? = null,
)
