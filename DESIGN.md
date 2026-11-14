# Tabbed interface reference and scope

Requested name: **MGU iDrive7-8 Root Companion**.

Reference: https://github.com/mhmm-ok/BMW-SUPRA-IDrive5-6-Android-Auto — inspected repository snapshot `4f8bc0663ba92541c1cbc4209e715de3fb8458de` and its released installer UI on 14 November 2026.

The observed installer uses a pale light background, rounded neutral cards, blue accents, a stepped setup flow and four bottom navigation destinations. This version implements a similar visual organization using original native Android Java views, custom drawn icons and a light platform theme. Labels/actions are tailored to the MGU rooting workflow: Setup, Monitor, Backups and SSH.

The reference repository did not include the installer's Android UI source in the inspected branch, so its released UI was examined for visual reference. A local UI-only preview copy was used for emulator inspection because the released optional graphics library was ARM64-only. That preview copy is not distributed. No code, images, payloads or vehicle protocol from the reference installer are included in this APK/source.

This is a UI/name update to the existing self-contained MGU app. It does not add Android Auto installation, NBT/EVO support, the reference's tethering/airplane-mode workflow, an interactive SSH terminal or an automatic coding restore.

The underlying application ID and signing identity remain the same as v0.3. The diagnostic backend, guarded request coordinator, SSH adapter and Ethernet/root helpers are unchanged. Tab changes do not reset connection/VIN/variant inputs or cancel active work; operation controls are locked during a running job.

User data remains protected by private storage, disabled app backups and the secure-window flag. Emulator screenshots for design review temporarily cleared the secure-window flag only within the separately installed test instrumentation. Production startup still sets it.

## v0.8.0 icon

User selected a newly generated shield/microchip icon with unlocked terminal, cyan/blue/red accent stripes and MGU lettering, then requested an embedded BMW roundel. Packaged as opaque raster artwork with Android adaptive foreground/background layers and 22% layer insets. Original reference app assets were not used.
