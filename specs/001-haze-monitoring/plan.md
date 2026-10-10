# Implementation Plan: Haze Monitoring MVP

**Branch**: `spec` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-haze-monitoring/spec.md`

## Summary

Build a focused native Android app that stores one monitoring location on the
device, reads Open-Meteo's current model-based US AQI directly by coordinates,
and uses best-effort hourly background checks to notify on fresh worsening
category transitions. The approved data route is feasible for the user's
confirmed non-commercial use: a live anonymous Kuala Lumpur request returned
`current.us_aqi=155`. The app must label estimates as US AQI, distinguish model
valid time from retrieval time, disclose coordinate transmission, credit
Open-Meteo and CAMS, and show delayed or unavailable monitoring honestly.

## Technical Context

**Language/Version**: Kotlin; choose the current stable Kotlin and Android Gradle
Plugin versions when implementation begins.

**Primary Dependencies**: Jetpack Compose; WorkManager 2.12.0 (current stable,
minSdk 24); Preferences DataStore; Google Play services fused location provider
for one bounded foreground location fix on API 24+; platform `HttpURLConnection`
and `org.json` to avoid a separate HTTP or JSON library. Recheck versions before
implementation.

**Storage**: One Preferences DataStore for the saved monitoring location,
preferences, latest valid estimate, check status, and alert baseline/state. No
Room or remote database. Exclude the DataStore file containing coordinates from
Android cloud backup and device-to-device transfer.

**Testing**: Local unit tests for US AQI integer rounding/category boundaries, timestamp parsing
and freshness, malformed-value rejection, baseline expiry, sensitivity floors,
threshold rearming, and duplicate/catch-up suppression. Android tests for
permission and UI states. Validate background scheduling on physical devices;
emulators alone cannot represent OEM battery policies.

**Target Platform**: Native Android, minSdk 24 (Android 7.0). Use the current
target/compile SDK required at implementation and release. Request notification
permission in context on Android 13+.

**Project Type**: Single-module Android mobile application.

**Performance Goals**: One small HTTPS GET per scheduled check; 60-minute
best-effort schedule; network timeout around 10 seconds; manual refresh reaches a
clear result within 10 seconds in at least 95% of controlled connected trials.

**Constraints**: No backend, API key, account, hosted storage, push service,
continuous/background location, foreground service, wake lock, map, chart,
forecast screen, advertising, or monetization. Open-Meteo free endpoint is only
for the confirmed non-commercial use. User-selected coordinates and the
device's network IP are disclosed to Open-Meteo on each request. CAMS Global
model data in Malaysia are about 45 km resolution, 3-hourly, and refreshed every
12 hours; hourly checks do not mean hourly model updates. The ordinary API
response does not include the underlying model-run initialization time.

**Scale/Scope**: One saved location per installation; one `us_aqi` current value;
four fixed sensitivity-floor options; a small bundled Malaysian locality list;
one simple home screen and settings flow. No historical views or general place
search.

## Constitution Check

### Pre-Research Gate

- **Zero backend**: Pass. The Open-Meteo free endpoint accepts anonymous
  coordinate-based HTTPS GETs; no key or app server is required for the
  user-confirmed non-commercial use.
- **Provider terms**: Pass with a fixed boundary. Free access prohibits
  commercial and promotional use and has published request limits. The user
  confirmed a strictly non-commercial purpose. Any change to that purpose is a
  feasibility blocker requiring a new provider/architecture review.
- **Battery and location**: Pass. WorkManager handles approximate hourly work;
  one foreground location fix is saved and reused. No background location,
  foreground service, or wake lock.
- **Notifications**: Pass. One local notification on a newer valid estimate
  crossing an eligible worsening category; local state prevents duplicates,
  recovery rearms, and Android restrictions do not create queued alerts.
- **Data transparency**: Pass. The app labels the index US AQI, identifies the
  source and model-valid time, shows retrieval time separately, discloses
  regional model estimates, and credits Open-Meteo/CAMS.
- **Privacy**: Pass with explicit user notice and backup exclusion. Coordinates
  are local between requests but are sent to Open-Meteo with the request; the
  provider's terms say troubleshooting logs may contain coordinates and IPs for
  up to 90 days. No location is sent to an Oh My Haze backend.
- **Project simplicity**: Pass. One Android module, one DataStore, one worker,
  one small HTTP client, and a small pure alert/classification function.

### Post-Design Gate

All design artifacts retain the approved provider, privacy, notification, and
scope constraints. No constitution violation or unjustified architecture layer
was introduced. Gate passes. The API cadence/scale and model-run timestamp
limitations remain documented product risks, not a current feasibility blocker.

## Design Decisions

1. **Current value request**: Call `https://air-quality-api.open-meteo.com/v1/air-quality`
   with required saved `latitude`/`longitude`, `current=us_aqi`,
   `domains=cams_global`, `timezone=auto`, and `timeformat=unixtime`. Do not
   request hourly forecast arrays in the MVP.
2. **Network path**: One small `AirQualityClient` builds an encoded HTTPS URL,
   performs GET with connect/read timeouts, and parses the current object with
   platform JSON support. Do not add Retrofit, OkHttp, Moshi, or a generated API
   SDK unless the implementation environment shows a concrete need.
3. **Current data and timestamps**: Treat `current.time` as a model-valid time,
   not an observation timestamp. Request Unix seconds for unambiguous age
   checks; use the response `timezone` identifier for local display and never
   reinterpret the timestamp in the device timezone. Keep `retrievedAt` separate.
   More than 18 hours model-valid age marks the estimate stale. More than two
   hours since a successful check marks monitoring delayed. Clearly document
   that the source model's run timestamp is not exposed, so the age check is only
   an estimate.
4. **Location**: Request foreground approximate location only after the user
   chooses current location. Use a bounded fused `getCurrentLocation` request,
   save its coordinates and a simple label, then use no location API in the
   worker. Offer a small bundled locality list if the user denies or skips the
   permission. Do not add external geocoding or a map.
5. **State**: Use Preferences DataStore for saved location, toggles, chosen
   sensitivity floor, latest valid value/timestamps, worker status, and the
   latest accepted AQI/model-time baseline used for threshold crossings.
   Exclude this store from Android backup to avoid
   transferring the saved coordinates. Changing the location resets the alert
   baseline; it never compares two different locations.
6. **Monitoring**: Enqueue one unique periodic network-constrained WorkManager
   request with a 60-minute interval. Do not add charging, idle, or unmetered
   constraints. Manual refresh uses one unique one-time request. A shared
   process-scoped non-blocking request guard prevents a scheduled/manual overlap
   from issuing a second GET. On failure, record status and wait for a later
   hourly or user-initiated check; do not rapid-retry.
7. **Alert behavior**: Round finite non-negative provider values to the nearest
   whole integer before categorization and threshold evaluation; display that
   rounded integer except preserve source values above 500 un-clamped. Use
   category floors 101, 151, 201, and 301. The default sensitivity floor is 101;
   higher choices remove lower floor events from eligibility. Compare each new
   fresh accepted integer value to the preceding accepted value. For a
   multi-category upward jump, emit at most one event for the highest newly
   crossed eligible category. Updating the accepted value after every valid
   check suppresses duplicates, rearms a threshold only after the value recovers
   below it, and prevents catch-up after notification suppression. A first
   estimate for a location is a silent baseline; a baseline older than 24 hours
   expires and the next estimate is also a silent baseline.
8. **Attribution and product messaging**: Show **US AQI**, explain regional
   model-estimate limitations, and provide Open-Meteo, CAMS, and CC BY 4.0
   attribution on the reading detail/About surface. Disclose before the first
   request that saved coordinates and network IP are visible to the provider.
9. **Android version**: Use API 24 as minSdk to take the current stable
   WorkManager 2.12.0 without pinning an older library. Compile/target the latest
   required SDK available at implementation.

## Project Structure

### Documentation (this feature)

```text
specs/001-haze-monitoring/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── open-meteo-air-quality.md
└── tasks.md                # Not created by planning; /speckit.tasks is next
```

### Source Code (proposed; no application code created in this planning task)

```text
app/
├── src/main/AndroidManifest.xml
├── src/main/java/<package>/
│   ├── MainActivity.kt
│   ├── data/                 # Open-Meteo GET/parser and Preferences DataStore
│   ├── location/             # One-shot provider and bundled locality list
│   ├── monitoring/           # One worker and pure estimate/alert decisions
│   ├── notifications/        # Local notification channel and tap routing
│   └── ui/                   # Compose home, onboarding, and settings
├── src/test/java/<package>/  # Pure classification/alert/parser tests
└── src/androidTest/java/<package>/ # Permission and UI-state checks
```

**Structure Decision**: One conventional Android `app` module. Keep components
inside the module and separate only the obvious UI, data, location, monitoring,
and notification responsibilities. Do not introduce domain/data modules,
repositories per entity, dependency-injection frameworks, a database, or a
general API abstraction.

## Physical-Device Strategy

- **Minimum-version check**: API 24 emulator for install, app start, Compose
  navigation, settings, and baseline/alert behavior.
- **Modern baseline**: One recent Pixel/AOSP physical phone, on an Android
  version supported by the release target.
- **OEM scheduling**: One current Samsung or other device with aggressive
  battery management. Validate screen-off checks, swipe-away behavior, reboot,
  battery saver, Doze, restricted standby, and delayed/unavailable status.
- **Permissions**: Android 12+ device for approximate location; Android 13+
  device for notification prompt, deny, restore, and app-level notification
  controls. Test one-time/approximate location, explicit update, manual fallback,
  and travel without silently changing the saved location.
- **Provider path**: Use a non-personal sample locality or a test double in
  deterministic cases. For one controlled live device request, verify the
  response field, timezone metadata, selected grid coordinate, attribution, and
  privacy notice. Do not use personal location in logs or screenshots.
- **Alert cases**: Test every boundary listed in the quickstart, fractional AQI
  rounding, multi-category
  jumps, stale/invalid/duplicate timestamps, 24-hour baseline expiry, recovery
  rearming, disabled notifications, and absence of catch-up after restoring
  Android permission.

## Complexity Tracking

No constitution violations or complexity exceptions.
