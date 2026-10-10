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
previous alert baseline and establishes one for the new coordinate on its next
fresh valid estimate. Automatic work never reads physical location.

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

An estimate is accepted only when its value and timestamp validate, its model
time is no more than five minutes ahead and no more than 18 hours old, and its
model-valid time is newer than the current accepted estimate. A failed or
rejected response does not overwrite the last accepted estimate.

### Monitoring Preferences

| Field | Type | Default / rules |
|---|---|---|
| `monitoringEnabled` | Boolean | `true` after onboarding completes |
| `notificationsEnabled` | Boolean | `true`; actual delivery also depends on Android system permission and channel state |
| `sensitivityFloor` | Int | Default `101`; allowed values `101`, `151`, `201`, `301` |
| `selectedLocation` | Saved Monitoring Location | One active location |

System notification availability is queried from Android and shown separately;
do not treat a stored preference as proof that Android can deliver.

### Alert Baseline and Check Status

| Field | Type | Rules |
|---|---|---|
| `baselineAqi` | Int? | Latest accepted integer US AQI used for the next crossing comparison |
| `baselineModelValidAt` | Instant? | Model-valid time associated with `baselineAqi` |
| `lastSuccessfulCheckAt` | Instant? | Retrieval time of the last response with valid JSON, finite non-negative AQI, and a fresh parseable model time |
| `checkStatus` | Enum | `NEVER_CHECKED`, `AVAILABLE`, `NETWORK_ERROR`, `HTTP_ERROR`, `INVALID_RESPONSE`, `STALE_ESTIMATE`, `DELAYED` |
| `lastCheckError` | Enum/String? | Bounded diagnostic category or safe user message; never persist the full request URL or coordinates in logs |

The saved location is the scope of the baseline. Replacing the location clears
both baseline fields. A first valid estimate, or a valid estimate received
after the previous baseline is over 24 hours old, silently establishes a new
baseline. Stale, invalid, duplicate-time, or older-time responses do not change
the baseline.

A well-formed, fresh response with a duplicate or older model-valid timestamp
still updates `lastSuccessfulCheckAt` and monitoring status because the provider
was reached and returned usable current data. It does not overwrite the saved
estimate or change the alert baseline. Invalid or stale responses do not count
as a successful usable check.

## Alert Transition Rules

The four upward crossing floors are 101, 151, 201, and 301. A transition
crosses a floor when the prior accepted integer AQI is below it and the new
accepted integer AQI is at or above it. When one check crosses multiple floors,
select the highest newly crossed floor at or above the user's sensitivity and
request at most one notification.

Every newer fresh valid value updates the baseline even when notification
delivery is unavailable or disabled. Consequently Android permission restoration
cannot replay a suppressed event. A floor rearms naturally when an accepted
value recovers below it; a later upward crossing may notify. Changing
sensitivity never sends a notification by itself. If the latest accepted value
already sits above the newly selected floor, only a subsequent recovery below
and new upward crossing can trigger that floor.

## Derived UI States

- **Estimate freshness**: `FRESH` when current time minus `modelValidAt` is at
  most 18 hours; otherwise `STALE` (retain and label last known value).
- **Monitoring delay**: `DELAYED` when more than two hours have passed since
  `lastSuccessfulCheckAt`, even if the estimate itself is fresh.
- **Notification availability**: derive from the app preference, Android
  notification permission where required, and notification-channel/system
  settings.
- **AQI category**: derived from the rounded integer `aqi`; do not persist a
  second independently editable category value.

The 18-hour data and two-hour monitoring cutoffs are product freshness
indicators. They do not provide a provider model-run timestamp or a guarantee
that a source model updated on schedule.
