Current version: [v0.8.0 validation](VALIDATION-v0.8.0.md).

Current version: [v0.7.0 validation](VALIDATION-v0.7.0.md).

Current version: [v0.6.0 validation](VALIDATION-v0.6.0.md).

For the current pre-release, see [v0.5.0 validation](VALIDATION-v0.5.0.md). The following records earlier version checks.

# MGU iDrive7-8 Root Companion v0.4.0 validation

Checked on 14 November 2026. This release updates the app name, light theme, card layouts, navigation, setup steps and presentation of existing controls. Diagnostic/SSH/rooting backend code is unchanged from v0.3.

## Completed

- Full real-dependency Gradle `assembleDebug`, `assembleDebugAndroidTest` and `lint` passed. Lint: zero errors, 32 warnings (English-only display text and existing platform/wake-lock advice). JDK 17, Gradle 8.9, AGP 8.7.3, Chaquopy 17.2.0, SDK 35 and host Python 3.13.9 were used.
- APK signature verified. App ID remains `dev.mgu.standalone`; version code is 2, version name `0.4.0-experimental`, min API 26, target API 35, ABIs arm64-v8a and x86_64. Signing certificate matches the previously delivered v0.3 standalone APK; emulator installation using update mode succeeded.
- Android 15/API 35 x86_64 emulator UI checks passed at the normal Pixel 6 configuration and at a compact 360 × 640 dp configuration. All four tabs displayed correctly; screenshots were reviewed. The full name wraps on the compact screen.
- UI regression checks verified that source IP, VIN and variant survive tab/step changes, the destructive-flash warning and typed VIN prompt remain present, and pressing Start with empty authorization does not start a job or create an in-progress marker. No vehicle traffic occurred.
- The real embedded runtime test passed again on this APK: Python 3.13.16, cryptography 42.0.8, native RSA key/certificate work, Java callbacks, JSch/Bouncy Castle identity loading, backend asset extraction, and parsing of all three flash contracts (174 / 173 / 173 frames). No flash engine or vehicle socket was invoked.
- `zipalign -c -P 16 4` passed. Native ELF alignment is checked during final packaging, including nested Python archives. Dependency/native payloads are unchanged from v0.3, which passed the same 140-library check.
- Source packaging checks all 39 pinned upstream files byte-for-byte and verifies archive integrity. APK/source SHA-256 hashes are provided.

The ten coordinator unit tests passed for the unchanged request coordinator in v0.3; those pure-Python guards were not modified for this UI release.

## Limits

No physical Redmagic, OnePlus, USB Ethernet adapter, ARM64 device or BMW head unit has been tested. No real MGU SSH login, flashing, enrollment, persistence, coding restore, Digital Key behavior, rooted-phone network change, power-loss recovery or physical 16-KB-page device behavior is claimed as tested.

This is a debug-signed experimental build. Existing production security/dependency review requirements, including the older Android cryptography wheel pin, still apply. Begin on the actual phone/adapter with the read-only vehicle check, not a flash.
