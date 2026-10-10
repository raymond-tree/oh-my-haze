<!--
Sync Impact Report — 2026-10-10
- Version change: 1.0.0 → 2.0.0. The approved source and index changed from
  Malaysian API/IPU station readings to Open-Meteo model-based US AQI estimates
  for a saved coordinate; this is a backward-incompatible product direction.
- Amended principles: I (non-commercial, terms-compliant Open-Meteo use); II
  (one-time saved location and approximately hourly work); III (US AQI alert
  floors, baseline crossings, rearming, and no catch-up); IV (US AQI and clear
  model-estimate disclosure); V (coordinate transmission and backup privacy);
  VI (AQI, saved-location, and alert-logic test coverage).
- Amended sections: MVP Scope (saved location, US AQI, and Open-Meteo/CAMS
  attribution) and Development Workflow and Compliance (provider terms and
  attribution review).
- Added sections: None. Removed sections: None.
- Follow-up TODOs: None. The matching feature spec is
  `specs/001-haze-monitoring/spec.md`.
-->

<!--
Sync Impact Report — 2026-10-10
- Version change: 2.0.0 → 2.1.0. Refined freshness policy: estimates can remain
  displayable through 18 hours, while alert decisions require model-valid age
  at most 12 hours and a newer upward category crossing.
- Amended principles: III (alert eligibility and baseline age); IV (distinguish
  last successful API check, model-valid time, and approximately 12-hour source
  cadence).
- Amended sections: None. MVP provider, platform, privacy, and scope unchanged.
- Follow-up TODOs: None. Matching details are in
  `specs/001-haze-monitoring/spec.md` and planning artifacts.
-->

# Oh My Haze Constitution

## Core Principles

### I. Zero Backend
All application functionality MUST run natively on Android. The app MUST NOT
depend on backend servers, cloud functions, authentication, or hosted databases.
For this MVP, it MUST call Open-Meteo's Air Quality API directly from the device
using its free, anonymous endpoint and MUST NOT embed an API key. This endpoint
MUST be used only for non-commercial purposes allowed by the provider's current
terms, including no advertising, subscriptions, commercial-product integration,
or promotional use. The app MUST respect published usage limits and MUST
re-evaluate feasibility before any commercial change. Preferences and monitoring
state MUST be stored locally. A provider or terms change that breaks these
conditions is a release blocker, not justification for silently adding a
backend or another provider.

### II. Battery-Efficient Background Monitoring
The app MUST use WorkManager to schedule checks approximately every 60 minutes,
treated as best-effort under Android scheduling. It MUST NOT use continuous GPS,
background location permission, foreground services, wake locks, or unnecessary
background work. A user-requested one-time location fix MUST be saved as the
monitoring location; ordinary and background checks MUST reuse those saved
coordinates. Checks MUST minimize network requests and battery use.

### III. Meaningful Notifications
The app MUST notify users only when a newer, valid US AQI estimate with a
model-valid age of no more than 12 hours crosses into an eligible worsening
category for the saved location. Estimates more than 12 and no more than 18
hours old may remain visible but MUST NOT change alert state or trigger a
notification. The 12-hour alert window is a conservative proxy for the normal
CAMS Global update cadence; the API does not expose a model-run timestamp. The
default minimum notification level MUST be 101 (Unhealthy for Sensitive Groups),
with only the four documented category-floor choices if sensitivity is
configurable. It MUST persist alert state locally to prevent duplicate alerts,
expire a baseline after more than 12 hours, rearm only after recovery below a
reached category, and respect notification permission and user preferences.
Invalid, missing, duplicate, old, or stale estimates MUST NOT trigger alerts.
Removing Android notification restrictions MUST NOT send catch-up alerts.

### IV. Accurate and Transparent Data
The app MUST use Open-Meteo's US AQI value and MUST label it **US AQI**. It MUST
not present the value as Malaysian API/IPU or an official Malaysian measurement.
The interface MUST display the estimate, classification, saved monitoring
location, model-valid time, and last successful API check as distinct
information. It MUST explain that the underlying model usually updates about
every 12 hours and hourly checks may return the same estimate. Missing, outdated,
or unavailable data MUST be clearly identified. The interface MUST explain that
Open-Meteo provides a regional model-based estimate, not a measurement at the
user's exact location, and MUST clearly attribute Open-Meteo and CAMS under the
applicable data license.

### V. Simplicity and Privacy
The app MUST use Kotlin, Jetpack Compose, WorkManager, and DataStore, and MUST
prefer Android-native capabilities and minimal dependencies. It MUST request
only foreground approximate location when the user explicitly chooses to use or
update current location, save the selected coordinates locally, and support a
bundled locality list when permission is denied. Background monitoring MUST use
the saved location, not the device's changing physical location. Before the
first provider request, the app MUST disclose that selected coordinates are
sent directly to Open-Meteo and that the provider may log coordinates and IP
addresses under its published privacy terms. The app MUST NOT include analytics,
advertising SDKs, accounts, or unnecessary data collection. Interfaces MUST
provide accessible labels and MUST NOT use color as the sole way to communicate
air-quality severity. Saved coordinates MUST be excluded from Android cloud
backup and device-to-device transfer.

### VI. Quality and Maintainability
Kotlin code MUST be readable and idiomatic, and architecture MUST remain simple
and modular. Changes to AQI classification, saved-location handling, alert
thresholds, stale-data handling, or duplicate prevention MUST include automated
tests for the affected critical logic. Feature development MUST follow GitHub
Spec Kit.

## MVP Scope

The MVP MUST provide saved-location selection; a current Open-Meteo US AQI
estimate; background monitoring on a best-effort approximately 60-minute
cadence; local notifications when conditions worsen; and visible estimate
freshness, monitoring status, notification availability, and last successful
check. The app MUST credit Open-Meteo and CAMS, and explain that the value is a
model-based estimate. Historical trends, forecast screens, maps, charts,
accounts, advanced notification rules, backend services, and iOS support are
outside the MVP and require a separately approved specification before
implementation.

## Development Workflow and Compliance

Each feature MUST have a GitHub Spec Kit specification, plan, and task list
before implementation. Those artifacts MUST define acceptance criteria for
data freshness, battery-aware scheduling, privacy, provider terms, attribution,
and notification behavior where applicable. Reviews MUST verify that changes
comply with this constitution and the approved feature artifacts. Any conflict
MUST be resolved by amending the governing artifact before implementation
proceeds.

## Governance

This constitution governs project decisions and takes precedence over
conflicting feature artifacts. Amendments MUST be reviewed with a sync impact
report and updated version metadata. Versioning follows semantic versioning:
MAJOR for backward-incompatible principle or governance changes, MINOR for
added principles or materially expanded requirements, and PATCH for
clarifications and wording changes. Every specification, plan, task list, and
implementation review MUST check compliance with these rules.

**Version**: 2.1.0 | **Ratified**: 2026-10-09 | **Last Amended**: 2026-10-10
