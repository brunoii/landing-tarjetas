# Apply Progress: super-inventory-stage5-suggested-list

**Change**: super-inventory-stage5-suggested-list
**Mode**: Strict TDD
**Artifact store**: hybrid
**Delivery strategy**: force-chained
**Chain strategy**: stacked-to-main
**Current work unit**: PR 2 UI/static/tests only
**Updated**: 2026-07-16

## Status

partial — 10/12 tasks complete. PR 1 backend/API/tests and PR 2 UI/static/tests slices are implemented. Verification/archive tasks remain out of scope for this apply batch.

## Completed Tasks

- [x] 1.1 Create `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedItemResponse.java` with only suggestion fields, excluding `checked`.
- [x] 1.2 Add `SupermarketService.listSuggestedItems()` as `@Transactional(readOnly = true)`, reusing `findActiveOrderedForList()` and filtering active items with non-null/non-blank `unit`, non-null `habitualObjective`, known `currentStock`, and `currentStock < habitualObjective`.
- [x] 1.3 Create `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedListController.java` for `GET /api/super/suggested-list`.
- [x] 2.1 Extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for eligible suggestions, subtraction, ordering, and exclusion of inactive, blank/null `unit`, missing objective, null stock, target met, or over-target items.
- [x] 2.2 Add read-only assertions that suggestion queries do not change `checked`, `currentStock`, stock movements, barcode aliases, or `updatedAt` when stable/exposed by `SuperItem`.
- [x] 3.1 Add `api.superSuggestedList()` in `src/main/resources/static/js/api.js` and update `src/main/resources/static/js/app.js` import cache-busting tokens for `api.js` and `supermarket.js`.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` to load suggestions separately, render a read-only suggested section, and keep `generatedSuperListText(items)` dependent only on `checked`.
- [x] 3.3 Add `super-suggested-card` markup in `src/main/resources/static/index.html` and minimal styles in `src/main/resources/static/css/styles.css`, with independent empty/loading state.
- [x] 4.1 Update `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` for Stage 5 cache tokens, allowed suggested-list semantics, and unchanged blocks for price, store, presentation, OCR, automation, and persistence.
- [x] 4.2 Extend `src/test/resources/static-ui-contract-tests.mjs` for `api.superSuggestedList()`, `app.js` import tokens, separate render behavior, and manual-list isolation.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` — 40/40 passing before existing file edits | ✅ Tests written first asserting suggested DTO fields and absence of `checked` | ✅ `mvn -Dtest=SupermarketControllerTests test` — 43/43 passing | ✅ Covered non-empty response and read-only response without `checked` | ✅ Record DTO kept minimal; no extra fields |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` — 40/40 passing before service edits | ✅ Tests written first for eligibility, exclusions, subtraction, ordering, and read-only state | ✅ `mvn -Dtest=SupermarketControllerTests test` — 43/43 passing | ✅ Covered eligible items plus inactive, blank unit, missing unit, missing objective, null stock, target-met, and over-target exclusions | ✅ Extracted `isSuggestedItem` predicate in service |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | N/A (new controller) | ✅ Tests written first against `GET /api/super/suggested-list`; RED failed with 404 | ✅ `mvn -Dtest=SupermarketControllerTests test` — 43/43 passing | ✅ Endpoint exercised with non-empty, filtered, and read-only datasets | ➖ None needed |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` — 40/40 passing before test edits | ✅ Test coverage written before production implementation | ✅ `mvn -Dtest=SupermarketControllerTests test` — 43/43 passing | ✅ 3 backend tests cover success, filtering, ordering, and subtraction | ✅ Added reusable `configuredStockItem` test helper |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` — 40/40 passing before test edits | ✅ Read-only assertions written before production implementation | ✅ `mvn -Dtest=SupermarketControllerTests test` — 43/43 passing | ✅ Asserted preservation of `checked`, `currentStock`, movement rows, barcode alias state, and stable `updatedAt` values | ✅ Reloaded timestamp baselines from repository to avoid JVM/DB precision mismatch |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing; ✅ `node src/test/resources/static-ui-contract-tests.mjs` — PASS before edits | ✅ Tests written first for `api.superSuggestedList()` and Stage 5 cache tokens; RED failed on missing Stage 5 tokens/helper | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing; ✅ Node static contract PASS | ✅ Java and Node both assert endpoint path and direct API import cache tokens | ➖ None needed |
| 3.2 | `src/test/resources/static-ui-contract-tests.mjs`; `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static UI behavior | ✅ Same static baseline as 3.1 | ✅ Tests written first for separate suggestion loading/render and manual-list isolation | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing; ✅ Node static contract PASS | ✅ Covered non-empty suggestions, empty state, and unchecked suggested items excluded from `generatedSuperListText(items)` | ✅ Extracted `renderSuperSuggestedItems()` and `superSuggestedItemText()` for deterministic contract coverage |
| 3.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static UI contract | ✅ Same static baseline as 3.1 | ✅ Tests written first for `super-suggested-card` markup, summary/list/empty targets, and CSS selectors | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing; ✅ Node static contract PASS | ✅ Covered loading summary, empty state, non-empty render, and responsive suggested item layout | ✅ Kept styles minimal and selector-scoped to suggested card/list |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static Java guard | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing before edits | ✅ Stage 5 static guard changes written first; RED failed before UI/API implementation | ✅ `mvn -Dtest=StaticUiContractTests test` — 26/26 passing | ✅ Allowed Stage 5 `suggested` semantics while still blocking price, store, presentation, OCR, external lookup, automation, and suggestion persistence terms | ✅ Removed stale Stage 4 token expectations from the guarded import map |
| 4.2 | `src/test/resources/static-ui-contract-tests.mjs` | Static Node behavior | ✅ `node src/test/resources/static-ui-contract-tests.mjs` — PASS before edits | ✅ Node contract changes written first; RED failed on missing Stage 5 token/helper | ✅ `node src/test/resources/static-ui-contract-tests.mjs` — PASS | ✅ Covered API fetch order/path, DOM render behavior, empty/non-empty suggestions, and manual-list isolation | ✅ Updated fake Supermarket DOM/API to treat `superSuggestedList` as read-only refresh data, not a mutation |

## Test Summary

- **Total tests written/extended**: 1 Java static contract test class extended; 1 Node static contract script extended.
- **Total tests passing**: 26/26 in `StaticUiContractTests`; Node static contract script completed successfully.
- **Layers used**: Static Java contracts; static Node behavior contracts with fake DOM/API.
- **Approval tests**: None — behavior was additive, not refactoring-only.
- **Pure functions created**: 2 (`renderSuperSuggestedItems`, `superSuggestedItemText`) with deterministic fake-DOM coverage.

## Verification Commands

| Command | Result | Notes |
|---------|--------|-------|
| `mvn -Dtest=StaticUiContractTests test` | ✅ PASS | Baseline before modifications: 26 tests passing. |
| `node src/test/resources/static-ui-contract-tests.mjs` | ✅ PASS | Baseline before modifications. |
| `mvn -Dtest=StaticUiContractTests test` | ✅ RED observed | New Stage 5 static expectations failed before implementation. |
| `node src/test/resources/static-ui-contract-tests.mjs` | ✅ RED observed | Failed on missing Stage 5 cache token/helper before implementation. |
| `mvn -Dtest=StaticUiContractTests test` | ✅ PASS | Final run: 26 tests, 0 failures, 0 errors. |
| `node src/test/resources/static-ui-contract-tests.mjs` | ✅ PASS | Final run completed successfully. |

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/resources/static/js/api.js` | Modified | Added read-only `api.superSuggestedList()` helper for `/api/super/suggested-list`. |
| `src/main/resources/static/js/app.js` | Modified | Updated `api.js` and `supermarket.js` cache-busting imports to Stage 5 tokens. |
| `src/main/resources/static/js/supermarket.js` | Modified | Loads suggestions separately, renders read-only suggestion cards, and preserves manual-list generation from `checked` items only. |
| `src/main/resources/static/index.html` | Modified | Added separate `super-suggested-card` with summary, list, and empty state; updated static asset cache token. |
| `src/main/resources/static/css/styles.css` | Modified | Added minimal styles for the suggested-list card/items, including mobile layout. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Updated Stage 5 cache/static contracts and guards to allow suggested semantics while keeping out-of-scope blocks. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Extended API/helper/render/manual-list isolation coverage and fake DOM/API support for suggestions. |
| `openspec/changes/super-inventory-stage5-suggested-list/tasks.md` | Modified | Marked assigned tasks 3.1, 3.2, 3.3, 4.1, and 4.2 complete. |
| `openspec/changes/super-inventory-stage5-suggested-list/apply-progress.md` | Modified | Merged prior PR 1 progress with PR 2 UI/static/tests progress. |

## Deviations from Design

None — implementation matches the PR 2 UI/static portion of `design.md`. Backend/API files were not modified.

## Issues Found

- `openspec/config.yaml` is absent in the repo-local OpenSpec folder, so testing mode was resolved from Engram testing capabilities.
- The Java static contract initially duplicated static index copy expectations inside the `supermarket.js` source assertion; this was corrected so static copy is asserted in `index.html`, while `supermarket.js` assertions focus on behavior selectors/functions.

## Remaining Tasks

- [ ] 5.1 Run targeted backend and static contract tests for the touched slices before each stacked PR boundary.
- [ ] 5.2 If verification passes, let archive phase sync `openspec/specs/super-inventory/spec.md` from this delta; do not archive before implementation verification.

## Workload / PR Boundary

- **Mode**: chained PR slice.
- **Current work unit**: PR 2 UI/static/tests.
- **Boundary**: Starts from `main` after merged PR 1 backend/API/tests; ends with UI/API helper/static contracts only.
- **Dependency diagram**: `main` → PR 1 backend/API/tests ✅ merged → 📍 PR 2 UI/static/tests → PR 3 archive/spec sync.
- **Estimated review budget impact**: focused UI/static/test slice; no backend source files touched.
