# v0.9.0 — Pre-release

- Added GitHub app updater matching MGU Store Installer: automatic daily checks, manual Check now, optional previews, update notifications and release-note review.
- Downloads and Android installation require user action. APK size/hash, app package, version and signing certificate must match.
- Added matching update controls to Settings and defer checks/download/install during vehicle work.
- Retained both USB-C Ethernet and experimental Wi-Fi ENET, existing themes, icon and vehicle authorization safeguards.
- VersionName0.9.0 / versionCode7; same package and signing identity.

APK build, lint, offline policy tests and emulator checks passed. Live GitHub checks and daily-job registration were verified; a complete future-release APK download/install flow remains untested. GitHub publication is pending. v0.8.0 users must manually install the first updater-enabled APK.

No physical phone, adapter or real BMW head-unit testing is claimed. Full credits for the original rooting work remain with https://github.com/cauliflowermossgrape/idrive7-8-ssh-root . This is an independent Android adaptation.
