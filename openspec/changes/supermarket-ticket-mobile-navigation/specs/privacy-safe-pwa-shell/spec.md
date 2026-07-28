# Delta for privacy-safe-pwa-shell

## ADDED Requirements

### Requirement: Local-only receipt verification data handling

The system MUST treat receipt verification evidence as local-only data. The shell, cache, and offline behavior MUST NOT store, replay, or expose authorized JPG receipt files or derived OCR payloads.

#### Scenario: Sensitive receipt verification bypasses persistence
- GIVEN a receipt verification action or OCR response
- WHEN shell or offline behavior is evaluated
- THEN the system MUST keep that traffic and evidence out of caches and persistent storage

#### Scenario: Offline shell stays privacy-safe
- GIVEN the app is used with limited connectivity
- WHEN receipt-related paths are unavailable
- THEN the system MUST preserve the shell fallback
- AND MUST NOT surface stored receipt evidence offline
