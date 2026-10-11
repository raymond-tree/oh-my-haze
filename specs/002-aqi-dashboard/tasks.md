# Tasks: AQI Dashboard and Location Update

**Input**: Design documents in `specs/002-aqi-dashboard/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, and `data-model.md`

**Tests**: Included because the user explicitly requested automated and emulator
verification. Each story has focused checks for its observable behavior.

**Organization**: Tasks are grouped by user story so each value increment can be
reviewed separately. All stories are P1 in the approved specification.

## Phase 1: User Story 1 — Compact AQI dashboard and supplied art (P1)

**Goal**: Show location, monitoring state, AQI/category, matching art, gauge,
last-check state, and refresh action in a compact accessible dashboard; support
light/dark mode and launcher icon variants; retain required provider credit only
in Settings.

**Independent Test**: Render each category, empty/stale/delayed states, and both
system themes. Confirm core content fits at 360 × 800 dp/default font scale,
remains scrollable at compact/enlarged layouts, and does not display a “US AQI”
label, provider/source details, or model timestamps on the dashboard.

- [x] T001 [US1] Add Compose coverage for compact dashboard states, “AQI” labeling, last-check/freshness separation, accessibility text, and absence of source/model copy in `app/src/androidTest/java/com/ohmyhaze/ui/HomeScreenRefreshTest.kt`.
- [x] T002 [US1] Add category-to-art and gauge mapping tests covering all six categories and boundary values in `app/src/test/java/com/ohmyhaze/ui/AqiPresentationTest.kt`.
- [x] T003 [US1] Crop, resize, and compress the six supplied category illustrations into caption-free cloud scenes under `app/src/main/res/drawable-nodpi/`, keeping packaged derivatives under the 1.5 MB combined budget.
- [x] T004 [US1] Generate legacy and adaptive launcher resources plus a themed monochrome cloud layer from `assets/app icon.png`, then reference the launcher icons in `app/src/main/AndroidManifest.xml`.
- [x] T005 [US1] Implement the category presentation mapping, image resources, accessible gauge semantics, and theme-aware severity colors in `app/src/main/java/com/ohmyhaze/ui/AqiPresentation.kt` and `app/src/main/java/com/ohmyhaze/ui/HomeScreen.kt`.
- [x] T006 [US1] Replace the text-heavy home content with a scrollable, responsive dashboard, remove model/source explanation from the dashboard, and show user-facing “AQI” plus clear freshness and last-check states in `app/src/main/java/com/ohmyhaze/ui/HomeScreen.kt`.
- [x] T007 [US1] Add system light/dark Material 3 colors and matching Android system-bar styles in `app/src/main/java/com/ohmyhaze/ui/theme/Theme.kt`, `app/src/main/res/values/styles.xml`, and `app/src/main/res/values-night/styles.xml`.
- [x] T008 [US1] Replace the home source-details panel with a compact Settings “Data attribution” entry and required CAMS/Open-Meteo/CC BY 4.0 credit in `app/src/main/java/com/ohmyhaze/ui/SettingsScreen.kt` and `app/src/main/java/com/ohmyhaze/ui/SourceDetails.kt`.
- [x] T009 [US1] Remove “US” qualifiers from visible Settings and notification copy while preserving internal category names and policy in `app/src/main/java/com/ohmyhaze/ui/SettingsScreen.kt`, `app/src/main/java/com/ohmyhaze/notifications/AirQualityNotifier.kt`, and `app/src/main/res/values/strings.xml`.
- [x] T010 [US1] Run `HomeScreenRefreshTest.kt` and `AqiPresentationTest.kt`, then fix failures in `app/src/androidTest/java/com/ohmyhaze/ui/HomeScreenRefreshTest.kt` and `app/src/test/java/com/ohmyhaze/ui/AqiPresentationTest.kt`.

## Phase 2: User Story 2 — Category-specific health guidance (P1)

**Goal**: Present two short, text-based, proportionate recommendations for each
category without relying on artwork, color, or an unlabeled icon.

**Independent Test**: Verify two distinct recommendation messages for all six
categories, accessible reading order, and a compact general-guidance disclaimer.

- [x] T011 [US2] Add unit tests for two recommendations per category, category mapping, and conditional particle/smoke language in `app/src/test/java/com/ohmyhaze/ui/AqiGuidanceTest.kt`.
- [x] T012 [US2] Add Compose assertions for visible category guidance and accessible recommendation wording in `app/src/androidTest/java/com/ohmyhaze/ui/HomeScreenRefreshTest.kt`.
- [x] T013 [US2] Implement the six-category guidance text and general-advice note in `app/src/main/java/com/ohmyhaze/ui/AqiPresentation.kt` using the approved EPA/AirNow guidance summarized in `specs/002-aqi-dashboard/research.md`.
- [x] T014 [US2] Render the current category's guidance as independent readable text in `app/src/main/java/com/ohmyhaze/ui/HomeScreen.kt`; keep particle-specific advice conditional and preserve scrolling/accessibility behavior.
- [x] T015 [US2] Run `AqiGuidanceTest.kt` and the guidance assertions in `app/src/androidTest/java/com/ohmyhaze/ui/HomeScreenRefreshTest.kt`; correct any wording, category, or accessibility failures in `app/src/main/java/com/ohmyhaze/ui/AqiPresentation.kt` and `HomeScreen.kt`.

## Phase 3: User Story 3 — Current location in location picker (P1)

**Goal**: Add a deliberate one-shot current-location action above the manual
locality list, with progress/error feedback and no loss of the saved location on
failure.

**Independent Test**: Open the picker, confirm the current-location action is
above manual places, verify it delegates to the existing foreground one-shot
flow, and confirm denial/failure leaves manual selection and the previous saved
place intact.

- [x] T016 [US3] Add location-picker UI tests for action order, progress/error states, disabled duplicate requests, manual fallback, and cancellation in `app/src/androidTest/java/com/ohmyhaze/location/LocationPickerTest.kt`.
- [x] T017 [US3] Add current-location action, in-flight state, and inline error presentation above the manual list in `app/src/main/java/com/ohmyhaze/ui/LocationPicker.kt` while keeping manual selection available.
- [x] T018 [US3] Wire the picker action to the existing coarse-permission launcher and `CurrentLocationProvider.getOneShotLocation()` flow in `app/src/main/java/com/ohmyhaze/MainActivity.kt`; save success through the existing `MonitoringStateStore.saveLocation` and schedule-sync path.
- [x] T019 [US3] Update location onboarding/current-location instrumentation coverage and retain response-revision regression coverage in `app/src/androidTest/java/com/ohmyhaze/location/LocationOnboardingTest.kt` and `app/src/test/java/com/ohmyhaze/monitoring/MonitoringResponseRaceTest.kt`.
- [x] T020 [US3] Run `LocationPickerTest.kt`, `LocationOnboardingTest.kt`, `ManifestLocationPermissionTest.kt`, and `MonitoringResponseRaceTest.kt`; verify denied permission or provider failure does not change the saved place and no fine/background permission is added.

## Phase 4: Final review and delivery

- [x] T021 [P] Update the three completed UX items with concise implementation evidence in `feedbacks.md`.
- [x] T022 Run `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest`, and `./gradlew assembleDebug`; record unavailable hardware checks accurately in `specs/002-aqi-dashboard/quickstart.md`.
- [x] T023 Inspect `HomeScreen.kt` at default, dark, landscape, and enlarged-text settings; inspect launcher resources under `app/src/main/res/mipmap-*` and verify the permission set in `app/src/main/AndroidManifest.xml`.
- [x] T024 Review `git diff --check`, feature artifacts, packaged asset size, and all user-facing AQI/source/model wording; update `/Users/raincloudmarketing/Documents/Codex/local-agent-context/SESSION.md` without replacing unrelated notes.
- [x] T025 Commit the completed MVP iteration on `codex/aqi-dashboard`; do not merge.

## Dependencies and order

- User Story 1 precedes User Story 2 because both add dashboard presentation and
  share the category mapping and home screen. User Story 3 follows because it
  changes the existing location picker and activity routing independently.
- Focused tests cover category mapping, all six guidance levels, location
  picker interaction, permission denial, and the existing response-revision
  protection.
- Final review follows all three stories. No task changes persisted data,
  provider requests, notification policy, or permission scope.

## Parallel opportunities

- T003 (illustration crop preparation) and T004 (launcher resource generation)
  touch separate resource files.
- T007 (theme resources) and T008 (Settings attribution entry) touch separate
  files from the core dashboard mapping.
- T021 (feedback notes) can be completed independently of final Gradle checks.

## Implementation strategy

Deliver the dashboard/art/theme increment first, then its guidance text, then
the current-location picker. Keep each increment reviewable with its focused
tests. Finish with the full unit, instrumentation, build, emulator/accessibility,
license-attribution, and manifest checks before committing the feature branch.
