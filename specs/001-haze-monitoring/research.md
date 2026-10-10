# Current Phase 0 Research: Open-Meteo Air Quality API

**Checked**: 2026-10-10 (UTC; client timezone Asia/Kuala_Lumpur)
**Decision**: **FEASIBLE for the approved non-commercial MVP, with documented
limitations.** The old DOE APIMS source investigation remains below as
historical research and is superseded for the MVP.

## Decision Summary

- **Decision**: Use Open-Meteo's Air Quality API with saved WGS84 latitude and
  longitude; request `current=us_aqi`, `domains=cams_global`, `timezone=auto`,
  and `timeformat=unixtime`.
- **Rationale**: The official API documents coordinate parameters and a
  current US AQI variable. Anonymous access is allowed without an API key for
  non-commercial use, which the user confirmed is the intended use. A real
  request for a sample Kuala Lumpur coordinate returned a numeric current US
  AQI value and model-valid time.
- **Alternatives considered**: DOE APIMS is retained as historical research but
  is not the MVP source. No second provider or backend was introduced. The
  paid Open-Meteo customer API is not used because the approved MVP is
  non-commercial, has no backend, and must not ship a recoverable API key in an
  Android package.
- **Feasibility condition**: The free endpoint remains appropriate only while
  the app is strictly non-commercial, with no ads, subscriptions, commercial
  product integration, or promotional use. A future change in that purpose is a
  provider and architecture gate.

## Official API Contract and Live Request

Official documentation: [Open-Meteo Air Quality API](https://open-meteo.com/en/docs/air-quality-api).
The endpoint accepts required WGS84 floating-point `latitude` and `longitude`
query parameters. `current` selects current-condition variables, and the
documented variable is `us_aqi`. The recommended implementation request is:

```text
GET https://air-quality-api.open-meteo.com/v1/air-quality
  ?latitude={savedLatitude}
  &longitude={savedLongitude}
  &current=us_aqi
  &domains=cams_global
  &timezone=auto
  &timeformat=unixtime
```

For one live request on 2026-10-10, the official documentation's API URL builder
was configured for Kuala Lumpur (`latitude=3.139`, `longitude=101.6869`) and
`current=us_aqi`. The returned JSON included:

```json
{
  "latitude": 3.0999985,
  "longitude": 101.70001,
  "utc_offset_seconds": 0,
  "timezone": "GMT",
  "current_units": {
    "time": "iso8601",
    "interval": "seconds",
    "us_aqi": "USAQI"
  },
  "current": {
    "time": "2026-10-10T14:00",
    "interval": 3600,
    "us_aqi": 155
  }
}
```

The request was read at approximately 2026-10-10 14:26 UTC. It was a successful
anonymous GET with no API key; `current.us_aqi` was numeric and the response
included a valid model time. The returned model-grid coordinate was roughly
4.5 km from the requested point. This confirms coordinate-based current US AQI
availability for the sample, not accuracy or availability across all locations
and times. The actual request URL was
[`air-quality-api.open-meteo.com/v1/air-quality?latitude=3.139&longitude=101.6869&current=us_aqi`](https://air-quality-api.open-meteo.com/v1/air-quality?latitude=3.139&longitude=101.6869&current=us_aqi).

The live request used the documentation builder's default ISO 8601/GMT
response. The implementation contract adds `timezone=auto` and
`timeformat=unixtime` for coordinate-local presentation and absolute timestamp
comparisons.

The shell's direct `curl` attempt and a direct browser navigation were blocked by
this execution environment. The live request itself succeeded when opened from
Open-Meteo's own URL builder. No deliberately missing-value response was
produced; the public API documentation does not define missing/null AQI
behavior. Therefore absence, null, non-finite values, invalid timestamps, or
negative AQI must be treated as invalid and must not be coerced to zero or used
for alert state.

## Current Conditions, Timestamps, Forecasts, and Timezones

- `current=us_aqi` is the right source field for the home screen. The endpoint
  also supports `hourly=us_aqi` and returns multi-day model forecast series, but
  the MVP does not request or display those forecast screens.
- The API's `current.time` is a model-valid time, not an instrument observation
  at the user's coordinate. The normal current response does not expose an
  upstream model-run initialization timestamp. `generationtime_ms` measures
  API/forecast generation time and is not the model's age.
- The documentation's general current-conditions note says current values are
  based on 15-minute model data. The live Kuala Lumpur response returned
  `current.interval=3600` seconds. Separately, the documented CAMS Global source
  used for Malaysia has a 3-hour model timestep and refreshes every 12 hours.
  These describe different processing/output layers; the response does not
  identify the exact underlying model run. Therefore hourly app checks do not
  imply hourly source-model updates.
- The official data-source table lists CAMS Global Atmospheric Composition
  Forecasts as global at about 45 km spatial resolution, 3-hour temporal
  resolution, updated every 12 hours, with a 5-day forecast horizon. Open-Meteo
  states that returned coordinates identify the grid-cell center and can be a
  few kilometres from the requested point. The UI must describe regional model
  estimates and avoid claims of exact-local measurement.
- The API documents `us_aqi` as numeric. If it returns a fractional value, the
  MVP normalizes to the nearest whole index before applying integer category
  boundaries. The current [US EPA AQI technical assistance document](https://www.airnow.gov/publications/air-quality-index/technical-assistance-document-for-reporting-the-daily-aqi/)
  says to round calculated index values to the nearest integer.
- ISO timestamps default to GMT if no `timezone` is supplied. The docs allow
  IANA time zones and `auto` (resolve the timezone from coordinates) and return
  `timezone` and `utc_offset_seconds`. The live test omitted `timezone` and
  returned GMT, confirming why the app must request `timezone=auto`. The MVP
  requests `timeformat=unixtime` for absolute comparisons and uses the returned
  timezone name to display local time rather than the device's current timezone.
- **Freshness decision**: Mark an estimate stale when the age of its
  model-valid timestamp exceeds 18 hours. The 18-hour product cutoff gives a
  six-hour buffer around the published 12-hour model update cadence. Also show
  app retrieval age independently, and show monitoring as delayed after more
  than two hours without a successful app check. Because the provider does not
  expose a model-run time, the stale rule cannot prove that an upstream cycle
  was refreshed on schedule; this limitation must remain visible in the plan.

## Access, Authentication, Terms, and Limits

- **Anonymous access**: The free public endpoint requires no account, API key,
  or signup for non-commercial use. The Air Quality API documentation says
  `apikey` is needed only for commercial customer resources on a
  `customer-` endpoint. A live anonymous request succeeded.
- **Commercial use**: [Open-Meteo Terms](https://open-meteo.com/en/terms) limit
  the free API to non-commercial use. They list private or non-profit apps
  without subscriptions or ads as examples of non-commercial use, and identify
  subscription/advertising apps and integrations into commercial products or
  promotional activities as commercial use. The user confirmed Oh My Haze is
  strictly non-commercial for this plan. Any changed intent would make the
  free endpoint incompatible. A paid customer API would require a key; shipping
  that key in a distributed Android app would expose it, and a backend is
  explicitly outside this MVP.
- **Published free-tier limits**: Terms state less than 10,000 API calls/day,
  5,000/hour, and 600/minute. [Pricing](https://open-meteo.com/en/pricing)
  also lists 300,000 calls/month for the open-access tier. A typical HTTP
  request is one API call; broad/multi-variable requests can count as more.
  The MVP requests one location and one AQI field, avoids rapid automatic
  retries, and targets one scheduled request/hour per installation (~24/day,
  720–744/month before manual checks). Public terms do not clearly state how a
  distributed mobile app's fleet volume is pooled versus per-IP accounting;
  treat overall capacity at scale as a risk and do not claim a fleet quota.
- **Service reliability**: The free endpoint has no uptime guarantee, and the
  provider reserves the right to block misuse. Provider accuracy, completeness,
  and uninterrupted availability are not warranted. The app must show
  unavailable/delayed states and must not offer a guaranteed alert SLA.
- **Direct Android suitability**: The official API is a simple HTTPS GET with
  query parameters and JSON, explicitly intended to be copied into an
  application. A small anonymous request is technically suitable for direct
  Android use while non-commercial terms remain satisfied. No app backend or
  client credential is needed. Each request exposes the saved coordinates to
  Open-Meteo; the provider's [privacy terms](https://open-meteo.com/en/terms)
  state that free-service troubleshooting logs may contain coordinates and IP
  addresses for up to 90 days. This must be disclosed before first request.
- **Local backup**: Android Auto Backup includes most internal app files by
  default for apps targeting API 23+ and supports explicit inclusion/exclusion
  rules. Since saved coordinates must not transfer through cloud backup or
  device-to-device migration, configure exclusions for both paths. See [Android
  Auto Backup documentation](https://developer.android.com/identity/data/autobackup).

## Attribution and License

- Open-Meteo says its data are under [Creative Commons Attribution 4.0
  International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/).
- The Air Quality API documentation requires clear attribution to the CAMS
  ENSEMBLE data provider and a reference to Open-Meteo. The main view or its
  immediately accessible details and an About/Source view should credit both.
- Suggested concise attribution: “Air-quality model data: Copernicus
  Atmosphere Monitoring Service (CAMS), delivered by Open-Meteo.com. US AQI
  categories are shown by Oh My Haze.” Include links to the source and license.
  Mark category labels as the app's presentation of the returned US AQI value.
- The CC BY 4.0 data license does not grant commercial-use rights to the free
  API service; the non-commercial Terms remain a separate condition.

## Android Compatibility and Runtime Constraints

- The AndroidX [WorkManager release notes](https://developer.android.com/jetpack/androidx/releases/work)
  list version 2.12.0 as stable on 2026-09-23 and raise the library's minimum
  SDK to API 24. API 24 (Android 7.0) is therefore the practical MVP floor if
  the project uses the current stable WorkManager dependency; recheck at
  implementation time.
- Android [periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
  is deferrable and inexact. WorkManager satisfies persistent best-effort
  scheduling but cannot guarantee an hourly check time or alert SLA.
- Android location guidance supports the intended foreground-only permission
  path; Android 12+ users can grant approximate location. Request a bounded
  one-time current fix only after an explicit user action and never request
  background location. Android 13+ runtime notification permission is separate
  and can be denied while checks continue.
- For privacy, Android [Auto Backup](https://developer.android.com/identity/data/autobackup)
  backs up most internal app files by default and its Android 12+ data-extraction
  rules support cloud and device-to-device transfer policies. The app must
  explicitly exclude the coordinate-containing DataStore file from both.

## Feasibility Decision and Risks

The Open-Meteo source passes the MVP feasibility gate for the user-confirmed
non-commercial purpose. It provides coordinate-based current US AQI, permits
anonymous access at the proposed per-install check rate, and supports direct
HTTPS client requests. There is no current blocker to completing planning.

Material risks and limits:

1. **Coarse regional grid**: CAMS Global is about 45 km resolution and can use a
   returned grid center away from the saved coordinate. The app is informative,
   not a substitute for a nearby monitor or official local reading.
2. **Model cadence and timestamp**: Published global data refresh every 12
   hours; the normal API response lacks the source-run timestamp. An 18-hour
   valid-time stale marker is a practical approximation, not proof of source
   freshness. Hourly polling cannot make the underlying model refresh faster.
3. **Free service scale and reliability**: Limits apply to free usage and the
   service has no uptime guarantee. Exact distributed-fleet quota pooling is
   not stated publicly; broad release volume needs a fresh terms/capacity review.
4. **Non-commercial-only service access**: Any future ads, paid app, subscription,
   commercial integration, or promotion breaks the approved free-tier
   assumption. Do not quietly ship a customer API key in the app.
5. **Location privacy**: The device stores coordinates locally but sends the
   selected monitoring point and network IP to Open-Meteo on each request; the
   public privacy statement says logs may retain them up to 90 days.
6. **Missing-value behavior**: No missing/null behavior is specified by the API
   docs and only a valid live sample was observed. Defensive parsing and
   invalid-value suppression are required.

## Sources

- [Open-Meteo Air Quality API documentation](https://open-meteo.com/en/docs/air-quality-api)
- [Open-Meteo Terms and Privacy](https://open-meteo.com/en/terms)
- [Open-Meteo pricing and API limits](https://open-meteo.com/en/pricing)
- [Open-Meteo model update overview](https://open-meteo.com/en/docs/model-updates)
- [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/)
- [Android WorkManager release notes](https://developer.android.com/jetpack/androidx/releases/work)
- [Android periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Android location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)
- [Android DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [US EPA AQI technical assistance document](https://www.airnow.gov/publications/air-quality-index/technical-assistance-document-for-reporting-the-daily-aqi/)
- [Android Auto Backup](https://developer.android.com/identity/data/autobackup)

---

# Historical Research — DOE APIMS Timestamp Semantics (Superseded for MVP on 2026-10-10)

The following findings describe the prior DOE APIMS direction only. They are
retained for history and are not current MVP requirements or a blocker for the
Open-Meteo plan.

**Checked**: 2026-10-09 (Asia/Kuala_Lumpur)
**Timestamp classification**: **CONDITIONAL (strong evidence; provider contract unconfirmed)**. Live values match Malaysia wall-clock hours encoded in a UTC epoch, including a matched station/hour on the public APIMS display. This conflicts with ArcGIS metadata, and the current display bundle contains two timestamp-formatting modes. A fixed UTC+8 normalization is plausible but not safe to ship without provider confirmation.

## 1. ArcGIS Field and Time-Reference Metadata

Queried the public layer metadata directly at:

```text
https://eqms.doe.gov.my/api3/publicmapproxy/PUBLIC_DISPLAY/CAQM_MCAQM_Current_Reading/MapServer/0?f=json
```

Observed metadata:

- Layer: `CAQM-MCAQM Current Reading` (Feature Layer), ArcGIS current version 11.3.
- `DATETIME`: `esriFieldTypeDate`, length 8.
- `dateFieldsTimeReference`: `null`.
- `preferredTimeReference`: `null`.
- `datesInUnknownTimezone`: `false`.
- `timeInfo`: `null`.
- `copyrightText`: empty.

Esri's [ArcGIS REST query documentation](https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer/) says a null `dateFieldsTimeReference` means date values are assumed to be UTC. The service does not mark this date as an unknown timezone or declare Malaysia time. On metadata alone, clients must interpret the epoch as UTC.

## 2–3. Raw Values, Current Time, and Station Comparison

Two live query snapshots, including a station-filtered query and a query of the full layer, showed the same eight-hour pattern:

| HTTP response date (UTC) | Raw `DATETIME` | Epoch interpreted as UTC | Malaysia local clock then | If raw UTC components are Malaysia wall time |
|---|---:|---|---|---|
| 2026-10-09 08:14:43 | `1791561600000` | 2026-10-09 16:00Z, 7h45m in the future | 16:14 | 2026-10-09 08:00Z, 14m old |
| 2026-10-09 09:54:19 | `1791565200000` | 2026-10-09 17:00Z, 7h06m in the future | 17:54 | 2026-10-09 09:00Z, 54m old |

The second full-layer query returned **68 features, all with `DATETIME=1791565200000`**. Records across different states included `CA01R` (API 81, Moderate), `CA03K` (API 152, Unhealthy), `CA06P` (API 147, Unhealthy), and `CA08P` (API 154, Unhealthy). Thus the offset is consistent across the sampled stations and two hourly timestamps. Under the local-wall-clock interpretation, the corrected instants are plausible hourly observations 14 and 54 minutes old; under the metadata-declared UTC interpretation, both are future data and must be rejected by the MVP.

This is strong evidence of a consistent +08:00 interpretation, not proof that every historical/future value follows it. The sample spans two adjacent hourly values; it is not a long-duration or multi-day validation.

## 4–6. Official Display and Likely Cause

[DOE press statements](https://www.doe.gov.my/wp-content/uploads/2021/09/10.-Kenyataan-Akhbar-NRE-Response-to-NST-Dr-Eliani-MD-18-Oktober-2017.pdf) refer to hourly readings on the official APIMS page. The legacy APIMS URL now redirects to the [public MyEQMS site](https://eqms.doe.gov.my/). At 2026-10-09 17:58 MYT, its APIMS dashboard showed 68 stations, an hourly table spanning 2026-10-08 17:00 through 2026-10-09 17:00, and the WP Putrajaya trend chart's final point at 2026-10-09 17:00 (API 170, station `CA17W`). A direct query to the ArcGIS layer at 17:58:51 MYT returned `CA17W`, Putrajaya, API 170.0, and `DATETIME=1791565200000`. Interpreting the epoch's UTC components as MYT gives 2026-10-09 17:00, matching the official display; interpreting it as a UTC instant gives 2026-10-10 01:00 MYT, in the future. This is an independent, same-station/same-value/same-hour match, though it does not prove the display and layer share an identical timestamp pipeline.

The current page loads [`app.74538535.js`](https://eqms.doe.gov.my/js/app.74538535.js). Its feature-detail code formats `DATETIME` in one of two ways:

- When `mapID` is `apims` and `window.publicportal.useClientSideApims` is true, it constructs `new Date(value)` and uses `Intl.DateTimeFormat` with `timeZone: "Asia/Kuala_Lumpur"`.
- Otherwise, it formats the epoch's UTC date/hour components directly with `getUTCDate()` and `getUTCHours()`.

The runtime flag is not present in the retrieved page shell, and the active mode and its relationship to the displayed table/chart remain unknown. The matched live display supports the local-wall-clock interpretation, but the bundle also contains an APIMS-specific branch that treats the epoch as a true instant and localizes it. Therefore the UI evidence does not establish the endpoint's formal timestamp contract.

The repeated multi-station pattern and matched public display strongly suggest that Malaysia wall-clock hours are being encoded in a UTC epoch. Public evidence cannot distinguish upstream naive-local-time serialization from an ArcGIS time-reference/configuration issue, or conclusively rule out a defect in the source feed. The ArcGIS JSON response itself remains a future UTC instant under its metadata. No DOE field note explains a timezone convention or workaround.

## 7. Normalization Assessment

A deterministic candidate rule would be:

1. Interpret the epoch's UTC date/time components as a `LocalDateTime`.
2. Resolve that local value in `Asia/Kuala_Lumpur`.
3. Convert the result to an absolute UTC instant.

For the sampled data, this is equivalent to subtracting eight hours and yields plausible fresh observations that agree with the official APIMS display. The page's runtime mode and the endpoint's formal contract remain unknown. **Do not silently apply this rule in the app.** It is safe only after DOE/JAS confirms that the field is Malaysia local time serialized as a UTC epoch, or corrects the service metadata/values so standard UTC parsing is valid.

## Smallest Additional Validation Needed

The smallest decisive validation is a written DOE/JAS confirmation (or an official API note/corrected layer) that `DATETIME` in this exact public layer is Malaysia local wall-clock time encoded as a UTC epoch, and that interpreting its UTC components in `Asia/Kuala_Lumpur` is the supported rule for all current station rows. Ask DOE to reconcile that rule with the layer's null UTC time-reference metadata and identify which public-display mode is authoritative. The live matched `CA17W` sample already provides a second-party display comparison; a new sample alone would not resolve the contract conflict. If DOE confirms/corrects the contract, classify **PASS**. If DOE says values are UTC, or cannot confirm the rule, classify **FAIL** and keep the source blocked.

## Other Phase 0 Source Findings

- Direct anonymous HTTPS queries to the APIMS layer returned live Malaysian API/IPU fields, 68 stations, station names/coordinates, and station IDs. No key or account was sent. A physical Android device was not connected, so direct on-device access is still unverified.
- DOE's [Government Open Data page](https://www.doe.gov.my/en/government-open-data/) describes IPU data as open data intended for free use, reuse, and sharing. However, the live layer has no endpoint-specific license, request quota, uptime commitment, or SLA. DOE's [environment data application conditions](https://www.doe.gov.my/en/application-for-environment-data/) do not clearly state how they apply to this public map feed. Attribution to DOE/JAS and APIMS is prudent.
- At one request per hour, traffic is 24 requests/device/day: approximately 24,000/day for 1,000 installs, 240,000/day for 10,000, and 2.4 million/day for 100,000. No published APIMS quota exists to establish capacity at those volumes.
- The official data.gov.my [air-pollution catalogue](https://data.gov.my/data-catalogue/air_pollution) is monthly pollutant concentrations, not live API/IPU. Its CC BY 4.0 license does not automatically cover the APIMS layer.
- Anonymous WAQI/AQICN and IQAir GETs failed with invalid/missing-key errors; both providers' documentation requires API keys. Their documented AQI/usage terms do not meet this MVP's keyless Malaysian API/IPU requirement.

## Historical APIMS Gate at the Time of That Review

At the time of the original APIMS review, timestamp semantics were
**CONDITIONAL**, and that source-specific gate was not passed. This finding is
superseded for the MVP by the Open-Meteo decision and its feasibility review
above. Do not carry the APIMS timestamp blocker forward to the current plan.
