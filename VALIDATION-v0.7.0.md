# v0.7.0 pre-release validation

Android SDK35/JDK17 build and Android lint passed with zero errors. APK versionName0.7.0-pre-release, versionCode5, package dev.mgu.standalone. Installed over the previous version on the Android35 emulator. Existing theme/settings instrumentation passed in both system day and night modes. Visually reviewed rendered settings in light and dark: filled Done and outlined Open Android Wi-Fi settings match the main app button shape, colors, font and spacing.

APK signature verified against the previous signing identity; 16KB ZIP alignment checked. This release changes Settings action-button styling and version metadata. Network/backend/icon behavior is unchanged from v0.6.0; see prior validation for those checks. No new unit tests were added for this cosmetic change. No physical phone, adapter or real BMW head unit testing is claimed.
