# Open-Meteo Air Quality API Contract

**Purpose**: Obtain the current model-based US AQI estimate for the one saved
monitoring location. This is the MVP's only air-quality provider contract.

## Request

```http
GET https://air-quality-api.open-meteo.com/v1/air-quality
    ?latitude={savedLatitude}
    &longitude={savedLongitude}
    &current=us_aqi
    &domains=cams_global
    &timezone=auto
    &timeformat=unixtime
Accept: application/json
```

- `latitude` and `longitude` are required WGS84 decimal coordinates from the
  locally saved monitoring location.
- Request only `current=us_aqi`; do not request hourly series or display a
  forecast in this MVP.
- Explicitly select `cams_global` for the Malaysia-first MVP and request the
  coordinate-resolved timezone. Unix seconds make freshness comparisons
  independent of device timezone; the returned IANA timezone is for display.
- Make the HTTPS request directly from Android. Free non-commercial access is
  anonymous and uses no key. Do not send a customer API key in the app and do
  not route through a backend or silently fall back to another provider.
- One normal scheduled check makes one small request. Keep automatic retry
  behavior bounded to a future scheduled check; do not poll or loop on errors.
- A request sends the selected coordinates and the device's network IP to
  Open-Meteo. The onboarding disclosure must say so before the first request.

Official parameter, timestamp, units, and response documentation: [Open-Meteo
Air Quality API](https://open-meteo.com/en/docs/air-quality-api).

## Successful Response Shape

The selected response fields are:

```json
{
  "latitude": 3.0999985,
  "longitude": 101.70001,
  "utc_offset_seconds": 28800,
  "timezone": "Asia/Kuala_Lumpur",
  "current_units": {
    "time": "unixtime",
    "interval": "seconds",
    "us_aqi": "USAQI"
  },
  "current": {
    "time": 1791612000,
    "interval": 3600,
    "us_aqi": 155
  }
}
```

This illustrates the request contract; the numerical reading and timestamp
vary. The live sample recorded in [research.md](../research.md) used the
documentation URL builder's ISO 8601 default rather than this planned
`timeformat=unixtime` request.

- `current.time` is a Unix timestamp in seconds when requested with
  `timeformat=unixtime`. It is the estimate's model-valid time, not a direct
  sensor observation time.
- `current.us_aqi` is numeric. Convert it to a finite double and reject negative
  values. Round to the nearest integer (nonnegative half values round up) for
  category mapping and notification thresholds. Display the rounded integer
  unless the source value is above 500; preserve such above-scale values
  un-clamped in the UI and classify them Hazardous.
- Top-level `latitude` and `longitude` identify the model grid cell used and
  may differ from the saved coordinates. The displayed location remains the
  user's saved place; describe the data as a regional grid estimate.
- The normal response does not provide a definitive upstream model-run
  initialization timestamp. `generationtime_ms` is API response generation
  time and MUST NOT be interpreted as data age.

## Validation and Error Handling

Accept only an HTTP success response with valid JSON, a `current` object, a
finite non-negative numeric `current.us_aqi`, and a parseable numeric
`current.time` no more than five minutes in the future. Record its retrieval
time as the last successful API check even when the model-valid time is old. A
timestamp no more than 18 hours old may replace the displayed estimate only if
it is newer than the displayed model-valid timestamp. When its age is more than
12 and no more than 18 hours, it is display-only: it cannot change the alert
baseline or trigger a notification. Beyond 18 hours, do not accept the returned
estimate; retain the prior displayed estimate and derive its freshness from its
own model-valid time. Alert evaluation requires a strictly newer
model-valid timestamp than the latest displayed estimate and alert baseline, an
age no more than 12 hours, and a genuine upward crossing from the prior
eligible baseline. A successful request or changed retrieval time alone never
triggers an alert. A baseline whose model-valid time is over 12 hours old
expires; the next eligible estimate is a silent baseline.

A well-formed response with a duplicate, older, or more than 18-hour-old model
time updates the successful-check time and status but cannot replace the
estimate or change alert state. A well-formed stale response means the provider
was reached; its model estimate may still be stale.

The public API documentation gives an HTTP 400 JSON error shape containing
`error` and `reason`. Treat non-2xx responses, invalid JSON, missing/null
fields, malformed values, and invalid/future timestamps as failed or invalid
checks. An old but well-formed model time is a successful API check, while the
estimate itself is marked stale or display-only by its age. Treat duplicate or
older model times as a successful data retrieval but not as a newer estimate.
Do not map invalid values to AQI 0, overwrite the last accepted estimate on
duplicate/older data, or trigger an alert. Missing/null behavior is not
specified by the provider; defensive rejection is required.

Keep the last successful API-check time and the displayed estimate's
retrieval/model-valid times distinct. Mark monitoring delayed after more than
two hours without a well-formed response containing valid AQI and model time.
No catch-up notification is
created from failed, display-only, stale, invalid, duplicated, or older data.

## Applying a Response to Local State

Before a request, Android snapshots the saved coordinates, request source, and
durable request revision. The app may coalesce an in-flight manual and scheduled
check for the same revision, but that process-scoped guard only reduces duplicate
traffic. Before applying either a successful response or an error, one atomic
DataStore update verifies that the captured revision is still current and, for
automatic work, monitoring is still enabled. If location or monitoring/alert
settings changed, discard the entire result without changing the current
estimate, alert baseline, last-success time, or monitoring status. Location and
setting changes increment the revision. Persisted revision checks remain the
safety mechanism across worker/process restarts; on restart, work reads current
state before making a new request. Within one current revision, only strictly
newer model-valid timestamps can replace estimates, so out-of-order responses
cannot regress the saved estimate.

## Use Conditions, Quotas, and Attribution

- The free endpoint is limited by [Open-Meteo's terms](https://open-meteo.com/en/terms)
  to non-commercial use. Those terms exclude ads, subscriptions, integration
  into commercial products, and promotional use. The owner confirmed this MVP
  is strictly non-commercial. Any change in purpose blocks release until
  provider and architecture feasibility is reviewed again.
- Published free-use caps are fewer than 10,000 calls/day, 5,000/hour, and
  600/minute; current [pricing](https://open-meteo.com/en/pricing) also lists
  300,000 calls/month for open access. The provider does not clearly define
  distributed mobile-fleet pooling. Target one request per enabled installation
  per hour, avoid retries, and review current limits/terms before broad release.
- Open-Meteo may retain troubleshooting logs containing coordinates and IP
  addresses for up to 90 days under its published terms. User coordinates are
  not sent to an Oh My Haze server.
- Credit Open-Meteo and the CAMS data provider with the displayed estimate or
  on an immediately accessible source/about surface. Link to the API and the
  [CC BY 4.0 license](https://creativecommons.org/licenses/by/4.0/). Data
  attribution does not remove the separate non-commercial limit on free API
  access.

## Provider Semantics

Open-Meteo's current-condition time describes a current model value, not an
exact-point measurement. Its documentation says current values use 15-minute
model data generally; the Kuala Lumpur live response exposed a 3600-second
interval. For Malaysia, the selected CAMS Global source is approximately
45-km/3-hourly and is updated every 12 hours. Hourly app checks therefore do
not imply hourly new source-model updates. `timezone=auto` resolves a timezone
from the coordinates; `timeformat=unixtime` keeps machine comparisons absolute,
while the response timezone name is used only to format times for the user.
