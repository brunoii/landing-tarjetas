# Apply Progress: PRD Etapa 3 — Movimientos

## Status

partial — PR 1 backend/API/tests slice and PR 2 UI/static-tests slice completed. Verification/archive tasks remain pending.

## Completed Tasks

- [x] 1.1 Backend RED cases for purchase, manual consumption, quick consumption, ordered/filtered history, and `ADJUSTMENT` with `quantity=null`.
- [x] 1.2 Backend RED cases for unknown stock, invalid quantity, enriched 409 metadata, and no snapshot/movement on validation or conflict.
- [x] 1.3 Backend RED cases for `quantity` and `allowNegativeStock` API error labels.
- [x] 2.1 Extended `SuperItemStockMovement` and added movement request/response DTOs with `ADJUSTMENT.quantity=null` semantics.
- [x] 2.2 Added `PESSIMISTIC_WRITE` stock-command repository lookup and recent movement repository queries with optional item filter.
- [x] 2.3 Added transactional purchase, consumption, quick consumption, adjustment, conflict-before-mutation, and history limit behavior in `SupermarketService`.
- [x] 2.4 Added dedicated `/api/super/movements` controller and item command endpoints.
- [x] 2.5 Preserved enriched 409 negative-stock metadata in the backend error model.
- [x] 3.1 Added failing static checks for Stage 3 API helpers, preserved 409 metadata, and cache tokens.
- [x] 3.2 Added failing UI static checks for row movement actions, modal labels, submit/retry, and movement history panel.
- [x] 4.1 Implemented API error metadata preservation and helpers for purchase, consumption, quick consumption, and movement history.
- [x] 4.2 Implemented supermarket row actions, reusable movement modal, explicit negative-stock confirmation/retry, and recent movement history.
- [x] 4.3 Updated cache-busting tokens to `20260714-super-inventory-stage3-api` and `20260714-super-inventory-stage3-ui`.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed with `mvn -Dtest=SupermarketControllerTests test` | ✅ New movement/history tests failed at compile on missing movement fields/types before production changes | ✅ 29/29 passed after backend implementation | ✅ Added default/max history limit and lock contract coverage; final 31/31 passed | ✅ Kept tests at API boundary with repository assertions only for persisted facts |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Unknown-stock, invalid quantity, 409, and no-mutation tests written before implementation | ✅ 29/29 passed after backend implementation | ✅ Expanded unknown-stock checks to purchase, manual consumption, and quick consumption; final 31/31 passed | ✅ Shared setup within existing test style |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Label tests for `quantity` and `allowNegativeStock` written before handler/model changes | ✅ 29/29 passed after handler/model implementation | ✅ Invalid boolean payload exercises unreadable-message label path | ✅ Reused existing Spanish field-label behavior |
| 2.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Tests referenced missing `quantity` getter and new movement types before production changes | ✅ 29/29 passed after entity/DTO implementation | ✅ Adjustment null quantity and movement quantity assertions cover separate paths | ✅ Added explicit getters and nullable fields without replacing table/entity |
| 2.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API + reflection contract | ✅ 24/24 baseline passed | ✅ History/filter tests failed before repository query/controller support existed | ✅ 29/29 passed after repository/controller implementation | ✅ Added reflection contract for `@Lock(PESSIMISTIC_WRITE)` and final 31/31 passed | ✅ Kept lock lookup as a dedicated repository method |
| 2.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Command, validation, conflict, no-mutation, and history-limit tests written before/around service implementation | ✅ 29/29 passed after service implementation | ✅ Additional unknown-stock and limit tests final 31/31 passed | ✅ Centralized command flow and limit normalization |
| 2.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Endpoint tests for item commands and `/api/super/movements` failed before mappings existed | ✅ 29/29 passed after controllers were added/updated | ✅ Filtered and unfiltered movement history paths covered | ✅ Dedicated movement controller keeps history mapping explicit |
| 2.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 24/24 baseline passed | ✅ Enriched 409 metadata expectations written before custom error model support | ✅ 29/29 passed after exception/model implementation | ✅ Label and invalid-format tests exercise adjacent error behavior | ✅ Added non-null JSON inclusion to avoid noisy null metadata on regular errors |
| 3.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static/UI contract | ✅ 26/26 baseline passed with `mvn -Dtest=StaticUiContractTests test`; node static script passed | ✅ Static tests failed on Stage 2 tokens, missing API helpers, and missing preserved error metadata | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26 and node script passed after API/tokens implementation | ✅ Node API calls cover purchase, consumption, quick consumption, history query, and 409 metadata propagation | ✅ Centralized API error creation to avoid double-reading response bodies |
| 3.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static/UI contract | ✅ 26/26 baseline passed with `mvn -Dtest=StaticUiContractTests test`; node static script passed | ✅ Static tests failed on missing row actions, modal IDs/labels, negative retry flow, and movement history panel | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26 after UI implementation | ✅ Node DOM test covers purchase modal submit, consumption 409 retry with `allowNegativeStock=true`, quick consumption, and item-filtered history | ✅ Reused existing modal/table styles and fake DOM helpers |
| 4.1 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static/API contract | ✅ Existing API static tests passed before edits | ✅ Tests referenced missing API helpers and metadata fields before production changes | ✅ API helpers and enriched errors passed focused static/node tests | ✅ 409 body/detail/status fields plus enriched stock metadata exercised | ✅ Extracted `apiError`, `errorBody`, and `errorMessage` helpers |
| 4.2 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static/UI contract | ✅ Existing supermarket static tests passed before edits | ✅ Tests referenced missing movement actions/modal/history exports and DOM behavior before production changes | ✅ UI/static tests passed after supermarket implementation | ✅ Purchase, manual consumption, quick consumption, negative retry, and filtered history paths covered | ✅ Kept scope inside existing supermarket module and style system |
| 4.3 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static cache contract | ✅ Existing cache-token checks passed with Stage 2 tokens before edits | ✅ Tests failed while files still referenced Stage 2 tokens | ✅ Stage 3 cache-token checks passed | ✅ Both Java and Node static checks assert API/UI token split | ➖ None needed |

## Corrective Pass TDD Evidence

| Gate Warning | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|--------------|-----------|-------|------------|-----|-------|-------------|----------|
| Hide the whole negative-stock field wrapper in purchase flow | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java`; `src/test/resources/static-ui-contract-tests.mjs` | Static/UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26; `node src/test/resources/static-ui-contract-tests.mjs` passed before corrective edits | ✅ Static/Node tests failed while only `#super-movement-allow-negative` was hidden and `.super-movement-negative-field` remained visible | ✅ Static/Node tests passed after `openSuperMovementModal` hides/shows `.super-movement-negative-field` and resets the checkbox separately | ✅ Tests cover both purchase hidden and consumption visible wrapper states | ✅ Minimal DOM selector change; no modal flow redesign |
| Expose item unit in movement history response | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ `mvn -Dtest=SupermarketControllerTests test` passed 31/31 before corrective edits after rerunning sequentially | ✅ History response test failed on missing `$[0].itemUnit` | ✅ `mvn "-Dtest=SupermarketControllerTests,StaticUiContractTests" test` passed 57/57 after adding `itemUnit` to `SuperItemStockMovementResponse` | ✅ Assertions cover quick consumption, manual consumption, and purchase history rows with the same item unit | ✅ Kept mapping local to the response DTO; no persistence shape change |

## Verification Evidence

- `mvn -Dtest=StaticUiContractTests test` — baseline before UI changes: ✅ 26 tests, 0 failures, 0 errors.
- `node src/test/resources/static-ui-contract-tests.mjs` — baseline before UI changes: ✅ passed with no output.
- `mvn -Dtest=StaticUiContractTests test` — RED after static/UI tests: ✅ expected failures for Stage 3 tokens, missing API helpers/error metadata, missing modal/history/action UI.
- `node src/test/resources/static-ui-contract-tests.mjs` — GREEN after implementation: ✅ passed with no output.
- `mvn -Dtest=StaticUiContractTests test` — GREEN after implementation: ✅ 26 tests, 0 failures, 0 errors.
- `mvn "-Dtest=SupermarketControllerTests,StaticUiContractTests" test` — targeted backend/static verification: ✅ 57 tests, 0 failures, 0 errors.
- `git diff --check` — ✅ no whitespace errors; Git emitted LF-to-CRLF working-tree warnings only.
- `mvn -Dtest=StaticUiContractTests test` — corrective baseline before PR2 UI gate fixes: ✅ 26 tests, 0 failures, 0 errors.
- `node src/test/resources/static-ui-contract-tests.mjs` — corrective baseline before PR2 UI gate fixes: ✅ passed with no output.
- `mvn -Dtest=SupermarketControllerTests test` — corrective baseline before backend response contract fix: ✅ 31 tests, 0 failures, 0 errors after rerunning sequentially. A parallel Maven run hit a transient JUnit discovery failure, not a test assertion failure.
- `mvn -Dtest=StaticUiContractTests test` — corrective RED after wrapper tests: ✅ failed as expected because `.super-movement-negative-field` was not hidden.
- `node src/test/resources/static-ui-contract-tests.mjs` — corrective RED after wrapper tests: ✅ failed as expected because `.super-movement-negative-field.hidden` stayed `false` for purchase.
- `mvn -Dtest=SupermarketControllerTests test` — corrective RED after item-unit contract test: ✅ failed as expected on missing `$.itemUnit` in movement history response.
- `mvn -Dtest=StaticUiContractTests test` — corrective GREEN: ✅ 26 tests, 0 failures, 0 errors.
- `node src/test/resources/static-ui-contract-tests.mjs` — corrective GREEN: ✅ passed with no output.
- `mvn "-Dtest=SupermarketControllerTests,StaticUiContractTests" test` — corrective targeted backend/static verification: ✅ 57 tests, 0 failures, 0 errors.
- `mvn test` — corrective full regression: ✅ 207 tests, 0 failures, 0 errors.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovement.java` | Modified | Added movement types plus nullable `quantity`, `notes`, and `source`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovementRequest.java` | Created | Added purchase/consumption command request with quantity validation, notes, and negative-stock flag. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemQuickConsumptionRequest.java` | Created | Added quick-consumption request with negative-stock flag. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovementResponse.java` | Created | Added movement history response DTO; corrective pass now exposes `itemUnit` for readable history rows. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockConflictException.java` | Created | Added rich negative-stock conflict exception metadata. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRepository.java` | Modified | Added active stock-command lookup with `PESSIMISTIC_WRITE`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovementRepository.java` | Modified | Added recent history queries with optional item filter. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified | Added transactional stock commands, validation, negative-stock conflicts, and history limits. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemController.java` | Modified | Added purchase, consumption, and quick-consumption command mappings. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemMovementController.java` | Created | Added dedicated `GET /api/super/movements` mapping. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiErrorResponse.java` | Modified | Added optional enriched error metadata fields. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified | Added labels and stock-conflict handling. |
| `src/main/resources/static/js/api.js` | Modified | Preserved API error status/body/details/enriched metadata and added movement helpers. |
| `src/main/resources/static/js/supermarket.js` | Modified | Added movement row actions, modal submit/retry flow, quick consumption, and recent history rendering; corrective pass now hides/shows the full negative-stock field wrapper. |
| `src/main/resources/static/index.html` | Modified | Added movement modal/history markup and Stage 3 UI cache token. |
| `src/main/resources/static/css/styles.css` | Modified | Added movement modal/history styling and adjusted row action layout. |
| `src/main/resources/static/js/app.js` | Modified | Updated Stage 3 API/UI cache tokens. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | Added focused backend/API tests for PR 1 slice; corrective pass asserts movement history `itemUnit`. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Added/updated static contracts for PR 2 UI/API/cache slice; corrective pass asserts the negative-stock wrapper selector. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added Node static contracts for API helpers, error metadata, movement UI actions, retry, and history; corrective pass asserts purchase hides and consumption shows the wrapper. |
| `openspec/changes/super-inventory-stage3-movements/tasks.md` | Modified | Marked tasks 3.1, 3.2, 4.1, 4.2, and 4.3 complete. |
| `openspec/changes/super-inventory-stage3-movements/apply-progress.md` | Modified | Merged cumulative apply progress and verification evidence. |

## Deviations from Design

None — implementation matches the PR 2 UI/static-tests slice. Backend rollback post-mutation simulation remains the existing PR 1 warning and was not addressed in this UI slice.

## Remaining Tasks

- [ ] 5.1 Execute/record directed verification in the dedicated verification phase if required by orchestrator.
- [ ] 5.2 Execute full `mvn test` verification and archive after approval.

## Workload / PR Boundary

- Mode: chained PR slice.
- Strategy: feature-branch-chain.
- Current work unit: PR 2 UI/static tests.
- Boundary: static/API helper tests, API helper metadata, supermarket UI actions/modal/history, cache tokens, and apply artifacts; no intentional backend behavior changes beyond using existing PR 1 endpoints.
- Review budget impact: PR 2 adds the UI/static slice on top of existing uncommitted PR 1 backend files in the working tree; final PR diff should be reviewed against the PR 1 branch to keep the child diff focused.
