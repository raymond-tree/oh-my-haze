---
description: "Dependency-ordered implementation tasks for the Oh My Haze MVP"
---

# Tasks: Haze Monitoring MVP

**Input**: Design documents in specs/001-haze-monitoring/

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/open-meteo-air-quality.md, quickstart.md, and .specify/memory/constitution.md

**Tests**: Included because the specification and constitution explicitly require deterministic AQI, alert, persistence, and stale-response tests.

**Organization**: Tasks follow the five user stories and their approved priorities. The stories share a single location, estimate, state store, and worker, so they are testable by story but not independently shippable in arbitrary order.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the smallest single-module Android project and runtime scaffold.

- [ ] T001 Initialize the Gradle wrapper, settings, version catalog, root build, and single app module for Kotlin and Jetpack Compose; set namespace and application ID to com.ohmyhaze and minSdk 24; include WorkManager, Preferences DataStore, the Google Play services fused location provider, and unit/instrumentation test dependencies, rechecking compatible stable versions when implementation begins. Keep HTTP and JSON on platform APIs. Configure gradle/wrapper/gradle-wrapper.properties, settings.gradle.kts, gradle/libs.versions.toml, build.gradle.kts, and app/build.gradle.kts.
- [ ] T002 Configure app permissions for internet, foreground approximate location only, and Android 13+ notifications; exclude datastore/monitoring_state.preferences_pb from both cloud backup and device transfer in app/src/main/AndroidManifest.xml, app/src/main/res/xml/backup_rules.xml, and app/src/main/res/xml/data_extraction_rules.xml.
- [ ] T003 Add the launch activity and a minimal Compose theme/scaffold so the empty app builds and launches on API 24 in app/src/main/java/com/ohmyhaze/MainActivity.kt, app/src/main/java/com/ohmyhaze/ui/theme/Theme.kt, and app/src/main/res/values/strings.xml.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define and persist the one-device state shared by all user stories.

- [ ] T004 Write DataStore persistence tests for an unset location, a saved location, preferences, estimate/check state, process recreation, and requestRevision changes in app/src/test/java/com/ohmyhaze/data/MonitoringStateStoreTest.kt.
- [ ] T005 Define persisted state and implement one Preferences DataStore in app/src/main/java/com/ohmyhaze/data/MonitoringState.kt and app/src/main/java/com/ohmyhaze/data/MonitoringStateStore.kt. Preserve the data-model.md field rules: label, “Non-empty display label; user-facing locality or ‘Current location’”; latitude, “Finite WGS84 latitude in [-90, 90]”; longitude, “Finite WGS84 longitude in [-180, 180]”; selectionSource, “CURRENT_LOCATION or BUNDLED_LOCALITY”; selectedAt, “Time this saved monitoring location was last explicitly selected”; sourceUsAqi, “Original finite non-negative provider number; retained so values above 500 are displayed without clamping”; aqi, “sourceUsAqi rounded to nearest whole number (nonnegative halves up) for category and alert comparisons”; category, “Derived from aqi: Good, Moderate, Unhealthy for Sensitive Groups, Unhealthy, Very Unhealthy, Hazardous”; modelValidAt, “Open-Meteo current.time; not an instrument-observation timestamp”; retrievedAt, “Device time when the valid provider response was received”; providerTimezone, “IANA timezone from Open-Meteo response, for readable local time display”; monitoringEnabled, “true after onboarding completes”; notificationsEnabled, “true; actual delivery also depends on Android system permission and channel state”; sensitivityFloor, “Default 101; allowed values 101, 151, 201, 301”; selectedLocation, “One active location”; requestRevision, “Durable version incremented when location, monitoring enabled, app notification preference, or sensitivity floor changes; unchanged for estimate/status writes”; baselineAqi, nullable “Latest alert-eligible integer US AQI used for the next crossing comparison”; baselineModelValidAt, nullable “Model-valid time associated with baselineAqi”; lastSuccessfulCheckAt, “Retrieval time of the last response with valid JSON, finite non-negative AQI, and parseable model time no more than five minutes in the future; an old model time still counts as a successful API check”; checkStatus, “Result of the last request: NEVER_CHECKED, AVAILABLE, NETWORK_ERROR, HTTP_ERROR, INVALID_RESPONSE”; and lastCheckError, “Bounded diagnostic category or safe user message; never persist the full request URL or coordinates in logs.” Persist timestamps as UTC epoch milliseconds, converting API Unix seconds at the client boundary. Provide a short shared state-change synchronization point; do not hold it during network I/O.

**Checkpoint**: The single local state store persists validated state and is excluded from backup before story work begins.

---

## Phase 3: User Story 1 - Choose a Monitoring Location (Priority: P1)

**Goal**: Save one explicit monitoring location and never follow the device's changing physical location.

**Independent Test**: Complete onboarding with current-location permission granted, denied, and skipped; save a location in each supported path, restart the app, and verify background-ready state retains the chosen coordinates without location tracking.

### Tests for User Story 1

- [ ] T006 [US1] Add onboarding/location UI tests for explicit approximate-location request, denial, skip, manual locality selection, saved-location persistence, and no background-location permission in app/src/androidTest/java/com/ohmyhaze/location/LocationOnboardingTest.kt.

### Implementation for User Story 1

- [ ] T007 [P] [US1] Add the small static Malaysian locality list with labels and valid WGS84 coordinates in app/src/main/java/com/ohmyhaze/location/BundledLocations.kt.
- [ ] T008 [P] [US1] Implement one bounded foreground current-location fix after explicit user action, requesting approximate location only and never background location, in app/src/main/java/com/ohmyhaze/location/CurrentLocationProvider.kt.
- [ ] T009 [US1] Build onboarding and location-update UI that saves current or bundled coordinates and label locally, explains before the first request that saved coordinates go directly to Open-Meteo, the provider may log coordinates and the device network IP, and the app has no account or backend; displays the exact text “Air quality is checked approximately every hour. Android battery settings may delay or occasionally prevent checks and alerts.”; and provides the bundled-list fallback in app/src/main/java/com/ohmyhaze/ui/OnboardingScreen.kt, app/src/main/java/com/ohmyhaze/ui/LocationPicker.kt, and app/src/main/res/values/strings.xml. Route each saved-location change through the shared state-change path, increment requestRevision, and clear the prior estimate, alert baseline, last successful check, and check status.

**Checkpoint**: A saved location is available on a clean install with either a one-time current-location choice or the bundled fallback; travel alone cannot change it.

---

## Phase 4: User Story 2 - Understand the Current US AQI Estimate (Priority: P1)

**Goal**: Show a clearly labelled, accurately timed Open-Meteo US AQI estimate and explain its regional model limits.

**Independent Test**: Feed valid, malformed, duplicate-time, display-only, stale, and unavailable fixtures; verify the reading, category, saved place, attribution, and three distinct time/cadence concepts are shown or rejected as specified.

### Tests for User Story 2

- [ ] T010 [P] [US2] Add contract/parser tests for the encoded HTTPS query and current.us_aqi response, Unix model time, timezone, missing/null/malformed fields, invalid numbers, future timestamps, duplicate/older timestamps, and HTTP errors in app/src/test/java/com/ohmyhaze/data/OpenMeteoAirQualityClientTest.kt.
- [ ] T011 [P] [US2] Add tests for all US AQI bands and boundary/fractional/above-500 values plus the 12-hour alert-eligibility and 18-hour display-freshness boundaries in app/src/test/java/com/ohmyhaze/monitoring/UsAqiTest.kt and app/src/test/java/com/ohmyhaze/monitoring/EstimateFreshnessTest.kt.

### Implementation for User Story 2

- [ ] T012 [P] [US2] Implement a small direct HTTPS GET client using latitude, longitude, current=us_aqi, domains=cams_global, timezone=auto, and timeformat=unixtime; parse and defensively validate the response with platform HTTP/JSON APIs in app/src/main/java/com/ohmyhaze/data/OpenMeteoAirQualityClient.kt.
- [ ] T013 [P] [US2] Implement US AQI rounding/category mapping and separate display-freshness and alert-age decisions, including display-only estimates over 12 through 18 hours old and stale estimates over 18 hours old, in app/src/main/java/com/ohmyhaze/monitoring/UsAqi.kt and app/src/main/java/com/ohmyhaze/monitoring/EstimateFreshness.kt.
- [ ] T014 [US2] Build the home estimate/details UI showing the saved location, US AQI label/value/category, model-valid time, last successful API-check time, freshness, delayed-check status, and Open-Meteo/CAMS attribution; explain that it is a regional model estimate and never an official Malaysian API/IPU reading; include “The model usually updates about every 12 hours. Hourly checks may show the same estimate.” in app/src/main/java/com/ohmyhaze/ui/HomeScreen.kt and app/src/main/java/com/ohmyhaze/ui/SourceDetails.kt.

**Checkpoint**: A fixture-backed estimate is correctly classified and presented without implying an exact-point measurement or new hourly model data.

---

## Phase 5: User Story 3 - Monitor Conditions Automatically (Priority: P1)

**Goal**: Run approximately hourly best-effort checks for the saved coordinates and safely discard results from superseded state.

**Independent Test**: Verify periodic scheduling and saved-coordinate reuse, then deterministically invalidate held responses through location/preference changes, overlapping sources, out-of-order completion, and process recreation.

### Tests for User Story 3

- [ ] T015 [P] [US3] Test unique approximately 60-minute network-constrained periodic scheduling, pause/resume behavior, and absence of charging/idle/unmetered constraints in app/src/test/java/com/ohmyhaze/monitoring/MonitoringSchedulerTest.kt.
- [ ] T016 [P] [US3] Add deterministic fake-client/DataStore tests for location change during a request, monitoring or alert-setting changes, same-revision manual/scheduled overlap, reverse-order responses, duplicate timestamps, and process recreation with an obsolete revision; assert obsolete results cannot alter estimate, baseline, last-success time, status, or notification output in app/src/test/java/com/ohmyhaze/monitoring/MonitoringResponseRaceTest.kt.

### Implementation for User Story 3

- [ ] T017 [US3] Implement unique periodic WorkManager scheduling at approximately 60 minutes with a connected-network constraint, plus enable/pause/resume and a unique one-time work entry point, in app/src/main/java/com/ohmyhaze/monitoring/MonitoringScheduler.kt.
- [ ] T018 [US3] Implement the shared automatic/manual request path in app/src/main/java/com/ohmyhaze/monitoring/AirQualityWorker.kt and app/src/main/java/com/ohmyhaze/data/MonitoringStateStore.kt: snapshot coordinates/source/requestRevision before the GET; run networking outside synchronization; use a process-scoped same-revision in-flight guard only to coalesce traffic; atomically validate requestRevision and automatic-monitoring-enabled state before applying any success or failure. For a matching revision, record the successful API-check time/status for any well-formed response; replace the displayed estimate only for a strictly newer model-valid time no more than 18 hours old; advance alert state only under the separate newer-time, no-more-than-12-hour rules. Preserve the estimate on duplicate, older, or over-18-hour data. Serialize configuration changes and the brief accepted-result/notification section; discard every field of a superseded response. On process restart, reload current state before a request and rely on persisted revision validation for late results. Do not rapid-retry, track location, use a foreground service, or hold a wake lock.

**Checkpoint**: Monitoring uses saved coordinates, exposes delayed/unavailable state, and stale responses have no effect even when the process-scoped guard is absent after restart.

---

## Phase 6: User Story 4 - Receive Alerts When Conditions Worsen (Priority: P1)

**Goal**: Send one local notification only for a newer eligible upward US AQI category crossing, with recovery rearming and silent baselines.

**Independent Test**: Exercise each threshold floor with first-reading, newer crossing, multi-category jump, duplicate, recovery, expired baseline, display-only, stale, invalid, and permission-disabled inputs; verify notification sequence and baseline changes.

### Tests for User Story 4

- [ ] T019 [US4] Add pure alert-policy tests for default floor 101 and choices 151/201/301; initial and expired silent baselines; one highest-category notification on a multi-floor jump; duplicate suppression; recovery/rearming; strict newer timestamps; the 12-hour boundary; display-only/no-alert behavior; and notification suppression without catch-up in app/src/test/java/com/ohmyhaze/monitoring/AlertPolicyTest.kt.

### Implementation for User Story 4

- [ ] T020 [P] [US4] Implement the pure alert transition policy using only newer valid integer US AQI values no more than 12 hours old, strict upward crossings, silent baseline establishment after a first sample or expired baseline, and recovery-based rearming in app/src/main/java/com/ohmyhaze/monitoring/AlertPolicy.kt.
- [ ] T021 [P] [US4] Implement the local notification channel, Android permission/availability check, concise category alert, and tap-through to the current home view without queuing suppressed events in app/src/main/java/com/ohmyhaze/notifications/AirQualityNotifier.kt.
- [ ] T022 [US4] Connect alert decisions to the worker's atomic state update and notification delivery in app/src/main/java/com/ohmyhaze/monitoring/AirQualityWorker.kt and app/src/main/java/com/ohmyhaze/data/MonitoringStateStore.kt; advance the baseline when delivery is unavailable, keep commit-and-notify serialized with location/alert-setting changes, and permit a missed notification if the process dies after commit without creating a catch-up alert.

**Checkpoint**: Only genuine newer category crossings can notify; baseline and permission behavior prevents duplicates and catch-up delivery.

---

## Phase 7: User Story 5 - Refresh and Manage Monitoring (Priority: P2)

**Goal**: Let users request an immediate check and manage the approved small set of monitoring and alert settings.

**Independent Test**: Use working, slow, and failing requests; pause monitoring and refresh manually; change each preference; and verify progress, persisted choices, notification availability, and unchanged last valid data on failure.

### Tests for User Story 5

- [ ] T023 [US5] Add UI tests for manual refresh progress/success/failure, manual refresh while automatic monitoring is paused, monitoring and notification toggles, the four sensitivity choices, notification-unavailable messaging, and accessible control labels in app/src/androidTest/java/com/ohmyhaze/ui/MonitoringSettingsTest.kt.

### Implementation for User Story 5

- [ ] T024 [US5] Add the home-screen manual refresh action using the shared request path so same-revision manual/scheduled checks join one active request and failures preserve the last valid estimate in app/src/main/java/com/ohmyhaze/ui/HomeScreen.kt and app/src/main/java/com/ohmyhaze/monitoring/MonitoringScheduler.kt.
- [ ] T025 [US5] Build minimal settings for monitoring on/off, app notifications on/off, sensitivity floors 101/151/201/301, and explicit saved-location update; show Android notification availability separately and persist each choice through the shared state-change path, increment requestRevision for monitoring/notification/sensitivity changes, and invalidate in-flight responses in app/src/main/java/com/ohmyhaze/ui/SettingsScreen.kt and app/src/main/java/com/ohmyhaze/ui/LocationPicker.kt.

**Checkpoint**: Manual and automatic checks share the same validation/alert path; preferences remain bounded, accessible, and durable.

---

## Phase 8: Polish & Cross-Cutting Validation

**Purpose**: Verify the approved MVP on Android and confirm provider/privacy release conditions before implementation is considered complete.

- [ ] T026 Run the documented debug build, unit tests, and connected instrumentation suite; resolve failures and keep the commands/results aligned with specs/001-haze-monitoring/quickstart.md and app/build.gradle.kts.
- [ ] T027 Execute the physical-device strategy in specs/001-haze-monitoring/quickstart.md on an API 24 emulator, a recent Pixel/AOSP device, and a Samsung or similarly managed OEM device; verify permission paths, reboot/Doze/battery delay messaging, saved-location reuse, notification denial/restoration, and one controlled non-personal live provider request without logging coordinates.
- [ ] T028 Before broad release, recheck current Open-Meteo non-commercial terms, request limits, attribution/license conditions, and service availability against specs/001-haze-monitoring/research.md; stop release if the use is no longer strictly non-commercial or the provider terms no longer support direct anonymous requests.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No feature dependencies; complete before Android story work.
- **Foundational (Phase 2)**: Depends on Setup and blocks all user stories.
- **User stories (Phases 3–7)**: Complete in the documented dependency order below; their tests may use fixtures, but the end-to-end journeys share the saved location, estimate client, worker, and alert state.
- **Polish (Phase 8)**: Depends on all stories selected for the MVP and is required before a release decision.

### User Story Dependencies

- **US1 (P1)**: Starts after Foundation; provides the saved coordinates and location UI.
- **US2 (P1)**: Follows US1 for the end-to-end estimate journey; its parser and AQI policy tests can be developed independently after Foundation.
- **US3 (P1)**: Follows US1 and US2; automatic requests depend on the saved location, API client, and persistent state.
- **US4 (P1)**: Follows US2 and US3; alert evaluation consumes accepted estimates from the shared request path.
- **US5 (P2)**: Follows US3 and US4; manual refresh and settings use the same worker, revision validation, alert policy, and notification state.

### Parallel Opportunities

- **US1**: After T006, T007 and T008 can proceed in parallel because locality data and one-shot location access are separate files.
- **US2**: T010 and T011 can be authored in parallel; after those tests, T012 and T013 can be implemented in parallel before T014 integrates them in the UI.
- **US3**: T015 and T016 can be authored in parallel. Keep scheduler and response-commit integration sequential where they share WorkManager entry points and persisted state.
- **US4**: After T019, T020 and T021 can proceed in parallel; T022 integrates policy, persistence, and delivery.
- **US5**: T023 is the independent test task; implement manual refresh and settings sequentially because both mutate or invoke the shared monitoring state.

## Parallel Examples

- **US1**: Start T007 and T008 together after T006; implement T009 after both complete.
- **US2**: Start T010 and T011 together; after they are written, T012 and T013 can proceed together; complete T014 after both.
- **US3**: Start T015 and T016 together. Implement T017, then T018 against the scheduler and tested state boundary.
- **US4**: Start T020 and T021 together after T019; complete T022 after both.
- **US5**: Run T023 before implementation; complete T024 before T025 so settings use the already established refresh path.

## Implementation Strategy

### First Useful Slice

1. Complete Setup and Foundation.
2. Complete US1 and US2 to prove location choice, the live Open-Meteo path, honest US AQI presentation, freshness, attribution, and privacy disclosure.
3. Validate that slice on an API 24 emulator and with one controlled non-personal live request.

### Core MVP Delivery

1. Add US3 and prove scheduled monitoring and stale-response rejection.
2. Add US4 and verify threshold crossings, recovery, notification permissions, and no catch-up behavior.
3. Complete US5 for manual refresh and the approved minimal settings.
4. Complete physical-device and provider-terms gates before release; Android scheduling remains best-effort and must not be marketed as an hourly alert guarantee.

## Notes

- Every implementation task names its target path; the package namespace is com.ohmyhaze.
- [P] marks work in separate files that has no dependency on incomplete tasks.
- The test-first tasks are required by SC-012, the alert/location acceptance scenarios, and constitution Principle VI.
- No backend, second provider, account, map, chart, forecast screen, continuous location, foreground service, wake lock, or monetization is included.
