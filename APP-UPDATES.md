# App updates — v0.9.0

Status: v0.9.0 APK built and checked in the Android35 emulator on 15 November 2026. Not yet published to GitHub. Published v0.8.0 artifacts remain unchanged.

## Behavior

Settings → App updates matches MGU Store Installer: daily checks enabled by default, optional preview releases, Check now, review notes, explicit Download, and explicit Install. The buttons follow this app’s Light/Dark/Auto styling.

Checks read public releases from https://github.com/statist1c/MGU-iDrive7-8-Root-Companion . No GitHub token is needed. Stable releases are the default; enable Include preview releases to see this project’s pre-releases. Semantic versions determine precedence. Drafts, malformed/older tags, multiple APK assets and releases without verification metadata are skipped. Only the first 100 releases are examined.

Android JobScheduler makes best-effort daily checks, and app startup checks when the last successful check is over a day old. Android may defer work while offline, in Doze, force-stopped or battery restricted. Notifications open app update Settings. Denying notification permission leaves manual checks available. There are no automatic downloads or silent installs.

Checks defer during backend vehicle work or the wired network helper. The updater never releases or changes the vehicle’s process network binding. Starting downloads/installation while vehicle work is active is refused; the update provider also refuses APK reads during active vehicle work. Internet must be available; a vehicle-only Wi-Fi connection is not an internet connection.

DownloadManager saves a user-requested update to app-private external storage. Download completion schedules verification; Settings also recovers tracked download state after process death. Before installation it checks size, SHA-256, package ID dev.mgu.standalone, higher versionCode, SemVer matching the release tag, and exactly matching signing certificates. Key rotation is not supported in this version. Installation always needs Android confirmation. If required, Install opens Android’s Allow from this source page; grant it and return to tap Install again.

## Components and permissions

New REQUEST_INSTALL_PACKAGES and RECEIVE_BOOT_COMPLETED permissions support sideload installation and persisted daily jobs. Existing INTERNET, ACCESS_NETWORK_STATE and POST_NOTIFICATIONS are reused. Two services require Android’s BIND_JOB_SERVICE permission. A DownloadManager receiver only handles the tracked request; a non-exported read-only provider exposes the verified update APK by a temporary URI grant. No general storage, silent-install, root or exact-alarm permission is added.

## Preparing future releases

- Publish a GitHub Release with exactly one universal APK, containing ARM64 and x86_64. Actions artifacts alone are not supported.
- Use matching SemVer tag and APK versionName, e.g. tag v0.9.0 and versionName 0.9.0; increment versionCode (prepared v0.9.0 is code7). GitHub’s pre-release flag controls the preview channel independently of the tag suffix. This deliberately avoids older builds’ custom versionName suffix differing from their release tag.
- Provide GitHub’s sha256 asset digest or a checksum asset named SHA256SUMS.txt or MGU-iDrive7-8-Root-Companion-<tag>-SHA256SUMS.txt. It must contain the exact APK asset filename and its actual final signed SHA-256.
- Sign all release APKs with the same durable key. Do not upload private keystores. Independently generated Actions debug APKs may use a different certificate and cannot update a local release APK. Mismatches are refused without suggesting uninstall/data loss.
- Existing v0.8.0 and older apps have no updater. The first updater-enabled release must be installed manually.

## Validation so far

Android build and lint passed. Fourteen coordinator tests, 16 standalone semantic-version cases and 17 repository URL/release-channel/version-policy cases passed. Android35 emulator: live GitHub metadata check, daily job enable/disable, vehicle-work guards, rejection of untracked/unverified APK access, updater Settings controls, Light/Dark/Auto themes, four tabs, form retention, blocked empty flash authorization and embedded Python/crypto runtime passed. Installed in place over the prior release with the same signing certificate. All 140 native ELF libraries and APK ZIP alignment passed 16KB checks. Future-release APK download/installation, background notification delivery on physical phones and real vehicle operation remain unverified.

The implementation is adapted from the user’s MGU Store Installer updater; see THIRD-PARTY.md and licenses/MGU-Store-Installer-GPL-3.0.txt.
