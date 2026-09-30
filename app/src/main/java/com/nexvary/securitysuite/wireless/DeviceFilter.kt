package com.nexvary.securitysuite.wireless

data class DeviceFilterState(
    val query: String = "",
    val minRssi: Int = -100,
    val source: RadioSource? = null,
    val category: DeviceCategory? = null,
    val matchedOnly: Boolean = false,
    val recentOnlyMs: Long? = 10 * 60_000L,
)

object DeviceFilter {
    fun apply(
        devices: List<DisplayDevice>,
        filter: DeviceFilterState,
        nowMs: Long = System.currentTimeMillis(),
    ): List<DisplayDevice> {
        val query = filter.query.trim().lowercase()
        return devices.asSequence()
            .filter { it.observation.rssi >= filter.minRssi }
            .filter { filter.source == null || it.observation.source == filter.source }
            .filter { filter.category == null || it.match?.category == filter.category }
            .filter { !filter.matchedOnly || it.match != null }
            .filter { filter.recentOnlyMs == null || nowMs - it.observation.lastSeenMs <= filter.recentOnlyMs }
            .filter { device ->
                if (query.isBlank()) true
                else listOfNotNull(
                    device.observation.name,
                    device.observation.address,
                    device.match?.label,
                    device.match?.reason,
                    device.match?.category?.name,
                ).joinToString(" ").lowercase().contains(query)
            }
            .sortedWith(
                compareByDescending<DisplayDevice> { it.match?.severity?.weight ?: 0 }
                    .thenByDescending { it.observation.rssi }
                    .thenByDescending { it.observation.lastSeenMs },
            )
            .toList()
    }
}
