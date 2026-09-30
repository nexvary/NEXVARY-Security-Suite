package com.nexvary.securitysuite.wireless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
