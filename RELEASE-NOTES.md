# v0.5.0 — Pre-release

- Added matching app settings with Light, Dark and Auto appearance.
- Added separate saved USB Ethernet and experimental Wi-Fi ENET profiles.
- Added Wi-Fi network/source binding, connection-loss checks and optional private IPv4 SSH override.
- Compatibility authorization now also matches the selected transport, Android network and SSH override.
- Added explicit wireless-interruption acknowledgement before Wi-Fi flash/fix-key.
- Preserved the four tabs, typed VIN authorization, backup acknowledgement and upstream rooting assets/workflow.

Wi-Fi requires an adapter forwarding raw diagnostic TCP 6801 and SSH TCP 22. Join its SSID manually and enter its diagnostic gateway. Successful diagnostic inspection does not guarantee post-flash SSH routing. Wired Ethernet is recommended for flashing.

Build, offline and emulator checks only. Not tested on a physical phone, Wi-Fi ENET adapter or real BMW head unit. Flashing can brick the module, erase coding/CAFD and delete Digital Key pairings.

Full credit for the original rooting work goes to [cauliflowermossgrape/idrive7-8-ssh-root](https://github.com/cauliflowermossgrape/idrive7-8-ssh-root). This is an Android adaptation of that work.

Publish with tag **v0.5.0** and GitHub’s **Set as a pre-release** option enabled.
