# Tasks: Super Inventory Stage 12 - reference price source reuse

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 480-620 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static/tests → PR 3 OpenSpec sync |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add nullable product source FK and closed backend contract. | PR 1 | Base `main`; include RED/GREEN/refactor backend tests. |
| 2 | Reuse Stage 11 selector/inline-create in product form with XOR UX. | PR 2 | Base `main` after PR 1; include static UI tests. |
| 3 | Sync OpenSpec after verify only. | PR 3 | Base `main` after PR 2; docs/spec only. |

## Phase 1: PR 1 Backend/API/tests

- [x] 1.1 RED: extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for `{id only}`, `{label only}`, `{neither}`, legacy `id=null`, `{id+label}` invalid, inactive id, and cleanup preserving observations.
- [x] 1.2 GREEN: modify `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java`, `SuperItemRequest.java`, `SuperItemResponse.java`, and `SuperItemRepository.java` to carry nullable `commercialPresentationPriceSourceId` plus legacy label snapshot.
- [x] 1.3 GREEN: update `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` and `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` to enforce `{id only}|{label only}|{neither}`, resolve active `SuperPriceSource`, snapshot its name, and clear source/date on price or presentation reset.
- [x] 1.4 REFACTOR: trim duplicated product-price-source normalization paths in `SupermarketService.java` without changing Stage 11 observation behavior.

## Phase 2: PR 2 UI/static/tests

- [x] 2.1 RED: extend `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` for product `commercialPresentationPriceSourceId`, selector/free-text exclusivity, Stage 11 inline-create reuse, and Stage 12 scope guard tokens.
- [x] 2.2 GREEN: modify `src/main/resources/static/index.html` and `src/main/resources/static/js/api.js` to add the product source selector, keep the free-text fallback, and reuse existing `/api/super/price-sources` helpers only.
- [x] 2.3 GREEN: update `src/main/resources/static/js/supermarket.js` so product payload/edit/reset/render paths send exactly one of `commercialPresentationPriceSourceId` or `commercialPresentationPriceSourceLabel`, allow neither, and refresh/select the inline-created source.
- [x] 2.4 REFACTOR: consolidate product-source field toggling/messages in `supermarket.js` so legacy free-text and reusable-source edit states stay deterministic.

## Phase 3: PR 3 OpenSpec sync

- [x] 3.1 After verification, update only `openspec/changes/super-inventory-stage12-reference-price-source-reuse/tasks.md` checkboxes and any verify-linked OpenSpec notes for the final chained state.
- [x] 3.2 Sync `openspec/specs/super-inventory/spec.md` only during archive flow; keep PR 3 free of runtime code changes.

### PR 3 sync notes

- Verification handoff recorded: PR 1 backend/API/tests and PR 2 UI/static/tests were verified before this OpenSpec-only sync slice.
- Root spec sync remains intentionally deferred to archive flow; this PR 3 slice keeps runtime code and tests unchanged.
