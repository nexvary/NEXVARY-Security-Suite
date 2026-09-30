package com.nexvary.securitysuite.wireless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolDecodersTest {
    @Test
    fun dultSeparatedBitIsDecoded() {
        val separated = ProtocolDecoders.decodeDult(byteArrayOf(7, 0))
        val nearOwner = ProtocolDecoders.decodeDult(byteArrayOf(7, 1))
        assertEquals(7, separated?.networkId)
        assertTrue(separated?.separated == true)
        assertFalse(nearOwner?.separated == true)
    }

    @Test
    fun findHubModesAreDecoded() {
        assertFalse(ProtocolDecoders.decodeFindHub(byteArrayOf(0x40))!!.separated)
        assertTrue(ProtocolDecoders.decodeFindHub(byteArrayOf(0x41))!!.separated)
    }

    @Test
    fun remoteIdBasicIdIsDecoded() {
        val id = "NEXVARY-TEST-DRONE-01".padEnd(20, ' ').take(20)
        val payload = byteArrayOf(0x0D, 0x01, 0x02, 0x12) + id.toByteArray()
        val decoded = ProtocolDecoders.decodeRemoteId(payload)!!
        assertEquals("Basic ID", decoded.messageType)
        assertEquals(2, decoded.protocolVersion)
        assertTrue(decoded.uasId!!.startsWith("NEXVARY"))
    }

    @Test
    fun remoteIdLocationMotionIsDecoded() {
        val payload = ByteArray(21)
        payload[0] = 0x0D
        payload[1] = 0x02
        payload[2] = 0x12
        payload[3] = 0x20
        payload[4] = 90
        payload[5] = 40
        payload[6] = 4
        val decoded = ProtocolDecoders.decodeRemoteId(payload)
        assertNotNull(decoded)
        assertEquals("Location", decoded!!.messageType)
        assertEquals("Airborne", decoded.status)
        assertEquals(90.0, decoded.headingDegrees!!, 0.001)
        assertEquals(10.0, decoded.horizontalSpeedMps!!, 0.001)
        assertEquals(2.0, decoded.verticalSpeedMps!!, 0.001)
    }
}
