# v0.9.1 validation — 15 November 2026

- Android build and lint passed (0 errors, 41 warnings).
- APK version 0.9.1, code 8; Android 8.0+; ARM64 and x86_64.
- Signature verified; signing certificate unchanged from v0.9.0.
- APK 16 KB ZIP alignment passed; all 140 native ELF libraries passed alignment checks.
- Installed successfully in the Android 15 emulator. Embedded Python 3.13.16, cryptography 42.0.8, SSH key parsing, backend assets and Java callback passed without vehicle traffic.
- UI automation did not complete reliably: authorization-dialog detection failed and a subsequent UI run stalled. No full UI regression pass is claimed for this build.
- Release packaging verifies the APK source fingerprint against the provided source ZIP and includes licensing notices.

No physical phone, Wi-Fi/USB adapter or real BMW head unit was tested. This is a debug-signed pre-release build, using the existing signing identity. No GitHub publication was performed.