# Apply Progress: Super Inventory Stage 13 Observation Current Price Sync

## Mode
Strict TDD

## PR Boundary
- Mode: forced chained / stacked-to-main
- Current work unit: PR 3 OpenSpec/docs sync only
- Boundary: cumulative OpenSpec task/apply-progress reconciliation and verify/archive handoff notes only
- Prior work retained: PR 1 backend/API/tests and PR 2 UI/static/tests from previous apply-progress
- Excluded: runtime code changes, runtime test changes, root spec archive sync, source admin, comparison, multiple prices/presentations, barcode/OCR/ticket/photo, Stage 15 scope

## Completed Tasks
- [x] 1.1 Backend RED coverage for missing/null/false `syncCurrentReferencePrice` preserving observation-only behavior and product price/source/date.
- [x] 1.2 Backend RED coverage for `syncCurrentReferencePrice: true` creating observations and syncing reusable/free/no-date price data into `SuperItem`.
- [x] 1.3 Backend RED coverage for invalid explicit sync rollback/frontier behavior and product edits staying non-historical.
- [x] 2.1 Added nullable `Boolean syncCurrentReferencePrice` to `SuperItemPriceObservationRequest`.
- [x] 2.2 Implemented `Boolean.TRUE.equals(...)` sync branch in `SupermarketService#createPriceObservation` inside the existing transaction.
- [x] 2.3 Kept source resolution single-pass and copied the resolved source to product fields.
- [x] 3.1 Added static RED contract for unchecked observation-form-only sync checkbox/help text.
- [x] 3.2 Added static RED contract for payload opt-in, sync refresh/feedback, unchanged API helper boundary, cache token, and excluded Stage 15/admin/comparison/OCR/photo/ticket scope.
- [x] 4.1 Added the unchecked explicit sync control inside `#super-price-observation-form` only.
- [x] 4.2 Wired UI payload to include `syncCurrentReferencePrice` only when checked, refresh items/observations after sync, reset the form, and show sync-specific feedback.
- [x] 4.3 Kept `src/main/resources/static/js/api.js` unchanged.
- [x] 5.1 Ran focused and full Maven test commands for PR1/PR2 implementation slices.
- [x] 5.2 Updated active OpenSpec tasks checkboxes; archive remains out of apply.
- [x] 6.1 Persisted cumulative apply progress for PR 1, PR 2, and PR 3 docs-only sync without runtime edits.
- [x] 6.2 Recorded verification/archive handoff notes and deferred root spec sync to archive.

## Files Changed
| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` | Modified by prior PR1 | Added nullable `syncCurrentReferencePrice` flag. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified by prior PR1 | Added opt-in current/reference price sync after observation persistence in the existing transaction. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified by prior PR1 | Added strict-TDD integration coverage for opt-out, opt-in sync, invalid rollback, and frontier behavior. |
| `src/main/resources/static/index.html` | Modified by prior PR2 | Added unchecked observation sync checkbox/help text and bumped app cache token. |
| `src/main/resources/static/js/app.js` | Modified by prior PR2 | Bumped supermarket module cache token for the Stage 13 UI slice. |
| `src/main/resources/static/js/supermarket.js` | Modified by prior PR2 | Added opt-in sync payload flag, sync-specific refresh/feedback, and kept unchecked/missing as observation-only. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified by prior PR2 | Added strict-TDD static coverage for checkbox scope, payload behavior, unchanged API helper boundary, exclusions, and cache tokens. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified by prior PR2 | Updated static DOM harness and unsupported-term allowlist for the new sync control/cache token. |
| `openspec/changes/super-inventory-stage13-observation-current-price-sync/tasks.md` | Modified | Added and completed PR 3 OpenSpec/docs-only handoff tasks and notes. |
| `openspec/changes/super-inventory-stage13-observation-current-price-sync/apply-progress.md` | Created | Persisted cumulative merged apply progress for PR1, PR2, and PR3 docs-only sync. |

## TDD Cycle Evidence
| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 2.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ 77/77 baseline via `mvn -Dtest=SupermarketControllerTests test` | ✅ Written first for missing/null/false opt-out behavior | ✅ 80/80 after implementation | ✅ 3 flag states exercised | ✅ Existing assertion helpers reused |
| 1.2 / 2.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ 77/77 baseline via `mvn -Dtest=SupermarketControllerTests test` | ✅ Written first; failed because product price remained 1500.00 instead of 1699.90 | ✅ 80/80 after implementation | ✅ Reusable source + free text/no-date paths exercised | ✅ Sync copy extracted to `syncCurrentReferencePrice` |
| 1.3 / 2.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration | ✅ 77/77 baseline via `mvn -Dtest=SupermarketControllerTests test` | ✅ Written first for invalid both-source sync and non-historical product edit frontier | ✅ 80/80 after implementation | ✅ Invalid request rollback + product update non-history paths exercised | ✅ Source resolution stayed single-pass |
| 3.1 / 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI contract | ✅ 26/26 baseline via `mvn -Dtest=StaticUiContractTests test` | ✅ Written first; failed because checkbox/help text was absent | ✅ 28/28 after implementation | ✅ Observation form contains sync; product form does not | ✅ Helper `htmlSection` extracted for scoped assertions |
| 3.2 / 4.2 / 4.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract + Node static behavior harness | ✅ 26/26 baseline via `mvn -Dtest=StaticUiContractTests test` | ✅ Written first; failed because payload/refresh/feedback/cache contract was absent | ✅ 28/28 after implementation | ✅ Unchecked payload omits sync; checked static contract requires sync branch/refresh; API helper remains unchanged | ✅ Updated unsupported-term allowlists and fake DOM checkbox reset |
| Cache token guard | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`, `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract | ✅ 28/28 focused before cache-token change | ✅ Written first; failed because index/app still pointed at Stage 12 UI token | ✅ 28/28 after token bump | ✅ App shell and supermarket module tokens both checked | ➖ None needed |
| 5.1 / 5.2 | `openspec/changes/super-inventory-stage13-observation-current-price-sync/tasks.md` | Process | N/A | ✅ Tasks/progress requirements identified before marking complete | ✅ `mvn test`: 261/261 | ➖ Process task | ➖ None needed |
| 6.1 / 6.2 | `openspec/changes/super-inventory-stage13-observation-current-price-sync/tasks.md`, `openspec/changes/super-inventory-stage13-observation-current-price-sync/apply-progress.md` | Process/docs | N/A (docs-only) | ➖ No runtime behavior; prior TDD evidence merged before docs sync | ➖ No runtime implementation; no test run required for docs-only sync | ➖ Process task | ➖ None needed |

## Test Summary
- Total tests written: 2 Java static contract tests plus static harness updates in PR2; previous PR1 retained 3 integration tests.
- Total tests passing: 261/261 via prior `mvn test` handoff evidence.
- Layers used: Integration, static UI contract, Node static behavior harness, process/docs.
- Approval tests: None — no refactoring-only task.
- Pure functions created: 0.

## Test Runs
- PR1 safety net: `mvn -Dtest=SupermarketControllerTests test` passed, 77/77.
- PR1 focused GREEN: `mvn -Dtest=SupermarketControllerTests test` passed, 80/80.
- PR2 safety net: `mvn -Dtest=StaticUiContractTests test` passed, 26/26.
- PR2 RED: `mvn -Dtest=StaticUiContractTests test` failed as expected on missing sync checkbox/payload/feedback contracts.
- PR2 GREEN/focused: `mvn -Dtest=StaticUiContractTests test` passed, 28/28.
- PR2 cache-token RED: `mvn -Dtest=StaticUiContractTests#indexLinksExpectedStaticAssets test` failed while app/index still used Stage 12 UI token.
- PR2 cache-token GREEN: `mvn -Dtest=StaticUiContractTests#indexLinksExpectedStaticAssets test` passed, 1/1.
- PR2 handoff: `mvn test` passed, 261/261.
- PR3 docs-only sync: no `mvn test` run; no runtime source or test files were changed in this batch.

## Deviations from Design
None — implementation matches design. `api.js` stayed unchanged, and PR3 did not touch runtime code.

## Issues Found
- `openspec/config.yaml` is absent in the repository; strict TDD mode was enforced from orchestrator injection and Maven detection.
- Updating `supermarket.js` in PR2 required bumping both `index.html` app and `app.js` supermarket cache tokens so users do not keep the Stage 12 static bundle.

## Verification / Archive Handoff
- Next phase should run `sdd-verify` for the accumulated Stage 13 chain.
- Archive must merge the accepted Stage 13 delta into `openspec/specs/super-inventory/spec.md` after verification passes.
- Keep archive out of apply/verify; PR3 is only active-change OpenSpec/docs sync.

## Remaining Tasks
None — Stage 13 apply tasks are complete. Verification and archive are separate SDD phases.
