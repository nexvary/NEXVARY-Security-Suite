package com.nexvary.securitysuite.wireless

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceFilterTest {
    private fun device(
        id: String,
        source: RadioSource,
        rssi: Int,
        category: DeviceCategory? = null,
        label: String? = null,
    ): DisplayDevice = DisplayDevice(
        observation = RadioObservation(
            stableId = id,
            source = source,
            address = id,
            name = id,
            rssi = rssi,
            lastSeenMs = 1_000L,
        ),
        match = category?.let {
            SignatureMatch(label ?: it.name, it, "test", "test", AlertSeverity.LOW)
        },
    )

    @Test
    fun filtersBySourceCategoryAndSignal() {
        val devices = listOf(
            device("a", RadioSource.BLE, -50, DeviceCategory.TRACKER),
            device("b", RadioSource.WIFI, -55, DeviceCategory.NETWORK),
            device("c", RadioSource.BLE, -90, DeviceCategory.TRACKER),
        )
        val filtered = DeviceFilter.apply(
            devices,
            DeviceFilterState(
                minRssi = -70,
                source = RadioSource.BLE,
                category = DeviceCategory.TRACKER,
                recentOnlyMs = null,
            ),
            nowMs = 1_000L,
        )
        assertEquals(listOf("a"), filtered.map { it.observation.stableId })
    }
}
