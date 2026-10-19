# Foyer 0.5 implementation plan

> Implement in this session with superpowers:executing-plans, then independent review. No commits without separate approval.

Goal: deliver the three approved settings sections without changing existing TV setup.
Architecture: SettingsActivity owns the remote UI; LauncherData shares discovery and saved state with MainActivity; Appearance computes bounded tile geometry. Existing AppOrder is reused for apps and input reconciliation.
Tech: native Java, Android SDK 34, Python/JVM tests.
Spec: ../specs/2026-10-19-foyer-settings-design.md

## Global constraints
API 23 minimum / target 34. No new permissions/dependencies. Preserve signing identity, preferences, system states and MIT licence. Explicit device targets only. No commit, push or PR.

## Review focus
- All apps hidden: utility controls and restoration remain reachable.
- Large tiles plus spacious spacing: columns/last rows stay in bounds.
- Returning from settings or app: preserve focus unless Back resets it.
- Missing/reconnected input: retain saved name/order without changing passthrough ID.
- Undo/removing stale app: preserve unrelated hidden choices and order.

## Tasks
1. [x] Write a failing JVM contract for appearance parsing, defaults and bounded layouts, and a physical acceptance check showing missing Foyer settings on the old build. Implement Appearance.java and run its test.
2. [x] Implement LauncherData.java for shared discovery, app hide/move/undo, appearance persistence and HDMI settings keyed by ID. Implement SettingsActivity.java with menu, Manage Home, Appearance and Inputs sections. Integrate MainActivity and outline icons; bump to 0.5.0/code 6. Build and run the full suite.
3. [x] Run independent review and fix findings. Update in place on the Android 14 Sony; exercise app visibility/move/undo, every appearance category, HDMI rename/icon/hide/order, cancellation, restart persistence and Home. Restore each TV's original customization after testing. Repeat core UI/Home checks on the Android 12 Sony, compare package/Home/system-state snapshots. Update docs and export the verified signed APK, metadata and public source ZIP.
