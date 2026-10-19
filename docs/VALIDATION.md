# Validation record

Checked on 19 October 2026.

- Java 17 / SDK 34 / build-tools 34.0.0 compilation completed successfully on macOS.
- Built package: `app.foyer.launcher`, version 0.5.0, version code 6, minimum API 23, target API 34.
- APK signature verification passed. The APK declares no requested Android permissions.
- All 29 tests passed after building, including the two checks against the real generated APK.
- The pure Java order model passed 20 cases, including preserving 40 apps, saved preferences, new/removed apps, duplicates, boundary moves, and non-mutating cancellation snapshots.
- Install/recovery cases run against a stateful simulated ADB device: preview, activation, OEM priority override, explicit previous-launcher disabling, partial failures, restoration failures, device mismatch, rejected signing updates, app-data preservation, inactive-profile isolation, invalid input, and inability to save recovery state.
- Python and shell syntax checks passed. The macOS Python cache location was unavailable in the sandbox; syntax was checked in memory without requiring that cache.
- The repository `LICENSE` exactly matches the original MIT licence, and the signed APK includes the same bytes at `assets/LICENSE`.
- Git ignores private signing files, recovery state, generated APKs, and build output. The source export excludes them and `.git` metadata. No private device addresses or machine-specific paths were found in the publishable source files.

## Physical TV checks

Foyer 0.2.0 was installed as an in-place update on a Sony BRAVIA VH1 running Android 12, using the original Foyer signing key. The previous Home app remained enabled during preview.

- Automatic discovery displayed 16 app tiles, plus Play Store and Settings in the utility row. No particular streaming app is required.
- After explicit approval, only the stock Google Home package was disabled. Home opened Foyer. The recovery script restored Google Home and its original enabled state, then Foyer was reactivated.
- State comparison confirmed no original packages were removed, all 15 previously disabled apps stayed unchanged, and the six recorded animation/power settings were preserved.
- Long-press OK entered Move mode without launching the app. Arrow movement, OK to save, and Back to cancel passed.
- On the final signed build, Home cancelled an unsaved move and returned to Foyer from Plex.
- A saved order survived stopping and restarting the Foyer process. Cancelling left the previously saved order intact.
- Plex, TiviMate, YouTube, and Apple TV opened from their tiles. All apps and TV Settings opened through the remote.

The earlier customized launcher was tested on an XGIMI HORIZON Ultra running Android 11; that is separate evidence. This Foyer 0.2.0 build has not been installed on that projector. HDMI source switching, a full TV reboot, Android 6 operation, and compatibility with other TVs remain untested. The 40-app case checks the order model; physical navigation was tested with the Sony's 16 app tiles.

The installer now explicitly uses `--user 0` for APK installation, so a fresh install does not add Foyer to inactive secondary Android profiles. A regression test exercises a device with an existing secondary-profile app list.

## Additional Android 14 preview

Foyer 0.2.0 was previewed on a Sony BRAVIA VU3 running Android 14. Discovery showed 16 app tiles. Remote reordering, save/process-restart persistence, Back cancellation, Plex/TiviMate/YouTube launch, All apps and TV Settings passed.

On this firmware, disabling only Google TV's launcher still leaves a Google setup recovery activity with higher Home priority than Foyer. The portable installer detects this and restores the original Home. It does not disable the setup package or additional Home candidates automatically. Before-and-after state checks confirmed the original launcher, disabled apps, and recorded settings were restored after both attempted switch paths.

This TV also exposed an installation-scope bug: the original ADB install command added Foyer to an inactive secondary profile. That never-launched trial installation was removed from the inactive profile. The installer was corrected to explicitly install for user 0; a subsequent physical update left Foyer absent from the secondary profile, and the 25-test suite passed with the new regression case.

## Foyer 0.3.0 UI checks

On the Sony BRAVIA VU3 / Android 14, the persistent Home hint was removed. Long-press showed app actions. Removing a tile persisted after restarting Foyer and left it available on the All apps icon grid; Add to Home restored it. Move from the context menu and cancellation passed. The native Inputs shortcut opened Sony's selector from Foyer's unprivileged app process. No Android permissions were added.

The TV's standard input map was empty despite HDMI-CEC detecting an Apple TV. Sony's native input selector and its Edit page also omitted HDMI entries. The new Inputs shortcut exposes that native selector rather than inventing input identifiers.

The approved activity-only Google setup recovery change was rejected by Android with a component-state SecurityException. The device-specific recovery restored/verified the original Google TV Home and recovery activity. The Android 14 TV remains a preview installation; no setup package was disabled.

A normal reboot of the Android 14 Sony restored its standard TV-input hardware registrations, including HDMI 3 and its Apple TV CEC child. The updated launcher then displayed direct HDMI shortcuts; no HDMI visibility, CEC, power or animation settings were modified. The TV also exposed its TV app after reboot, which discovery appended to the saved Home order.

The final signed 0.3.0 build opened the registered HDMI 3 passthrough from its tile. The native Live TV activity came to the foreground and TV Input Manager reported a main session on the HDMI hardware port associated with Apple TV. External-input screenshot capture was unavailable; the user watching the TV confirmed that the Apple TV picture was visible. The final state comparison preserved all original package/disabled states and recorded settings, with only Foyer added.

Foyer 0.3.0 was also updated in place on the Sony BRAVIA VH1 / Android 12. Its Home was configured through the app menu to show YouTube, TiviMate and Plex only, in that order. All 13 removed tiles remained available in All apps, the layout survived reopening, each of the three apps launched, and Home returned to Foyer. Installed/disabled packages and recorded settings remained unchanged.

## Optional Home button service (0.4.0)

The signed APK includes a disabled-by-default Accessibility service that receives only `typeWindowStateChanged` events from `com.google.android.apps.tv.launcherx`. Its compiled configuration explicitly disables screen-content retrieval, key filtering and gesture execution. The exact Home activity event was observed on the Android 14 Sony before implementing the match; other screens/packages are ignored. The pure Java redirect policy passed 18 cases, and disable/recovery tests cover preserving other accessibility services. No runtime permission or internet permission was requested.

After the user explicitly enabled Foyer Home button in Android Accessibility settings on the Sony BRAVIA VU3 / Android 14, the first physical Home event opened Foyer. Google TV and the setup package remained enabled.

The final redirect timing policy accepts fresh rapid Home events while ignoring duplicate event timestamps; the first cooldown-based implementation was rejected by the physical rapid-press test and corrected. The complete suite then passed all 28 tests with no skips. On the Android 14 Sony, Home returned to Foyer from YouTube, TiviMate, Plex, Android Settings, and the active Apple TV HDMI input. Repeated Home presses passed, and Android Settings stayed accessible without a redirect. Foreground verification completed in roughly 0.8–1.0 seconds including ADB command/polling overhead; this is not a frame-level latency measurement.

Physical recovery disabled only Foyer Home button and returned Home to the original Google TV launcher. The same user-authorized service was re-enabled and Home returned to Foyer again. Other accessibility services and app package states were preserved.

The Home service remained enabled after a full TV reboot. Foyer was already foreground when the TV reconnected, and another Home press stayed in Foyer. Final state comparison preserved all original packages, disabled-package states and recorded animation/power settings; only Foyer was added. The other Sony and XGIMI were not changed by this update.

## Back focus (0.4.1)

On the Sony BRAVIA VU3 / Android 14, Back on normal Home now focuses the first visible app. The previous build's missing reset was reproduced before the change. Physical remote-key checks passed for resetting from the second tile, staying on the first tile, closing All apps before a subsequent focus reset, and returning from Android Settings to its originating tile before another Back resets focus. Back cancelled an unsaved move and restored its prior position; a subsequent Back focused the first app.

The signed update preserved the existing three-app Home and hidden choices. The Home redirect remained enabled. Installed packages, disabled packages, underlying default Home, and recorded animation/power settings matched the snapshot before the update. All 28 automated tests passed after building. UI Automator returned no root on this TV during this check, so focus was verified through Android's activity view hierarchy and screenshots. These initial checks used the Android 14 Sony.

The signed 0.4.1 APK was also updated in place on the Sony BRAVIA VH1 / Android 12. Its YouTube, TiviMate, Plex Home order and hidden tiles were preserved. Physical remote checks passed for Home opening Foyer, Back resetting focus to YouTube, returning from Settings before another Back resets focus, and closing All apps before the focus reset. The optional Home Accessibility service stayed off. Installed/disabled package states, underlying Home selection, and recorded animation/power settings were unchanged.

## Foyer settings (0.5.0)

The new Customize utility opens a non-exported native Foyer settings activity with Manage Home, Appearance and Inputs. No requested permissions or runtime dependencies were added. Existing package identity, signing certificate and MIT licence are unchanged. The final signed APK built successfully and all 29 automated tests passed, including the generated APK checks. The appearance model exercised 72 combinations of size, spacing and app count; input-order tests include absent-port preservation.

A temporary signed Android instrumentation APK exercised real views, controls and SharedPreferences on the Sony BRAVIA VU3 / Android 14 (37 checks) and BRAVIA VH1 / Android 12 (34 checks). Cases covered hiding/Undo and moving apps, preserving unrelated hidden apps, resume refresh, every appearance choice and preset, input rename/cancel/blank reset, icons/order/visibility, empty Home restoration access, and hiding every registered input without exposing a fallback. Original Foyer preferences were restored before each test finished. The temporary test APK was then removed from both TVs. Some Sony keyboard interactions consumed an initial Back to dismiss the keyboard, so the input-editing test allows that before navigating to the parent page.

Independent review identified stale settings on resume. That condition was reproduced and corrected; the follow-up review found no remaining actionable issues. Rebuilds retain focus and scroll it into view. Physical remote key events reached each new section and returned through Back to Home on the Android 14 Sony; the final release also retains the existing first-app Back behavior. Android's UI Automator returned no root while the optional service was bound on that TV, so physical verification used activity view hierarchy and screenshots. Instrumentation temporarily stopped its previously enabled Home service; the same service was rebound, and Home was verified afterward. No new Accessibility opt-in was enabled on the Android 12 Sony.

Final physical D-pad/OK/Back navigation passed through Manage Home, Appearance and Inputs on both Sonys. On the Android 12 Sony, the Minimal preset was applied through the remote, Foyer was stopped and restarted, and the large tiles, black background and always-visible names persisted. Appearance defaults were then restored through the remote. The two original three-app Home orders were visually verified after testing. Final state comparison preserved original installed packages (155 / 248 including Foyer), disabled states, underlying Home resolvers and recorded animation/power settings. The temporary test package is excluded from the first Android 14 testing snapshot, which was taken after that test package was installed.

## XGIMI HORIZON Ultra migration (0.5.0)

The same signed 0.5.0 APK was installed on an XGIMI HORIZON Ultra / XK03H running Android 11. The earlier custom `com.ansub.tvhome` launcher was retained. Adding a second Home app cleared Android's previous preferred Home, so the saved preview recovery record restored that selection before normal activation was retried. Foyer became the normal Home without disabling another package or enabling its optional Accessibility service. Recovery from active Foyer back to the previous custom launcher passed, and Foyer was reactivated with a final device-specific recovery record.

The previous Plex, TiviMate, YouTube Home order was recreated; 12 other tiles were hidden only inside Foyer and stayed available in the All apps icon grid. Physical D-pad checks passed for all three app launches and Home return, Back focus reset, All apps and Back, and all three Foyer settings sections. Two standard HDMI shortcuts were exposed; live HDMI switching and a full projector reboot were not tested in this migration.

Final comparison showed 122 installed packages versus 121 before: only Foyer was added, every original package remained installed, the previously disabled Google Home package remained unchanged, and all six recorded animation/power settings were preserved. No XGIMI firmware or projection settings were changed. The APK/build and install/recovery scripts were unchanged from the Sony-tested 0.5.0 release.
