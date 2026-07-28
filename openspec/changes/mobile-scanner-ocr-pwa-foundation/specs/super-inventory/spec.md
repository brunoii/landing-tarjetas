# Delta for super-inventory

## ADDED Requirements

### Requirement: OCR ticket review remains transient and privacy-bound

The system MUST keep ticket OCR bounded to transient review data. It MUST surface actionable readiness diagnostics when OCR runtime prerequisites such as datapath, native runtime, or language data are unavailable. It MUST fall back to manual review messaging and MUST NOT read, upload, cache, version, or persist the real ticket directory or ticket artifacts automatically.

#### Scenario: Ready OCR returns review candidates only
- GIVEN a valid ticket image and OCR runtime prerequisites are available
- WHEN the user requests OCR candidate extraction
- THEN the system MUST return review-first candidates and safe diagnostics only

#### Scenario: Unready OCR fails safely
- GIVEN OCR runtime prerequisites are missing or invalid
- WHEN the user requests OCR candidate extraction
- THEN the system MUST return actionable readiness warnings and no persisted ticket data

### Requirement: Mobile scanner and OCR behavior is evidence-backed

The system MUST treat mobile scanner, navigation, and review layouts as validated behavior only after representative device evidence. Unsupported or degraded mobile states SHOULD preserve manual barcode entry and OCR review access instead of hiding workflows.

#### Scenario: Mobile validation preserves workflow access
- GIVEN representative mobile evidence exists for the current shell behavior
- WHEN the user enters scanner or OCR review flows on mobile
- THEN the system MUST keep the primary workflow reachable without relying on desktop-only layout assumptions

## MODIFIED Requirements

### Requirement: Barcode manual-first sin impacto en inventario

Barcode operations MUST NOT modify `currentStock`, `checked`, or stock movements. Manual entry or paste MUST remain available as the mandatory fallback. Camera scanning MAY exist only as progressive enhancement and MUST be secure-context aware, expose readiness messaging when scanning cannot start, and support repeatable start/stop cleanup without leaking duplicate listeners or media streams. Duplicate live-scan reads MUST be suppressed until scan state changes or the user restarts scanning. Stage 1, 2, 3, and the OCR review boundary MUST remain intact.
(Previously: barcode behavior was manual-first with no inventory impact, but secure-context readiness and repeatable scanner lifecycle cleanup were not explicit.)

#### Scenario: Inventory state stays unchanged
- GIVEN a product with `currentStock`, `checked`, and stock history
- WHEN the user scans, looks up, attaches, or removes a barcode
- THEN those values MUST remain unchanged
- AND MUST NOT register a stock movement

#### Scenario: Manual fallback remains mandatory
- GIVEN the browser lacks scanner support, is not in a secure context, or camera permission is denied
- WHEN the user types or pastes a barcode manually
- THEN the same lookup and attachment flow MUST remain available

#### Scenario: Duplicate scan is ignored
- GIVEN the live scanner already resolved a barcode value
- WHEN the camera emits the same code again without an intervening reset or state change
- THEN the system MUST suppress the duplicate read
- AND MUST NOT repeat lookup, attachment, or stock-action handoff automatically

#### Scenario: Scanner lifecycle restarts cleanly
- GIVEN the user starts, stops, and restarts scanning in the same session
- WHEN the scanner is reinitialized
- THEN the system MUST avoid duplicated listeners or leaked streams
- AND MUST show readiness messaging if scanning cannot resume
