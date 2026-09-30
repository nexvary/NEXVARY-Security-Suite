# NEXVARY Security Suite

Android wireless-awareness and privacy toolkit focused on **local, receive-oriented observation** of nearby Wi-Fi and Bluetooth LE broadcasts.

## First integrated capability: Wireless Watch

The initial module is inspired by and partially adapted from the open-source Fieldwatch project (MIT). It is being rebuilt behind NEXVARY-owned interfaces so the suite can grow without coupling the whole application to a single upstream UI.

### Current scope
- Bluetooth LE discovery and advertisement metadata
- Wi-Fi scan result observation through Android system APIs
- Device-signature matching
- DULT service UUID `0xFCB2` recognition and near-owner/separated mode decoding
- Google Find Hub `0xFEAA` frame recognition
- ASTM/OpenDroneID `0xFFFA` Bluetooth Remote ID decoding
- Watch terms and local notifications
- Arabic RTL + English UI
- No server dependency for scanning

### Safety / accuracy
A signature match is an indicator, not proof of device ownership, identity, intent, or exact physical location. Android radio APIs also impose scan throttling and permission limits. The application does not attempt to connect to discovered devices.

## Build
Open the repository in Android Studio with JDK 17, or run the CI workflow. The project targets Android 10+.

## Third-party attribution
Fieldwatch portions are used/adapted under the MIT License. See `licenses/FIELDWATCH-MIT.txt` and `THIRD_PARTY_NOTICES.md`.
