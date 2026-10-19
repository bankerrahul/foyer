# Building and signing Foyer

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

## Publishable repository

Commit source, tests, resource files, documentation, curated screenshots under `docs/assets/`, and `LICENSE`. Do not commit `.secrets/`, `.state/`, signing keys, generated APKs, passwords, local SDK paths, device addresses, or private device captures. Review every public screenshot for account details and personal information first.

The provided source archive excludes these local files. The ZIP is source-only; for installation use the matching signed APK and its metadata separately.
