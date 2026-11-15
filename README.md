# MGU iDrive7-8 Root Companion

<img src="docs/images/icon-v0.8.0.png" alt="MGU Root Companion shield app icon" width="128" />

An experimental, self-contained **Android version of [idrive7-8-ssh-root](https://github.com/cauliflowermossgrape/idrive7-8-ssh-root)** for BMW MGU iDrive 7/8 head units over USB Ethernet or experimental Wi-Fi ENET.

**v0.9.0 pre-release**

New: Settings → App updates, with daily/manual GitHub checks, optional previews and user-confirmed downloads/installation. See [APP-UPDATES.md](APP-UPDATES.md). The v0.9.0 APK and matching source are available from GitHub Releases.

## Full credits

**Full credit for the original rooting work, research, diagnostic/flashing workflow and supplied assets goes to [cauliflowermossgrape](https://github.com/cauliflowermossgrape) and the [original project](https://github.com/cauliflowermossgrape/idrive7-8-ssh-root).**

This project is an Android adaptation of the author's work, adding a phone interface and bundled runtime. It does not claim the original rooting method as its own. The pinned upstream source is preserved in [`vendor/upstream`](vendor/upstream), together with its original license.

## Features

- **Setup:** guided connection, VIN/compatibility checks, root setup and verification.
- **Monitor:** live status, logs, issue filtering and log export.
- **Backups:** private key/data export and read-only coding dumps.
- **SSH:** repair, persistence and access-management controls.
- **App updates:** GitHub checks and verified, user-confirmed app updates.
- **Settings:** Light, Dark or Auto appearance; separate USB/Wi-Fi connection profiles.
- **Wi-Fi ENET (experimental):** VXSCAN ENET Wi-Fi/USB profile, including the user-reported MHD ENET_4472 address range; selected-network routing and optional SSH override.
- Runs entirely on the phone: **no PC or Termux required at runtime**.

<img src="docs/images/light.png" alt="Light appearance — emulator preview" width="260" /> <img src="docs/images/dark.png" alt="Dark appearance — emulator preview" width="260" />

<img src="docs/images/updates-v0.9.0-light.png" alt="App update settings in Light mode — emulator preview" width="260" /> <img src="docs/images/updates-v0.9.0-dark.png" alt="App update settings in Dark mode — emulator preview" width="260" />

## Requirements

- Android 8.0 or newer; USB-C Ethernet + ENET cable, or a Wi-Fi ENET adapter that forwards raw diagnostic TCP 6801 and head-unit SSH TCP 22.
- A correctly assigned address and route for the selected connection.
- An authorized, parked vehicle and stable phone/vehicle power.

**Phone root is optional.** Some phones can configure Ethernet through Android settings; others may need the optional rooted network helper.

## Install

1. Download the APK from this repository's **Releases** page and install it.
2. Open the gear icon to choose Light, Dark or Auto and USB Ethernet or Wi-Fi ENET. Join adapter Wi-Fi in Android settings, enter its diagnostic gateway on Connect, then select the actual head-unit variant.
3. Run **Check compatibility & read VIN** first and verify the VIN physically.
4. Read the warnings and verify a restorable coding backup before considering any flash.

See [adapter setup and identity limitations](docs/WIFI-ADAPTER.md). Standard USB-C Ethernet support remains available. The app uses the selected shield/chip icon with MGU lettering and a BMW roundel. This independent project is not affiliated with or endorsed by BMW; third-party marks are not covered by the project source license.


## Wi-Fi ENET limitations

Wi-Fi support is experimental and adapter-dependent. Join the adapter SSID manually and stay connected if Android reports no internet. The app binds to the selected Wi-Fi network and source address; it does not select cellular or VPN routes. Private IPv4 and usable IPv4 link-local endpoints are allowed. Configure an optional SSH IPv4 override in Settings if required by the adapter. Bluetooth, proprietary app-only protocols, arbitrary port remapping and automatic SSID discovery are not supported.

**Prefer wired Ethernet for flashing.** A Wi-Fi dropout can interrupt programming and leave the head unit unusable. A successful VIN/compatibility check verifies diagnostics, not that a closed SSH port will become reachable after flashing. No physical Wi-Fi adapter has been validated.

## Important warnings

**Flashing can permanently damage the head unit, erase coding/CAFD and delete Digital Key pairings.** A diagnostic dump is not a guaranteed coding-tool backup. Never repeat flashing merely to fix reset settings.

The upstream default root SSH key is shared. Protect exported keys and keep the diagnostic network isolated.

**Experimental: build and emulator checks passed, but this app has not been tested on a physical Android phone or real BMW head unit.** Compatibility and safe operation are not guaranteed. See [`VALIDATION.md`](VALIDATION.md).

## Build and documentation

Build requirements: JDK 17, Python 3.13 and Android SDK 35. From `android/` run `./gradlew assembleDebug lint` (`gradlew.bat` on Windows).

See the [detailed setup/build guide](docs/BUILD-AND-USAGE.md), [design notes](DESIGN.md), [license](LICENSE) and [third-party notices](THIRD-PARTY.md). This project is not affiliated with BMW.

## License

The maintained Android companion is **GPL-3.0-only**; see [LICENSE](LICENSE) and
[licensing/source policy](LICENSING.md). The original rooting project in
`vendor/upstream` retains its **Unlicense**, with full upstream credit preserved.
Third-party notices and license texts are in [THIRD-PARTY.md](THIRD-PARTY.md) and
[licenses/](licenses/). BMW branding is excluded from the software-license grant.

Each APK release must include its matching source ZIP, license notices and checksums.
The original v0.9.0 APK/source are preserved; its licensing supplement adds notices
without replacing the binary. Future releases use `scripts/prepare_release.py` to
verify source fingerprints before packaging. No private signing keys are included.

### v0.9.0 release files

- [Published APK](https://github.com/statist1c/MGU-iDrive7-8-Root-Companion/releases/download/v0.9.0/MGU-iDrive7-8-Root-Companion-v0.9.0-pre-release.apk)
- [Matching original source ZIP](https://github.com/statist1c/MGU-iDrive7-8-Root-Companion/releases/download/v0.9.0/MGU-iDrive7-8-Root-Companion-v0.9.0-source.zip)
- [GPLv3 and dependency license supplement](release-notices/MGU-iDrive7-8-Root-Companion-v0.9.0-licenses.zip)
- [License supplement SHA256](release-notices/MGU-iDrive7-8-Root-Companion-v0.9.0-licenses-SHA256.txt)

The license supplement is hosted in this repository; the published APK and its
original matching source ZIP have not been replaced. Read [LICENSING.md](LICENSING.md)
for the current distribution terms and future release-source procedure.
