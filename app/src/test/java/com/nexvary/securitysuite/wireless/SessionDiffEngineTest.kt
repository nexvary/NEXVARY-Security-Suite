package com.nexvary.securitysuite.wireless

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDiffEngineTest {
    private fun session(id: String, ids: List<String>): SessionSnapshot =
        SessionSnapshot(
            id = id,
            startedAtMs = 1,
            endedAtMs = 2,
            devices = ids.map {
                SessionDevice(
                    stableId = it,
                    source = RadioSource.BLE,
                    name = it,
                    address = it,
                    category = null,
                    label = null,
                    severity = AlertSeverity.INFO,
                    firstSeenMs = 1,
                    lastSeenMs = 2,
                    strongestRssi = -50,
                    observations = 1,
                )
            },
        )

    @Test
    fun comparesAddedRemovedPersisted() {
        val diff = SessionDiffEngine.compare(
            session("a", listOf("one", "two")),
            session("b", listOf("two", "three")),
        )
        assertEquals(listOf("three"), diff.added.map { it.stableId })
        assertEquals(listOf("one"), diff.removed.map { it.stableId })
        assertEquals(listOf("two"), diff.persisted.map { it.stableId })
    }
}
