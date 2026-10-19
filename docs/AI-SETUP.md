# Setup guide for Codex and Claude Code

Use this guide when a user asks an AI assistant to install or update Foyer, or restore their previous Home. It describes the existing workflow; it does not grant approval for unrelated device changes. Read [AGENTS.md](../AGENTS.md), [build and signing](BUILD.md), and [installation and recovery](INSTALL.md) before acting. For updates, use the update section below rather than repeating initial Home activation.

## Identify the TV and prepare the computer

- Work in a local session that can reach the TV over the user's network or USB. Use only the device the user identifies; do not scan the network or guess among connected devices. Ask for the model and exact ADB serial or debugging address when missing.
- Guide the user through Developer options and USB, network, or wireless debugging as supported by their TV. Debugging menus vary. The user must approve the connection on the TV; newer wireless-debugging setups may require pairing and a separate connection port. Do not assume enabling USB debugging exposes network ADB.
- Check Python, Java, Android SDK, and ADB against the build guide. Help prepare missing tools using official sources and follow the user's local approval and SDK license prompts. The build script does not install dependencies itself.
- Reuse an existing checkout and private signing key when updating. Never overwrite an existing key or uninstall a signature-mismatched installation. If the original key is unavailable, stop and explain the update limitation.

## Build, preview, and select Home

1. Build and run the documented tests. Keep the generated APK and matching integrity metadata together.
2. Inspect the target TV's current Home and installed Foyer state. Use the installer with its explicit `--serial` target. It saves a device-specific recovery record before installation. Keep the record outside Git.
3. Preview before activation. Discover the user's installed apps rather than assuming Plex, TiviMate, YouTube, or any other favourites. On updates, preserve saved order and hidden choices; omit `--apps` unless the user requests an order reset. See [app discovery and order](INSTALL.md#app-discovery-and-order) for `apps.txt`, preferred ordering, and reset options.
4. Check app launches, remote focus, All apps, and access to Android Settings. Then try normal Home selection with the documented activation command and verify that the Home button opens Foyer from another app. Keep the previous launcher installed and retain the recovery records.
5. If the TV overrides normal Home selection, explain the exact restriction. Do not disable any package automatically. Disabling only the reviewed previous Home package requires explicit user approval and the documented exact-package flag. Never extend that approval to setup packages or other Home candidates.
6. On supported Google TV firmware, explain the optional Foyer Home button Accessibility redirect as an alternative. It is off by default. The user must enable it manually and approve Android's prompt. Explain that the original Home may briefly appear; do not silently enable the service through ADB.
7. Verify recovery before relying on the switch, then return to the user's chosen Home using only the already approved method. Coordinate any TV restart with the user and distinguish checked restart/wake behavior from untested behavior.

Keep this work scoped to the home screen. Do not debloat, uninstall packages, clear app data, change firmware or projector controls, or alter picture, HDMI, animation, power, or unrelated accessibility settings. Input discovery and Foyer shortcut preferences do not authorize changes to the TV's HDMI configuration.

## Update an existing installation

1. Identify the user's TV and original setup folder. Preserve its signing files and first successful activation record. Check the working tree before fetching the official repository's latest source; do not discard local edits, replace private files, or overwrite a signing key. Explain any missing signing files or conflicting local changes before proceeding.
2. Compare the installed app's version with the latest source's application version in `AndroidManifest.xml`. Documentation-only commits are not launcher updates. If no newer app version is available, tell the user; do not reinstall unnecessarily or invent a version bump to make an update appear newer.
3. Record the existing Foyer preferences and Home method before updating. Preserve app order, hidden apps, appearance, input shortcuts, default Home selection, and the optional Home service's enabled state. Do not reset ordering with `--apps` or repeat `--activate` for a routine update.
4. Build and test the newer source with the original signing key, then run the documented [update command](INSTALL.md#updates) against the explicit TV serial. Keep the APK and matching metadata together. If Android rejects the signing identity, stop; never uninstall or clear data to bypass the error.
5. Verify the installed version, saved layout, app launches, Settings access, and Home return from another app. Compare the Home method with the recorded state, preserving other accessibility services and package states. If verification fails, report the failure and use the earlier recovery record when needed to restore the previous Home; do not silently replace the Home method or disable packages.
6. Report the old and new app versions and the checks completed. Retain the original recovery record and signing files. A new preview record that points back to Foyer is not a replacement for the original fallback record.

## Preserve recovery information

Keep `.secrets/` and `.state/` private. Preserve the original signing files and the state from the first successful activation. A later preview record whose original Home is Foyer cannot restore an earlier launcher. During migration from another custom launcher, explain that the saved previous Home may be that custom launcher rather than the factory Home.

If asked to switch back, read the [recovery guide](INSTALL.md#recovery) and inspect the saved record for the identified TV. If Foyer's Accessibility redirect is enabled, disable only that service, preserving other accessibility services. Restore any separate Home selection/package change using its applicable recovery record. Verify the remote's Home button opens the previous Home. Leave Foyer, other apps, and their data installed. If the required record is missing or the target cannot be verified, ask before making recovery changes.

## Report the result

Tell the user what changed, what was verified, and anything that remains untested. Give the exact setup folder, private signing backup location, recovery state path, and restore command. Ask them to retain the setup folder for future updates and restoration. Keep private device addresses and records out of public issues, commits, and screenshots. Do not describe simulated ADB tests as physical-device verification.
