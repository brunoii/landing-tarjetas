# Proposal: Super Inventory Stage 13 Observation Current Price Sync

## Intent

Remove the remaining two-step friction between recording a manual price observation and keeping the product current/reference price aligned. Stage 13 should let the explicit observation flow optionally sync the same data into `SuperItem` without turning product edits into automatic history.

## Scope

### In Scope
- Extend manual `POST /api/super/items/{id}/price-observations` with an explicit optional flag to also sync the observed price, source, and observed date into the product current/reference price.
- Keep the operation atomic: when sync is requested, observation creation and product update succeed or fail together.
- Preserve Stage 10-12 behavior when sync is not requested, including append-only history, legacy-safe source states, and reusable/free-text/no-source handling.
- Add UI affordance and feedback for the optional sync inside the existing manual observation form only.

### Out of Scope
- Automatic history generation from product create/update flows.
- Source admin, comparison, charts, multiple prices/presentations, barcode, OCR, ticket, photo, scraping, or Stage 15 scope.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `super-inventory`: extend manual price observations so the explicit create action MAY also update the product current/reference price in the same transaction when requested.

## Approach

Keep a single explicit observation action and add an optional `updateCurrentReferencePrice`-style flag. Backend validation must preserve the closed source rule `{id only}`, `{label only}`, `{neither}` and reject invalid combinations before any write. When sync is requested, reuse the same validated payload to create the append-only observation and copy those values into `SuperItem` inside one transaction. UI should make the sync opt-in and visible, not defaulted or implicit.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Add explicit optional sync and atomicity rules |
| `src/main/java/.../SuperItemPriceObservation*.java` | Modified | Request/response contract for sync flag |
| `src/main/java/.../SupermarketService.java` | Modified | Atomic observation + optional product sync flow |
| `src/main/resources/static/index.html` | Modified | Explicit opt-in control in observation form |
| `src/main/resources/static/js/{api,supermarket}.js` | Modified | Payload, refresh, feedback, guardrails |
| `src/test/.../SupermarketControllerTests.java` | Modified | Observation-only, observation+sync, rollback coverage |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Sync feels implicit | Med | Default opt-out, explicit copy, and spec/tests guarding Stage 10 frontier |
| Partial writes on failure | Med | Single transaction with rollback tests for requested sync |
| Review exceeds 400 lines | High | Deliver as forced chained backend/API/tests → UI/static/tests → OpenSpec |

## Rollback Plan

Remove the optional sync flag and transactional product-update branch, returning `POST /price-observations` to observation-only behavior while preserving persisted history and product prices.

## Dependencies

- Existing Stage 10 append-only observation endpoint and Stage 11-12 reusable/free-text source contract

## Success Criteria

- [ ] Manual observation without sync preserves Stage 10-12 behavior exactly.
- [ ] Manual observation with explicit sync updates the product current/reference price, source, and observed date atomically with the observation.
- [ ] API and UI keep sync optional/explicit and reject invalid source combinations without partial persistence.
