Current release: **v0.8.0 pre-release**, versionCode6, selected adaptive shield icon. Use GitHub tag v0.8.0 and mark as a pre-release. Earlier release notes below are historical.

Current release: **v0.7.0 pre-release**, versionCode5. Settings action buttons now match the main app in both themes. Use tag v0.7.0 and mark as a pre-release. Older version instructions below are historical.

Current release: **v0.6.0 pre-release**, versionCode4. See [adapter guide](WIFI-ADAPTER.md). Use GitHub tag v0.6.0 and mark the release as a pre-release. The earlier release documentation below remains historical.

# v0.5.0 pre-release additions

Use the gear at the top of the app for Light, Dark and Auto (phone system theme). Preferences persist across restarts. The four main tabs remain available.

Select USB Ethernet or Wi-Fi ENET before starting an operation. Each profile saves its phone source and diagnostic gateway independently. Wi-Fi gateway starts empty: obtain it from the adapter maker. Join its SSID in Android Wi-Fi settings; no internet connection is required. Leave phone source blank to detect an assigned address. Optional Wi-Fi SSH IPv4 override is in App settings. Only adapters forwarding raw TCP 6801 diagnostics and TCP 22 SSH can work. No adapter-specific certification is claimed. The root IP helper remains wired-only; normal Wi-Fi needs no phone root.

Connection profile changes are disabled during a backend job. A compatibility check is tied to the VIN, variant, addresses, interface, Android network handle and transport; changing these requires inspection again. Wi-Fi flash/fix-key also requires explicit wireless-interruption acknowledgement. Prefer wired flashing. Theme changes do not stop the foreground backend.

The release APK retains the previous local signing identity, package ID and increments versionCode to 3. Install over v0.4.0 to retain app data; export private keys/backups first. A build signed with another key cannot update it. Never distribute a signing keystore.

GitHub release: tag `v0.5.0`, title `v0.5.0 — Pre-release`, check **Set as a pre-release**, and upload the APK plus matching source/checksums. This package does not publish a release automatically.

---

# MGU iDrive7-8 Root Companion — v0.4.0

This is a separate **self-contained Android app project**. The APK embeds Python, the original MGU diagnostic/programming backend, cryptography and a Java SSH client. At runtime it uses no PC, Termux, local bridge server, token pairing, shell package manager or second app. Optional rooted Ethernet configuration calls the phone's already-existing `su`; it does not install root or bypass Android security.

**Delivery status: the renamed, tabbed debug APK has been built and signature-verified.** Install `MGU-iDrive7-8-Root-Companion-v0.4.0.apk`; no Termux or PC is required at runtime. The matching source is `MGU-iDrive7-8-Root-Companion-source.zip`. The app keeps the same ID and signing key as the previous v0.3 standalone APK, so it can be installed over that version without uninstalling. The earlier Termux companion is a separate app and its data is not imported automatically.

## New tabbed design

The light card layout and navigation are inspired by [the referenced Android Auto installer](https://github.com/mhmm-ok/BMW-SUPRA-IDrive5-6-Android-Auto). The UI is an original native implementation for the existing MGU backend.

- **Setup:** Connect → Vehicle → Root → Verify, with connection state, VIN/variant fields, compatibility checks and prominent flash warnings.
- **Monitor:** current job, live output, issue filtering, follow mode and full-log export. Clearing the display does not delete saved logs.
- **Backups:** private SSH identity/coding-dump/log counts, guarded export and read-only coding dumps.
- **SSH:** repair, persistence, disable/stop, host-key trust, optional rooted Ethernet setup and advanced refused-key repair.

Navigation stays available during a job, while fields and operation controls are locked. Switching tabs preserves form values. The live VIN/BTLD gate, fresh matching check, typed authorization and backup acknowledgement remain in place. See `DESIGN.md` for reference/provenance and `VALIDATION.md` for checks actually performed.

**Not tested on a physical Android phone, USB Ethernet adapter or real BMW head unit.** The full build and lint succeeded, ten offline coordinator tests passed, and an Android 15/API 35 x86_64 emulator test verified embedded Python/cryptography, Java callbacks, SSH key parsing and all three bundled flash-asset sets without sending vehicle traffic. The app also handled an absent Ethernet connection cleanly. Actual MGU SSH negotiation and vehicle operations remain unverified. See `VALIDATION.md`.

## Install and start

1. Download `MGU-iDrive7-8-Root-Companion-v0.4.0.apk` and open it. For a v0.3 standalone installation, choose Update and keep existing app data; do not uninstall first. Android may ask you to allow installation from your browser/file manager. This is an experimental debug build, not a Play Store release.
2. Open **MGU iDrive7-8 Root Companion** and allow notifications. In Android battery settings, allow unrestricted battery use for this app.
3. Under **Setup → Connect**, attach USB-C Ethernet and ENET, then tap **Detect Ethernet address**. If none is assigned, use Android settings or the confirmed helper under **SSH → Phone Ethernet** on an already-rooted phone. An example phone IP is `169.254.250.10/16`; use an unused address appropriate to your connection, never the gateway's own address.
4. Verify the gateway (default `169.254.254.36`), continue to **Vehicle**, select the actual hardware variant, and run only **Check compatibility & read VIN** first. Confirm the VIN against the physical vehicle. Never use flashing as a connection test.
5. Review the risk/backup requirements below before considering any vehicle change. Export needed private keys/logs before uninstalling or clearing app data.

## Workflow

1. Install one APK. Python, cryptography and SSH are bundled at build time; the app does not download executable components on the phone.
2. Connect a supported USB-C Ethernet adapter and ENET cable to your authorized parked vehicle.
3. Tap **Detect assigned Ethernet address**. If Android already assigned a usable link-local address, the app selects that wired network. Otherwise open Android connection settings. On an already-rooted phone, an optional in-app dialog previews and applies wired addressing plus a dedicated source-specific route, after your explicit confirmation and root manager approval. Root is not needed by the diagnostic backend itself.
4. Select the actual ID7/ID8 hardware variant and run the **read-only compatibility check**. It verifies the gateway route, live VIN and the upstream supported BTLD gate. A port being open does not establish root access or safe flashing.
5. Vehicle changes require a matching check less than five minutes old and a typed VIN authorization. A mutation consumes that check. Flash/fix-key additionally require acknowledgement of a separately verified restorable coding backup and stable power.
6. Logs, key bundles and diagnostic dumps remain in private app storage. Export them through Android's document picker before clearing app data or uninstalling. The ZIP export includes private keys and VIN; choose a trusted destination.

## What one APK cannot silently do

Ordinary Android apps cannot universally assign static Ethernet addresses or add missing USB network drivers. Some phones expose Ethernet settings; some need root for the route. The Redmagic 11 Pro and rooted OnePlus 8T are candidates, not confirmed compatible devices. No PC is required for runtime operation after an APK exists, but an Android build environment is required to produce that APK.

The app does not create a guaranteed coding-tool restore, recode the module, re-pair Digital Keys or provide a firmware rollback. A coding dump is a reference, not a substitute for a verified restorable backup. It does not remove the need for a restore plan or all other vehicle service tools.

## Material risks

Flash and fix-key can permanently damage the head unit, wipe coding/CAFD and erase Digital Key pairings. Do not repeat flashing to fix reset settings. Use only on an authorized parked vehicle with suitable stable power. Keep the adapter/cable stable and verify charging does not reset Ethernet. Android foreground services and wake locks reduce interruption risk but cannot prevent force-stop, OS termination, overheating, power loss or cable failure.

Persistent SSH increases exposure and the original default root login key is shared. Keep the wired diagnostic network isolated. SSH host keys use trust on first use: the first observed key is logged/pinned per VIN and IP, subsequent unexpected changes are rejected, and an explicitly authorized flash/fix-key may replace a pin once. A separate local-only review/reset dialog can remove a remembered pin after you investigate a changed fingerprint; do not reflash just to repair host-key trust. Resetting permits first-use trust on the next SSH connection and does not independently authenticate a potentially hostile wired endpoint.

The original desktop OpenSSH dependency is replaced by a bundled maintained JSch client here. This is a new, unverified transport for the MGU. Default SSH algorithms stay modern; an advanced opt-in allows RSA/SHA1 for a selected operation, without automatic downgrade. A successful build cannot establish that the head unit accepts the handshake. If SSH/persistence fails after flashing, debug mode may remain enabled under upstream behavior; follow the logs, not an assumed final state. Never automatically retry flashing.

## Build prerequisites and command

For published updates, keep using the same private signing key. The included GitHub Actions workflow produces evaluation/debug artifacts; its automatically generated debug key is not the signing key of the attached v0.4.0 APK and can change between clean runners. Use the supplied APK for this release. Configure a persistent private release key before publishing future CI-built updates, and never commit the key or its passwords to the repository.

- JDK 17, Android SDK platform 35, Gradle 8.9 (wrapper included).
- Host Python **3.13** to build the matching embedded Python distribution.
- Internet access to Google Maven, Maven Central, the Gradle repository and Chaquopy's Python wheel index.

From `android/`:

```text
./gradlew --no-daemon assembleDebug lint
```

On Windows use `gradlew.bat`. Set `ANDROID_HOME`/`sdk.dir` and ensure Python 3.13 is discoverable; alternatively pass `-PstandaloneBuildPython=<absolute Python 3.13 executable path>`. Output: `app/build/outputs/apk/debug/app-debug.apk`. The build and lint command completed successfully for the delivered APK.

The included GitHub Actions workflow builds from the enclosing `MguStandalone` repository root. It sets up the build host's JDK/Python and uploads a debug APK. It has not been run or published by this delivery. Its Python setup is for the build machine, not an external app required on the phone.

## Dependencies and review before distribution

- Chaquopy 17.2.0 embeds Python 3.13 for arm64-v8a and x86_64.
- JSch 2.28.8 provides in-app SSH; Bouncy Castle 1.87 supplies additional algorithms.
- `cryptography==42.0.8` is pinned to the available Chaquopy Android wheel for Python 3.13. This is an older package, not a claim of current cryptography security currency. Review its advisories and native dependencies, and update/rebuild the wheel as necessary before production use. All 140 ELF libraries in the APK and nested archives passed a 16-KB segment-alignment check, and APK ZIP alignment passed; this is not a physical 16-KB-page device test. A security-reviewed dependency set and target-device tests remain release prerequisites.
- The original upstream source/assets are pinned to commit `6d4928e6dca9812e7882318afdd7cba9840c2d62` and bundled unchanged. A Gradle ZIP task places the CLI, diagnostic library, required assets, script and original LICENSE inside the APK. Generated keys/logs are never packaged.

Retain third-party license notices when distributing an APK. Original upstream Unlicense is under `vendor/upstream/LICENSE`; Python/Chaquopy, cryptography, JSch and Bouncy Castle have their own terms. Build/runtime dependencies are not included in this source archive.

## Source layout

- `android/app/src/main/python/standalone_backend.py`: allowlist, fresh-match authorization, bounded source-bound transport, live VIN/BTLD gate and embedded SSH adapter boundary.
- `android/app/src/main/java/dev/mgu/standalone/`: UI, connected-device foreground service, Ethernet routing, optional explicit root setup and Java SSH transport.
- `vendor/upstream/`: pinned original source/assets. Its own real-car reports are not validation of this app.
- `tests/`: offline tests with mocked networking/backend/Java SSH. Run `python -m unittest discover -s tests -v` from the project root.

App ID `dev.mgu.standalone` remains unchanged from v0.3 and is separate from the older `dev.mgu.companion` Termux front end. No cross-app key/configuration migration is implemented. Export keys before removing old apps/Termux. If a prior helper has already configured the reserved network rule/table, this helper refuses to overwrite it; remove/review that setup first.

Upstream: https://github.com/cauliflowermossgrape/idrive7-8-ssh-root . This project is not affiliated with BMW.

