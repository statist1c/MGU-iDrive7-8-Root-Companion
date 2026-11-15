# v0.9.0 validation — 15 November 2026

- Android SDK35/JDK17/Gradle8.9 assembleDebug and lint passed with zero errors. APK package dev.mgu.standalone, versionName0.9.0, versionCode7, minAPI26, ARM64+x86_64.
- 14 offline coordinator tests, 16 semantic-version cases and 17 repository-URL/channel/version policy cases passed.
- Installed over v0.8.0 on the Android35 x86_64 emulator. Same certificate SHA256 9bdd954bb11f0235a25b6a7a8179c04b5ffc8afa0a45ccbe24aa50ad82f1d32c retained.
- Updater instrumentation passed: actual GitHub release metadata GET, persisted daily job registration/cancellation, vehicle-work check/download guards, rejection of untracked update/provider access and all expected Settings controls. These checks did not download or install an update APK.
- Existing runtime checks passed: Python3.13.16, cryptography42.0.8, SSH key parsing, original contracts and Java callback. Existing tab/form/empty-flash-authorization regression passed.
- Light, Dark and Auto theme checks passed. Settings update-card previews visually reviewed in day/night modes.
- APK signature and ZIP16KB alignment verified; all 140 packaged ELF libraries have PT_LOAD alignment of at least16KB. Upstream snapshot remains byte-for-byte unchanged.

No future-release download-to-install flow, physical phone, adapter or real BMW head unit was tested. Android background scheduling is best effort, not a proven exact daily schedule. Signing-key rotation is intentionally unsupported. Existing dependency caveats in earlier validation remain applicable.
