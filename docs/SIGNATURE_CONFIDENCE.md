# Signature confidence

NEXVARY Security Suite exposes why a classification occurred.

## protocol
A protocol identifier or payload structure matched, such as:
- DULT service UUID `FCB2`
- Google Find Hub service data `FEAA`
- Bluetooth Remote ID `FFFA`

## heuristic
A device name or Wi-Fi SSID matched a built-in keyword catalog. This is useful for triage but can produce false positives.

## custom
A locally configured user keyword matched.

## generic
The app can describe the radio object (for example a Wi-Fi scan result) without asserting a specific device family.

Severity is separate from confidence. For example, a protocol-backed separated DULT frame is high attention, while a generic Wi-Fi network is informational.
