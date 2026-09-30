package com.nexvary.securitysuite.wireless

enum class RadioSource { BLE, WIFI }
enum class DeviceCategory { TRACKER, DRONE, CAMERA, BODY_CAMERA, VEHICLE, NETWORK, OTHER }

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
)

data class DisplayDevice(val observation: RadioObservation, val match: SignatureMatch?)
data class CustomSignature(val label: String, val keyword: String, val source: RadioSource? = null)
