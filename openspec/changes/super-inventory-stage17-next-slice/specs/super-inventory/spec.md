# Delta for super-inventory

## ADDED Requirements

### Requirement: Scan-session handoff preserves manual-first inventory control

The system MUST let barcode resolution feed an active `inventory-scan-session` while preserving the existing manual purchase and consumption entry points. Resolved scans MUST create or enrich reviewable drafts only and MUST NOT replace manual fallback behavior.

#### Scenario: Resolved scan queues a draft
- GIVEN a barcode resolves to an existing `SuperItem`
- WHEN the user sends it to the scan workflow
- THEN the system MUST queue it as a reviewable draft
- AND MUST keep manual inventory actions available

#### Scenario: Manual fallback remains available
- GIVEN scanning is unsupported, denied, or the code is entered manually
- WHEN the user continues the inventory workflow
- THEN the system MUST preserve the same bounded session and review flow

### Requirement: Scan workflows require explicit confirmation before stock mutation

The system MUST NOT mutate `currentStock`, `checked`, or movement history from scan lookup, queueing, editing, removal, cancellation, or session expiry. Only an explicit confirm action on reviewed drafts MAY invoke existing stock-movement commands.

#### Scenario: Draft changes remain non-mutating
- GIVEN queued scan-originated drafts exist
- WHEN the user edits, removes, cancels, or abandons them
- THEN the system MUST leave `currentStock`, `checked`, and movement history unchanged

#### Scenario: Confirmation delegates to existing movement rules
- GIVEN reviewed drafts are ready for confirmation
- WHEN the user explicitly confirms them
- THEN the system MUST run existing movement validation and atomicity rules
- AND MUST NOT apply partial or automatic stock mutation before confirmation
