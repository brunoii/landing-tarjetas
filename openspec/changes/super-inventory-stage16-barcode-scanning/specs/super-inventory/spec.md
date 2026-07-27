# Delta for super-inventory

## ADDED Requirements

### Requirement: Explicit stock-action handoff after barcode resolution

The system MUST treat scanning as product resolution only. After a barcode resolves to a `SuperItem`, the UI MUST offer explicit purchase and consumption entry points using the existing stock-movement flows. Scanning, lookup, or alias attachment MUST NOT execute any stock mutation automatically.

#### Scenario: Resolved scan offers explicit next actions
- GIVEN a scanned or manually entered barcode resolves to an existing `SuperItem`
- WHEN the item result is shown
- THEN the system MUST show explicit purchase and consumption entry points
- AND MUST NOT mutate `currentStock`, `checked`, or movement history before confirmation

#### Scenario: Unresolved code stays in alias flow
- GIVEN a scanned or manually entered barcode has no active alias
- WHEN the result is shown as not found
- THEN the system MUST allow attaching that exact code to an existing `SuperItem`
- AND MUST NOT open or execute stock mutations automatically

### Requirement: Barcode scanning preserves the Stage 15 OCR boundary

The system MUST keep Stage 16 barcode scanning scoped to the barcode alias UI. It MUST NOT introduce ticket OCR ingestion, OCR candidate review, or automatic coupling between barcode camera APIs and the Stage 15 OCR workflow.

#### Scenario: OCR flow remains separate
- GIVEN the user uses barcode scanning features
- WHEN the UI renders scanner states or follow-up actions
- THEN the system MUST keep ticket OCR as a separate manual-review workflow
- AND MUST NOT reuse the scan flow to create OCR candidates or ticket persistence

## MODIFIED Requirements

### Requirement: Alias locales de barcode sobre SuperItem existente

The system MUST persist barcode codes as text associated only to an existing `SuperItem`. Manual entry, paste, and progressive browser scans MUST preserve the exact text value, including leading zeros. Lookup and attachment MUST use exact-text alias semantics. The system MUST NOT create a Base Product, parallel catalog, or alternate product identity.
(Previously: Barcode aliases were manual-entry only, while exact text and leading-zero preservation already applied.)

#### Scenario: Lookup with exact active alias
- GIVEN an active alias `0075012345678` associated to a `SuperItem`
- WHEN the user scans, types, or pastes exactly `0075012345678`
- THEN the system MUST return the associated `SuperItem`
- AND MUST preserve the code as text without trimming leading zeros

#### Scenario: Lookup without active alias
- GIVEN no active alias exists for a detected code
- WHEN the user resolves that exact code
- THEN the system MUST report it as not found
- AND MAY allow attaching it manually to an existing `SuperItem`

#### Scenario: Attaching an exact scanned code
- GIVEN an existing `SuperItem` and a code without an active alias
- WHEN the user attaches the scanned or entered code to that item
- THEN the system MUST create an active alias for that exact text value

#### Scenario: Reject active duplicate alias
- GIVEN a code already exists as an active alias
- WHEN the user tries to attach the same text value again
- THEN the system MUST reject the operation as duplicate
- AND MUST NOT create another active alias

### Requirement: Barcode manual-first without inventory impact

Barcode operations MUST NOT modify `currentStock`, `checked`, or stock movements. Manual entry or paste MUST remain available as the mandatory fallback. `BarcodeDetector` and `getUserMedia` MAY exist only as progressive enhancement inside the barcode feature. Duplicate live-scan reads MUST be suppressed until the scan state changes or the user restarts scanning. Stage 1, 2, 3, and the existing Stage 15 OCR boundary MUST remain intact.
(Previously: Barcode behavior was manual-first with no inventory impact, and camera/browser scanning was not part of the contract.)

#### Scenario: Inventory state stays unchanged
- GIVEN a product with `currentStock`, `checked`, and stock history
- WHEN the user scans, looks up, attaches, or removes a barcode
- THEN those values MUST remain unchanged
- AND MUST NOT register a stock movement

#### Scenario: Manual fallback remains mandatory
- GIVEN the browser lacks scanner support or camera permission is denied
- WHEN the user types or pastes a barcode manually
- THEN the same lookup and attachment flow MUST remain available

#### Scenario: Duplicate scan is ignored
- GIVEN the live scanner already resolved a barcode value
- WHEN the camera emits the same code again without an intervening reset or state change
- THEN the system MUST suppress the duplicate read
- AND MUST NOT repeat lookup, attachment, or stock-action handoff automatically
