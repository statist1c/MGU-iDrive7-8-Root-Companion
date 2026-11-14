# v0.6.0 pre-release validation

Build and lint passed, zero lint errors. Fourteen offline coordinator tests passed, including a Wi-Fi source 169.254.255.106, acceptance of adapter router 169.254.255.1 as a permitted local address (not evidence of diagnostic reachability), continued wired-source rejection of the reserved upper range, and public-address rejection.

Android35 x86_64 emulator installed the APK over the previous build. Embedded Python3.13.16, cryptography42.0.8, bundled SSH key, all three original contracts and Java callback passed without vehicle traffic. Light/Dark/Auto, Wi-Fi profile, Java address-policy regression, emulator network binding/release, settings UI, four tabs, form retention and blocked empty flash authorization passed. Native libraries and APK checked for 16KB alignment and signature identity retained.

Source upstream files remain unchanged. No physical phone, adapter, MGU SSH or BMW programming was tested. General adapter product claims and user network screenshots do not establish interoperability. Existing dependency caveats in VALIDATION.md apply.
