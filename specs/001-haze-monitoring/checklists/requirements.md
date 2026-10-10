# Specification Quality Checklist: Haze Monitoring MVP

**Purpose**: Validate specification completeness and quality before planning
**Created**: 2026-10-10
**Feature**: [spec.md](../spec.md)

**Review Ownership**: This is a reviewer-owned requirements-quality review
artifact. Mark an item `[x]` only when the criterion is satisfied.
**Marker Semantics**: `[x]` means the specification criterion was reviewed and
satisfied; it does not mean implementation work is complete.

## Content Quality

- [x] No implementation details beyond user-specified Android and source constraints
- [x] Focused on user value and product needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No `[NEEDS CLARIFICATION]` markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions are identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover the primary flows
- [x] Feature outcomes are measurable in Success Criteria
- [x] No unintended implementation details leak into the specification
- [x] US AQI category boundaries and alert thresholds are explicit
- [x] Saved monitoring location is distinct from device physical location
- [x] Saved-coordinate backup exclusions cover cloud backup and device-to-device
  transfer
- [x] Provider attribution, non-commercial terms, and model-estimate limitations
  are explicit
- [x] Research and specification agree that Open-Meteo is feasible only for the
  approved non-commercial MVP
- [x] Fractional provider values have a defined whole-index rounding rule for US
  AQI category and alert thresholds
- [x] Last successful API check, model-valid time, and source model update
  cadence are distinguished in the specification and UI expectations
- [x] Display freshness and alert eligibility have separate, explicit age rules
- [x] Location/configuration changes and process restart have deterministic
  stale-response acceptance criteria

## Notes

- The data source was changed from DOE APIMS to Open-Meteo's coordinate-based
  Air Quality API. The prior APIMS timestamp investigation remains in
  [research.md](../research.md) as historical research and is superseded for
  the MVP.
- The specification uses these disjoint integer bands: 0–50 Good, 51–100
  Moderate, 101–150 Unhealthy for Sensitive Groups, 151–200 Unhealthy,
  201–300 Very Unhealthy, 301+ Hazardous.
- Fractional provider values are rounded to the nearest integer (nonnegative
  halves up) before category and alert-floor comparisons, consistent with the
  US AQI nearest-integer convention documented by EPA.
- Default notification floor is US AQI 101; optional sensitivity choices are
  101, 151, 201, or 301. Estimates may remain displayed through 18 hours, but
  only model-valid times no more than 12 hours old can affect alert state. The
  12-hour alert rule is a conservative proxy because Open-Meteo does not expose
  the source model-run timestamp; checks do not guarantee new hourly model data.
- Open-Meteo free-tier use is non-commercial only. The user confirmed the
  intended app is non-commercial; a future commercial or promotional purpose
  requires a new provider and architecture review.
- No implementation code or task list has been created.
