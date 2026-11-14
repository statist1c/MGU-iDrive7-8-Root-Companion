# v0.6.0 — Pre-release

- Added named VXSCAN ENET Wi-Fi/USB connection guidance; user-reported adapter SSID MHD ENET_4472. Hardware identity and SSH forwarding remain unverified.
- Fixed Wi-Fi validation to accept adapter-assigned 169.254.255.x source/router addresses, including the range shown in user photos. Wired automatic address rules remain unchanged.
- Preserved standard USB-C Ethernet, Light/Dark/Auto themes, separate saved profiles, selected-network binding and all existing authorization safeguards.
- Added BMW MGU lettering inside the existing terminal icon.
- Package identity/signing retained; versionCode4, versionName0.6.0-pre-release.

The photographed device's client address is not a phone preset. The router 169.254.255.1 is not confirmed as a diagnostic or SSH endpoint. See docs/WIFI-ADAPTER.md. Wi-Fi requires raw diagnostic TCP6801 and head-unit SSH TCP22 routing. Prefer wired Ethernet for flashing.

Build, offline and emulator checks only. Not tested on a physical phone, Wi-Fi adapter or BMW head unit. Flashing can brick the module, erase coding/CAFD and delete Digital Key pairings.

Full credit for the original rooting work goes to https://github.com/cauliflowermossgrape/idrive7-8-ssh-root . This is an Android adaptation of that work. No BMW affiliation is claimed.

Publish with GitHub tag v0.6.0 and Set as a pre-release enabled.
