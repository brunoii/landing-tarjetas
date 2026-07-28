# Privacy-safe-pwa-shell Specification

## Purpose

Define minimal installable/offline shell behavior without caching or exposing sensitive scanner, OCR, or ticket data.

## Requirements

### Requirement: Privacy-safe offline shell

The system MUST provide manifest and service-worker primitives for the static shell. The worker MUST handle same-origin GET requests only, exact public shell URLs may be cached, unmatched URLs are network-only, and the offline fallback page only for navigation. It MUST cache only non-sensitive shell assets needed for app startup and MUST NOT cache receipt images, OCR payloads, barcode results, upload bodies, or API responses carrying sensitive ticket data.

#### Scenario: Offline shell uses safe assets only
- GIVEN the user already loaded the shell online
- WHEN the app shell starts later with limited connectivity
- THEN the system MUST serve only previously cached non-sensitive shell assets

#### Scenario: Sensitive requests bypass cache
- GIVEN a scanner, OCR, or ticket-related request or response
- WHEN the service worker handles network activity
- THEN the system MUST bypass cache storage for that traffic

#### Scenario: Unmatched shell requests stay network-only
- GIVEN a same-origin GET request outside the exact public shell allowlist
- WHEN the service worker handles that request
- THEN the system MUST keep the request network-only
- AND MUST use the offline fallback page only for navigation failures

### Requirement: Evidence-backed mobile shell behavior

The system MUST preserve usable mobile navigation and scanner/OCR entry affordances based on representative device evidence, not CSS-only assumptions. When PWA or secure-context capabilities are unavailable, the shell MUST keep manual fallback routes visible.

#### Scenario: Mobile shell stays usable on validated devices
- GIVEN a representative mobile device validation pass
- WHEN the user opens navigation, scanner, or OCR entry points
- THEN the shell MUST keep those controls reachable without horizontal-trap regressions

#### Scenario: Unsupported shell still exposes fallback
- GIVEN installability or secure-context support is unavailable
- WHEN the shell renders mobile actions
- THEN the system MUST keep manual entry and review-first alternatives visible
