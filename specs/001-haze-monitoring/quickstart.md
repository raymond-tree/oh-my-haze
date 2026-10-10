# Quickstart: Haze Monitoring MVP

This guide describes how to validate the implementation after it is built. It
does not imply application code or a runnable Android project exists yet.

## Implementation Prerequisites

- Android Studio and a JDK supported by the Android Gradle Plugin selected for
  implementation.
- Android SDK Platform 24 or newer; a current compile/target SDK appropriate to
  the release.
- A recent Pixel/AOSP Android phone, plus a Samsung or other OEM phone with
  aggressive battery management for background-work validation.
- No Open-Meteo API key, backend, account, paid geocoding service, map key, or
  test account is required.

The selected minimum is API 24 (Android 7.0), matching the current stable
WorkManager 2.12.0 requirement checked during planning. Recheck dependency and
SDK requirements before implementation. Android Auto Backup includes internal
files by default; verify backup rules exclude saved coordinates from both cloud
backup and device-to-device transfer.

## Build and Local Checks

Once the Android project and wrapper exist, run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Expected results:

- Debug APK assembles without provider secrets or an app server.
- Unit tests cover AQI rounding/categories, time parsing/freshness, invalid
  response rejection, silent baseline establishment/expiry, upward threshold
  crossings, recovery/rearming, duplicate suppression, and permission-denied
  suppression without catch-up.

For instrumentation checks with a connected device/emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## Onboarding and Location Checks

1. Start with a clean install and confirm the model-estimate/coordinate
   disclosure appears before the first provider request.
2. Confirm the explanation includes exactly: “Air quality is checked
   approximately every hour. Android battery settings may delay or occasionally
   prevent checks and alerts.”
3. Choose current location. Grant foreground approximate location only, confirm
   one fix is saved, then revoke permission and verify normal monitoring still
   uses the saved coordinates.
4. Repeat with location permission denied or skipped. Select a bundled Malaysian
   locality and confirm no map, paid geocoder, or location permission is needed.
5. Travel or simulate a different device fix without requesting an explicit
   location update; the saved monitoring location must remain unchanged.
6. Explicitly choose another location; confirm the prior alert baseline is
   discarded and the new location starts with a silent baseline.

## API and Data-State Checks

Use a non-personal test coordinate for live verification. Confirm the Android
client makes an HTTPS GET to the contract in
[contracts/open-meteo-air-quality.md](contracts/open-meteo-air-quality.md),
with one current variable, saved WGS84 coordinates, `timezone=auto`, and
`timeformat=unixtime`; no authentication, API key, or app backend is present.

Verify the following response handling with deterministic fixtures or a local
test double. Do not log full URLs, user coordinates, or raw location values.

| Input | Expected behavior |
|---|---|
| Finite valid current AQI with fresh Unix timestamp | Save/display as US AQI, classify/alert after nearest-integer rounding, record retrieval separately |
| Missing/null AQI or timestamp, negative/non-finite value, malformed JSON, HTTP error | Keep last valid estimate; mark check unavailable/invalid; no alert |
| Timestamp more than five minutes in future | Reject response; no estimate or alert update |
| Model-valid age exactly 18 hours | Still within the defined freshness limit |
| Model-valid age more than 18 hours | Mark stale; do not replace estimate or update alert state |
| Last usable check more than two hours ago | Show monitoring delayed even if stored estimate remains fresh |
| Duplicate or older model-valid timestamp in otherwise valid fresh response | Update successful-check status/time; do not replace accepted estimate, change alert baseline, or trigger an alert |
| Newer valid estimate after a baseline gap over 24 hours | Establish a new silent baseline |

One controlled live request verifies provider connectivity and response shape;
it does not prove broad geographic coverage, accuracy, model freshness, or
service availability. Confirm the returned grid coordinate may differ from the
selected coordinate and present the value as a regional model estimate.

## US AQI Classification and Alert Checks

Test at least these inputs after provider-value rounding:

| US AQI | Expected category |
|---:|---|
| 0, 50 | Good |
| 51, 100 | Moderate |
| 101, 150 | Unhealthy for Sensitive Groups |
| 151, 200 | Unhealthy |
| 201, 300 | Very Unhealthy |
| 301, 500, above 500 | Hazardous; preserve/display values above 500 |
| 100.5 | Display 101; Unhealthy for Sensitive Groups |
| 500.5 | Display un-clamped 500.5; Hazardous |

Verify alert transitions for every configured floor:

| Sensitivity floor | Upward crossing that is eligible | Recovery value that rearms |
|---:|---|---|
| 101 (default) | 101, 151, 201, 301 | At or below 100 for the 101 floor |
| 151 | 151, 201, 301 | At or below 150 for the 151 floor |
| 201 | 201, 301 | At or below 200 for the 201 floor |
| 301 | 301 | At or below 300 for the 301 floor |

For each floor, confirm a below-to-at-or-above crossing can send one local
notification; a jump across multiple eligible categories sends one notification
for the highest newly crossed category; unchanged or worsening values above an
already reached floor do not duplicate; falling below the floor rearms it; and
a later upward recross may alert again. A first reading already above a floor
and the first reading after baseline expiry are silent baselines. If Android
notification delivery is disabled, accepted values still advance the baseline;
restoring permission does not deliver a catch-up notification.

Changing sensitivity alone must not notify. If the currently accepted estimate
is already above the newly selected floor, a notification requires a later
recovery below and upward recrossing of that floor.

## Background and Physical-Device Checks

- On an API 24 emulator, verify install, launch, one-location setup, manual
  refresh, settings, classification, and local alert state.
- On Android 12+, verify approximate foreground permission and that the app
  never requests background location.
- On Android 13+, verify notification permission grant, denial, revocation,
  system channel disablement, and restoration without catch-up alerts.
- On a recent Pixel/AOSP phone, enable screen-off hourly monitoring, restart,
  background/close the app, enable battery saver and Doze, and verify honest
  status rather than exact timing assumptions.
- Repeat relevant scheduling cases on a current Samsung or other OEM device;
  record delays and confirm the UI warns after the two-hour success window.
- Confirm WorkManager constraints do not require charging, idle, or unmetered
  network, and that no foreground service, wake lock, or continuous GPS is
  used.
- Verify a manual refresh returns a clear result within 10 seconds in controlled
  connected cases, and errors preserve the last valid estimate.

## Release Gate

Before broad release, recheck Open-Meteo terms, non-commercial eligibility,
published request limits, service status, and attribution requirements. The
free endpoint remains approved only for a strictly non-commercial app without
ads, subscriptions, commercial integration, or promotional use. A change in
purpose is a provider/architecture blocker. Do not add a fallback data provider
without an approved specification revision.
