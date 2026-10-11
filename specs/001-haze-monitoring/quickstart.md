# Quickstart: Haze Monitoring MVP

This guide describes how to build and validate the current Android MVP
implementation and the remaining physical-device checks.

## Implementation Prerequisites

- Android Studio and a JDK supported by the Android Gradle Plugin selected for
  implementation.
- Android SDK Platform 24 or newer; a current compile/target SDK appropriate to
  the release.
- A recent Pixel/AOSP Android phone, plus a Samsung or other OEM phone with
  aggressive battery management for background-work validation.
- No Open-Meteo API key, backend, account, paid geocoding service, map key, or
  test account is required.

The selected minimum is API 24 (Android 7.0), matching WorkManager 2.12.0.
The build enables Java-time core-library desugaring so timestamp handling also
works on API 24–25. Android Auto Backup includes internal files by default;
the app's backup rules exclude saved coordinates from cloud backup and
device-to-device transfer.

## Build and Local Checks

Run from the repository root:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Expected results:

- Debug APK assembles without provider secrets or an app server.
- Unit tests cover AQI rounding/categories, separate 12-hour alert eligibility
  and 18-hour display freshness, silent baseline establishment/expiry, all
  four upward alert floors, recovery/rearming, duplicate suppression,
  no-catch-up behavior, request coalescing, and superseded-response rejection.

For instrumentation checks with a connected device/emulator:

```bash
./gradlew connectedDebugAndroidTest
```

The instrumented suite also verifies the WorkManager schedule, onboarding and
settings controls, and the Open-Meteo HTTPS query contract and response parsing
through Android's platform JSON implementation, including missing/malformed
values, HTTP errors, timezone, Unix model time, and future timestamps. API
responses in parser tests use a fake connection; the suite does not depend on
live provider access.

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
| Finite valid current AQI and timestamp | Save/display as US AQI; record retrieval separately; evaluate alerts only if the model time is no more than 12 hours old and the newer value crosses a floor |
| Missing/null AQI or timestamp, negative/non-finite value, malformed JSON, HTTP error | Keep estimate and last-success time; mark check unavailable/invalid; no alert |
| Timestamp more than five minutes in future | Reject response; no estimate or alert update |
| Model-valid age exactly 12 hours | Alert-eligible if timestamp is newer and it crosses a category floor |
| Model-valid age more than 12 through exactly 18 hours | May replace the displayed estimate as display-only; do not change alert baseline or notify |
| Model-valid age more than 18 hours in a well-formed response | Update check time; do not accept the response as current; retain the stored estimate and derive freshness from its timestamp; no alert-state change |
| Last well-formed API response more than two hours ago | Show monitoring delayed regardless of stored estimate age |
| Duplicate or older model-valid timestamp in an otherwise well-formed response | Update successful-check status/time; do not replace estimate, change alert baseline, or trigger an alert, even if AQI differs |
| Newer alert-eligible estimate after alert baseline is over 12 hours old | Establish a new silent baseline |

One controlled live request verifies provider connectivity and response shape;
it does not prove broad geographic coverage, accuracy, model freshness, or
service availability. Confirm the returned grid coordinate may differ from the
selected coordinate and present the value as a regional model estimate.

Keep the three user-facing times/cadences distinct in the UI and fixtures:
last successful app/API check; the estimate's model-valid time; and the
underlying CAMS Global model's approximately 12-hour update cadence. Use concise
copy such as: “The model usually updates about every 12 hours. Hourly checks may
show the same estimate.” Do not claim that hourly polling obtains hourly new
model observations.

## Deterministic Request-Race Checks

Use a fake API client with held responses and a DataStore test instance. Assert
that request results are committed only for the saved location and monitoring
state version captured before the request:

1. Start a request for location A and hold its success response. Change the
   saved location to B, which increments the durable request revision and
   clears A's estimate/baseline/status. Complete A's response; assert it cannot
   alter B's estimate, baseline, last-success time, check status, or notifications.
2. Hold an automatic response, then disable monitoring or change sensitivity /
   the in-app notification setting. Complete the response; assert the stale
   revision changes no stored values or notification output. A fresh manual
   request made under the new revision remains usable if monitoring is paused.
3. Request manual refresh and scheduled work under the same revision; assert
   they join one in-flight GET. Also deliver two separate same-revision
   responses in reverse order; after the newer model-valid timestamp commits,
   the older one cannot overwrite estimate or alert baseline. A duplicate model
   timestamp, even with a different AQI, can update only successful-check
   time/status.
4. Persist revision N, recreate the store/coordinator as after process restart,
   and deliver a delayed result carrying N after saved location/settings advance
   to N+1. Assert complete rejection. The restarted worker must read current
   coordinates and revision before issuing its request.
5. Serialize the brief state commit/notification section with location and
   monitoring-setting changes; never hold that synchronization during network
   I/O. If process death occurs after baseline commit but before notification
   delivery, a missed notification is acceptable; no queued catch-up is allowed.

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

For each floor, with a newer estimate no more than 12 hours old, confirm a
below-to-at-or-above crossing can send one local notification; a jump across
multiple eligible categories sends one notification for the highest newly
crossed category; unchanged or worsening values above an already reached floor
do not duplicate; falling below the floor rearms it; and a later upward recross
may alert again. A first alert-eligible reading already above a floor and the
first eligible reading after a baseline older than 12 hours are silent
baselines. A 12-to-18-hour estimate remains displayable but cannot change the
alert baseline, rearm a floor, or send a notification. If Android notification
delivery is disabled, eligible values still advance the baseline; restoring
permission does not deliver a catch-up notification.

Changing sensitivity alone must not notify. If the latest alert-eligible
baseline is already above the newly selected floor, a notification requires a
later eligible recovery below and upward recrossing of that floor.

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

### Private manual test on the connected Realme

The owner will operate the realme RMX5000 (API 36) directly; phone mirroring is
off. Android Studio detects it; although the first installation attempt timed
out, the owner subsequently confirmed that the app opens on 2026-10-11.
Installation and launch are verified by that report. Record any error text
from the remaining checks; no screenshot is required. Build success alone
does not prove a passed device test.

Use **Choose a place manually → Kuala Lumpur → Save this location** for the
controlled live request, keeping personal coordinates out of the test report.
Record pass/fail and brief observations for these checks:

| Check | Action and expected result | Result |
|---|---|---|
| Install and live estimate | App opens; manual locality setup displays US AQI, model-valid time, and last successful check separately. First reading produces no alert. | Owner confirmed launch and all three estimate fields, 2026-10-11; first-reading notification behavior pending |
| Manual refresh and failure | With internet, Refresh now returns within 10 seconds in controlled conditions. Temporarily disconnect, refresh, and confirm a clear failure preserves the last estimate; restore connectivity. | Pass by owner report, 2026-10-11: connected check within 10 seconds; offline error preserves last estimate |
| Saved location and settings | Change monitoring location to another bundled locality; confirm the location changes. Change a setting, close and reopen the app, and confirm both persist. Restore automatic monitoring, app alerts, and the 101 default afterward. | Pass by owner report, 2026-10-11: changed location and 151 sensitivity persist after removal from Recent apps/reopen; restoration to defaults not separately confirmed |
| Notification availability | Deny, grant, revoke, and restore Android notification permission; also disable and restore the alert channel. The app reports availability correctly; restoration alone sends no catch-up alert. | Partial pass by owner report, 2026-10-11: app-wide disable/restore shows Unavailable → Available, no immediate catch-up alert; initial permission dialog pending; channel-only check skipped at owner's request, unverified |
| OEM background behavior | With monitoring enabled and internet available, note the last successful check, leave the app in the background with the screen off, and check after more than two hours. Repeat with battery saver and after reboot. Record check times and any delay warning; exact hourly execution is not required. Swiping away and Android force-stop are different actions. | Partial pass by owner report, 2026-10-11 MYT: app closed/screen locked around 2pm+, Last successful check advanced to around 4pm+. Exact interval, battery mode, and delay warning not recorded; reboot deferred by owner; Doze and battery-saver checks unverified |
| Foreground location permission | When testing this path privately, choose approximate foreground access, save a fix, then revoke location permission. Saved-location monitoring still works; no background-location request appears. Do not include coordinates in the report. | Pending |

Record the date, phone Android/ColorOS version, battery mode, and observations.
A screen-off interval alone does not establish that Android entered Doze;
record Doze as unverified unless independently confirmed. Live unchanged AQI
cannot prove alert crossing/rearming behavior; the deterministic suite covers
those rules. The Realme can provide OEM-device evidence, while API 24 and the
separate Pixel/AOSP physical-device checks remain required for T027.

## Release Gate

Before broad release, recheck Open-Meteo terms, non-commercial eligibility,
published request limits, service status, and attribution requirements. The
free endpoint remains approved only for a strictly non-commercial app without
ads, subscriptions, commercial integration, or promotional use. A change in
purpose is a provider/architecture blocker. Do not add a fallback data provider
without an approved specification revision.
