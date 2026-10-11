# Feature Specification: AQI Dashboard and Location Update

**Feature Branch**: `codex/aqi-dashboard`

**Created**: 2026-10-11

**Status**: Draft

**Input**: User description: Complete the final planned Oh My Haze MVP UX iteration by integrating the supplied app icon and AQI illustrations, presenting a compact and accessible AQI dashboard with category-specific health guidance, and allowing users to choose their current location from the location-change screen.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Understand air quality at a glance (Priority: P1)

As a user, I want the home screen to make my saved place's current AQI and
monitoring state easy to scan so that I can understand the situation without
reading a long block of technical text.

**Why this priority**: The home screen is the app's primary value. The estimate,
its category, and whether it is current must be clear before secondary details.

**Independent Test**: Show each of the six AQI categories, a missing estimate,
an old estimate, and a delayed check. Confirm the saved place, AQI value,
category, matching illustration and gauge marker are clear; the last-check time
and monitoring status remain distinct; and no technical source details appear
on the dashboard.

**Acceptance Scenarios**:

1. **Given** a saved location and valid estimate, **When** the home screen opens,
   **Then** it shows the location, a compact location-change action, monitoring
   status, settings action, live AQI number, the matching full category
   illustration, and a six-category color gauge whose marker identifies the
   current category.
2. **Given** an estimate is available, **When** the user views its status,
   **Then** the last successful check is labeled “Last checked”; estimate
   freshness and monitoring-delay states remain distinct; and model-specific
   timestamps and source details are not shown on the dashboard.
3. **Given** any AQI category is displayed, **When** the illustration appears,
   **Then** it shows the complete matching supplied artwork, including the
   cloud scene, category name, AQI range, and recommendations. The live AQI
   number and status remain dynamic interface text; a separate visible category
   heading or recommendation panel is not repeated beside the artwork.
4. **Given** no usable estimate exists or a previous estimate is old,
   **When** the home screen opens, **Then** the screen clearly identifies the
   missing, older, or stale state, preserves the existing last-check and
   error information, and keeps manual refresh available.
5. **Given** the home screen is opened on a standard portrait phone at normal
   font size, **When** an estimate and its core status are available, **Then**
   the location, status, live AQI value, gauge, complete category artwork,
   last-check state, and refresh action fit in the initial view without
   requiring scrolling.
6. **Given** a smaller or landscape display or enlarged text is used,
   **When** the home screen content exceeds the available space, **Then** the
   user can scroll to all content and no text or control is clipped.
7. **Given** the device uses a light or dark appearance, **When** Oh My Haze is
   opened, **Then** the dashboard and secondary screens use the matching theme
   while keeping text, controls, and AQI severity distinguishable.
8. **Given** the app is viewed in the launcher on supported Android versions,
   **When** the launcher applies its icon mask or themed-icon treatment,
   **Then** the supplied app icon remains recognizable and is not clipped or
   distorted.

---

### User Story 2 - Get proportionate advice for the AQI category (Priority: P1)

As a user, I want short recommendations that change with the displayed AQI
category so that I can make a practical choice about outdoor activity.

**Why this priority**: A category is easier to act on when it is paired with
concise guidance that does not exaggerate low readings or overstate what a
household measure can do.

**Independent Test**: Display values in all six AQI categories and verify that
each category receives the recommendations in its matching full artwork, that
the copy changes with the category, and that assistive technology can read each
recommendation from matching app text semantics.

**Acceptance Scenarios**:

1. **Given** a current estimate is assigned to any of the six AQI categories,
   **When** the home screen displays it, **Then** the full matching artwork
   shows its three concise recommendations and changes whenever the displayed
   estimate changes category.
2. **Given** the category is Good or Moderate, **When** recommendations are
   shown, **Then** they describe ordinary activity as generally acceptable and
   avoid overstating health risks.
3. **Given** the category is Unhealthy for Sensitive Groups or higher,
   **When** recommendations are shown, **Then** advice becomes progressively
   more protective and distinguishes sensitive groups from the general public
   where appropriate.
4. **Given** particle-specific protections are mentioned, **When** the text
   refers to filtration or a respirator, **Then** it describes filtration as a
   way to reduce particles rather than eliminate all indoor pollution, and any
   particulate respirator advice is conditional on particle or smoke exposure
   and describes a well-fitting particulate respirator rather than an ordinary
   surgical mask.
5. **Given** a recommendation is read with an accessibility service,
   **When** the service reaches the category artwork, **Then** the category
   name, range, summary, and all recommendation wording are available through
   matching app text semantics without being announced twice.
6. **Given** health guidance is displayed, **When** the user reads it, **Then**
   it is identified as general guidance, not a
   diagnosis or individualized medical advice, and pollutant-specific guidance
   may differ.

---

### User Story 3 - Use current location when changing the saved place (Priority: P1)

As a user, I want to choose my current location from the location-change screen
so that I can monitor a new nearby place without searching through the locality
list.

**Why this priority**: Users who relocate their monitoring location should be
able to make the same deliberate, one-time location choice available during
onboarding, while retaining a manual fallback.

**Independent Test**: Open location change, select the current-location action
with permission already granted and with permission missing, and verify that a
single foreground fix is saved. Repeat with permission denied, location
unavailable, and a manual locality choice; verify understandable feedback and
that failed fixes do not replace the saved place.

**Acceptance Scenarios**:

1. **Given** the user opens the location-change screen, **When** its options are
   shown, **Then** a prominent “Use my current location” action appears above
   the manual locality list.
2. **Given** the user chooses that action and approximate foreground permission
   is not granted, **When** location access is needed, **Then** the app requests
   approximate location permission only at that moment; when permission is
   already granted, the app proceeds without another prompt.
3. **Given** permission is granted, **When** the app obtains one bounded
   location fix, **Then** it saves the returned coordinates as the selected
   monitoring location and returns to the dashboard.
4. **Given** permission is denied, location is unavailable, or a fix fails,
   **When** the user remains on the location-change screen, **Then** the app
   displays an understandable error, leaves the existing saved location
   unchanged, and keeps manual locality selection available.
5. **Given** the user chooses a bundled locality instead, **When** they save
   that choice, **Then** the existing manual fallback continues to work without
   location permission.
6. **Given** a saved location is changed while an API request is in flight,
   **When** the response returns, **Then** existing request-revision protections
   prevent it from changing the new location's estimate, baseline, check
   status, or notification state.
7. **Given** any location flow is used, **When** the selection is saved,
   **Then** the app does not enable continuous tracking or request background
   location permission.

### Edge Cases

- A user denies or later revokes approximate-location permission, disables
  device location, or receives no location fix before the request times out.
- A location fix contains invalid coordinates or a provider failure; the
  existing saved location and monitoring state must remain intact.
- A user opens the location picker from onboarding versus settings; cancellation
  and manual fallback must return to the correct screen.
- A current-location request is repeated while one is already in progress.
- A manual or scheduled estimate request is pending when a new location is
  saved.
- There is no estimate yet, the last estimate is stale, or the API check is
  delayed independently of estimate freshness.
- AQI values are at category boundaries or above 500; the established AQI
  classification remains unchanged.
- The portrait category artwork becomes tall at narrow widths; it remains
  fully visible and reachable by scrolling rather than being cropped.
- The artwork's embedded English copy remains at its authored size; native
  AQI/status text and controls continue to respond to font scaling, and all
  category copy remains available to accessibility services.
- A launcher does not support themed icons; the normal supplied app icon must
  still be shown.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The home screen MUST prioritize the saved monitoring location,
  monitoring status, live AQI number, full category artwork, six-part category
  gauge, last-check state, and refresh action. The complete artwork supplies
  the visible category name, range, summary, and recommendations.
- **FR-002**: The home screen MUST label the displayed value “AQI” and show the
  last successful check with the label “Last checked.” It MUST keep estimate
  freshness and monitoring-delay status separate, and MUST NOT display
  model-valid timestamps, model update cadence, or provider/source details on
  the dashboard.
- **FR-003**: The app MUST preserve the current AQI category ranges and display
  labels: 0–50 Good; 51–100 Moderate; 101–150 Unhealthy for Sensitive Groups;
  151–200 Unhealthy; 201–300 Very Unhealthy; and 301 or higher Hazardous.
- **FR-004**: The app MUST map each complete supplied AQI illustration to its
  matching category and show the full artwork without cropping its category
  name, AQI range, summary, cloud scene, or recommendations. The live AQI number
  and status remain dynamic; the dashboard MUST NOT add a separate visible
  category heading or recommendation panel that duplicates the artwork.
- **FR-005**: The app MUST use the supplied application icon as its launcher
  icon, support adaptive icons on Android versions that support them, and
  include a themed monochrome layer where the supplied art can be adapted
  without obscuring or distorting the original icon.
- **FR-006**: At normal text size on a standard portrait Android phone, the
  location, monitoring status, live AQI value, gauge, full category artwork,
  last-check state, and refresh action MUST be visible without scrolling.
  Narrow screens, landscape orientation, and increased font size MUST remain
  usable through responsive layout and scrolling; the artwork MUST retain its
  aspect ratio and remain whole, and scrolling MUST NOT be disabled to meet the
  one-screen objective.
- **FR-007**: The app MUST support light and dark appearance with legible text,
  controls, severity labels, and status indicators in both.
- **FR-008**: The app MUST display the three recommendations embedded in the
  matching full artwork and update that artwork with the estimate's category.
  Matching wording MUST also be stored as app text or structured data for
  accessibility and consistency; the app MUST NOT show a second visible
  recommendation panel.
- **FR-009**: Recommendation wording MUST be proportionate to the category,
  MUST NOT diagnose a user, MUST NOT imply ordinary surgical masks are
  particulate respirators, and MUST NOT claim an air cleaner removes all indoor
  pollution. Particle-specific respirator or filtration guidance MUST be
  conditional on particle or smoke exposure. Embedded poster wording MUST meet
  the same requirements before release; if wording changes, its matching image
  and app text MUST be updated together.
- **FR-010**: The dashboard MUST omit model-specific explanation, model-valid
  timestamps, source names, and source links. Settings MUST provide a compact
  “Data attribution” entry that credits the Copernicus Atmosphere Monitoring
  Service (CAMS) and Open-Meteo and links the CC BY 4.0 license.
- **FR-011**: The location-change screen MUST place a prominent “Use my current
  location” action above the bundled manual locality list.
- **FR-012**: The current-location action MUST reuse the existing foreground
  location provider, request approximate location permission only when needed
  after explicit user action, obtain a single location fix, and save the
  selected coordinates through the existing saved-location state path.
- **FR-013**: If current-location permission or a location fix is unavailable,
  the app MUST show a clear error, preserve the previously saved location, and
  retain manual locality selection as a fallback. The app MUST NOT enable
  continuous tracking or request background-location permission.
- **FR-014**: Saving a new monitoring location MUST continue to use the durable
  request-revision protections so responses from an old location cannot change
  the current estimate, alert baseline, successful-check time, status, or
  notification output.
- **FR-015**: Essential statuses and recommendations MUST remain readable by
  accessibility services, AQI severity MUST NOT rely on color alone, and
  interactive controls MUST provide accessible names and adequate touch
  targets. Accessibility semantics for full artwork MUST expose its category,
  range, summary, and recommendations once, while the dynamic AQI number and
  freshness remain separately readable.
- **FR-016**: The app MUST preserve Open-Meteo requests and required attribution,
  AQI calculations and categories, WorkManager monitoring, notification thresholds
  and baseline behavior, freshness windows, atomic DataStore revision handling,
  privacy protections, strict non-commercial operation, and the zero-backend
  architecture.
- **FR-017**: This iteration MUST NOT add accounts, analytics, maps, forecasts,
  new data providers, backend services, continuous location, or unnecessary
  dependencies.

### Key Entities *(include if feature involves data)*

- **Dashboard estimate**: The existing saved location and AQI estimate with its
  category, freshness, and monitoring status, presented as a compact view of
  current app state. Model-valid time stays internal to freshness decisions;
  required provider credit is available in Settings.
- **Category artwork and guidance**: One of six complete supplied category
  artworks, with its category name, range, summary, cloud scene, and three
  recommendations. Matching app text provides accessible semantics.
- **Current-location selection**: A user-requested one-time location result
  saved as the existing monitoring location, with manual locality selection
  remaining available.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a 360 × 800 dp portrait viewport at the default text scale, the
  location, monitoring status, live AQI number, gauge, complete category
  artwork, last-check state, and refresh action are visible without scrolling.
- **SC-002**: All six category-boundary fixtures display the matching complete
  artwork and gauge marker; the AQI number agrees with the artwork's category
  and range, and no separate visible category or recommendation copy is
  duplicated.
- **SC-003**: Each of the six categories displays its three artwork
  recommendations, with matching app text semantics that accessibility
  inspection can read once alongside the category name and range.
- **SC-004**: The same home and location flows remain usable in light and dark
  appearance, on narrow and landscape viewports, and with enlarged text; content
  remains reachable by scrolling, the complete artwork is not cropped, and no
  essential dynamic text or control is clipped. Embedded artwork text remains
  at its authored size.
- **SC-005**: The supplied icon is visible in the launcher on API 24 and as an
  adaptive icon on API 26 or later; on a launcher that supports themed icons,
  its monochrome representation remains recognizable.
- **SC-006**: A user can save current coordinates from the location-change
  screen after one explicit action and one foreground fix; denial or failure
  leaves the existing saved location unchanged and the manual choice remains
  available.
- **SC-007**: No background-location permission or continuous location request
  is introduced, and existing monitoring, AQI, alert, and request-revision
  regression tests continue to pass.
- **SC-008**: The dashboard contains no “US AQI” label, model-valid timestamp,
  model-cadence copy, or provider/source details; Settings still exposes the
  required CAMS and Open-Meteo attribution and CC BY 4.0 license link.

## Assumptions

- This feature is a focused enhancement to the existing native Android MVP;
  its data provider, saved-state model, monitoring flow, and notification logic
  remain in place.
- The supplied AQI files are full infographic posters with embedded category
  names, ranges, summaries, and recommendations. The dashboard uses the full
  matching artwork as one category card; a live AQI number, gauge, freshness,
  monitoring state, last-check time, and refresh action remain native UI.
- The embedded artwork copy is fixed-size English text and does not scale or
  localize independently. Matching app text semantics MUST stay synchronized
  for accessibility. The separate general-guidance note remains native text.
- Health guidance is general and adapted from public AQI activity guidance. The
  app does not identify the pollutant driving the aggregate AQI, so
  particle-specific advice is explicitly conditional and is not a diagnosis or
  substitute for local public-health guidance.
- The one-screen target applies at normal text size on a standard portrait
  phone. Accessibility text scaling and small or landscape displays may scroll.
- Adaptive and themed launcher icons apply only to Android versions and
  launchers that support those formats; a standard launcher icon remains
  available on older supported Android versions.
