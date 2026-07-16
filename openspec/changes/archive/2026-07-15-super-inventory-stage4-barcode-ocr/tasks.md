# Tasks: Etapa 4 — Alias local de barcode

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 750-950 |
| 400-line budget risk | High |
| 800-line budget risk | Medium/High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static contracts |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Alias JPA/API with DB uniqueness and invariant tests | PR 1 | base = feature/tracker branch |
| 2 | Manual-first UI lookup/attach/remove and blockers | PR 2 | base = PR 1 branch |

## Phase 1: Backend RED

- [x] 1.1 In `SupermarketControllerTests.java`, add failing tests for lookup found/not found, leading zeros, attach, soft remove, inactive alias/item.
- [x] 1.2 Add failing invariant tests: lookup/attach/remove preserve `currentStock`, `checked`, and `SuperItemStockMovementRepository` rows.
- [x] 1.3 Add failing duplicate/schema tests: active `active_code` unique constraint, inactive duplicates allowed, race/`DataIntegrityViolationException` translated to 409.
- [x] 1.4 Add failing validation tests for blank/oversized `code` and `format`, using explicit `SupermarketLimits` constants.
- [x] 1.5 Add failing DELETE test: `/items/{itemId}/barcode-aliases/{aliasId}` rejects aliases from another item.

## Phase 2: Backend GREEN

- [x] 2.1 Create `SuperItemBarcodeAlias.java` and repository with `code`, optional `format`, `active`, nullable unique `activeCode`, item FK, timestamps.
- [x] 2.2 Add `BARCODE_CODE_MAX_LENGTH` and `BARCODE_FORMAT_MAX_LENGTH` to `SupermarketLimits`; use them in entity/request validation.
- [x] 2.3 Create `SuperItemBarcodeAliasRequest/Response.java` and `SuperBarcodeLookupResponse.java` with text-only code fields.
- [x] 2.4 Modify `SupermarketService.java`: trim-only normalization, active item checks, duplicate precheck plus DB-conflict 409, item-owned soft deactivate.
- [x] 2.5 Create `SuperItemBarcodeAliasController.java` for GET lookup, POST attach, DELETE remove; do not call stock movement methods.

## Phase 3: UI RED/GREEN

- [x] 3.1 Update `StaticUiContractTests.java` and `static-ui-contract-tests.mjs` RED cases for API helpers, manual fallback, remove, limits, blockers.
- [x] 3.2 Modify `src/main/resources/static/js/api.js` with lookup/attach/remove helpers and update cache-token imports in `app.js`/`supermarket.js`.
- [x] 3.3 Modify `supermarket.js` for manual input lookup, found-row highlight, attach-to-existing-item, remove alias, string-only validation; no `Number(code)`.
- [x] 3.4 Modify `index.html` and `css/styles.css` for the manual barcode panel; add no camera, `BarcodeDetector`, `getUserMedia`, OCR, external lookup, prices, stores, presentations, suggestions, or purchase automation.

## Phase 4: Verification

- [x] 4.1 Run `mvn -Dtest=SupermarketControllerTests test`.
- [x] 4.2 Run `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs`.
- [x] 4.3 Run `mvn test`, inspect `git diff --stat`, and keep PR 1/PR 2 under the 800-line review budget when possible.
