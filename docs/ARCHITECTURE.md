# Architecture

## Wireless Watch v0.2

```
Android Wi-Fi / BLE APIs
        |
        v
WifiScanner / BleScanner
        |
        v
WirelessMonitorService (foreground)
        |
        v
ObservationBus
        |
        v
WirelessViewModel
   |       |       |
   |       |       +--> NotificationHelper
   |       +----------> SessionRecorder / SessionStore
   +------------------> SignatureEngine / ProtocolDecoders
                           |
                           +--> BuiltInSignatures
                           +--> DULT / Find Hub / Remote ID
```

### Design rules

- Scanners collect only metadata that Android exposes through public APIs.
- Classification is separated from acquisition.
- Protocol-backed matches are distinguished from heuristic name/SSID matches.
- Signature matches are indicators, not claims of ownership, intent, or exact device identity.
- Session history is local-only and bounded.
- Export is explicit and user-initiated.
- No cloud service is required for the Wireless Watch module.
