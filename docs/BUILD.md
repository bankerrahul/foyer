# Building, signing, and developing Foyer

## Prerequisites

- Python 3.9 or newer, using only its standard library.
- A Java 17 JDK, including `javac` and `keytool`.
- Android SDK with platform 34 and build-tools 34.0.0.
- ADB / Android SDK Platform Tools for device installation, not for compilation.

Obtain the SDK from [Android's command-line tools](https://developer.android.com/studio#command-tools) and a Java 17 JDK such as [Temurin](https://adoptium.net/temurin/releases/?version=17).

After setting `JAVA_HOME`, `ANDROID_HOME`, and the SDK command-line-tools path, install the required SDK packages:

```sh
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

Follow the SDK's licence prompts. Foyer's build scripts do not download or install dependencies, accept licences, or modify your system automatically.

Make Platform Tools available for the installation and recovery commands:

```sh
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Get the source and run from the repository:

```sh
git clone https://github.com/bankerrahul/foyer.git
cd foyer
sh build.sh
sh test.sh
```

The build compiles resources, Java, and DEX; assembles and aligns the APK; signs it; verifies its signature; checks its package and absence of requested permissions; and writes matching checksum/certificate metadata. `out/` is regenerated on each build.

The APK is self-contained at runtime. GitHub and the build computer are not needed to use the installed launcher.

## Signing identity

The first local build generates `.secrets/foyer.jks` and `.secrets/keystore.pass`, using a unique key and a randomly generated password. Both files are private and ignored by Git. Preserve them together outside the repository as a private backup.

Future in-place updates require the same package identifier, signing key, and key alias. Increase `android:versionCode` for each release and update `android:versionName` in `AndroidManifest.xml`.

To build with an existing signing key stored elsewhere:

```sh
sh build.sh --keystore /private/path/foyer.jks --password-file /private/path/keystore.pass --alias foyer
```

The password file contains the password on its first line. The password is not passed as a literal command-line argument or written to build metadata. The key alias defaults to `foyer`.

If an update is rejected because it was signed with another key, the installer stops. It does not uninstall the existing app or erase saved settings. Restore the original signing files instead.

An unsigned or independently signed rebuild will not be bit-identical to the supplied signed APK. The metadata identifies a specific built artifact; it is an integrity check, not a substitute for trusting the source of the APK and metadata.

## Developer reference

Foyer is a native Java app with no third-party runtime libraries. The package is `app.foyer.launcher`. The build uses Python's standard library and Android SDK tools. The manifest declares minimum Android 6 / API 23 and target API 34. The UI is designed for landscape 16:9 displays. The minimum API is not a claim of testing every Android version or TV.

| Source | Purpose |
| --- | --- |
| [MainActivity.java](../src/app/foyer/launcher/MainActivity.java) | Home layout, All apps, remote navigation, and tile actions |
| [SettingsActivity.java](../src/app/foyer/launcher/SettingsActivity.java) | Manage Home, appearance, and input customization |
| [LauncherData.java](../src/app/foyer/launcher/LauncherData.java) | App/input discovery and saved preferences |
| [scripts/](../scripts/) | Build, signing, installation, and recovery |
| [tests/](../tests/) | Java models, generated APK checks, and simulated ADB recovery cases |

Run `sh test.sh` after changes and `sh build.sh` for Android code, resources, or manifest changes. The [validation record](VALIDATION.md) reports 29 passing automated tests alongside physical checks on the documented devices. Simulated-device tests do not establish compatibility with every TV.

Normal Home selection and OEM overrides differ by firmware. The XGIMI Android 11 check used normal selection; the Sony Android 12 check used an explicitly approved stock launcher disable; the Sony Android 14 check used the optional Accessibility redirect while retaining Google TV packages. Consult the validation record for version-specific checks and restart limitations. Standard TV input registrations are required for direct HDMI shortcuts; an accessible native selector is offered only when the standard map is empty.

The APK declares no requested Android permissions, including internet access. The optional Home redirect is a user-enabled Accessibility service. Its screen-content retrieval, key filtering, and gesture capabilities are disabled; see [installation details](INSTALL.md#optional-google-tv-redirect).

For an AI-assisted device setup, follow [AI-SETUP.md](AI-SETUP.md). For starting app order, reset flags, and updates that retain preferences, see [app discovery and order](INSTALL.md#app-discovery-and-order).

## Publishable repository

Commit source, tests, resource files, documentation, curated screenshots under `docs/assets/`, and `LICENSE`. Do not commit `.secrets/`, `.state/`, signing keys, generated APKs, passwords, local SDK paths, device addresses, or private device captures. Review every public screenshot for account details and personal information first.

The provided source archive excludes these local files. The ZIP is source-only; for installation use the matching signed APK and its metadata separately.

The original MIT license and copyright notice are preserved in `LICENSE` and bundled in the APK at `assets/LICENSE`. Retain them when redistributing the code or a substantial portion of it.
