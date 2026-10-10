# Feature Specification: Haze Monitoring MVP

**Feature Branch**: `spec`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: Revise Oh My Haze to monitor a user-saved location with Open-Meteo model-based US AQI estimates and best-effort local alerts.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose a Monitoring Location (Priority: P1)

As a user, I want to choose the place I care about so that the app monitors
air-quality estimates for that saved place without following my movements.

**Why this priority**: A saved location is required before the app can show a
useful estimate or monitor for worsening conditions.

**Independent Test**: Complete onboarding once with foreground location granted
and once with it denied. In both cases, confirm that the user can save a
monitoring location, receive an estimate, and keep that location unchanged while
the device moves.

**Acceptance Scenarios**:

1. **Given** a first-time user opens Oh My Haze, **When** onboarding begins,
   **Then** the app explains that it uses model-based estimates and checks the
   saved location approximately hourly before offering to use the current
   location or choose a place manually.
2. **Given** the user chooses “Use current location”, **When** location is
   required, **Then** the app requests foreground approximate location
   permission, obtains one location fix, and saves the selected coordinates
   locally.
3. **Given** location permission is denied, unavailable, or not preferred,
   **When** the user continues onboarding, **Then** the app offers a built-in
   list of Malaysian localities with bundled coordinates and does not require a
   paid geocoding service, map, or location permission.
4. **Given** a current-location or manual choice is saved, **When** the user
   restarts the app or the device, **Then** the saved monitoring location is
   retained where Android permits and remains the location used by monitoring.
5. **Given** the user travels while a monitoring location is saved, **When** a
   background check runs, **Then** it reuses the saved coordinates and does not
   read or continuously track the device's physical location.
6. **Given** the user explicitly chooses to update the location, **When** they
   select current location or another bundled locality, **Then** only that
   action changes the saved monitoring location and its alert baseline.
7. **Given** the app is about to send a location request, **When** the user
   reviews onboarding, **Then** the app explains that saved coordinates are sent
   directly to Open-Meteo for each check and that the app has no account or
   backend service.

---

### User Story 2 - Understand the Current US AQI Estimate (Priority: P1)

As a user, I want to understand the current US AQI estimate, its age, and what
it represents so I can make sense of conditions near my saved location.

**Why this priority**: A clear, honestly described current estimate is the core
information the app exists to provide.

**Independent Test**: Display a valid estimate, an old estimate, an invalid
response, and an unavailable response. Confirm the US AQI label, category,
location, model-valid time, last check time, attribution, and uncertainty are
clear in each state.

**Acceptance Scenarios**:

1. **Given** a valid current value is available, **When** the home screen opens,
   **Then** it shows the numeric value labelled **US AQI**, its category, the
   saved monitoring location, the model-valid time, the last successful check
   time, and the data source as distinct details.
2. **Given** the estimate is displayed, **When** the user views its details,
   **Then** the app explains that Open-Meteo supplies model-based estimates from
   a regional grid, not a measurement taken at the user's exact location, and
   clearly attributes Open-Meteo and CAMS. It also says the underlying model
   usually updates about every 12 hours, so hourly checks may show the same
   estimate.
3. **Given** the model-valid time is more than 12 but no more than 18 hours old,
   **When** the estimate is displayed, **Then** it remains visible as an older
   estimate with a clear notice that it cannot trigger alerts.
4. **Given** the model-valid time is more than 18 hours old, **When** the estimate
   is displayed, **Then** it is marked stale and cannot trigger a worsening
   notification. The 18-hour cutoff allows a six-hour display buffer around the
   approximately 12-hour model update cadence. A well-formed response still
   updates the last successful API check time, distinct from the stale estimate
   time.
5. **Given** an automatic check has not succeeded for more than two hours,
   **When** the home screen opens, **Then** the monitoring check is separately
   marked delayed regardless of whether the last stored estimate is fresh,
   display-only, or stale.
6. **Given** a newer retrieval fails or omits a usable current value or model
   time, **When** the failure is shown, **Then** the last valid estimate and its
   original timestamps remain visible with a clear stale or unavailable status.
7. **Given** the provider returns a negative, non-finite, malformed, future-dated,
   missing, or null AQI value or timestamp, **When** the response is processed,
   **Then** it is rejected, does not replace the last valid estimate, and cannot
   trigger an alert.
8. **Given** category-boundary values are displayed, **When** the user views the
   reading, **Then** each value has the correct category name and a distinct,
   consistent colour, with severity never conveyed by colour alone.
9. **Given** the provider returns an AQI above 500, **When** it is displayed,
   **Then** the original number is preserved and the category is Hazardous.
10. **Given** the provider returns a finite fractional AQI, **When** it is
   displayed and evaluated for alerts, **Then** category and alert evaluation
   use the nearest whole-number US AQI, with nonnegative half values rounded up
   (for example, 100.5 classifies as 101 and Unhealthy for Sensitive Groups).
   Values above 500 remain visible as returned and classify as Hazardous.
11. **Given** a well-formed response repeats or moves backward from the displayed
    model-valid timestamp but its AQI value differs, **When** it is processed,
    **Then** only the last successful API check time and check status may update;
    the displayed estimate, alert baseline, and notification state do not change.

---

### User Story 3 - Monitor Conditions Automatically (Priority: P1)

As a user, I want monitoring to continue when I am not using the app so I do not
need to repeatedly open it to check whether conditions have worsened.

**Why this priority**: Background monitoring and timely alerts deliver the
primary value proposition.

**Independent Test**: Enable monitoring, background and close the app, restart
the device, and verify saved-location reuse, best-effort check status, and local
state persistence under supported Android conditions.

**Acceptance Scenarios**:

1. **Given** onboarding is complete, **When** monitoring starts, **Then** checks
   are scheduled approximately every 60 minutes on a best-effort basis.
2. **Given** the app is backgrounded, removed from recent apps, or the device
   restarts, **When** Android permits background work, **Then** monitoring
   resumes without promising an exact execution time.
3. **Given** monitoring is enabled, **When** an automatic check runs, **Then** it
   uses the saved coordinates without requesting location again.
4. **Given** the user disables monitoring, **When** a scheduled check would run,
   **Then** no automatic request is made while manual refresh remains available.
5. **Given** Android battery controls delay or prevent work, **When** the user
   opens the app, **Then** the monitoring status and last successful check make
   the delay or unavailability clear.
6. **Given** a request fails, **When** the worker records the failure, **Then**
   it does not enter a rapid automatic retry loop and waits for a later
   scheduled or user-requested check.

---

### User Story 4 - Receive Alerts When Conditions Worsen (Priority: P1)

As a user, I want one local notification when a newer alert-eligible estimate
worsens into a US AQI category, without duplicate or catch-up alerts.

**Why this priority**: Alerts let users learn about worsening conditions
without repeatedly checking the app.

**Independent Test**: Feed the alert logic fresh, duplicate, stale, invalid,
improving, and worsening model-valid readings at every US AQI threshold. Verify
the baseline, sensitivity floor, notification sequence, and rearming behavior.

**Acceptance Scenarios**:

1. **Given** the first alert-eligible valid estimate for a saved location is
   already Unhealthy for Sensitive Groups (US AQI 101 or higher), **When** it
   establishes the baseline, **Then** the app shows an in-app warning and sends
   no worsening notification for that initial baseline.
2. **Given** the default sensitivity floor is 101 and a baseline is below it,
   **When** a newer estimate no more than 12 hours old first crosses 101, 151,
   201, or 301, **Then** exactly one local notification is sent for the highest
   newly reached alert-eligible category, if notifications are enabled and
   permitted.
3. **Given** one alert-eligible estimate jumps across more than one eligible
   category, **When** it is processed, **Then** at most one notification is sent
   for the highest newly reached eligible category.
4. **Given** the user selects a higher sensitivity floor, **When** a newer
   alert-eligible estimate crosses a category below that floor, **Then** no notification is
   sent for that category, while crossings at or above the selected floor remain
   eligible.
5. **Given** an eligible threshold has been reached, **When** later alert-eligible estimates
   remain in that category or worsen without crossing another eligible category,
   **Then** no duplicate notification is sent for that threshold.
6. **Given** an alert-eligible estimate falls below a reached category and later crosses
   that category upward again, **When** the new crossing is processed, **Then**
   that category may alert again.
7. **Given** the first alert-eligible valid estimate is already at or above the selected
   sensitivity floor, **When** it establishes a baseline, **Then** the current
   severity is visible in the app and no catch-up notification is sent.
8. **Given** notification permission or the app notification setting is denied,
   disabled, or restricted, **When** conditions worsen, **Then** the app updates
   its alert state without queuing a later notification; restoring permission
   does not send a catch-up notification.
9. **Given** the previous alert baseline's model-valid time is more than 12 hours
   old, **When** the next alert-eligible estimate arrives, **Then** it establishes
   a new baseline, displays its current severity, and sends no worsening
   notification for that estimate.
10. **Given** a delivered notification is tapped, **When** Oh My Haze opens,
    **Then** the user reaches the current view for the location that generated
    the alert.
11. **Given** a newer valid estimate is more than 12 but no more than 18 hours
    old, **When** it is received, **Then** it may replace the displayed estimate
    but does not change the alert baseline or send a notification.

---

### User Story 5 - Refresh and Manage Monitoring (Priority: P2)

As a user, I want to refresh the estimate and manage a small set of monitoring
and alert settings.

**Why this priority**: Manual checks and a few clear settings make the MVP useful
when the user wants confirmation or needs to control battery and notifications.

**Independent Test**: Refresh with working, slow, and failing network states;
change each setting; and verify subsequent UI, requests, and alert behavior.

**Acceptance Scenarios**:

1. **Given** a saved monitoring location exists, **When** the user requests a
   manual refresh, **Then** the app shows progress followed by a clear success
   or failure state.
2. **Given** a check for that location is already in progress, **When** another
   manual or scheduled check is requested, **Then** the app reuses or joins the
   in-progress request for the same saved-location and monitoring-state version
   instead of issuing a duplicate.
3. **Given** a manual check returns a newer valid estimate, **When** it is
   processed, **Then** it follows the same baseline, freshness, and notification
   rules as an automatic check.
4. **Given** settings are open, **When** the user changes monitoring,
   notifications, sensitivity, or saved location, **Then** the displayed state
   and subsequent behavior reflect that choice.
5. **Given** Android notification permission is unavailable or disabled,
   **When** settings are open, **Then** the app explains that system alerts
   cannot be delivered and shows their availability separately from monitoring.
6. **Given** a user navigates the home screen and settings with assistive
   technology, **When** they inspect severity, monitoring, and controls, **Then**
   each has a descriptive accessible label.
7. **Given** a request is in progress, **When** the saved location or a setting
   that affects monitoring or alerts changes, **Then** its eventual response is
   discarded and cannot change the current estimate, alert baseline, last
   successful check, monitoring status, or notifications.
8. **Given** the process is interrupted after a request starts, **When** work
   resumes, **Then** it reloads the current saved state and rejects any result
   carrying a superseded state version.

### Edge Cases

- The user has no usable current location, denies permission, chooses
  approximate location, or later revokes permission.
- The device travels after onboarding; the saved monitoring location must not
  silently follow it.
- The requested coordinate maps to a nearby model grid cell; the returned grid
  coordinate differs from the saved coordinate.
- The API returns no current object, an absent/null AQI value, a non-finite or
  negative number, an invalid timestamp, a timestamp too far in the future, or
  an HTTP/network error.
- The model-valid time is duplicated, older than the last accepted time, or more
  than 18 hours old even though the API request itself succeeded; a 12-to-18
  hour estimate is display-only and cannot affect alerts.
- The app's last successful check is delayed while the stored estimate remains
  fresh, or a recently successful check returns a model estimate that is stale.
- The global model is between its approximately 12-hour updates; hourly checks
  may return the same estimate and do not prove that a new model run occurred.
- The index lands on any category boundary or exceeds 500.
- A new location is chosen while a check or an alert transition is in progress.
- A request completes after the saved location or a monitoring/alert setting
  changes; its superseded result must not update current state.
- Manual and scheduled responses arrive out of order, or the process restarts
  while a request is pending.
- The user changes sensitivity while an alert category is already reached.
- Android notification permission is denied, revoked, disabled in system
  settings, or restored after an alert was suppressed.
- Android Doze, battery saver, reboot, connectivity loss, or OEM battery
  management delays a periodic check.
- Manual and automatic requests overlap, or the provider returns an error JSON
  with HTTP 400.
- Published API limits are approached or provider terms, pricing, or service
  availability change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Oh My Haze MUST be a native Android app focused on helping users
  monitor air quality around a location they choose.
- **FR-002**: Before location permission is requested, onboarding MUST explain
  why a one-time location fix may be used. The app MUST request only foreground
  approximate location permission and MUST NOT request background location.
- **FR-003**: The app MUST save one selected monitoring location's coordinates
  and display label locally. Background checks MUST use that saved location,
  which MUST remain distinct from the device's continuously changing physical
  location.
- **FR-004**: If current location is denied, unavailable, or not preferred, the
  user MUST be able to select a location from a bundled locality list without a
  paid geocoder, map, account, or location permission. Users MUST be able to
  change the saved location manually at any time.
- **FR-005**: The app MUST NOT continuously track the user. It MUST obtain a
  location fix only after the user chooses to use or update current location,
  and ordinary monitoring MUST NOT access location services.
- **FR-006**: The primary data source MUST be Open-Meteo's Air Quality API using
  the selected latitude and longitude and the current `us_aqi` value. Requests
  MUST use HTTPS directly from the Android app, without an app backend, user
  account, or API key, and MUST NOT fall back to another provider.
- **FR-007**: The free endpoint MUST be used only while Oh My Haze remains a
  strictly non-commercial app. Ads, subscriptions, or use as part of a
  commercial product or promotion are outside this approval; any such change
  MUST trigger a new provider and architecture feasibility review before
  release.
- **FR-008**: Before sending coordinates, the app MUST tell users that their
  saved monitoring coordinates are sent directly to Open-Meteo for estimates.
  It MUST explain that the app has no account or backend and that Open-Meteo may
  log request coordinates and IP addresses under its published privacy terms.
- **FR-009**: The app MUST classify US AQI as follows: 0–50 Good; 51–100
  Moderate; 101–150 Unhealthy for Sensitive Groups; 151–200 Unhealthy;
  201–300 Very Unhealthy; and 301 or higher Hazardous. If Open-Meteo returns a
  value above 500, the app MUST display the un-clamped provider value and use
  Hazardous.
- **FR-010**: The main view MUST show the reading explicitly labelled **US AQI**,
  its category, saved location, data source and attribution, the model-valid
  time, and the last successful check time as distinct information.
- **FR-011**: The app MUST explain that Open-Meteo supplies model-based estimates
  over a regional grid, not measurements taken at the user's exact location.
  It MUST NOT describe the result as an official Malaysian API/IPU reading or as
  an exact local measurement.
- **FR-012**: The app MUST mark an estimate stale when its model-valid time is
  more than 18 hours old and retain and clearly label the last estimate. An
  estimate more than 12 but no more than 18 hours old MAY remain visible but
  MUST be labelled too old to trigger alerts. The screen MUST distinguish the
  last successful API check, estimate model-valid timestamp, and underlying
  model's approximately 12-hour update cadence; hourly polling MUST NOT imply
  hourly new model data. The app MUST separately mark monitoring delayed when
  no well-formed API response with finite non-negative AQI and parseable
  timestamp has arrived for more than two hours. Such a response updates the
  last successful API check time even if its model-valid time is more than 18
  hours old; check delay and estimate staleness MUST be shown separately. A
  retrieval time MUST NOT be presented as the model-valid time.
- **FR-013**: A usable estimate MUST include a numeric, finite, non-negative
  `current.us_aqi` and a parseable `current.time` that is not more than five
  minutes in the future. If the numeric value is fractional, the app MUST round
  it to the nearest whole number (nonnegative halves up) for classification and
  alert evaluation. It MUST display that rounded index except that any source
  value above 500 MUST remain visible un-clamped. The timestamp MUST be
  interpreted using the response timezone or UTC epoch, not the device's default
  timezone. Missing, null, malformed, non-finite, negative, stale, or invalid
  values MUST NOT replace the last valid estimate or affect alert state.
- **FR-014**: The app MUST retain the last valid estimate and its original
  model-valid and retrieval times when a later request fails, is malformed, or
  returns no usable current value.
- **FR-015**: Automatic checks MUST be scheduled approximately every 60 minutes
  as best-effort background work. The app MUST NOT promise exact execution
  times, use a foreground service, use continuous GPS, hold wake locks, or run a
  hosted backend.
- **FR-016**: Monitoring MUST be enabled by default after onboarding. Users MUST
  be able to pause and resume it. The app MUST show whether monitoring is
  enabled, delayed, or unavailable and the time of the last successful check.
- **FR-017**: The app MUST provide a manual refresh action, clear progress and
  failure states, preserve the last valid reading on failure, and avoid
  concurrent duplicate requests for the same saved-location and monitoring-
  state version. Automatic failures MUST NOT cause rapid retry loops.
- **FR-018**: The default notification sensitivity floor MUST be US AQI 101
  (Unhealthy for Sensitive Groups). A simple four-choice setting MUST let users
  select floors of 101, 151, 201, or 301; crossings below the selected floor
  MUST not notify.
- **FR-019**: For a saved location with an established baseline, only a newer,
  valid estimate no more than 12 hours old and with a model-valid timestamp
  newer than the latest displayed estimate and alert baseline may influence
  alert state or send a notification. It MUST cross an alert floor upward from
  the preceding eligible baseline; a successful API request or changed
  retrieval time alone MUST NOT trigger an alert. A single jump across
  categories MUST send at most one local Android notification for the highest
  newly reached eligible category.
- **FR-020**: The first alert-eligible valid estimate for a location MUST establish its
  baseline. If it is US AQI 101 or higher, the app MUST show the severity
  in-app without sending an initial worsening notification. Changing the saved
  location MUST establish a new baseline for that location.
- **FR-021**: Each reached alert category MUST suppress duplicates and rearm
  only after a newer alert-eligible valid estimate returns below its floor (US
  AQI at or below 100 for the 101 floor, 150 for 151, 200 for 201, and 300 for
  301). A category is crossed upward when the previous alert-eligible integer
  AQI was below its floor and the new eligible AQI reaches or exceeds it. The
  first alert-eligible estimate, and the next alert-eligible estimate after a
  baseline's model-valid time is more than 12 hours old, MUST establish a silent
  baseline without a worsening notification. Estimates 12 to 18 hours old may
  update the displayed estimate but MUST NOT change alert state. An alert
  baseline older than 12 hours MUST expire.
- **FR-022**: If notifications are disabled or unavailable, the app MUST update
  current severity and alert state without queuing a later system notification.
  Restoring permission or removing a system restriction MUST NOT produce a
  catch-up notification for a previously suppressed transition.
- **FR-023**: The onboarding explanation MUST include the exact text: “Air
  quality is checked approximately every hour. Android battery settings may
  delay or occasionally prevent checks and alerts.”
- **FR-024**: Preferences, selected coordinates, last valid estimate, and alert
  state MUST be stored on-device. Saved coordinates MUST be excluded from
  Android cloud backup and device-to-device transfer. No remote database,
  analytics, account, advertising SDK, push service, maps, charts, forecast
  screen, or AI feature may be introduced in the MVP.
- **FR-025**: The app MUST provide a clear notification availability status,
  meaningful text labels for every AQI category, and accessible names for
  essential controls and status information.
- **FR-026**: The app MUST respect Open-Meteo's published non-commercial usage
  conditions and request limits. It MUST include clear attribution to Open-Meteo
  and CAMS with the displayed estimate and identify any app-side AQI category
  labeling as a derived presentation.
- **FR-027**: A response MUST be applied only if the saved location and
  monitoring/alert configuration version for which it was requested are still
  current. If either changed before completion, the response MUST be discarded
  atomically and MUST NOT update the current estimate, alert baseline, last
  successful check, monitoring status, or send a notification.

### Key Entities *(include if feature involves data)*

- **Saved monitoring location**: One user-selected label, latitude, longitude,
  and selection source (one-time current location or bundled locality); distinct
  from the device's current physical location.
- **Air-quality estimate**: The Open-Meteo US AQI value and category, saved
  monitoring location, provider/grid coordinates when available, model-valid
  time, last retrieval time, and freshness status.
- **Monitoring preferences**: Monitoring enabled state, notification enabled
  state, chosen alert sensitivity floor, and saved monitoring location.
- **Alert baseline and threshold state**: The latest alert-eligible rounded US
  AQI value and model-valid time for the saved location. New upward crossings
  are derived by comparing that baseline with the next newer alert-eligible
  value; no per-category notification history is required.
- **Monitoring status**: Whether background monitoring is enabled, the last
  successful check time, the last error or delayed/unavailable state, and
  notification availability.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a first-use usability test, at least 90% of participants can
  identify the current US AQI category, saved location, estimate freshness,
  last successful check, and monitoring status within 15 seconds of opening the
  home screen.
- **SC-002**: 100% of tested boundary values (0, 50, 51, 100, 101, 150, 151,
  200, 201, 300, 301, 500, and a value above 500) display the specified US AQI
  category and a visible text label. Fractional inputs round to the nearest
  whole number before the same boundary rules are applied.
- **SC-003**: 100% of estimates more than 18 hours old are marked stale;
  estimates more than 12 and no more than 18 hours old remain labelled
  display-only; and no estimate older than 12 hours triggers a worsening
  notification.
- **SC-004**: For every tested eligible worsening transition, exactly one local
  notification is sent for the highest newly reached eligible category before
  rearming; an initial already-unhealthy baseline and any transition suppressed
  by notification restrictions send zero notifications.
- **SC-005**: In 100% of location-permission-denied trials, a user can complete
  location setup from the bundled locality list and obtain an estimate without
  granting location permission or using a paid external geocoder.
- **SC-006**: In 100% of background check trials, the app reuses the saved
  coordinates and does not continuously access device location. Location only
  changes after a user-requested update.
- **SC-007**: The interface marks the monitoring check delayed after more than
  two hours without a successful request and shows the last success time and
  notification availability in 100% of tested states.
- **SC-008**: In a controlled connected test, at least 95% of manual refreshes
  show a success or understandable failure within 10 seconds; all failed
  refreshes preserve the last valid estimate and timestamps.
- **SC-009**: 100% of manual and scheduled checks with the same saved-location
  and monitoring-state version join or reuse one active request; independently
  completed or out-of-order responses cannot regress the estimate or duplicate
  an alert.
- **SC-010**: At least 90% of usability-test participants understand that the
  displayed US AQI is a model-based regional estimate, not a measurement at
  their exact location or an official Malaysian API/IPU value.
- **SC-011**: At least 90% of usability-test participants can distinguish the
  model-valid time from the app's last successful check time.
- **SC-012**: Deterministic tests prove that responses from superseded location
  or monitoring-state versions cannot change the current estimate, alert
  baseline, successful-check time, monitoring status, or notification output,
  including after a process restart.

## Assumptions

- The MVP remains strictly non-commercial: no ads, subscriptions, or use as part
  of a commercial product or promotional activity. The user confirmed this
  condition for the current plan. A change in commercial intent requires a new
  terms and architecture review before release.
- Open-Meteo's Air Quality API provides the MVP's only air-quality values.
  Results are model-based estimates. They are not official Malaysian API/IPU
  measurements and must never be presented as such.
- Malaysia is the primary audience. The manual fallback is a small bundled list
  of Malaysian localities and coordinates, not a general-purpose place search.
- The app requests `current=us_aqi`, uses `domains=cams_global` for Malaysia,
  and asks Open-Meteo for a timezone resolved from the saved coordinates.
  It does not display hourly or multi-day forecast screens.
- Estimates remain useful to display for up to 18 hours, a six-hour buffer
  around the approximately 12-hour CAMS Global update cadence. Only estimates
  no more than 12 hours old may influence alerts; an older estimate may remain
  visible but cannot alert. This is a conservative age proxy because the API
  does not expose a definitive model-run timestamp. Hourly polling does not
  guarantee hourly new model data.
- An alert baseline expires after 12 hours without a newer alert-eligible
  estimate; the next eligible estimate silently establishes a new baseline.
  Changing locations creates a new baseline rather than comparing values from
  different places.
- A sensitivity floor of 101 is the default because it alerts as soon as the
  estimate reaches Unhealthy for Sensitive Groups; the four category-floor
  choices add a small, bounded setting without introducing a free-form slider.
- Automatic checks are best-effort and may be delayed or prevented by Android,
  device battery policies, network availability, Open-Meteo availability, or
  published API limits. The app does not promise uninterrupted monitoring.
- The initial practical minimum Android version is API 24. The implementation
  plan defines device coverage and validates compatibility before coding.
- Current checks use model time and retrieval time separately. Times are parsed
  using provider timezone metadata and displayed in a clear local format.
- iOS, web/PWA, backend or push services, accounts, advertising, monetization,
  continuous location, maps, charts, forecasts, historical trends, and AI
  health advice are outside the MVP.
