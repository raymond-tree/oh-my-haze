# Data Model: Haze Monitoring MVP

**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

## Storage Boundary

Use one Preferences DataStore for the one saved monitoring location, user
preferences, latest usable estimate, monitor status, and the current alert
baseline. This is local application state; there is no database service, sync,
account, analytics store, or server. Exclude the DataStore file that contains
coordinates from cloud backup and device-to-device transfer. Android Auto
Backup includes internal app files by default unless the app configures backup
rules, so implementation must explicitly configure both backup paths (see
[Android Auto Backup](https://developer.android.com/identity/data/autobackup)).

Store timestamps as UTC epoch milliseconds except the Open-Meteo contract's
`current.time`, which is requested as Unix seconds and converted at the API
boundary. Keep model-valid time separate from retrieval time.

The UI and state distinguish: (1) `lastSuccessfulCheckAt`, when the app last
received a well-formed estimate response; (2) `modelValidAt`, the time the
displayed estimate represents; and (3) CAMS Global's underlying approximately
12-hour model update cadence. Hourly app checks may return the same estimate
and do not promise new hourly model data.

## Entities

### Saved Monitoring Location

There is exactly one active location. It represents the place the user selected
to monitor, not the device's continuously changing physical location.

| Field | Type | Rules |
|---|---|---|
| `label` | String | Non-empty display label; user-facing locality or “Current location” |
| `latitude` | Double | Finite WGS84 latitude in `[-90, 90]` |
| `longitude` | Double | Finite WGS84 longitude in `[-180, 180]` |
| `selectionSource` | Enum | `CURRENT_LOCATION` or `BUNDLED_LOCALITY` |
| `selectedAt` | Instant | Time this saved monitoring location was last explicitly selected |

Changing this entity is an explicit user action. A location update discards the
previous alert baseline; the next alert-eligible estimate silently establishes
one for the new coordinate. Automatic work never reads physical location.

Bundled localities are static app resources containing a label and coordinates;
they are not downloaded or stored as a user search index.

### Air-Quality Estimate

Keep only the latest accepted estimate for the current saved location.

| Field | Type | Rules |
|---|---|---|
| `sourceUsAqi` | Double | Original finite non-negative provider number; retained so values above 500 are displayed without clamping |
| `aqi` | Int | `sourceUsAqi` rounded to nearest whole number (nonnegative halves up) for category and alert comparisons |
| `category` | Enum | Derived from `aqi`: Good, Moderate, Unhealthy for Sensitive Groups, Unhealthy, Very Unhealthy, Hazardous |
| `modelValidAt` | Instant | Open-Meteo `current.time`; not an instrument-observation timestamp |
| `retrievedAt` | Instant | Device time when the valid provider response was received |
| `providerTimezone` | String | IANA timezone from Open-Meteo response, for readable local time display |

The AQI ranges are integer bands: 0–50, 51–100, 101–150, 151–200, 201–300,
and 301+. Values over 500 remain visible at the source value and are classified
Hazardous. A fractional provider value is rounded to the nearest whole number
for category and alert logic; values at or below 500 display the rounded index,
and values above 500 display the original un-clamped provider value. Nonnegative
half values round up. Do not coerce missing or invalid values to zero.

For display, accept an estimate only when its value and timestamp validate, its
model time is no more than five minutes ahead and no more than 18 hours old, and
its model-valid time is newer than the current displayed estimate. A valid
estimate more than 12 but no more than 18 hours old may replace the displayed
estimate with a “too old to trigger alerts” status. Beyond 18 hours, retain the
last displayed value as stale. A failed or rejected response does not overwrite
the last displayed estimate.

### Monitoring Preferences

| Field | Type | Default / rules |
|---|---|---|
| `monitoringEnabled` | Boolean | `true` after onboarding completes |
| `notificationsEnabled` | Boolean | `true`; actual delivery also depends on Android system permission and channel state |
| `sensitivityFloor` | Int | Default `101`; allowed values `101`, `151`, `201`, `301` |
| `selectedLocation` | Saved Monitoring Location | One active location |
| `requestRevision` | Long | Durable version incremented when location, monitoring enabled, app notification preference, or sensitivity floor changes; unchanged for estimate/status writes |

System notification availability is queried from Android and shown separately;
do not treat a stored preference as proof that Android can deliver.

### Alert Baseline and Check Status

| Field | Type | Rules |
|---|---|---|
| `baselineAqi` | Int? | Latest alert-eligible integer US AQI used for the next crossing comparison |
| `baselineModelValidAt` | Instant? | Model-valid time associated with `baselineAqi` |
| `lastSuccessfulCheckAt` | Instant? | Retrieval time of the last response with valid JSON, finite non-negative AQI, and parseable model time no more than five minutes in the future; an old model time still counts as a successful API check |
| `checkStatus` | Enum | Result of the last request: `NEVER_CHECKED`, `AVAILABLE`, `NETWORK_ERROR`, `HTTP_ERROR`, `INVALID_RESPONSE` |
| `lastCheckError` | Enum/String? | Bounded diagnostic category or safe user message; never persist the full request URL or coordinates in logs |

The saved location is the scope of the baseline. Replacing the location clears
both baseline fields and the current estimate, last successful-check time, and
check status, then increments `requestRevision`. A first alert-eligible valid
estimate silently establishes a new baseline. An estimate 12 to 18 hours old
may update the displayed estimate but does not update the alert baseline. An
alert baseline whose model-valid time is over 12 hours old expires; the next
alert-eligible estimate silently establishes a new baseline. Stale, invalid,
duplicate-time, older-time, or alert-ineligible estimates do not trigger or
change alert state.

A well-formed response with a duplicate, older, or more than 18-hour-old
model-valid timestamp still updates `lastSuccessfulCheckAt` and monitoring
status because the API returned a usable AQI and timestamp. An over-18-hour
response cannot replace the saved estimate; freshness is derived from the
model-valid time of the estimate currently stored for display. A duplicate or
older timestamp does not replace the saved estimate or change the alert
baseline. A response with missing or invalid AQI or time is not a successful
usable API check. Any response whose captured
`requestRevision` no longer matches current state is discarded entirely,
including its check time, status, and error status.

## Alert Transition Rules

Only an estimate no more than 12 hours old may influence alert state. Its
model-valid time must be strictly newer than both the latest displayed estimate
and the alert baseline. A floor is crossed only when the prior alert-eligible
integer AQI is below it and the new eligible integer AQI is at or above it. The
12-hour limit follows the usual CAMS Global update cadence as a conservative age
proxy; the API exposes no model-run ID, so it cannot prove that a new model cycle
was published. Estimates more than 12 and no more than 18 hours old remain
displayable but do not change the alert baseline, rearm thresholds, or trigger
notifications. If the last alert-eligible baseline's model-valid time is over
12 hours old, the next eligible estimate silently establishes a new baseline.
When one eligible check crosses multiple floors, select the highest newly
crossed floor at or above the user's sensitivity and request at most one
notification.

Every newer alert-eligible valid value updates the alert baseline even when
notification delivery is unavailable or disabled. Consequently Android
permission restoration cannot replay a suppressed event. A floor rearms when an
eligible value recovers below it; a later upward crossing may notify. Changing
sensitivity never sends a notification by itself. If the latest alert-eligible
baseline already sits above the newly selected floor, only a subsequent
recovery below and new upward crossing can trigger that floor.

## Derived UI States

- **Display freshness**: `FRESH` through 12 hours; `DISPLAY_ONLY` for more than
  12 and through 18 hours (show a clear “too old to trigger alerts” notice);
  `STALE` after 18 hours (retain and label the last known value).
- **Alert eligibility**: A strictly newer valid model timestamp no more than 12
  hours old may update the alert baseline and be evaluated for a genuine upward
  category crossing. Retrieval time alone never makes an estimate eligible.
- **Monitoring status**: `DELAYED` when more than two hours have passed since
  `lastSuccessfulCheckAt`, regardless of estimate freshness or last request
  error. Keep this derived status separate from `checkStatus` and estimate age,
  so stale data and delayed monitoring can both be shown.
- **Notification availability**: derive from the app preference, Android
  notification permission where required, and notification-channel/system
  settings.
- **AQI category**: derived from the rounded integer `aqi`; do not persist a
  second independently editable category value.

The 12-hour alert and 18-hour display cutoffs are product freshness indicators;
the two-hour monitor-delay cutoff is independent. None provides a provider
model-run timestamp or guarantees that the source model updated on schedule.
