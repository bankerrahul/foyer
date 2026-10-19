<div align="center">
  <img src="docs/assets/foyer.svg" width="88" height="88" alt="Foyer logo">
  <h1>Foyer</h1>
  <p><strong>A quieter home for your TV.</strong></p>
  <p>Your apps. Your layout. Straight to watching.</p>
  <p>Android TV &amp; Google TV · v0.5.0 · Native Java · MIT licensed</p>
  <p>
    <a href="#before-and-after">See the difference</a> ·
    <a href="#get-started">Get started</a> ·
    <a href="docs/INSTALL.md">Install &amp; recovery</a> ·
    <a href="https://github.com/bankerrahul/foyer/issues">Report an issue</a>
  </p>
</div>

![Foyer home screen on an XGIMI HORIZON Ultra, showing Plex, TiviMate and YouTube above a row of utility shortcuts](docs/assets/home.png)

Foyer is a customizable launcher for Android TV and Google TV. It puts the apps you choose on a calm home screen, with large tiles, remote-friendly navigation, and quick access to HDMI inputs and settings. Sponsored rows and recommendation feeds stay out of the way.

**No accounts, ads, analytics, or internet permission.** The installed launcher works on its own, without GitHub, a server, or the computer used to build it.

## Before and after

| Original Home | Foyer 0.5.0 |
| :---: | :---: |
| [![Original XGIMI home screen with promotional content and recommendation rows](docs/assets/home-before.png)](docs/assets/home-before.png) | [![The same XGIMI with Foyer, three chosen apps and utility shortcuts](docs/assets/home.png)](docs/assets/home.png) |
| Promotional banners and content rows | Your chosen apps and shortcuts |

Actual captures from the same XGIMI HORIZON Ultra, before customization and after installing Foyer 0.5.0. Click either image for full resolution. The three apps shown are a personal selection; a fresh installation discovers the apps on **your** TV and starts alphabetically.

## Make room for what you watch

| Feature | What you can do |
| --- | --- |
| Choose your Home | Show the apps you want, reorder them with the remote, and hide unwanted tiles without uninstalling anything. |
| Keep every app within reach | Open **All apps** for a scrollable logo grid, including apps hidden from Home. Add them back whenever you like. |
| Make it yours | Adjust tile size, spacing, background, app names, date, and clock. Choose **Minimal** for large tiles on black. |
| Jump to HDMI | Open registered HDMI inputs from Home. Rename, reorder, hide, or change their shortcut icons inside Foyer. |
| Keep your setup | App order, hidden tiles, appearance, and input preferences save locally and survive in-place updates. |
| Return to the previous Home | Preview before switching. Installation saves a device-specific recovery record and includes a restore script. |

<table>
  <tr>
    <td width="50%"><img src="docs/assets/minimal.png" alt="Minimal preset with large app tiles, visible names and a black background"><br><strong>Minimal</strong><br>Just the essentials, with the clock and date hidden.</td>
    <td width="50%"><img src="docs/assets/manage-home.png" alt="Manage Home settings with app visibility checkboxes, move controls and undo"><br><strong>Manage Home</strong><br>Choose apps, change their order, and undo a removal.</td>
  </tr>
  <tr>
    <td width="50%"><img src="docs/assets/appearance.png" alt="Appearance settings for tiles, spacing, background, app names, date and clock"><br><strong>Appearance</strong><br>Make the layout comfortable from your sofa.</td>
    <td width="50%"><img src="docs/assets/inputs.png" alt="Inputs settings showing a registered HDMI port"><br><strong>Inputs</strong><br>Arrange shortcuts for the inputs your TV exposes.</td>
  </tr>
</table>

Settings and Minimal screenshots were captured on a Sony BRAVIA VH1 running Foyer 0.5.0. Apps and available inputs vary by device.

## Get started

This repository includes the launcher source and local build, installation, and recovery scripts. Build an APK, preview it on your TV, then choose whether to make it Home.

### 1. Build

You need **Python 3.9+**, **Java 17**, and the **Android SDK with platform 34 and build-tools 34.0.0**. Installation also needs Android Platform Tools / ADB. No Gradle installation is required. See the [build guide](docs/BUILD.md) for SDK setup and signing details.

```sh
git clone https://github.com/bankerrahul/foyer.git
cd foyer

export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"

sh build.sh
sh test.sh
```

The build produces `out/foyer.apk` and matching `out/foyer.apk.json` integrity metadata. Keep them together. The first build creates a unique signing key in `.secrets/`; back it up privately and reuse it for updates. An independently signed build cannot update an installation signed with someone else's key.

### 2. Preview on your TV

Enable debugging on the TV, authorize your computer, and use its exact ADB device serial or debugging IP and port:

```sh
sh install.sh --serial YOUR_TV_IP:5555
```

The script installs Foyer and opens a preview. It saves a recovery record and prints its path before installation. It checks the APK checksum and preserves app data during updates. It never scans your network or chooses another connected TV.

Check app launches, remote navigation, Settings, and any HDMI shortcuts. On newer wireless-debugging setups, use the connection port shown by the TV after pairing; see the [installation guide](docs/INSTALL.md).

### 3. Make Foyer Home

After checking the preview:

```sh
sh install.sh --serial YOUR_TV_IP:5555 --activate
```

Activation verifies Android's Home selection and the foreground screen. If selection fails, it attempts to restore the previous Home. **The script never disables another launcher automatically.** Manufacturer overrides may need a separate, explicit choice described in the [installation guide](docs/INSTALL.md#oem-launchers-that-take-over-home).

Some Google TV devices need the optional [Home button redirect](#google-tv-home-button). Check Home from another app and after a restart before relying on your setup.

## Use your remote

| Action | Control |
| --- | --- |
| Navigate and open an app | Arrow keys, then **OK** |
| Reorder Home | Hold **OK** on an app → **Move** → arrows → **OK** to save, **Back** to cancel |
| Hide a Home tile | Hold **OK** → **Remove from Home** |
| Restore a hidden app | **All apps** → hold **OK** on it → **Add to Home** |
| Undo the last removal | **Customize** → **Manage Home** → **Undo removal** |
| Reset focus on Home | Press **Back** to focus the first visible app |
| Change the layout or inputs | **Customize** in the utility row |
| Open Android's device settings | **Settings** in the utility row |

Back closes All apps before returning to normal Home navigation. Leaving during a move cancels the unsaved edit. Newly installed apps join the saved order; removed apps disappear. Resetting app order keeps hidden choices intact.

For an optional starting order, use `apps.txt` or `--apps pkg.one,pkg.two` during installation. Use `--apps auto` to reset ordering to alphabetical discovery. Omit `--apps` when updating to preserve a saved order. [More about discovery and ordering](docs/INSTALL.md#app-discovery-and-order).

## Return to your previous Home

Keep the recovery record from your **first successful activation**. A later update record may point back to Foyer itself.

```sh
sh recover.sh --serial YOUR_TV_IP:5555 --state .state/YOUR_ACTIVATION_STATE.json
```

Recovery checks the device identity, restores the previous Home selection and any launcher enabled state changed during that activation, and verifies the result. Foyer stays installed. Keep debugging authorized and available until recovery has been tested; the script needs a working ADB connection. [Full recovery and update guide](docs/INSTALL.md#recovery).

### Google TV Home button

Some Google TV firmware protects its Home selection. Foyer includes an optional **Foyer Home button** Accessibility service, off by default. Enable it manually under **Android Settings → Accessibility** and approve Android's prompt. When Google TV's Home opens, the service brings Foyer forward; the original screen may appear briefly.

The service receives Google TV launcher window-state events and matches its exact Home activity. Its configuration disables screen-content retrieval, key filtering, and gestures. It has no internet access. Google TV and its setup package remain enabled with this method. It is a Home redirect, not an assistive accessibility tool.

Turn the service off to return to normal Google TV Home behavior, or run:

```sh
python3 scripts/device.py disable-home-redirect --serial YOUR_TV_IP:5555
```

This preserves other accessibility services. If you also changed Home selection or disabled a launcher through a separate activation, use that activation's recovery record afterward. [Redirect and recovery details](docs/INSTALL.md#optional-google-tv-redirect).

## Compatibility

Foyer targets Android TV and Google TV on landscape 16:9 displays. The manifest declares **Android 6 / API 23 minimum** and **API 34 target**; the minimum is not a claim of testing every Android version.

Foyer 0.5.0 has been installed and checked on:

| Device | Android | Home method checked |
| --- | --- | --- |
| XGIMI HORIZON Ultra / XK03H | 11 | Normal Home selection; recovery to the previous custom launcher |
| Sony BRAVIA VH1 | 12 | Home selection with an explicitly approved stock launcher disable; recovery verified |
| Sony BRAVIA VU3 | 14 | Optional Accessibility Home redirect; stock Google TV packages retained |

Home selection, wake/restart behavior, HDMI registration, and vendor settings depend on firmware. Direct HDMI shortcuts require standard inputs exposed by the TV. If none are registered, Foyer can offer a native input selector when one is accessible. It cannot create missing hardware registrations. Samsung Tizen, LG webOS, and Fire TV compatibility are not claimed.

Foyer changes the home screen. It does not debloat the TV, modify firmware or picture settings, or remove advertising inside other apps. No measured performance improvement is claimed. See the [validation record](docs/VALIDATION.md) for automated checks, physical-device results, and remaining limits.

## Build on Foyer

Foyer is a small native Java app with no third-party runtime libraries. The package is `app.foyer.launcher`. The build uses Python's standard library and Android SDK tools.

| Start here | Purpose |
| --- | --- |
| [MainActivity.java](src/app/foyer/launcher/MainActivity.java) | Home layout, All apps, remote navigation, and tile actions |
| [SettingsActivity.java](src/app/foyer/launcher/SettingsActivity.java) | Manage Home, appearance, and input customization |
| [LauncherData.java](src/app/foyer/launcher/LauncherData.java) | App and input discovery and saved preferences |
| [scripts/](scripts/) | Build, signing, installation, and recovery |
| [tests/](tests/) | Java models, generated APK checks, and simulated ADB recovery cases |

Run `sh test.sh` after changes and `sh build.sh` for Android code, resources, or manifest changes. The current validation record reports **29 passing automated tests**, alongside physical checks on the devices above. Simulated-device tests do not establish compatibility with every TV.

Have a bug or an idea? [Open an issue](https://github.com/bankerrahul/foyer/issues) with the Foyer version, TV model, Android version, and steps to reproduce. Remove device addresses and personal information from logs or screenshots. Keep signing keys, recovery records, and build output private. See [build and signing](docs/BUILD.md) and [installation and recovery](docs/INSTALL.md) before changing those paths.

## License

[MIT](LICENSE). The original license and copyright notice are preserved in this repository and bundled in the APK at `assets/LICENSE`. Keep them when redistributing the code or a substantial portion of it. Third-party app names, logos, and promotional artwork in screenshots belong to their respective owners; their appearance does not imply endorsement.
