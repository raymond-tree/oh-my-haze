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
   freshness, category gauge, full matching artwork, last-check state, and
   refresh action. Confirm the AQI number remains live text outside the image.
3. Exercise values around all six category boundaries: 0/50, 51/100, 101/150,
   151/200, 201/300, and 301+. Confirm the full artwork's category and range
   agree with the current number and that the matching recommendations change.
4. Confirm there is no separate visible category heading or recommendation
   panel, and that one compact general-guidance note remains outside the image.
5. Confirm the dashboard uses the label “AQI” and does not show a “US AQI”
   qualifier, source name/link, model-valid timestamp, or model update cadence.
6. Verify no-estimate, older/stale estimate, delayed monitoring, and failed
   refresh states; freshness and monitoring delay remain separate.
7. Inspect light/dark appearance, landscape, narrow width, and largest available
   system font size. The complete artwork must remain uncropped and reachable
   by scrolling; native text and controls must respond to font scaling. Confirm
   the image semantics expose its category, range, summary, and recommendations
   once and match the app text resources.
8. Check that the poster's embedded English text remains legible at the default
   rendered size and does not duplicate the accessible announcement. In dark
   mode, inspect the pale poster as an intentional contained card.
9. Open Settings > Data attribution. Confirm the short CAMS/Open-Meteo credit and
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
- Package the complete six AQI poster compositions from `assets/`; do not crop
  out the category text or recommendations. Keep the supplied originals
  unchanged and package only optimized full-image derivatives.
- Keep all six full-image resources below the plan's 1.5 MB combined target
  only if the smallest embedded text remains legible at its rendered size. If
  that conflicts, revise the size target instead of degrading the text.

## Historical validation record — 2026-10-11

This evidence is for the original crop-based implementation. The complete
poster-card design above is a follow-up and has not yet been implemented or
validated.

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
