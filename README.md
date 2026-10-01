# NEXVARY Security Suite

Android wireless-awareness and privacy toolkit focused on **local, receive-oriented observation** of nearby Wi-Fi and Bluetooth LE broadcasts.

## Wireless Watch v0.2.0

The current build includes:

- Foreground Wi-Fi + Bluetooth LE observation service
- Device-signature classification with confidence and severity
- DULT `0xFCB2` near-owner / separated decoding
- Google Find Hub `0xFEAA` nearby / separated decoding
- Bluetooth Remote ID `0xFFFA` decoding for ID, status, motion, location, altitude and operator fields
- Built-in signatures for trackers, drones, cameras, body cameras, vehicle networks, mesh nodes, sensors, wearables, access-control devices and network equipment
- User-defined Device Signatures
- Local alerts and high-attention protocol alerts
- Live search, RSSI, source and category filters
- Local session recording, history and comparison
- CSV session export through Android share
- Arabic RTL + English interface
- No scanner server dependency

## Accuracy boundary

A signature match is an indicator, not proof of device ownership, intent, or exact physical location. Android radio APIs impose platform permission, visibility and scan-throttling limits. The app does not attempt to connect to discovered devices.

## Requested stage tranche

The implementation ledger for the next stage plus the following 90 micro-stages is in:

`docs/STAGES_002_092.md`

## Build

Use JDK 17. GitHub Actions runs unit tests and builds a debug APK on every push to `main`.

## Third-party attribution

The first Wireless Watch work was inspired by and partially adapted from Fieldwatch under MIT. See:
- `licenses/FIELDWATCH-MIT.txt`
- `THIRD_PARTY_NOTICES.md`

The repository does not copy Fieldwatch's packed `radiodb.bin` lookup database.

## v0.3.0 identity / coexistence update

- Unique install id: `com.nexvary.securitysuite.lab` so this build can coexist with the previous app.
- Launcher identity: NEXVARY shield/N icon with electric-blue, metallic-silver, gold and neon accents.
- Rich dark NEXVARY theme with electric blue, metallic silver, gold, neon green and amber status accents.
- About page now exposes live official NEXVARY links for Website, Facebook, YouTube, X, email and GitHub.

