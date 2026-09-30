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
            val separated = dult?.separated == true
            return SignatureMatch(
                label = "DULT tracker",
                category = DeviceCategory.TRACKER,
                confidence = "protocol",
                reason = "FCB2 service; " + when (dult?.separated) {
                    true -> "separated"
                    false -> "near owner"
                    null -> "mode unknown"
                },
                severity = if (separated) AlertSeverity.HIGH else AlertSeverity.MEDIUM,
            )
        }

        observation.serviceData["FEAA"]?.let { data ->
            ProtocolDecoders.decodeFindHub(data)?.let { decoded ->
                return SignatureMatch(
                    label = "Google Find Hub",
                    category = DeviceCategory.TRACKER,
                    confidence = "protocol",
                    reason = if (decoded.separated) "FEAA separated frame" else "FEAA nearby frame",
                    severity = if (decoded.separated) AlertSeverity.HIGH else AlertSeverity.MEDIUM,
                )
            }
        }

        if ("FFFA" in services) {
            val rid = observation.serviceData["FFFA"]?.let(ProtocolDecoders::decodeRemoteId)
            val emergency = rid?.status == "Emergency" || rid?.status == "RID failure"
            return SignatureMatch(
                label = "Remote ID",
                category = DeviceCategory.DRONE,
                confidence = "protocol",
                reason = rid?.let {
                    "OpenDroneID " + (it.messageType ?: "frame") +
                        (it.status?.let { s -> " · " + s } ?: "")
                } ?: "OpenDroneID service UUID",
                severity = if (emergency) AlertSeverity.CRITICAL else AlertSeverity.MEDIUM,
            )
        }

        custom.firstOrNull { rule ->
            (rule.source == null || rule.source == observation.source) &&
                rule.keyword.isNotBlank() &&
                upper.contains(rule.keyword.uppercase())
        }?.let {
            return SignatureMatch(
                label = it.label,
                category = DeviceCategory.OTHER,
                confidence = "custom",
                reason = "custom keyword: " + it.keyword,
                severity = AlertSeverity.MEDIUM,
            )
        }

        BuiltInSignatures.entries.firstOrNull { signature ->
            (signature.source == null || signature.source == observation.source) &&
                signature.keywords.any { keyword -> upper.contains(keyword.uppercase()) }
        }?.let { signature ->
            val keyword = signature.keywords.first { upper.contains(it.uppercase()) }
            return SignatureMatch(
                label = signature.label,
                category = signature.category,
                confidence = "heuristic",
                reason = "name/SSID contains " + keyword,
                severity = signature.severity,
            )
        }

        return if (observation.source == RadioSource.WIFI) {
            SignatureMatch(
                label = "Wi-Fi network",
                category = DeviceCategory.NETWORK,
                confidence = "generic",
                reason = "Android Wi-Fi scan result",
                severity = AlertSeverity.INFO,
            )
        } else {
            null
        }
    }
}
