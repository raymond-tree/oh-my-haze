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
- `illustration`: one of the six optimized supplied cloud-scene crops, mapped
  one-to-one from category.
- `recommendations`: two text messages selected from the same category mapping.
- `freshness`: `EstimateFreshness.displayStatus(modelValidAt, now)`; only its
  user-friendly status is rendered. The model timestamp remains internal.
- `lastChecked`: `MonitoringState.lastSuccessfulCheckAt`, rendered as “Last
  checked” with a localized date/time or “Not yet checked.”
- `monitoringStatus`: existing enabled/paused state plus the existing two-hour
  delayed-check threshold, shown separately from estimate freshness.
- `checkError` and `notificationStatus`: existing status values where relevant.

The AQI number, category, text label, freshness, and recommendations remain
available as text. The image is decorative. A custom gauge exposes a single
concise semantic description (value and category) and does not make six
individually focusable color-only segments.

### Category presentation mapping

| Existing category | Display name | Asset | Guidance |
|---|---|---|---|
| GOOD | Good | Optimized Good cloud crop | 2 category-specific text messages |
| MODERATE | Moderate | Optimized Moderate cloud crop | 2 category-specific text messages |
| UNHEALTHY_FOR_SENSITIVE_GROUPS | Unhealthy for Sensitive Groups | Optimized USG cloud crop | 2 category-specific text messages |
| UNHEALTHY | Unhealthy | Optimized Unhealthy cloud crop | 2 category-specific text messages |
| VERY_UNHEALTHY | Very Unhealthy | Optimized Very Unhealthy cloud crop | 2 category-specific text messages |
| HAZARDOUS | Hazardous | Optimized Hazardous cloud crop | 2 category-specific text messages |

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
