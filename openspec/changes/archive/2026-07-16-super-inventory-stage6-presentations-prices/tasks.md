# Tasks: Presentación comercial default Etapa 6A

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-800 full change; each stacked PR planned under 800 |
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
| 1 | Backend presentation contract | PR 1 | Base `main`; entity/DTO/service and backend tests only. |
| 2 | UI presentation capture/render | PR 2 | Base `main` after PR 1; static assets and static contracts. |
| 3 | Spec/archive sync | PR 3 | Base `main` after PR 2; only if archive/spec sync is required. |

## Phase 1: Backend/API

- [x] 1.1 Add `ITEM_PRESENTATION_LABEL_MAX_LENGTH = 120` to `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java`.
- [x] 1.2 Add nullable `commercialPresentationLabel` and `commercialPresentationQuantity` columns to `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java`.
- [x] 1.3 Extend `SuperItemRequest.java` and `SuperItemResponse.java` with nullable presentation fields, preserving legacy request/response compatibility.
- [x] 1.4 Add `SupermarketService.applyCommercialPresentation(...)`: trim blank label to null, require positive quantity and `unit`, reject quantity without label, and clear both when absent.

## Phase 2: Backend Tests

- [x] 2.1 Extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for legacy null presentation and valid create/update/list exposure.
- [x] 2.2 Add invalid presentation tests for blank label, non-positive quantity, quantity without `unit`, and quantity without label; assert persisted item is unchanged.
- [x] 2.3 Add no-collateral-mutation assertions for `checked`, `currentStock`, stock movements, suggested list results, and barcode aliases.

## Phase 3: UI/static

- [x] 3.1 Add presentation inputs and display column/label in `src/main/resources/static/index.html`; update only `/css/styles.css` and `/js/app.js` cache tokens when those assets change.
- [x] 3.2 Update `src/main/resources/static/js/supermarket.js` limits, payload, validation, edit/reset, and render for default presentation only; do not add prices, stores, multiple presentations, or automation.
- [x] 3.3 Update `src/main/resources/static/js/app.js` cache-busting imports only for `./api.js` and `./supermarket.js`; preserve unrelated module tokens.
- [x] 3.4 Add minimal presentation styling in `src/main/resources/static/css/styles.css` only if required by the new markup.

## Phase 4: Static Contract Tests

- [x] 4.1 Update `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` to allow only presentation terms and verify `index.html` CSS/app tokens plus `app.js` api/supermarket tokens.
- [x] 4.2 Extend `src/test/resources/static-ui-contract-tests.mjs` for payload/render/edit/reset presentation contracts, cache tokens, and continued blocking of `price`, `prices`, `store`, shops, multiple presentations, and automation.

## Phase 5: Verification/Archive

- [x] 5.1 Run `mvn -Dtest=SupermarketControllerTests test` for PR 1 and backend regression scope.
- [x] 5.2 Run `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs` for PR 2.
- [x] 5.3 If verification passes, let archive phase sync `openspec/specs/super-inventory/spec.md`; do not archive before implementation verification.
