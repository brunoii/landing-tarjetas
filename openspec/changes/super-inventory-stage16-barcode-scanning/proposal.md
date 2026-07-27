# Proposal: Super Inventory Stage 16 Barcode Scanning

## Intent

Add a bounded barcode scanner to the barcode alias flow so users can resolve a product faster, then choose purchase or consumption actions without automatic stock mutation.

## Proposal question round

Auto mode assumptions: manual fallback stays mandatory, duplicate scans are suppressed, and scanning only hands off to explicit stock actions.

## Scope

### In Scope
- Progressive scanning with `BarcodeDetector` + `getUserMedia` inside the existing barcode UI.
- Resolve scanned codes through alias lookup; if missing, allow attaching the code to an existing `SuperItem`.
- Show the resolved item with purchase/consumption entry points; no stock change until confirmation.
- UI/static-contract coverage for a bounded scanner path.

### Out of Scope
- New scan-session persistence, batching, audit trails, or draft movement APIs.
- Auto stock mutation, OCR/ticket coupling, external catalog lookup, or product creation.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: extends barcode handling from manual-first text entry to progressive client-side scanning while preserving alias semantics and explicit stock-movement boundaries.

## Approach

Keep backend contracts unchanged where possible. Add scanner state in the static UI, preserve scanned values as text, reuse existing alias lookup/attach endpoints, debounce duplicate reads, and render explicit follow-up actions that call existing purchase/consumption flows.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/resources/static/index.html` | Modified | Scan trigger and result panel. |
| `src/main/resources/static/js/supermarket.js` | Modified | Scanner state, lookup/attach, action handoff. |
| `src/main/resources/static/css/styles.css` | Modified | Mobile scan layout. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Barcode-only scanner API contract. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | UI browser API contract. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Browser/camera support gaps | High | Mandatory manual fallback. |
| Duplicate reads | Medium | Client debounce + explicit action gate. |
| Wrong alias match from text coercion | Medium | Preserve leading zeros; validate as text. |

## Rollback Plan

Revert the scanner UI/test changes and the Stage 16 spec delta. Manual barcode lookup/attach and stock movement flows remain the fallback.

## Dependencies

- Secure-context camera access where scanning is used.
- Existing barcode alias and movement endpoints remain unchanged.
- Forced chained delivery within the 400-line review budget.

## Success Criteria

- [ ] On supported mobile browsers, a scanned barcode resolves an existing alias or can be attached to an existing item.
- [ ] If camera access is unavailable or denied, manual entry still completes the same lookup/attach flow.
- [ ] Scanning alone never changes `currentStock`, `checked`, or movement history.
- [ ] Purchase/consumption still happens only through existing explicit actions after the item is shown.
