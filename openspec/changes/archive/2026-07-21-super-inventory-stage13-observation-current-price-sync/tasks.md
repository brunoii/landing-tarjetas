# Tasks: Super Inventory Stage 13 Observation Current Price Sync

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 520-760 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static tests → PR 3 OpenSpec/docs sync |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | API contract and atomic service sync | PR 1 | Base `main`; includes backend tests and `mvn test`. |
| 2 | Explicit opt-in UI flow | PR 2 | Base PR 1 branch/main after PR 1; includes static UI tests. |
| 3 | OpenSpec/docs sync only | PR 3 | Base PR 2 branch/main after PR 2; no archive. |

## Phase 1: Backend RED tests

- [x] 1.1 In `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, add failing coverage that missing/null/false `syncCurrentReferencePrice` preserves observation-only behavior and product price/source/date.
- [x] 1.2 Add failing coverage that `syncCurrentReferencePrice: true` creates the observation and updates `SuperItem` current/reference price, resolved/free source, and observed date.
- [x] 1.3 Add failing rollback/frontier coverage: invalid sync requests mutate neither observations nor product, and product create/update still creates no history.

## Phase 2: Backend GREEN/REFACTOR

- [x] 2.1 Modify `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` to add nullable `Boolean syncCurrentReferencePrice`; legacy missing/null means false.
- [x] 2.2 Modify `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java#createPriceObservation` to use `Boolean.TRUE.equals(...)` and copy validated price/source/date into the same active `SuperItem` transaction.
- [x] 2.3 Refactor only inside `SupermarketService` if needed to keep source resolution single-pass and preserve the closed `{id only}`, `{label only}`, `{neither}` rule.

## Phase 3: UI RED tests

- [x] 3.1 In `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, add failing contract coverage for an unchecked observation-form-only sync checkbox/help text.
- [x] 3.2 Add failing static coverage that `src/main/resources/static/js/supermarket.js` sends `syncCurrentReferencePrice` only when checked, refreshes items after sync, and keeps excluded Stage 15/admin/comparison/OCR/photo/ticket scope absent.

## Phase 4: UI GREEN/REFACTOR

- [x] 4.1 Modify `src/main/resources/static/index.html` to add the unchecked explicit sync control inside `#super-price-observation-form` only.
- [x] 4.2 Modify `src/main/resources/static/js/supermarket.js` to read the checkbox, include the flag only when true, refresh observations/items after sync, reset the form, and show sync-specific feedback.
- [x] 4.3 Leave `src/main/resources/static/js/api.js` unchanged unless tests prove a payload helper boundary is needed.

## Phase 5: Verification and OpenSpec sync

- [x] 5.1 Run `mvn test` after each work unit and before handoff.
- [x] 5.2 Update `openspec/changes/super-inventory-stage13-observation-current-price-sync/tasks.md` checkboxes during apply; keep archive out of apply/verify.

## Phase 6: PR 3 OpenSpec/docs handoff

- [x] 6.1 Persist cumulative apply progress for PR 1 backend/API/tests, PR 2 UI/static/tests, and PR 3 OpenSpec/docs-only sync without runtime code/test edits.
- [x] 6.2 Record verification/archive handoff notes: `mvn test` was already run in the implementation slices, this PR 3 docs slice does not require a new runtime test run, and root spec sync remains deferred to archive.

### PR 3 sync notes

- PR 3 is OpenSpec/docs-only and intentionally changes no runtime source or tests.
- Verification handoff: PR 1 backend/API/tests and PR 2 UI/static/tests already recorded strict-TDD evidence and full `mvn test` success in cumulative apply-progress.
- Archive handoff: merge the accepted Stage 13 delta into `openspec/specs/super-inventory/spec.md` only during `sdd-archive`; do not perform root spec sync in this apply slice.
