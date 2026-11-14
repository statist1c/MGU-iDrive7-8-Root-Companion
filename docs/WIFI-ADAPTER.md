# VXSCAN ENET Wi-Fi/USB profile (experimental)

Product-page identification: VXSCAN ENET WIFI/USB DoIP adapter, Automan item OVXT006. Source: https://automan.co/productdetail/3586.html
User photographs show SSID MHD ENET_4472 and a connected Android device with IP 169.254.255.106, router/DNS 169.254.255.1, subnet 255.255.0.0. Exact hardware/firmware identity and SSH forwarding remain unverified. An SSID is not proof of make or model.

## Connection

1. Choose Wi-Fi ENET in App settings. Standard USB-C Ethernet remains a separate supported profile.
2. Join the adapter SSID using Android Wi-Fi settings; stay connected without internet.
3. Detect the actual phone source address. Do not copy the photographed device's IP: each client needs its own address.
4. Enter the vehicle diagnostic gateway. The Wi-Fi router 169.254.255.1 is not confirmed as the diagnostic gateway. Obtain the diagnostic endpoint from adapter documentation or a known working setup; do not assume the router is it.
5. Leave SSH override blank unless the actual reachable head-unit SSH address is known. Start with read-only compatibility/VIN checks.

Wi-Fi accepts adapter-assigned IPv4 link-local addresses including 169.254.255.x, plus RFC1918 addresses. Source and destination must differ. Public, loopback and multicast addresses remain blocked. USB Ethernet address restrictions and the optional wired root helper remain unchanged.

The adapter must carry raw BMW ENET/HSFZ TCP6801 and head-unit SSH TCP22. General DoIP/BimmerCode support does not prove SSH forwarding. A successful diagnostic check cannot certify post-flash SSH access. No physical adapter, phone or BMW head unit has been validated by this project. Prefer wired Ethernet for programming.

Wi-Fi credentials, MAC addresses and user photographs are not bundled in the source or APK.
