# Licensing and release-source policy

Effective15 November2026, the maintained Android companion and its combined
application are distributed under GNU GPL version3 only (GPL-3.0-only), without
warranty. LICENSE contains the full unmodified GPLv3 text. NOTICE identifies the
project and preserves attribution. This is a user-authorized licensing update.

## Scope and preserved permissions

- vendor/upstream is the original author’s unchanged Unlicense snapshot. Its
  original LICENSE and all notices remain intact; no ownership or authorship of
  the rooting method is claimed.
- Third-party libraries retain their own license texts and copyright notices in
  licenses/ and THIRD-PARTY.md. Applying GPLv3 to the combined application does
  not remove their permissions or change their original licenses.
- The updater was adapted from MGU Store Installer’s GPLv3 source. The top-level
  project license is now consistent with that integration.
- Previously distributed source copies retain the permissions already granted.
  Earlier Unlicense grants are not revoked. The old root text is archived as
  licenses/LEGACY-ROOT-UNLICENSE.txt for provenance.
- BMW name/roundel and other third-party marks are outside the software-license
  grant. This update makes no trademark-permission claim and leaves the icon as-is.

## Existing v0.9.0 release

The original APK and its matching source ZIP are preserved, not silently rebuilt
or replaced. Their SHA256 values are:

    e4c5f53a15dd0b83572441c6290048551a83402918888760c3f73342a132291f  MGU-iDrive7-8-Root-Companion-v0.9.0-pre-release.apk
    b1ec022b381b2e1b77b1d8b337f406da6b4b13a25eae926738daaf09acf8aa21  MGU-iDrive7-8-Root-Companion-v0.9.0-source.zip

Both files are at:
https://github.com/statist1c/MGU-iDrive7-8-Root-Companion/releases/tag/v0.9.0

The license-supplement ZIP is available alongside those download links in the README:
https://github.com/statist1c/MGU-iDrive7-8-Root-Companion/raw/refs/heads/main/release-notices/MGU-iDrive7-8-Root-Companion-v0.9.0-licenses.zip

It supplements those existing files. It supplies the
current GPLv3 distribution notice and dependency texts without altering the
original APK or source. The repository’s newer license-packaging/source-stamp
build rules apply to future builds; they are not represented as part of the
already published v0.9.0 binary. Keep the original source ZIP available with that
APK; the moving main branch is not a substitute for its matching source.

## Future release procedure

1. Build and verify the signed APK using a durable signing key. Keep keys private.
   The Android build now packages LICENSE, NOTICE, LICENSING.md, THIRD-PARTY.md and
   licenses/ into assets/license-notices/, plus assets/source-manifest.json.
2. From the project directory run:

       python scripts/prepare_release.py --apk /path/to/signed.apk --tag v0.10.0 --out /path/to/release-output

   Use the actual next version tag, matching versionName. Output must be outside
   the source tree. This tool packages files; signing/signature verification must
   already have been done by the build/release process.
3. The tool refuses an APK without its source stamp or with changed stamped
   inputs. It creates a verified matching source ZIP, notice ZIP and SHA256SUMS.
   Build scripts, wrapper, upstream source/assets and dependency versions are
   included. Compiler/SDK/Maven packages remain obtainable from their respective
   official distributions; source links are listed in THIRD-PARTY.md.
4. Attach the signed APK, generated source ZIP, notices ZIP and checksum file to
   the same GitHub Release. Keep that source accessible for as long as the APK is
   offered. Do not publish keystores, generated per-car identities or private logs.
5. If code, resources, dependency versions or packaged notices change after the
   APK was built, rebuild it before preparing its source archive. Dependency
   upgrades also require reviewing/updating the notice inventory.

This documentation is a maintenance/distribution policy, not a legal opinion or
an assertion that every possible third-party licensing issue has been audited.
