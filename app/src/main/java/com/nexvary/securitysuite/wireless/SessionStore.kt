package com.nexvary.securitysuite.wireless

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("wireless_sessions", Context.MODE_PRIVATE)

    fun load(): List<SessionSnapshot> {
        val raw = prefs.getString(KEY, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(decodeSession(array.getJSONObject(i)))
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.startedAtMs }
    }

    fun save(session: SessionSnapshot): List<SessionSnapshot> {
        val sessions = (listOf(session) + load().filterNot { it.id == session.id })
            .sortedByDescending { it.startedAtMs }
            .take(MAX_SESSIONS)
        persist(sessions)
        return sessions
    }

    fun delete(id: String): List<SessionSnapshot> {
        val sessions = load().filterNot { it.id == id }
        persist(sessions)
        return sessions
    }

    fun clear(): List<SessionSnapshot> {
        persist(emptyList())
        return emptyList()
    }

    private fun persist(sessions: List<SessionSnapshot>) {
        val array = JSONArray()
        sessions.forEach { array.put(encodeSession(it)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun encodeSession(session: SessionSnapshot): JSONObject = JSONObject()
        .put("id", session.id)
        .put("started", session.startedAtMs)
        .put("ended", session.endedAtMs)
        .put("devices", JSONArray().apply {
            session.devices.take(MAX_DEVICES_PER_SESSION).forEach { d ->
                put(
                    JSONObject()
                        .put("id", d.stableId)
                        .put("source", d.source.name)
                        .put("name", d.name)
                        .put("address", d.address)
                        .put("category", d.category?.name)
                        .put("label", d.label)
                        .put("severity", d.severity.name)
                        .put("first", d.firstSeenMs)
                        .put("last", d.lastSeenMs)
                        .put("rssi", d.strongestRssi)
                        .put("count", d.observations),
                )
            }
        })

    private fun decodeSession(obj: JSONObject): SessionSnapshot {
        val devicesJson = obj.optJSONArray("devices") ?: JSONArray()
        val devices = buildList {
            for (i in 0 until devicesJson.length()) {
                val d = devicesJson.getJSONObject(i)
                add(
                    SessionDevice(
                        stableId = d.getString("id"),
                        source = runCatching { RadioSource.valueOf(d.getString("source")) }.getOrDefault(RadioSource.BLE),
                        name = d.optString("name").takeIf { it.isNotBlank() && it != "null" },
                        address = d.optString("address").takeIf { it.isNotBlank() && it != "null" },
                        category = d.optString("category").takeIf { it.isNotBlank() && it != "null" }
                            ?.let { runCatching { DeviceCategory.valueOf(it) }.getOrNull() },
                        label = d.optString("label").takeIf { it.isNotBlank() && it != "null" },
                        severity = runCatching { AlertSeverity.valueOf(d.optString("severity")) }
                            .getOrDefault(AlertSeverity.INFO),
                        firstSeenMs = d.optLong("first"),
                        lastSeenMs = d.optLong("last"),
                        strongestRssi = d.optInt("rssi", -127),
                        observations = d.optInt("count", 1),
                    ),
                )
            }
        }
        return SessionSnapshot(
            id = obj.getString("id"),
            startedAtMs = obj.optLong("started"),
            endedAtMs = obj.optLong("ended"),
            devices = devices,
        )
    }

    private companion object {
        const val KEY = "sessions_json"
        const val MAX_SESSIONS = 30
        const val MAX_DEVICES_PER_SESSION = 500
    }
}
