# v0.5.0 pre-release validation

- JDK17 / Gradle8.9 / Android SDK35 build and Android lint passed with zero errors.
- 13 offline coordinator tests passed: original authorization, live VIN/BTLD gates, source-bound transport and SSH bridge; new Wi-Fi address policy, changed-network/transport/SSH gate rejection, wireless acknowledgement, lost-network guard and SSH override propagation.
- Android35 x86_64 emulator: embedded Python3.13.16, cryptography42.0.8, bundled SSH key, three upstream contracts and Java callback passed without vehicle traffic.
- Emulator UI: Light/Dark/Auto palettes, saved Wi-Fi profile, selected emulator Wi-Fi network binding/release, settings controls and Java private IPv4 policy passed. Auto was checked under both system day and night modes. Four-tab navigation, form retention and blocked empty flash authorization passed.
- APK signature, upgrade identity and 16KB native alignment checked. APK versionName0.5.0-pre-release/versionCode3, minAndroid26, ARM64+x86_64.

No real phone, USB/Wi-Fi ENET adapter, head-unit SSH or vehicle programming was tested. Emulator Wi-Fi is a virtual network, not evidence of adapter interoperability. No root/flash action was authorized during validation. Existing dependency-review caveats in VALIDATION.md still apply.
