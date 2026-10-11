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

- Use the three recommendations already visible in each complete supplied
  poster as the category-specific on-screen guidance. The poster replaces both
  the cropped cloud image and the separate visible recommendations panel.
- Keep matching category, range, summary, and recommendation strings in app
  resources or structured presentation data so the image is not the only
  accessible copy. The image semantics and matching text must expose each item
  once, without a duplicate visible heading or recommendation list.
- Review the embedded advice against FR-009 before release. If a line needs
  revision, cover the source line with an image-aligned Compose text overlay
  and update its matching app resource together; do not leave different visible
  and accessible wording. The source artwork stays unchanged in `assets/`.
- Keep one compact native note below the artwork that identifies the advice as
  general and says local pollutant-specific guidance may differ.

**References**: [AirNow AQI Basics](https://www.airnow.gov/aqi/aqi-basics/), [US EPA Air Quality Guide for Particle Pollution (2024)](https://nepis.epa.gov/Exe/ZyPDF.cgi?Dockey=P1016PK7.pdf), [AirNow wildfire smoke guidance](https://www.airnow.gov/wildfires/when-smoke-is-in-the-air/).

### Compose layout and accessibility

- Use the existing Compose Material 3 dependency. The numeric AQI value,
  freshness, monitoring state, last-check time, and controls remain native text
  and actions. The full category artwork exposes its category name, AQI range,
  summary, and recommendations through one matching semantic description.
- Keep the dashboard vertically scrollable. Compose's default interactive
  component semantics and adequate touch targets are preferred; any custom
  gauge exposes one concise category/value description instead of relying on
  color alone. Avoid announcing poster text twice.
- Preserve the full portrait artwork at its aspect ratio. Render it at 90% of
  the available content width: about 295 × 369 dp at 360 dp screen width, which
  keeps the default AQI card and refresh control in the 360 × 800 dp layout.
  Use vertical scrolling on 320 dp, landscape, and enlarged-font layouts instead
  of cropping the artwork. Original and corrected poster text remain fixed
  relative to the image; native interface text and controls use scalable text.
- Add light and dark Material 3 color schemes based on the system appearance.
  The supplied artwork keeps its pale background as a contained card in dark
  appearance; other text, controls, and AQI severity colors must remain
  distinguishable in both themes.

**References**: [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [adaptive display sizes](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes).

### Supplied artwork and launcher icons

- Use the supplied 512 × 512 PNG as the launcher-art source. Provide the
  required legacy icon for API 24–25 and an adaptive icon for API 26+. Keep the
  cloud mark inside the adaptive safe area and put background art in its own
  layer. Add a simple monochrome cloud mark for supported themed launchers.
- Use the full 1122 × 1402 composition of each supplied AQI infographic,
  retaining its cloud character, category, range, summary, and recommendations.
  Keep originals in `assets/` for provenance and package optimized full-image
  derivatives. The six original PNGs total 1,846,906 bytes; retain the plan's
  1.5 MB package target only if the embedded copy remains legible after
  optimization.
- Use `drawable-nodpi` for dashboard image assets because Compose controls their
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
- **Use a cropped cloud scene with separate visible recommendations**: no longer
  selected because it discards the supplied complete design and duplicates its
  content model. The full poster is selected, with matching app text semantics
  and copy review to address accessibility and consistency.
