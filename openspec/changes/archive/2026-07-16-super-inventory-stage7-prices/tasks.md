# Tasks: Precio actual de presentación default Etapa 7

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500-700 full change; each stacked PR planned under 800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static/tests → PR 3 OpenSpec archive/spec sync if needed |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Add nullable reference price to backend/API contract | PR 1 | Base `main`; include backend tests. |
| 2 | Add UI capture/render and static guards | PR 2 | Base updated `main` after PR 1; include static tests. |
| 3 | Sync accepted OpenSpec after verification | PR 3 | Base updated `main` after PR 2; only if archive/spec changes are needed. |

## Phase 1: Backend/API/tests

- [x] 1.1 Add `commercialPresentationPricePesos` to `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` as nullable `BigDecimal` column `commercial_presentation_price_pesos` with precision 12 and scale 2.
- [x] 1.2 Add nullable `commercialPresentationPricePesos` to `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` and `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` with positive, two-decimal `BigDecimal` validation.
- [x] 1.3 Update `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` so price is normalized to scale 2, requires non-blank `commercialPresentationLabel`, and never mutates checked, stock, movements, suggestions, barcodes, or manual list.
- [x] 1.4 Extend `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` for legacy null, valid price round-trip, zero/negative/3-decimal rejection, scale-2 normalization, price without presentation rejection, and no collateral mutation.
- [x] 1.5 Run `mvn -Dtest=SupermarketControllerTests test` for PR 1 evidence.

## Phase 2: UI/static/tests

- [x] 2.1 Update `src/main/resources/static/index.html` with input `#super-item-presentation-price-pesos`, “Precio ref.” column, matching responsive `data-label`, and any required table `colspan` changes.
- [x] 2.2 Update `src/main/resources/static/js/supermarket.js` to send, edit, reset, validate, and render `superItemCommercialPresentationPriceLabel`; exclude price from manual and suggested list totals.
- [x] 2.3 Update `src/main/resources/static/css/styles.css` only for required price/table styling; update `src/main/resources/static/index.html` CSS cache token only if this file changes.
- [x] 2.4 Update `src/main/resources/static/js/app.js` cache token only if `src/main/resources/static/js/supermarket.js` import changes; update `src/main/resources/static/index.html` app token only if `src/main/resources/static/js/app.js` changes.
- [x] 2.5 Extend `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` and `src/test/resources/static-ui-contract-tests.mjs` to allow only current/reference price while keeping stores/shops/history/multiple presentations/totals/automation blocked.
- [x] 2.6 Run `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs` for PR 2 evidence.

## Phase 3: OpenSpec sync

- [x] 3.1 After verification, archive `openspec/changes/super-inventory-stage7-prices/` and merge the accepted delta into `openspec/specs/super-inventory/spec.md` only if the archive phase is requested.
- [x] 3.2 Keep PR 3 limited to OpenSpec archive/spec sync and its verification report; do not mix backend or UI code.
