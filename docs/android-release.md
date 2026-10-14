# Android release signing

The first release uses application ID `com.ohmyhaze`, version name `1.0`, and
version code `1`. Increase versionCode for each subsequent Play upload.
The project uses AGP 9.2.1, Gradle 9.4.1, a Java 25 Gradle daemon, Java 17
source/target, compile/target SDK 37, and minimum SDK 24. Release is not debuggable.
The notification testing gesture and its debug/release implementations were removed.

## Create the upload key once

In your own interactive terminal, from the repository root:

```bash
bash scripts/android-release.sh create-key
```

The helper uses Android Studio's bundled JDK if JAVA_HOME is unset. Keytool
creates a 3072-bit RSA upload key with a 10,000-day certificate in JKS format.
Its default path is `~/.android/oh-my-haze/upload.jks`, outside the repository;
the alias is `oh-my-haze-upload`. The directory is mode 700 and the key mode 600.
An existing key is never overwritten. Type and confirm your own keystore
password at keytool's hidden prompts, then choose a key password (Enter uses
the same password). Passwords are never generated or supplied in command arguments.

## Build with environment-only credentials

```bash
bash scripts/android-release.sh build
```

The helper asks for the passwords again using hidden terminal input, exports them
only for its process and child build, runs the available debug unit tests, release lint,
a debug build, and the signed release bundle, then verifies the JAR signature,
compares its certificate to the upload keystore, and prints public SHA-256 fingerprints. It uses no persistent Gradle daemon
or configuration cache and unsets passwords on exit. Do not use shell tracing,
Gradle debug logging, build scans, or configuration caching for signing builds.
Do not put passwords into command lines, chat, Gradle properties, or `.env` files.

The Gradle signing configuration reads these four environment variables:

- `OH_MY_HAZE_KEYSTORE_PATH`: existing absolute keystore path outside the repo.
- `OH_MY_HAZE_KEY_ALIAS`: upload key alias.
- `OH_MY_HAZE_STORE_PASSWORD`: keystore password.
- `OH_MY_HAZE_KEY_PASSWORD`: private key password.

The helper defaults the path and alias; override those two non-secret variables
if needed. Partial environment configuration is rejected. Debug builds and unit
tests need no signing credentials; release packaging fails without them and
never uses the Android debug key. Keystore/private-key and common credential
files are ignored by Git, including nested paths. Ignoring is defense in depth:
keep all signing files outside Git and never force-add them.

Output: `app/build/outputs/bundle/release/app-release.aab`.

## Verify and retain evidence

`jarsigner -verify` must report `jar verified.`. A self-signed certificate warning
is expected for an Android upload key. Compare the bundle's SHA-256 certificate
fingerprint (`keytool -printcert -jarfile ...`) with the keystore's fingerprint
(`keytool -list -v -keystore ~/.android/oh-my-haze/upload.jks -alias oh-my-haze-upload`;
enter its password only at the hidden terminal prompt). They must match.
Also retain the bundle's SHA-256 file checksum for the release record.
AAB files use JAR signing, so use jarsigner rather than APK-only apksigner.

If a device is attached, run `./gradlew connectedDebugAndroidTest` separately.
API 24–25 launcher checks and physical Pixel/Samsung background, Doze, reboot,
and notification validation were still pending in the previous project handoff.
Creating a signed bundle does not complete those device checks or Play listing,
privacy/Data safety, content rating, or account-specific release requirements.

## Back up before publishing

Keep at least two encrypted backups of `upload.jks` in separate secure locations,
with one offline. Save the keystore password, key password, alias, certificate
fingerprint, and app ID in a password manager; keep passwords separate from
unencrypted copies of the key. Test restoring a backup by listing its certificate
and confirming the fingerprint. Never commit or share the private keystore.
This is an upload key: when you later enroll in Play App Signing, Google manages
the app signing key and this key authenticates future bundle uploads.

References: [Android app signing](https://developer.android.com/studio/publish/app-signing)
and [command-line bundle builds](https://developer.android.com/build/building-cmdline).
Nothing in these helpers uploads to Google Play or commits files.
