# Data Model: AQI Dashboard and Location Update

## Persistence

There is no persistence change. `MonitoringState`, its `AirQualityEstimate`,
`SavedMonitoringLocation`, and `MonitoringStateStore` remain the source of truth.
No DataStore key, migration, table, API request, or notification field is added.

## Presentation projections

### Dashboard reading

Derived from the existing saved state at render time:

- `locationLabel`: saved location label, or the existing not-set fallback.
- `aqiValue`: `AirQualityEstimate.displayValue`.
- `category`: existing `UsAqiCategory` classification and display name.
- `illustration`: one of the six optimized, complete supplied category posters,
  mapped one-to-one from category. The full image includes the cloud scene,
  category name, AQI range, summary, and three recommendations.
- `posterText`: matching app strings or structured presentation data for the
  visible category name, range, summary, and recommendations, used for
  accessibility semantics and consistency checks.
- `recommendations`: the three messages embedded in the matching poster, kept
  in sync with `posterText`.
- `freshness`: `EstimateFreshness.displayStatus(modelValidAt, now)`; only its
  user-friendly status is rendered. The model timestamp remains internal.
- `lastChecked`: `MonitoringState.lastSuccessfulCheckAt`, rendered as “Last
  checked” with a localized date/time or “Not yet checked.”
- `monitoringStatus`: existing enabled/paused state plus the existing two-hour
  delayed-check threshold, shown separately from estimate freshness.
- `checkError` and `notificationStatus`: existing status values where relevant.

The live AQI number, freshness, monitoring status, last-check time, and actions
remain native interface content. The full poster is presented as one accessible
group whose semantics expose the embedded category name, range, summary, and
recommendations once; do not show or announce a duplicate category heading or
recommendation panel. Keep the poster text mirrored in app resources or
structured data. Its English typography is fixed in the image and does not
respond to font scaling or localization; the native interface remains
responsive and the poster remains reachable by scrolling. A custom gauge
exposes one concise semantic category description instead of six individually
focusable color-only segments. The general-guidance note remains native text.

### Category presentation mapping

| Existing category | Display name | Asset | Guidance |
|---|---|---|---|
| GOOD | Good | Complete supplied Good poster | 3 matching app-text recommendations |
| MODERATE | Moderate | Complete supplied Moderate poster | 3 matching app-text recommendations |
| UNHEALTHY_FOR_SENSITIVE_GROUPS | Unhealthy for Sensitive Groups | Complete supplied Sensitive Groups poster | 3 matching app-text recommendations |
| UNHEALTHY | Unhealthy | Complete supplied Unhealthy poster | 3 matching app-text recommendations |
| VERY_UNHEALTHY | Very Unhealthy | Complete supplied Very Unhealthy poster | 3 matching app-text recommendations |
| HAZARDOUS | Hazardous | Complete supplied Hazardous poster | 3 matching app-text recommendations |

This mapping is presentation-only; all thresholds and category decisions
remain in `monitoring/UsAqi.kt`.

### Location-picker transient state

- `selectedLocality`: existing in-screen selection for manual fallback.
- `isRequestingLocation`: true only while the user-requested permission/fix flow
  is active; repeated actions are disabled while true.
- `locationError`: transient, readable denial/unavailable message. It does not
  mutate the previously saved location.
- Success calls the existing save path with the one-shot coordinates and closes
  the picker. Failure leaves saved state unchanged and manual selection enabled.

## Attribution projection

Settings exposes a small entry/dialog with the required credit: air-quality
data from the Copernicus Atmosphere Monitoring Service (CAMS), delivered by
Open-Meteo, with a CC BY 4.0 license link. It stores no data and does not show
model cadence or model-valid timestamps.
