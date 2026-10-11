# Research: AQI Dashboard and Location Update

**Date**: 2026-10-11

## Decisions

### Dashboard copy and data attribution

- Use “AQI” in visible labels, settings, and notification copy. The app continues
  to calculate the existing Open-Meteo US AQI index internally; the user's
  requested shorter label changes only presentation.
- Show the category, freshness state, and “Last checked” time. Keep the model
  valid time only for existing freshness and alert decisions; do not render it
  or explain the source cadence on the dashboard.
- Remove the expanded source/model panel from the dashboard. A short “Data
  attribution” entry in Settings credits both CAMS and Open-Meteo and links CC BY
  4.0. This is required: Open-Meteo's current Air Quality API documentation asks
  all users to credit CAMS and Open-Meteo, and its terms license API data under
  CC BY 4.0, which requires appropriate credit.
- Keep the onboarding disclosure that chosen coordinates are sent directly to
  Open-Meteo; this is privacy information, not dashboard source copy.

**References**: [Open-Meteo Air Quality API documentation](https://open-meteo.com/en/docs/air-quality-api), [Open-Meteo Terms](https://open-meteo.com/en/terms), [Open-Meteo Pricing and CC BY 4.0 explanation](https://open-meteo.com/en/pricing).

### Category recommendations

- Use two concise, text-based recommendations for each category, progressively
  reducing the amount and intensity of outdoor activity as AQI rises.
- Keep the recommendation for Good and Moderate levels reassuring and
  proportionate; identify sensitive groups at the levels where the EPA guidance
  does so.
- Do not infer which pollutant drives the combined index. Any particle/smoke
  advice must be conditional and say filtration or a well-fitting particulate
  respirator can reduce exposure, not eliminate it.
- Add one compact note that recommendations are general guidance and local
  pollutant-specific health advice may differ.

**References**: [AirNow AQI Basics](https://www.airnow.gov/aqi/aqi-basics/), [US EPA Air Quality Guide for Particle Pollution (2024)](https://nepis.epa.gov/Exe/ZyPDF.cgi?Dockey=P1016PK7.pdf), [AirNow wildfire smoke guidance](https://www.airnow.gov/wildfires/when-smoke-is-in-the-air/).

### Compose layout and accessibility

- Use the existing Compose Material 3 dependency and semantic text for the AQI
  number, category, freshness state, and recommendations. Color and illustration
  are supporting cues only.
- Keep the dashboard vertically scrollable. Compose's default interactive
  component semantics and adequate touch targets are preferred; any custom
  gauge exposes one concise category/value description instead of relying on
  color alone. Decorative cloud art has no accessibility label.
- Use normal `sp` typography with wrapping and a centered max-width content
  column; do not constrain text to fixed-height boxes. Retain scrolling for
  landscape, compact, and enlarged-font layouts.
- Add light and dark Material 3 color schemes based on the system appearance.
  AQI severity colors must maintain readable contrast in both.

**References**: [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [adaptive display sizes](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes).

### Supplied artwork and launcher icons

- Use the supplied 512 × 512 PNG as the launcher-art source. Provide the
  required legacy icon for API 24–25 and an adaptive icon for API 26+. Keep the
  cloud mark inside the adaptive safe area and put background art in its own
  layer. Add a simple monochrome cloud mark for supported themed launchers.
- The six supplied AQI files are large infographics. Crop only their cloud
  scenes, remove embedded category/recommendation text, resize, and encode as
  WebP. Keep originals in `assets/` for provenance; package only optimized
  derivatives.
- Use `drawable-nodpi` for dashboard WebP assets because Compose controls their
  rendered size. Do not add an icon or image dependency.

**References**: [Android app icons](https://developer.android.com/studio/write/create-app-icons), [adaptive icon design](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [Android image density guidance](https://developer.android.com/training/multiscreen/screendensities), [Android drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).

### Current-location action

- Reuse `CurrentLocationProvider.getOneShotLocation()`, which already obtains
  one bounded, low-power foreground fix after coarse permission is granted.
- Pass the existing permission result, progress state, and clear failure text
  into `LocationPicker`. Request permission only from the user's button action.
- Save successful coordinates with the existing `MonitoringStateStore.saveLocation`
  call and `syncMonitoringSchedule` callback so request revision changes and
  old-response rejection remain atomic. A denial/error leaves current saved
  state untouched; the bundled locality picker remains available.

## Alternatives Rejected

- **Remove data attribution entirely**: incompatible with current CC BY 4.0 and
  Open-Meteo Air Quality API attribution terms. Keep required credit in Settings.
- **Show model timestamps on the dashboard**: rejected per user's product
  direction; retain timestamp use internally for freshness and alert safety.
- **Add a chart, map, provider, or dependency**: outside the MVP and unnecessary
  for the dashboard or one-time location choice.
- **Use the infographic poster unchanged**: embeds category and health copy that
  can be clipped or contradict dynamic values and is not accessible as text.
