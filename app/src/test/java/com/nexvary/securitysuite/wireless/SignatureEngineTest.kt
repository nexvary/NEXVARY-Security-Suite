package com.nexvary.securitysuite.wireless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureEngineTest {
    private val engine = SignatureEngine()

    @Test
    fun separatedDultGetsHighAttention() {
        val observation = RadioObservation(
            stableId = "x",
            source = RadioSource.BLE,
            address = "AA",
            name = null,
            rssi = -50,
            lastSeenMs = 1,
            serviceUuids = setOf("FCB2"),
            serviceData = mapOf("FCB2" to byteArrayOf(1, 0)),
        )
        val match = engine.classify(observation)!!
        assertEquals(DeviceCategory.TRACKER, match.category)
        assertEquals(AlertSeverity.HIGH, match.severity)
    }

    @Test
    fun builtInNameSignatureClassifiesDrone() {
        val observation = RadioObservation(
            stableId = "dji",
            source = RadioSource.WIFI,
            address = "BB",
            name = "DJI-Mavic-Test",
            rssi = -60,
            lastSeenMs = 1,
        )
        val match = engine.classify(observation)!!
        assertEquals(DeviceCategory.DRONE, match.category)
        assertTrue(match.label.contains("DJI"))
    }
}
