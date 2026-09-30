package com.nexvary.securitysuite.wireless

class SignatureEngine {
    fun classify(
        observation: RadioObservation,
        custom: List<CustomSignature> = emptyList(),
    ): SignatureMatch? {
        val upper = observation.name.orEmpty().uppercase()
        val services = observation.serviceUuids.map { it.uppercase() }.toSet()

        if ("FCB2" in services) {
            val dult = observation.serviceData["FCB2"]?.let(ProtocolDecoders::decodeDult)
            val mode = when (dult?.separated) {
                true -> "separated"
                false -> "near owner"
                null -> "mode unknown"
            }
            return SignatureMatch("DULT tracker", DeviceCategory.TRACKER, "protocol", "FCB2 service; " + mode)
        }

        observation.serviceData["FEAA"]?.let { data ->
            ProtocolDecoders.decodeFindHub(data)?.let { decoded ->
                return SignatureMatch(
                    "Google Find Hub",
                    DeviceCategory.TRACKER,
                    "protocol",
                    if (decoded.separated) "FEAA separated frame" else "FEAA nearby frame",
                )
            }
        }

        if ("FFFA" in services) {
            val rid = observation.serviceData["FFFA"]?.let(ProtocolDecoders::decodeRemoteId)
            return SignatureMatch(
                "Remote ID",
                DeviceCategory.DRONE,
                "protocol",
                rid?.messageType?.let { "OpenDroneID " + it } ?: "OpenDroneID service UUID",
            )
        }

        custom.firstOrNull { rule ->
            (rule.source == null || rule.source == observation.source) &&
                rule.keyword.isNotBlank() &&
                upper.contains(rule.keyword.uppercase())
        }?.let {
            return SignatureMatch(it.label, DeviceCategory.OTHER, "custom", "custom keyword: " + it.keyword)
        }

        val rules = listOf(
            Triple(DeviceCategory.TRACKER, "Item tracker", listOf("AIRTAG", "CHIPOLO", "PEBBLEBEE", "MOTO TAG", "SMARTTAG", "TILE")),
            Triple(DeviceCategory.DRONE, "Drone / controller", listOf("DJI", "SKYDIO", "AUTEL", "PARROT", "HOVERAIR", "MAVIC")),
            Triple(DeviceCategory.BODY_CAMERA, "Body camera", listOf("AXON", "BODYCAM", "BODY CAM", "BWC", "WATCHGUARD VIDEO")),
            Triple(DeviceCategory.CAMERA, "Camera", listOf("GOPRO", "INSTA360", "OSMO", "REOLINK", "HIKVISION", "DAHUA", "WYZE", "EUFY", "ARLO", "RING")),
            Triple(DeviceCategory.VEHICLE, "Vehicle network", listOf("TESLA", "MBUX", "UCONNECT", "CARPLAY", "AUDI MMI", "BMW", "RIVIAN")),
        )
        rules.forEach { (category, label, keywords) ->
            val hit = keywords.firstOrNull { upper.contains(it) }
            if (hit != null) {
                return SignatureMatch(label, category, "heuristic", "name/SSID contains " + hit)
            }
        }

        return if (observation.source == RadioSource.WIFI) {
            SignatureMatch("Wi-Fi network", DeviceCategory.NETWORK, "generic", "Android Wi-Fi scan result")
        } else null
    }
}
