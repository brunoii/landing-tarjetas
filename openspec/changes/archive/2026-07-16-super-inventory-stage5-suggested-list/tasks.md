# Tasks: Lista sugerida de compras Etapa 5

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-850 full change; each PR planned under 800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static/tests → PR 3 OpenSpec archive/spec sync |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend suggested-list contract | PR 1 | Base `main`; DTO/controller/service and backend tests. |
| 2 | UI suggested section | PR 2 | Base `main` after PR 1; static assets and static contracts. |
| 3 | Spec/archive sync | PR 3 | Base `main` after PR 2; only if apply/verify requires OpenSpec sync. |

## Phase 1: Backend/API

- [x] 1.1 Create `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedItemResponse.java` with only suggestion fields, excluding `checked`.
- [x] 1.2 Add `SupermarketService.listSuggestedItems()` as `@Transactional(readOnly = true)`, reusing `findActiveOrderedForList()` and filtering active items with non-null/non-blank `unit`, non-null `habitualObjective`, known `currentStock`, and `currentStock < habitualObjective`.
- [x] 1.3 Create `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedListController.java` for `GET /api/super/suggested-list`.

## Phase 2: Backend Tests

- [x] 2.1 Extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for eligible suggestions, subtraction, ordering, and exclusion of inactive, blank/null `unit`, missing objective, null stock, target met, or over-target items.
- [x] 2.2 Add read-only assertions that suggestion queries do not change `checked`, `currentStock`, stock movements, barcode aliases, or `updatedAt` when stable/exposed by `SuperItem`.

## Phase 3: UI/static

- [x] 3.1 Add `api.superSuggestedList()` in `src/main/resources/static/js/api.js` and update `src/main/resources/static/js/app.js` import cache-busting tokens for `api.js` and `supermarket.js`.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` to load suggestions separately, render a read-only suggested section, and keep `generatedSuperListText(items)` dependent only on `checked`.
- [x] 3.3 Add `super-suggested-card` markup in `src/main/resources/static/index.html` and minimal styles in `src/main/resources/static/css/styles.css`, with independent empty/loading state.

## Phase 4: Static Tests

- [x] 4.1 Update `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` for Stage 5 cache tokens, allowed suggested-list semantics, and unchanged blocks for price, store, presentation, OCR, automation, and persistence.
- [x] 4.2 Extend `src/test/resources/static-ui-contract-tests.mjs` for `api.superSuggestedList()`, `app.js` import tokens, separate render behavior, and manual-list isolation.

## Phase 5: Verification/Archive

- [x] 5.1 Run targeted backend and static contract tests for the touched slices before each stacked PR boundary.
- [x] 5.2 If verification passes, let archive phase sync `openspec/specs/super-inventory/spec.md` from this delta; do not archive before implementation verification.
