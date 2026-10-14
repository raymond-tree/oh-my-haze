# First Android release verification

Verified locally on 2026-10-14 (Asia/Kuala_Lumpur). Nothing was uploaded to
Google Play, and no commit was created.

- Bundle: `app/build/outputs/bundle/release/app-release.aab`
- Bundle size: 11,296,235 bytes.
- Application ID: `com.ohmyhaze`; version name `1.0`; version code `1`.
- Release manifest: `android:debuggable=false`.
- Upload keystore: `~/.android/oh-my-haze/upload.jks`, outside Git.
- Upload alias: `oh-my-haze-upload`; RSA 3072; SHA256withRSA certificate.
- Certificate valid until 2054-03-01.
- Keystore mode 600; parent directory mode 700.

Bundle SHA-256:

```text
6b6deff33f84d4f4e1ba0aa676e96be5c7a0533b2c15781c458ea2e77fa45376
```

Public upload certificate SHA-256:

```text
DE:C2:CD:D3:0E:8F:DF:91:83:3F:CD:4E:A2:BE:B2:A7:5F:76:98:33:C3:BD:0F:AB:9A:A7:06:B5:85:3F:8D:C3
```

## Verification

- `jarsigner -verify -strict` cryptographically verified the JAR signature;
  status 4 reflects the self-signed certificate not being in the Java trust store.
  Java also reports no timestamp and a streaming-reader warning because Android's
  bundle ZIP places the signing manifest after payload entries. Random-access JAR
  verification succeeds; separately checked SHA-256 manifest digests for all 215
  payload entries, with no missing or mismatched digests.
- Compared the bundle certificate fingerprint with the public certificate in
  the JKS entry `oh-my-haze-upload`; they match. This independent comparison
  read only the existing JKS structure and needed no password input.
- ZIP CRC integrity passes. No removed notification-test strings were found in
  bundled DEX files.
- Debug APK build and all 28 available unit tests pass. Release lint passes
  with zero errors; existing non-blocking warnings remain.
- Git exclusions cover signing files and local credentials; no tracked signing
  credentials or keystore files were found. No passwords were generated,
  displayed, recorded here, or committed.

No device/emulator was connected for instrumentation checks. Prior API 24–25
and physical Pixel/Samsung background, reboot, Doze, and notification checks
remain pending. Store listing and Play account requirements are separate from
this verified signing/build deliverable.

## Owner backup action

Before any upload, make two encrypted backups of
`~/.android/oh-my-haze/upload.jks` in separate secure locations, including one
offline. Store both passwords and the alias in a password manager. Test a restored
copy by listing its certificate and matching the public fingerprint above.
Keep the private keystore and passwords out of Git. See
[release instructions](android-release.md) for the repeatable build workflow.
