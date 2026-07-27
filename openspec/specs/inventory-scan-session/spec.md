# Inventory Scan Session Specification

## Purpose

Define a bounded server-backed session that queues resolved scan results as reviewable draft stock lines without changing stock automatically.

## Requirements

### Requirement: Bounded scan session lifecycle

The system MUST provide one active inventory scan session per user workflow, store resolved items as reviewable draft purchase or consumption lines, and allow users to edit or remove drafts before confirmation.

#### Scenario: Resolved item enters the active session
- GIVEN an active scan session and a barcode resolved to an existing `SuperItem`
- WHEN the user adds it to the workflow
- THEN the system MUST queue a reviewable draft line in that session

#### Scenario: Session reset does not change stock
- GIVEN an active or expired scan session with queued drafts
- WHEN the user clears it or the session expires
- THEN the system MUST discard the session state
- AND MUST NOT mutate stock or movement history

### Requirement: Explicit confirmation gate

The system MUST NOT mutate stock when a barcode is scanned, looked up, or queued. Only an explicit confirm action for selected draft lines MAY apply stock movements, and that confirmation SHALL use existing movement validation atomically.

#### Scenario: Confirm applies reviewed drafts atomically
- GIVEN queued draft lines that satisfy movement rules
- WHEN the user explicitly confirms them
- THEN the system MUST apply the allowed stock movements atomically

#### Scenario: Invalid or unconfirmed draft stays non-mutating
- GIVEN a queued draft line that is invalid or never confirmed
- WHEN validation fails or the workflow is abandoned
- THEN the system MUST leave stock unchanged
