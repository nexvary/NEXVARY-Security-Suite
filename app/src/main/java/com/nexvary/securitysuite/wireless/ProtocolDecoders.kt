package com.nexvary.securitysuite.wireless

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ProtocolDecoders {
    data class DultResult(val networkId: Int, val separated: Boolean)
    data class FindHubResult(val separated: Boolean, val ephemeralIdHex: String?)
    data class RemoteIdResult(
        val appCode: Int?,
        val counter: Int?,
        val messageType: String?,
        val protocolVersion: Int?,
        val uasId: String? = null,
        val status: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitudeBarometricM: Double? = null,
        val altitudeGeoM: Double? = null,
        val operatorId: String? = null,
    )

    fun decodeDult(payload: ByteArray): DultResult? {
        if (payload.size < 2) return null
        val networkId = payload[0].toInt() and 0xFF
        val nearOwner = payload[1].toInt() and 0x01 == 1
        return DultResult(networkId, !nearOwner)
    }

    fun decodeFindHub(payload: ByteArray): FindHubResult? {
        if (payload.isEmpty()) return null
        val separated = when (payload[0].toInt() and 0xFF) {
            0x40 -> false
            0x41 -> true
            else -> return null
        }
        val eid = if (payload.size > 1) payload.copyOfRange(1, payload.size.coerceAtMost(21)).toHex() else null
        return FindHubResult(separated, eid)
    }

    fun decodeRemoteId(payload: ByteArray): RemoteIdResult? {
        if (payload.size < 3) return null
        val appCode = payload[0].toInt() and 0xFF
        val counter = payload[1].toInt() and 0xFF
        val header = payload[2].toInt() and 0xFF
        val type = header ushr 4
        val protocol = header and 0x0F
        val typeLabel = when (type) {
            0 -> "Basic ID"
            1 -> "Location"
            2 -> "Auth"
            3 -> "Self ID"
            4 -> "System"
            5 -> "Operator ID"
            15 -> "Message pack"
            else -> "Type " + type
        }

        var uasId: String? = null
        var status: String? = null
        var lat: Double? = null
        var lon: Double? = null
        var altBaro: Double? = null
        var altGeo: Double? = null
        var operatorId: String? = null

        if (protocol in 0..2) {
            when (type) {
                0 -> if (payload.size >= 24) uasId = ascii(payload, 4, 20)
                1 -> if (payload.size >= 21) {
                    val statusCode = (payload[3].toInt() ushr 4) and 0x0F
                    status = when (statusCode) {
                        0 -> "Undeclared"
                        1 -> "Ground"
                        2 -> "Airborne"
                        3 -> "Emergency"
                        4 -> "RID failure"
                        else -> "Status " + statusCode
                    }
                    lat = i32le(payload, 7)?.times(1e-7)
                    lon = i32le(payload, 11)?.times(1e-7)
                    altBaro = u16le(payload, 15)?.times(0.5)?.minus(1000.0)
                    altGeo = u16le(payload, 17)?.times(0.5)?.minus(1000.0)
                }
                5 -> if (payload.size >= 24) operatorId = ascii(payload, 4, 20)
            }
        }

        return RemoteIdResult(appCode, counter, typeLabel, protocol, uasId, status, lat, lon, altBaro, altGeo, operatorId)
    }

    private fun i32le(bytes: ByteArray, offset: Int): Int? {
        if (offset + 4 > bytes.size) return null
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun u16le(bytes: ByteArray, offset: Int): Int? {
        if (offset + 2 > bytes.size) return null
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String? {
        if (offset >= bytes.size) return null
        val end = (offset + length).coerceAtMost(bytes.size)
        return bytes.copyOfRange(offset, end).toString(Charsets.UTF_8)
            .trim('\u0000', ' ', '\t', '\r', '\n').takeIf { it.isNotBlank() }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
