# Installation and recovery

## Scope

These scripts operate on the single device specified by `--serial`, Android user 0 only. They never root or flash firmware, change projector/image settings, uninstall packages, clear app data, change animations or power settings, or disable arbitrary background apps.

Debugging must be available and authorized. Keep a working connection while reviewing the new Home screen and retain your device-specific recovery state outside Git.

## Preview first

```sh
sh install.sh --serial YOUR_TV_IP:5555
```

For paired wireless debugging on newer TVs, use the port shown by the TV. Pair first with `adb pair IP:PAIRING_PORT`; then use the debugging connection port with `--serial`. For USB or an emulator, use the exact identifier from `adb devices`.

The installer checks the APK and matching `.apk.json`, connection, active user, device identity, and previous Home app. It saves a recovery record before installing and uses `adb install --user 0 --no-incremental -r`; it never replaces a signature-mismatched installation by uninstalling it.

Check app order, D-pad focus, opening apps, long-press reordering, saving and cancelling a move, All apps, Settings, and any HDMI shortcuts you need. After activation, test Home from another app. Also test a restart manually before relying on automatic startup behaviour.

## Activate

```sh
sh install.sh --serial YOUR_TV_IP:5555 --activate
```

This selects Foyer and verifies both Android's resolved Home activity and the actual foreground window. On failure it attempts recovery and leaves Foyer installed.

## OEM launchers that take over Home

Some firmware assigns its own launcher a higher priority. The script does not disable it automatically. Inspect the selected Home first:

```sh
adb -s YOUR_TV_IP:5555 shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Only if you have reviewed that app and accept disabling its whole package for user 0, pass its exact package:

```sh
sh install.sh --serial YOUR_TV_IP:5555 --activate --disable-launcher com.google.android.tvlauncher
```

`--disable-launcher` must exactly match the currently selected previous Home package and requires `--activate`. It does not infer package names, disable every Home candidate, or operate on an activity alone. Some manufacturers bundle device controls into their launcher; do not assume every OEM package is equivalent to Google's launcher.

The previous launcher's original enabled state is saved before this step. Even if a disable command partly succeeds and then fails, recovery attempts to restore it.

## Recovery

Use the state path printed for the relevant activation:

```sh
sh recover.sh --serial YOUR_TV_IP:5555 --state .state/YOUR_ACTIVATION_STATE.json
```

The record is tied to the Android device identity, so it can still be used if its IP changes. The script rejects a record for another device. It restores the previous launcher's prior enabled state if the activation attempted to disable it, selects the previous Home activity, presses Home, and verifies the foreground window. Recovery does not remove Foyer or delete its record.

If recovery cannot reconnect or restore the original launcher, it reports failure and retains the state file and installed fallback. Reauthorize debugging or restore connectivity, then retry recovery. There is no guaranteed recovery while ADB is unavailable.

Once the old Home screen is confirmed working, Foyer can be uninstalled manually from Android settings if desired. Do not remove the active launcher first.

## Updates

Keep the recovery state from your first activation and the private signing key. Build with the same key and an increased version code, then run preview/install without `--activate`:

```sh
sh install.sh --serial YOUR_TV_IP:5555
```

Android preserves the existing Home selection and Foyer preferences for an in-place update. If an update breaks Foyer, use the earlier activation record to return to the previous launcher. A later preview record whose original Home is already Foyer is not a useful fallback and is rejected for recovery.

## Migrating from the earlier custom launcher

Foyer uses `app.foyer.launcher`, a different package and signing key from the earlier `com.ansub.tvhome` build. It installs alongside it. No automatic migration or removal is performed.

On a device currently using that earlier build, a Foyer activation records it as the previous Home app. The stock Google launcher may already be disabled; Foyer's installer leaves that unrelated prior state alone. Recovering this Foyer activation returns to the earlier custom launcher. To return all the way to the original Google Home app, use the separate recovery record/script from the earlier setup.

## App discovery and order

Every fresh installation discovers the device's enabled launchable apps. No Plex, TiviMate, or YouTube installation is required. Home launchers are omitted; Play Store and TV Settings use the utility row. Normal launchable apps may need a mouse if their own UI does not support a TV remote.

Hold OK on an app and choose Move to enter Move mode. Arrows move it, OK saves, and Back cancels. Leaving the launcher cancels an unsaved move. Installation and removal events refresh the app list, retaining the relative order of remaining apps.

Remove from Home hides a tile inside Foyer only. The app remains installed and visible on the All apps logo grid. Hold OK there and choose Add to Home to restore it. Hidden choices are preserved when app order is reset.

`--apps pkg.one,pkg.two` supplies a preferred starting order and resets any manually saved order; it never hides other apps. `--apps auto` resets to alphabetical discovery. Omit `--apps` for an update that keeps the user's existing order.

## Optional Google TV redirect

Foyer Home button is an opt-in Accessibility service, off by default. Enable it manually under Android Settings → Accessibility when protected Google TV Home activities prevent ordinary activation. It reacts only to Google TV's Home window and opens Foyer; it does not disable Google TV or its setup package. The launcher can briefly flash before the redirect completes. Screen-content reading, key filtering, gestures and networking are not enabled.

To restore normal Home behavior, turn the service off or run `python3 scripts/device.py disable-home-redirect --serial YOUR_TV_IP:5555`. This preserves other accessibility services and app data. Existing package-disabling recovery records remain applicable if that separate method was used. These paths are distinct: `recover.sh` restores Home selection/package state; `disable-home-redirect` turns off Foyer's accessibility redirect.

A fresh second launcher can clear Android's preferred Home selection, as observed on the Android 11 XGIMI. If activation reports no working default Home after preview, use that preview's saved recovery record to restore the previous Home selection, then retry activation. Keep the earlier launcher's package and the preview record throughout this migration.
