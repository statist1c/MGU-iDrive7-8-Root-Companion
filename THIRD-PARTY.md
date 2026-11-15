# Third-party notices and source references

The maintained Android companion is GPL-3.0-only; see LICENSE and LICENSING.md.
The original rooting backend remains unchanged under its own Unlicense. This
notice bundle does not relicense third-party components or grant trademark rights.
Full credit for the rooting workflow, research, diagnostic/flashing code and
supplied assets goes to cauliflowermossgrape/idrive7-8-ssh-root.

## Runtime components identified in the v0.9.0 APK

| Component | Version / scope | Retained notice location |
|---|---|---|
| Original MGU backend | commit 6d4928e6dca9812e7882318afdd7cba9840c2d62; unchanged | vendor/upstream/LICENSE; licenses/UPSTREAM-UNLICENSE.txt |
| Chaquopy | 17.2.0; MIT | licenses/chaquopy-17.2.0/LICENSE.txt |
| CPython for Android | 3.13.16, Chaquopy target 3.13.16-0; PSF and incorporated licenses | licenses/python-3.13.16/LICENSE; THIRD-PARTY-LICENSES.rst in that directory |
| OpenSSL | 3.5.9, identified in libcrypto_python.so; Apache-2.0 | licenses/openssl-3.5.9/LICENSE.txt |
| SQLite | public-domain code in Python runtime | licenses/sqlite/COPYRIGHT.html |
| cryptography | 42.0.8; Apache-2.0 OR BSD-3-Clause | licenses/android-python/cryptography-42.0.8.dist-info/ |
| Rust crate notices | registry crates from cryptography42.0.8 Cargo.lock, including build-only crates as a conservative superset | licenses/cryptography-rust/ |
| cffi | 1.17.1; MIT | licenses/android-python/cffi-1.17.1.dist-info/LICENSE |
| libffi | Chaquopy package3.3; MIT/build-tool notices | licenses/android-python/chaquopy_libffi-3.3.dist-info/ |
| pycparser | 3.12; BSD | licenses/android-python/pycparser-3.12.dist-info/licenses/LICENSE |
| setuptools/pkg_resources | 68.2.2; MIT, plus vendor terms below | licenses/android-python/setuptools-68.2.2.dist-info/LICENSE |
| pkg_resources vendored modules | importlib_resources5.10.2, jaraco.context4.3.0, jaraco.functools3.6.0, jaraco.text3.7.0, more_itertools9.1.0, packaging23.1, platformdirs2.6.2, typing_extensions4.4.0, zipp3.7.0 | licenses/setuptools-68.2.2-vendored/ |
| pyelftools | 0.26; public-domain/BSD material per included notice | licenses/android-python/java/_vendor/pyelftools-0.26.dist-info/LICENSE.txt |
| JSch fork | 2.28.8; BSD-style, JZlib and jBCrypt notices | licenses/jsch-2.28.8/ |
| Bouncy Castle bcprov-jdk18on | 1.87; permissive Bouncy Castle license | licenses/bcprov-jdk18on-1.87/ |
| MGU Store Installer updater | adapted from the user’s GPLv3 project | licenses/MGU-Store-Installer-GPL-3.0.txt |

License texts were taken from the actual APK’s nested Python archives, the exact
Maven JARs used for the build, or version-pinned upstream sources. See
licenses/NOTICE-SOURCES.json, licenses/cryptography-rust/CARGO-NOTICE-SOURCES.json
and licenses/setuptools-68.2.2-vendored/NOTICE-SOURCES.json for provenance. The
setuptools upstream vendor metadata restores notices omitted from its wheel.
The CPython history/license document includes notices for incorporated standard
library code; its list also covers components that may be absent from Android.
Rust notices are a superset, not an assertion that all48 crates are linked.

The Python runtime is supplied by Chaquopy’s Android build, including its Android
adaptations. This project does not apply additional changes to Python, OpenSSL,
SQLite, JSch or Bouncy Castle. It embeds their binary distributions.

## Build/source components

The Gradle8.9 wrapper retains its existing Apache-2.0 notices. The distribution
license and notice are preserved in licenses/gradle-8.9/. JDK17, Android SDK35 and
AGP8.7.3 are build tools, not software copied into the APK by this notice bundle;
their own distributions retain their terms. The Python transitive versions above are pinned in app/build.gradle to prevent
unreviewed dependency drift. Runtime notice inventory must be
reviewed when dependency versions change. It is not a security certification or
an independent legal audit.

## Source locations

- Backend: https://github.com/cauliflowermossgrape/idrive7-8-ssh-root/tree/6d4928e6dca9812e7882318afdd7cba9840c2d62
- Chaquopy: https://github.com/chaquo/chaquopy/tree/17.2.0 ; Android build scripts under target/.
- CPython: https://github.com/python/cpython/tree/v3.13.16
- OpenSSL: https://github.com/openssl/openssl/tree/openssl-3.5.9
- SQLite: https://sqlite.org/src ; https://sqlite.org/copyright.html
- cryptography: https://pypi.org/project/cryptography/42.0.8/ ; source archive URL and hash in licenses/SOURCE-ARCHIVES.json.
- cffi: https://pypi.org/project/cffi/1.17.1/ ; pycparser: https://pypi.org/project/pycparser/3.12/
- Chaquopy packages/build recipes: https://github.com/chaquo/chaquopy/tree/17.2.0/server/pypi
- setuptools: https://github.com/pypa/setuptools/tree/v68.2.2 ; pyelftools: https://github.com/eliben/pyelftools/tree/v0.26
- JSch: https://github.com/mwiede/jsch/tree/jsch-2.28.8
- Bouncy Castle: https://github.com/bcgit/bc-java/tree/r1rv87
- Gradle: https://github.com/gradle/gradle/tree/v8.9.0
- Updater adaptation: https://github.com/statist1c/MGU-Store-Installer

## Design and trademarks

The original tab/card design was inspired by mhmm-ok/BMW-SUPRA-IDrive5-6-Android-Auto.
No code, images or installer payloads from that reference are distributed.
The selected shield artwork was generated for this project. Its BMW name/roundel
are third-party marks excluded from the software-license grant. No BMW endorsement
or permission is claimed. The logo remains unchanged pending the user’s decision.
