<!--
Sync Impact Report
- Version change: Unratified → 1.0.0 (initial ratification)
- Added principles: I. Zero Backend; II. Battery-Efficient Background Monitoring;
  III. Meaningful Notifications; IV. Accurate and Transparent Data;
  V. Simplicity and Privacy; VI. Quality and Maintainability.
- Added sections: MVP Scope; Development Workflow and Compliance.
- Removed sections: None.
- Follow-up TODOs: None.
-->

# Oh My Haze Constitution

## Core Principles

### I. Zero Backend
All application functionality MUST run natively on Android. The app MUST NOT
depend on backend servers, cloud functions, authentication, or hosted databases.
It MUST use free, publicly accessible air-quality APIs that require no API key,
and MUST store preferences and monitoring state locally. This keeps the service
independent of backend operations and limits data collection.

### II. Battery-Efficient Background Monitoring
The app MUST use WorkManager to schedule checks at a 30–60 minute interval,
treated as best-effort under Android scheduling. It MUST NOT use continuous GPS,
foreground services, or unnecessary background work. Automatic station selection
MUST resolve the nearest station when needed and cache that selection. Checks
MUST minimize network requests and battery use.

### III. Meaningful Notifications
The app MUST notify users only when a valid, non-stale reading crosses a worsening
threshold or severity category. It MUST persist alert state locally to prevent
duplicate alerts and MUST respect notification permission and user preferences.
Invalid, missing, or stale readings MUST NOT trigger alerts.

### IV. Accurate and Transparent Data
The app MUST use Malaysia’s Air Pollutant Index (API/IPU) as its primary
indicator and MUST NOT conflate it with US AQI. It MUST display the reading,
classification, monitoring station, and observation timestamp. Missing, outdated,
or unavailable data MUST be clearly identified. The interface MUST explain that
a station reading represents its area and is not an exact measurement at the
user’s coordinates.

### V. Simplicity and Privacy
The app MUST use Kotlin, Jetpack Compose, WorkManager, and DataStore, and MUST
prefer Android-native capabilities and minimal dependencies. It MUST request
location only when needed to select a station and MUST support manual station
selection. It MUST NOT include analytics, advertising SDKs, or unnecessary data
collection. Interfaces MUST provide accessible labels and MUST NOT use color as
the sole way to communicate air-quality severity.

### VI. Quality and Maintainability
Kotlin code MUST be readable and idiomatic, and architecture MUST remain simple
and modular. Changes to alert thresholds, station selection, stale-data handling,
or duplicate prevention MUST include automated tests for the affected critical
logic. Feature development MUST follow GitHub Spec Kit.

## MVP Scope

The MVP MUST provide a current Malaysian API/IPU reading; automatic or manual
station selection; background monitoring on a best-effort 30–60 minute cadence;
local notifications when conditions worsen; and visible monitoring status and
last successful check. Historical trends, forecasts, advanced notification
preferences, and iOS support are outside the MVP and require a separately
approved specification before implementation.

## Development Workflow and Compliance

Each feature MUST have a GitHub Spec Kit specification, plan, and task list
before implementation. Those artifacts MUST define acceptance criteria for
data freshness, battery-aware scheduling, privacy, and notification behavior
where applicable. Reviews MUST verify that changes comply with this constitution
and the approved feature artifacts. Any conflict MUST be resolved by amending
the governing artifact before implementation proceeds.

## Governance

This constitution governs project decisions and takes precedence over conflicting
feature artifacts. Amendments MUST be reviewed with a sync impact report and
updated version metadata. Versioning follows semantic versioning: MAJOR for
backward-incompatible principle or governance changes, MINOR for added principles
or materially expanded requirements, and PATCH for clarifications and wording
changes. Every specification, plan, task list, and implementation review MUST
check compliance with these rules.

**Version**: 1.0.0 | **Ratified**: 2026-10-09 | **Last Amended**: 2026-10-09
