# Feature Specification: Haze Monitoring MVP

**Feature Branch**: `main`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: Initial MVP for Oh My Haze, a native Android app for Malaysian air-quality monitoring and worsening-condition alerts.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose a Monitoring Station (Priority: P1)

As a user, I want a nearby monitoring station selected for me, or to choose one
myself, so that the reading and alerts are relevant to the place I care about.

**Why this priority**: A station is required before the app can provide a local
reading or monitor for worsening conditions.

**Independent Test**: Complete setup with location permission granted and confirm
the nearest available station is selected. Repeat with permission denied and
confirm that manual selection still enables use.

**Acceptance Scenarios**:

1. **Given** a supported Android device, **When** the user installs and opens
   Oh My Haze, **Then** the app presents its native Android setup flow.
2. **Given** setup explains why location is requested and the user grants
   permission, **When** available stations are found, **Then** the nearest station
   is selected and its name and location are shown.
3. **Given** location permission is denied or unavailable, **When** the user
   continues setup, **Then** the app offers manual station selection and does
   not require location permission to show readings.
4. **Given** a station is already selected, **When** ordinary monitoring checks
   run, **Then** the selected station is reused without another location request.
5. **Given** the user has travelled, **When** they explicitly refresh their
   location, **Then** the nearest available station is selected and displayed.
6. **Given** a user-selected station, **When** the app is reopened, **Then** that
   station remains selected.
7. **Given** location is used to choose a station, **When** selection is
   complete, **Then** the app retains the station choice without retaining the
   user's precise coordinates or transmitting them to the data source.

---

### User Story 2 - Understand Current Air Quality (Priority: P1)

As a user, I want to see the current Malaysian API/IPU reading and its age so I
can quickly understand whether conditions are concerning.

**Why this priority**: The current reading and its provenance are the core
information the app exists to provide.

**Independent Test**: Show a valid, stale, and unavailable reading for a selected
station and verify that each state is clearly distinguishable.

**Acceptance Scenarios**:

1. **Given** a valid reading is available, **When** the home screen opens,
   **Then** it shows the API/IPU value, classification, station name and location,
   data source, observation time, and last successful check time as distinct
   timestamps.
2. **Given** a reading is more than two hours older than the current check,
   **When** it is displayed, **Then** it is clearly marked outdated and is not
   described as current.
3. **Given** the source has no usable reading, **When** the home screen opens,
   **Then** it shows an unavailable state and does not substitute another index,
   a forecast, or fabricated data.
4. **Given** a previous valid reading exists and a later retrieval fails,
   **When** the failure is shown, **Then** the previous value and its original
   observation time remain visible with an outdated or unavailable status.
5. **Given** the source returns a negative value, malformed reading, unknown
   station, or future observation time, **When** the response is received,
   **Then** it is rejected as invalid, does not replace the previous reading,
   and cannot trigger an alert.
6. **Given** the public source provides Malaysian station readings,
   **When** a reading is requested, **Then** it is available without an Oh My
   Haze account or private credential.
7. **Given** readings at the category boundaries, **When** they are displayed,
   **Then** each has the correct category, visible text label, and a distinct,
   consistent category colour, with severity never conveyed by colour alone.
8. **Given** a station reading is displayed, **When** the user views its details,
   **Then** the app explains that the reading represents the station's area and
   is not an exact measurement at the user's coordinates.

---

### User Story 3 - Monitor Conditions Automatically (Priority: P1)

As a user, I want monitoring to continue when I am not using the app so I can
learn about worsening conditions without repeatedly checking.

**Why this priority**: Automatic monitoring and timely alerts are the product's
main value.

**Independent Test**: Enable monitoring for a selected station, put the app in
the background, and verify monitoring status, check results, and persistence
across an app restart and a supported device reboot.

**Acceptance Scenarios**:

1. **Given** setup is complete and a station is selected, **When** onboarding
   finishes, **Then** monitoring is enabled by default and the target check
   interval is 60 minutes.
2. **Given** monitoring is enabled, **When** the app is backgrounded, removed
   from recent apps, or the device restarts, **Then** monitoring resumes when
   platform conditions permit and the app does not claim an exact execution time.
3. **Given** a station is already selected, **When** an automatic check runs,
   **Then** it checks that station without repeatedly requesting location.
4. **Given** the user disables monitoring, **When** a scheduled check would run,
   **Then** no automatic check is made; manual refresh remains available.
5. **Given** monitoring is enabled or disabled, **When** the user opens the app,
   **Then** the current monitoring state and last successful check are visible.

---

### User Story 4 - Receive Worsening-Condition Alerts (Priority: P1)

As a user, I want a local notification when a fresh reading crosses into a more
serious API/IPU category, without repeated alerts for unchanged conditions.

**Why this priority**: Alerts deliver the promised value when users are away from
the app.

**Independent Test**: Feed a station a sequence of valid observations across
category boundaries, including repeated, stale, missing, improving, and
station-change cases; verify the notification sequence and in-app status.

**Acceptance Scenarios**:

1. **Given** the first fresh, valid observation for a station is already
   Unhealthy or worse, **When** it is received, **Then** the app shows the
   concerning state but does not send a push notification claiming that
   conditions worsened.
2. **Given** a baseline of Good or Moderate, **When** a newer fresh observation
   enters Unhealthy, **Then** one Unhealthy alert is shown; it appears in the app
   while the app is foregrounded and as a local notification otherwise, if
   notifications are enabled and permitted.
3. **Given** conditions have reached Unhealthy, **When** a newer fresh observation
   enters Very Unhealthy or Hazardous, **Then** one notification is sent for the
   highest newly reached category.
4. **Given** one observation jumps across multiple worsening categories,
   **When** it is processed, **Then** only one alert is shown for the highest
   newly reached category.
5. **Given** observations remain in the same category, are duplicates, are older
   than the last accepted observation, or are stale or invalid, **When** they are
   checked, **Then** no worsening notification is sent.
6. **Given** a previously triggered threshold has been crossed downward,
   **When** a later fresh observation crosses it upward again, **Then** that
   threshold may alert again; each threshold rearms only after conditions fall
   below it.
7. **Given** one or more scheduled checks were missed, **When** the next reading
   arrives, **Then** missed checks alone do not change alert state; only a newer,
   fresh, valid observation can trigger an alert.
8. **Given** the selected station changes, **When** its reading is evaluated,
   **Then** it is compared only with that station's own alert history and never
   with the previous station's reading.
9. **Given** notification permission is denied or notifications are disabled,
   **When** conditions worsen, **Then** no system notification is sent, current
   severity remains visible in the app, monitoring continues, and notification
   status is visible in settings.
10. **Given** a worsening notification is tapped, **When** the app opens,
    **Then** the user reaches the current air-quality view for the station that
    generated the alert.

---

### User Story 5 - Refresh a Reading Manually (Priority: P1)

As a user, I want to request a fresh reading on demand so I can check conditions
without waiting for the next automatic monitoring cycle.

**Why this priority**: A manual check gives users control and makes the app useful
when travelling or when they want confirmation.

**Independent Test**: Request a refresh with a working source and with a failing
source, and verify the loading, success, failure, and retained-reading states.

**Acceptance Scenarios**:

1. **Given** a station is selected, **When** the user activates refresh,
   **Then** the app shows progress and then a clear success or failure result.
2. **Given** a manual refresh or automatic check is already in progress for the
   selected station, **When** another check for that station is requested,
   **Then** the app reuses the in-progress check instead of issuing a duplicate
   request.
3. **Given** a valid prior reading exists and refresh fails, **When** the failure
   is shown, **Then** the prior reading remains visible with its original
   observation time and a clear status.
4. **Given** refresh returns a new valid reading, **When** it is displayed,
   **Then** the reading and its observation and retrieval times are updated; the
   app shows an in-app worsening alert if a threshold is crossed.

---

### User Story 6 - Manage Basic Settings (Priority: P2)

As a user, I want a small set of clear controls so I can manage monitoring,
notifications, and my selected station.

**Why this priority**: Basic controls let users manage battery use and alert
delivery without making setup complicated.

**Independent Test**: Change each available setting and verify that the displayed
state and subsequent monitoring or alert behavior match the user's choice.

**Acceptance Scenarios**:

1. **Given** settings are open, **When** the user changes monitoring or
   notifications, **Then** the updated state is shown and respected.
2. **Given** settings are open, **When** the user views or changes the station,
   **Then** the current selection is clear and a manual replacement is available.
3. **Given** system notification permission is denied, **When** the user views
   settings, **Then** the permission status and its effect on alert delivery are
   explained.
4. **Given** a user navigates the home screen and settings with assistive
   technology, **When** they inspect severity, monitoring state, and controls,
   **Then** each has an accessible text label and usable name.

### Edge Cases

- No nearby stations are returned, or the selected station is no longer available.
- The user denies location permission, turns it off later, or has no usable location.
- The device is offline, the public data source is unavailable, or a response is
  missing required reading, station, or observation-time information.
- A source returns a malformed, negative, future-dated, or duplicate observation.
- A reading is more than two hours old, including after multiple missed checks.
- Monitoring or notification permission is unavailable, revoked, or disabled.
- The app or device restarts while monitoring is enabled or an alert threshold is
  active.
- The user changes stations while one station has stale data or active alert state.
- A manual refresh and an automatic check overlap for the same selected station.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The product MUST be a native Android app focused on Malaysian haze
  and air-quality monitoring.
- **FR-002**: Before requesting location permission, the app MUST explain that
  location is used to select a nearby monitoring station. It MUST request
  location only during setup or after the user explicitly asks to refresh it.
- **FR-003**: When location is available and permitted, the app MUST select the
  nearest available Malaysian monitoring station and show its name and location.
- **FR-004**: The app MUST allow manual station selection when location is denied,
  unavailable, or not preferred. It MUST retain the selected station across
  normal app restarts and MUST NOT continuously track, retain, or transmit the
  user's precise location after using it locally to select a station.
- **FR-005**: Readings MUST come from a free, publicly accessible Malaysian data
  source that requires no account, app credential, or API key. Oh My Haze MUST NOT
  require its own hosted service or hosted database to retrieve readings and MUST
  NOT substitute US AQI or an unidentified forecast for Malaysian API/IPU.
- **FR-006**: The app MUST classify API/IPU values as follows: 0–50 Good; 51–100
  Moderate; 101–200 Unhealthy; 201–300 Very Unhealthy; and above 300 Hazardous.
  Each category MUST use a distinct, consistent colour and a visible text label.
- **FR-007**: For the selected station, the app MUST show the current API/IPU
  value, classification, station name and location, data source, source
  observation time, and last successful retrieval/check time. The two timestamps
  MUST be distinguishable. It MUST explain that a station reading represents
  the station's area, not exact conditions at the user's coordinates.
- **FR-008**: A reading whose observation time is more than two hours old MUST be
  marked outdated. The app MUST preserve and label the last valid reading when
  newer data is unavailable.
- **FR-009**: For missing stations, unavailable sources, connectivity failures,
  or invalid responses, the app MUST show a clear unavailable or error state and
  MUST NOT fabricate a value or present stale data as current. A valid reading
  MUST contain a nonnegative integer API/IPU value, match a known station, and
  include a parseable observation time that is not later than its retrieval time.
- **FR-010**: After onboarding and station selection, background monitoring MUST
  be enabled by default unless the user disables it.
- **FR-011**: The target automatic check interval MUST be 60 minutes. Timing is
  best-effort under Android scheduling; the app MUST NOT promise an exact interval
  or run more than one scheduled automatic check per hour. Manual refreshes are
  excluded from this cap.
- **FR-012**: The app MUST reuse the selected station for automatic checks and
  MUST keep selected-station, monitoring, notification, and alert state on the
  device. It MUST persist those choices and the last successful check across
  normal app restarts and device restarts where Android permits.
- **FR-013**: Users MUST be able to enable or disable automatic monitoring and
  separately enable or disable notifications. The app MUST display monitoring
  status and last successful check.
- **FR-014**: For a station with an established baseline, any fresh observation
  from automatic monitoring or manual refresh MUST alert on a worsening
  transition into Unhealthy (101+), Very Unhealthy (201+), or Hazardous (301+).
  While the app is foregrounded, the alert MUST be shown in the app; otherwise,
  it MUST be sent as a local notification if notifications are enabled and
  permitted. A single observation that jumps across multiple thresholds MUST
  produce at most one alert for the highest newly reached category.
- **FR-015**: The first fresh, valid observation for each station MUST establish
  its baseline. If that reading is already Unhealthy or worse, the app MUST show
  an in-app warning but MUST NOT send a worsening notification solely for that
  first observation.
- **FR-016**: The app MUST suppress repeat alerts while a threshold remains
  reached. Each threshold MUST rearm only after a fresh observation falls below
  it: API/IPU 100 or lower for Unhealthy, 200 or lower for Very Unhealthy, and
  300 or lower for Hazardous.
- **FR-017**: Only a valid observation newer than the last accepted observation
  for that station and no more than two hours old may affect alert state. Missing,
  invalid, stale, duplicate, or older observations and missed checks MUST NOT
  trigger alerts or change the baseline. Alert history MUST be station-specific.
- **FR-018**: If notifications are disabled or system permission is denied, the
  app MUST continue monitoring where permitted, update its alert state without
  queuing a later duplicate system notification, keep current severity visible,
  and explain that system alerts cannot be delivered. Tapping a delivered alert
  MUST open the air-quality view for its station.
- **FR-019**: The app MUST provide a clear manual refresh action with progress,
  success, and failure states. It MUST prevent duplicate concurrent refreshes
  and automatic checks for the same station, reuse an in-progress check where
  possible, and preserve the last valid reading on failure.
- **FR-020**: Settings MUST include monitoring enablement, notification
  enablement, selected-station viewing and change, and notification permission
  status. Advanced threshold customization is outside the MVP.
- **FR-021**: Severity MUST be communicated through text as well as colour, and
  essential controls and status information MUST be accessible to assistive
  technology.

### Key Entities *(include if feature involves data)*

- **Monitoring station**: A Malaysian measurement location with a name, location,
  and availability for selection.
- **Air-quality reading**: An API/IPU value and classification associated with a
  station and named data source, with a source observation time and a separate
  retrieval/check time.
- **Monitoring preferences**: The user's selected station and monitoring and
  notification choices.
- **Alert threshold state**: Per-station record of the latest accepted reading
  and which worsening thresholds have alerted or rearmed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a first-use usability test, at least 90% of participants can
  identify the current API/IPU category, selected station, reading age, and
  monitoring status within 15 seconds of opening the home screen.
- **SC-002**: 100% of tested classification boundary values are displayed in the
  correct Malaysian API/IPU category; each category uses a distinct, consistent
  colour, and observation and retrieval times are shown separately.
- **SC-003**: 100% of readings older than two hours are labelled outdated, and
  no stale, missing, or invalid reading triggers a worsening alert.
- **SC-004**: With notifications enabled and permitted, each eligible worsening
  transition produces exactly one alert for the highest newly reached category
  before rearming; unchanged categories produce no repeat alert, and the first
  already-unhealthy observation produces zero worsening alerts.
- **SC-005**: When notification permission is denied, zero system notifications
  are delivered while monitoring status and current readings remain available.
- **SC-006**: Automatic monitoring makes no more than 24 scheduled checks in any
  24-hour period and makes no repeated location request after a station is
  selected, excluding an explicit user-requested location refresh.
- **SC-007**: In a controlled connected test, at least 95% of manual refreshes
  show a success or understandable failure state within 10 seconds; failed
  refreshes preserve the last valid reading in 100% of cases.
- **SC-008**: 100% of overlapping manual and scheduled checks for the same
  station result in one network request, not duplicate requests.

## Assumptions

- The exact free, keyless public source is not selected in this specification.
  Planning must confirm a source that provides Malaysian station-level API/IPU
  readings, a station directory with locations, and observation timestamps, and
  verify that its publication cadence supports the two-hour stale default. The
  station directory must support local nearest-station selection without
  transmitting the user's precise coordinates.
- The minimum supported Android version and device compatibility matrix are
  planning decisions and must be set before implementation.
- Two hours is the initial stale cutoff because the target monitoring cadence is
  hourly; it may be adjusted during planning if the source's published cadence
  requires it.
- If the first observation is already Unhealthy or worse, an in-app warning is
  sufficient; no worsening notification is sent until a later threshold crossing.
- A newly selected station with no prior history establishes its own baseline.
  Returning to a previously selected station resumes that station's alert state.
- A manual refresh updates the visible reading and station baseline. If it finds
  a worsening transition while the app is open, the app shows an in-app alert
  instead of a system notification.
- Monitoring can be delayed or unavailable due to Android scheduling, device
  state, permissions, connectivity, or source availability; the interface must
  describe the actual status without promising uninterrupted checks.
- Times are shown in the user's local time zone. iOS, web/PWA, server-driven
  push, continuous GPS, forecasts, historical charts, maps, accounts,
  monetization, and AI-generated health advice are outside this MVP.
