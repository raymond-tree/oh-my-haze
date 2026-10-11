# Quickstart: AQI Dashboard and Location Update

## Prerequisites

- JDK 17 and the Android SDK installed/configured for this repository.
- API 37 emulator available for UI/instrumentation checks; a physical API 24+
  device or API 24 emulator is needed to verify the legacy launcher fallback.
- For themed launcher inspection, use a launcher that supports themed icons on
  Android 13 or newer.

## Build and automated checks

From the repository root:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

`connectedDebugAndroidTest` requires an available, booted emulator/device. If
none is available, run the unit tests and build, then record the device check as
pending rather than implying it passed.

## Manual dashboard checks

1. Launch a saved-location build at 360 × 800 dp with default font size.
2. Confirm the initial screen shows location, monitoring status, AQI value and
   category, matching crop, accessible category gauge, two recommendations,
   last-check state, and refresh action.
3. Exercise values around all six category boundaries: 0/50, 51/100, 101/150,
   151/200, 201/300, and 301+. Confirm artwork and recommendation text match.
4. Confirm the dashboard uses the label “AQI” and does not show a “US AQI”
   qualifier, source name/link, model-valid timestamp, or model update cadence.
5. Verify no-estimate, older/stale estimate, delayed monitoring, and failed
   refresh states; freshness and monitoring delay remain separate.
6. Inspect light/dark appearance, landscape, narrow width, and largest available
   system font size. All content must remain reachable by scrolling without
   clipping. Check TalkBack order, recommendation wording, icon semantics, and
   touch targets.
7. Open Settings > Data attribution. Confirm the short CAMS/Open-Meteo credit and
   CC BY 4.0 link work.

## Manual location checks

1. Open location change from both Home and Settings. Verify “Use my current
   location” is shown above manual locations.
2. With approximate location already granted, tap once and confirm the action
   saves one foreground fix and returns to the dashboard.
3. With permission missing, tap once and confirm the app requests coarse
   permission only then. Deny permission and verify an understandable message,
   unchanged saved location, and usable manual choices.
4. Disable device location or force a provider timeout; verify the same fallback
   behavior and that retry cannot start while a request is already in progress.
5. Select and save a bundled place without granting location permission.
6. Confirm the manifest requests neither fine nor background location and that
   old API responses cannot overwrite the newly saved location.

## Launcher and asset checks

- API 24–25: legacy raster launcher icon displays without clipping.
- API 26+: adaptive foreground/background mask correctly and preserve the cloud
  mark in the safe area.
- Themed-icon launcher: monochrome cloud mark remains recognizable.
- Dashboard cloud crops do not contain embedded captions or recommendations;
  original user-supplied files remain under `assets/`, while only optimized
  crops are packaged.
- Verify the six packaged illustration resources together stay below the 1.5
  MB budget in the plan.

## Validation record — 2026-10-11

- `./gradlew testDebugUnitTest connectedDebugAndroidTest assembleDebug` passed.
  The emulator suite ran 19 instrumentation tests on the Pixel_10a API 37 AVD.
- Manual display checks passed at 360 × 800 dp with default text, in light and
  dark themes, at 1.5× text scale, and in landscape. The default dashboard fits
  without scrolling; enlarged landscape content scrolls to the recommendations,
  last-check label, and full refresh button.
- Launcher manifest references the supplied icon's five legacy raster sizes
  and an API 26+ adaptive resource with a monochrome layer. The six packaged
  illustrations total 204,732 bytes. API 24–25 launcher rendering was not
  checked because the available AVD runs API 37.
- Screenshot from the 360 × 800 dp light-mode check:
  `/Users/raincloudmarketing/.codex/visualizations/2026/10/11/01a12a7c-a0eb-70e2-9d0d-8b4fe76b9857/aqi-dashboard-360x800.png`
