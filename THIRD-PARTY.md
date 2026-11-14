# Dependencies and source references

Visual reference for the v0.4 tab/card design: https://github.com/mhmm-ok/BMW-SUPRA-IDrive5-6-Android-Auto . Original native views/icons were implemented for this app; no reference app code, assets or installer payloads are distributed. See `DESIGN.md`.

This archive contains the application source, Gradle wrapper and pinned upstream source/assets. It does not contain downloaded Chaquopy, Python, cryptography, JSch or Bouncy Castle build/runtime artifacts. The Android build resolves and packages those dependencies; review their terms and retain license notices before distributing an APK.

- Upstream MGU backend: https://github.com/cauliflowermossgrape/idrive7-8-ssh-root — original Unlicense retained under `vendor/upstream/LICENSE`.
- Chaquopy: https://chaquo.com/chaquopy/doc/current/android.html — embedded Android Python build/runtime; https://github.com/chaquo/chaquopy .
- Python: https://docs.python.org/3/license.html .
- cryptography: https://github.com/pyca/cryptography — Apache/BSD terms and native OpenSSL dependencies need their notices retained. Inspected Android wheel index: https://chaquo.com/pypi-13.1/cryptography/ .
- JSch fork: https://github.com/mwiede/jsch — BSD-family notices, plus its bundled third-party notices. Selected release: https://github.com/mwiede/jsch/releases/tag/jsch-2.28.8 .
- Bouncy Castle: https://www.bouncycastle.org/licensing/ — selected `bcprov-jdk18on` 1.87, listed at https://www.bouncycastle.org/download/bouncy-castle-java/ .
- Gradle wrapper: original Apache 2.0 notices retained in wrapper scripts; https://github.com/gradle/gradle .
- Android connected-device foreground service requirements: https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device .

Dependency versions are source configuration choices, not a successful build or security certification. See `README.md` and `VALIDATION.md` for unresolved integration/security checks and the blocked APK build.
