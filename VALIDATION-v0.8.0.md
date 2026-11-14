# v0.8.0 pre-release validation

Android SDK35/JDK17 build and lint passed with zero errors. APK versionName0.8.0-pre-release, versionCode6, package dev.mgu.standalone. Installed over the previous build in the Android35 x86_64 emulator. Existing theme/settings instrumentation passed; rendered adaptive launcher icon visually reviewed and adjusted to keep the full shield and MGU lettering visible. Launcher layers use percentage-based safe margins independent of drawable size. No additional vehicle or root operations ran.

APK signature checked against the existing identity; 16KB ZIP alignment verified. No networking, SSH or upstream workflow changes; prior validation and dependency caveats still apply. No new unit tests were needed for this asset/version change. No physical phone, adapter or real BMW head unit testing is claimed.
