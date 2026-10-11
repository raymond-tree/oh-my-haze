# Implementation Plan: AQI Dashboard and Location Update

**Branch**: `codex/aqi-dashboard` | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-aqi-dashboard/spec.md`

## Summary

Refresh the existing single-module Android app with a compact AQI dashboard
that uses each complete supplied category infographic as one artwork card,
adaptive launcher resources, light and dark themes, and a one-time
current-location action in the existing location picker. Keep the live AQI
number, category gauge, freshness, monitoring state, last-check time, and
refresh action in the interface without duplicating visible category or
recommendation copy outside the artwork. Keep provider requests, the stored AQI
estimate, WorkManager scheduling, notification policy, and request-revision
safety unchanged. Use “AQI” in app copy, hide model timestamps and provider
details from the dashboard, and place the required CAMS/Open-Meteo credit and
CC BY 4.0 link in Settings.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17

**Primary Dependencies**: Android Gradle Plugin 9.2.1; Jetpack Compose BOM
2026.09.00; Material 3; WorkManager 2.12.0; Preferences DataStore 1.2.1;
Play Services Location 21.4.0. No dependency additions are planned.

**Storage**: Existing Preferences DataStore only; no schema or persistence
changes.

**Testing**: Existing JUnit 4 unit tests and Compose/Android instrumentation
tests; `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest`, and
`./gradlew assembleDebug`.

**Target Platform**: Android API 24+ (compile/target API 37), phones with
adaptive launcher icons on API 26+.

**Project Type**: Single-module native Android application.

**Performance Goals**: Keep the existing first-screen response and background
work behavior. Package six optimized full-poster AQI illustrations; target a
combined illustration payload below 1.5 MB while keeping embedded text legible
at the rendered size. Fit the core dashboard at 360 × 800 dp and default font
scale while allowing normal scrolling at larger font scales or smaller
viewports.

**Constraints**: Preserve the zero-backend, non-commercial Open-Meteo use;
required CAMS/Open-Meteo attribution and CC BY 4.0 credit; coarse foreground
location only after an explicit action; no continuous or background location;
existing AQI categories, estimate-age decisions, atomic DataStore updates, and
alert behavior. Do not add dependencies or new persisted fields.

**Scale/Scope**: One home screen, one location picker, Settings attribution
entry, six full category infographics with matching accessible text semantics,
launcher icon resources, and light/dark colors. No new service, data source, or
module.

## Constitution Check

**Pre-research gate**: PASS after the user-directed Constitution 3.0.0 update.

- I. Zero backend: retain direct existing Open-Meteo requests and required
  attribution; no endpoints, accounts, or data providers are added.
- II. Battery-efficient monitoring: the location action reuses one bounded
  foreground fix; scheduled checks remain on saved coordinates.
- III. Notifications: no alert policy, threshold, baseline, or freshness rule
  changes.
- IV. Accurate data: user-facing value is labeled “AQI”; stale-data state and
  last successful check remain visible. The dashboard omits model/source detail;
  Settings provides required attribution.
- V. Simplicity and privacy: Kotlin/Compose/DataStore architecture is retained;
  no fine/background permission or new collection is introduced.
- VI. Quality: existing monitoring behavior remains covered by regression tests;
  UI and location changes receive focused tests; work follows Spec Kit.

**Post-design gate**: PASS. The selected design changes presentation and a
previously available, explicit location-selection flow only. It adds no runtime
dependency, persisted model, backend, data request, or permission.

## Phase 0 Research

Decisions and supporting references are recorded in [research.md](research.md).
The key decisions are to reuse Compose Material 3 and existing state, present
the complete category illustration as one card, keep matching app text for
accessibility semantics, preserve a scrollable page, reuse the existing
one-shot provider, and retain legally required data credit in Settings only.

## Phase 1 Design

- [data-model.md](data-model.md) describes the existing estimate, presentation
  projections, category guidance/assets, and transient location request state.
- No API contract is needed: there is no new external call or cross-module
  interface.
- [quickstart.md](quickstart.md) lists build, test, emulator, accessibility, and
  launcher verification steps.

## Follow-up Design Decision — 2026-10-11

- Use each complete 1122 × 1402 supplied category poster as one dashboard
  artwork card. At a 328 dp content width, the image is approximately 410 dp
  tall and retains its complete category, AQI range, summary, cloud scene, and
  three recommendations.
- Keep the live AQI number, compact six-part gauge, freshness, monitoring
  status, last-check time, refresh action, and general-guidance note as native
  interface content. Remove the duplicate visible category heading and
  separate recommendations panel.
- Mirror the poster copy in app text or structured data for matching
  accessibility semantics. Poster lettering remains fixed-size English text;
  native interface text responds to font scaling, and scrolling remains
  available on narrow, landscape, and enlarged-text layouts.
- Keep the pale poster as a contained artwork card in dark appearance. Optimize
  full images under the existing 1.5 MB combined target only while their text
  stays legible.
- Follow-up implementation work is tracked by T026–T030 in the task list. The
  current implementation and historical validation record still refer to the
  earlier crop-based version.

## Project Structure

### Documentation (this feature)

```text
specs/002-aqi-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (existing single app module)

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/ohmyhaze/
│   ├── MainActivity.kt                 # screen routing/current-location reuse
│   ├── data/                           # unchanged saved monitoring state
│   ├── location/                       # unchanged one-shot provider
│   ├── monitoring/                     # unchanged AQI/freshness rules
│   ├── notifications/                  # remove US AQI qualifier from copy
│   └── ui/
│       ├── HomeScreen.kt               # dashboard composition
│       ├── AqiPresentation.kt           # category art, text semantics, and colors
│       ├── LocationPicker.kt            # current location action and feedback
│       ├── SettingsScreen.kt            # attribution entry
│       └── theme/Theme.kt               # light/dark color schemes
└── res/
    ├── drawable-nodpi/                  # six optimized full category infographics
    ├── mipmap-*/                        # legacy density launcher fallback
    ├── mipmap-anydpi-v26/               # adaptive launcher icon
    ├── drawable/                        # monochrome launcher layer
    └── values[-night]/                  # platform theme/system bars

app/src/test/java/com/ohmyhaze/          # category presentation unit coverage
app/src/androidTest/java/com/ohmyhaze/   # dashboard/location/permission checks
```

**Structure Decision**: Keep the existing `app` module and its clear `ui`,
`data`, `location`, `monitoring`, and `notifications` packages. Add only a small
presentation mapping for category assets and text. Do not introduce a feature
module, general repository layer, third-party icon library, or persistence
abstraction.

## Complexity Tracking

No Constitution Check violations or added architectural complexity.
