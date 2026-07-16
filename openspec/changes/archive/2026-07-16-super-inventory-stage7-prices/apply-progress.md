# Apply Progress: Precio actual de presentación default Etapa 7

Change: super-inventory-stage7-prices
Mode: Strict TDD
Artifact store: hybrid/both
Work units: PR 1 backend/API/tests completed; PR 2 UI/static/tests completed
Chain strategy: stacked-to-main
Updated: 2026-07-16

## Status

success — PR 1 backend/API/tests and PR 2 UI/static/tests slices are completed. Tasks 1.1 through 2.6 are implemented and verified with targeted backend/API and static UI contract tests.

## Completed Tasks

- [x] 1.1 Added nullable `commercialPresentationPricePesos` to `SuperItem` as `commercial_presentation_price_pesos` with precision 12 and scale 2.
- [x] 1.2 Added nullable `commercialPresentationPricePesos` to request/response contracts with positive, max-2-decimal validation and response exposure.
- [x] 1.3 Updated `SupermarketService` so price requires a non-blank commercial presentation label, normalizes to scale 2, clears with the presentation, and avoids stock/checked/movement/suggestion/barcode/manual-list mutation paths.
- [x] 1.4 Extended `SupermarketControllerTests` for legacy null, valid price round-trip/update/clear, zero/negative/3-decimal rejection, scale-2 normalization, price-without-presentation rejection, and collateral mutation protection.
- [x] 1.5 Ran `mvn -Dtest=SupermarketControllerTests test` successfully.
- [x] 2.1 Added `#super-item-presentation-price-pesos`, “Precio ref.” table column, responsive `data-label="Precio ref."`, and changed supermarket group row `colspan` from 9 to 10.
- [x] 2.2 Updated `supermarket.js` to include price in create/update payloads, validate positive price with presentation label, edit/reset the field, and render `superItemCommercialPresentationPriceLabel` with fallback `formatPesos`; generated and suggested lists still omit price totals.
- [x] 2.3 Confirmed no CSS change was required; existing responsive table styles cover the additional column, so the CSS cache token stayed unchanged.
- [x] 2.4 Updated the `supermarket.js` import cache token in `app.js` and the `app.js` token in `index.html`; preserved unrelated static tokens.
- [x] 2.5 Extended Java and Node static UI contract tests to allow only the current/reference price field while keeping stores/shops/history price semantics/multiple presentations/totals/automation blocked.
- [x] 2.6 Ran `mvn -Dtest=StaticUiContractTests test` and `node src/test/resources/static-ui-contract-tests.mjs` successfully for PR 2 evidence.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 47/47 baseline passed before PR 1 edits | ✅ Price tests written first; build failed on missing `SuperItem` getter/setter | ✅ `mvn -Dtest=SupermarketControllerTests test` passed 51/51 after entity field | ✅ Legacy null + persisted getter/setter assertions | ✅ Getter/setter placed with presentation fields |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 47/47 baseline passed before PR 1 edits | ✅ Request/response JSON assertions written before contract fields | ✅ `mvn -Dtest=SupermarketControllerTests test` passed 51/51 after request/response updates | ✅ Valid price + zero/negative/3-decimal validation cases | ➖ None needed |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 47/47 baseline passed before PR 1 edits | ✅ Service behavior tests written first for label requirement, scale 2, clear, and no collateral mutation | ✅ `mvn -Dtest=SupermarketControllerTests test` passed 51/51 after service implementation | ✅ Price with label/no quantity, price without label, clear, and no-mutation paths | ✅ Removed accidental helper trailing whitespace; tests still passed 51/51 |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 47/47 baseline passed before PR 1 edits | ✅ New controller tests added before production implementation | ✅ Final targeted run passed 51/51 | ✅ Four new tests cover legacy, valid, invalid, and no-collateral behavior | ✅ Helper methods kept close to existing test style |
| 1.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 47/47 baseline passed before PR 1 edits | N/A — verification command task | ✅ Final `mvn -Dtest=SupermarketControllerTests test` passed 51/51 | N/A — verification command task | N/A — verification command task |
| 2.1 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static contract | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26; ✅ Node static contract passed before PR 2 edits | ✅ Tests first expected price input, “Precio ref.” header, responsive label, and 10-column group rows; failed before implementation | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26; ✅ Node static contract passed | ✅ HTML contract + rendered row responsive label + group row `colspan` covered | ➖ None needed |
| 2.2 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static behavior/unit | ✅ Same 26/26 Java and Node baseline before PR 2 edits | ✅ Tests first expected payload inclusion, positive/label validation, edit/reset hydration, backend label rendering, and list omission; failed before implementation | ✅ Java static contracts passed 26/26; ✅ Node static contract passed | ✅ Valid price, zero/negative, missing presentation label, create/update/edit/reset/render/list paths covered | ✅ Reused existing payload/label helpers and `formatPesos`; no total behavior added |
| 2.3 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static contract | ✅ Same 26/26 Java and Node baseline before PR 2 edits | ✅ Tests preserved CSS token and existing responsive table contracts while adding the column; would fail on unnecessary CSS token churn | ✅ Java static contracts passed 26/26; ✅ Node static contract passed | ✅ Existing responsive table checks plus new data-label coverage prove CSS remains sufficient | ➖ No CSS change needed |
| 2.4 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static contract | ✅ Same 26/26 Java and Node baseline before PR 2 edits | ✅ Tests first expected Stage 7 supermarket/app cache tokens and preserved unrelated tokens; failed before implementation | ✅ Java static contracts passed 26/26; ✅ Node static contract passed | ✅ index app token + app supermarket token + preserved CSS/API/module tokens covered | ➖ None needed |
| 2.5 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static contract | ✅ Same 26/26 Java and Node baseline before PR 2 edits | ✅ Tests first allowed exact current/reference price tokens while continuing to block stores/shops/history/multiple presentations/totals/automation; failed until allowlist matched the new exact selector/API field | ✅ Java static contracts passed 26/26; ✅ Node static contract passed | ✅ Exact allowlist plus unsupported-term scan covers allowed price without reopening broader price semantics | ✅ Narrowed static guard to allowed exact terms only |
| 2.6 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Verification command | ✅ Same 26/26 Java and Node baseline before PR 2 edits | N/A — verification command task | ✅ `mvn -Dtest=StaticUiContractTests test` passed 26/26; ✅ `node src/test/resources/static-ui-contract-tests.mjs` passed | N/A — verification command task | N/A — verification command task |

## Test Summary

- Total tests written/extended in PR 1: 4 backend/API integration tests.
- Total tests written/extended in PR 2: Java static contract assertions plus Node static behavior assertions for input, payload, validation, render, edit/reset, cache tokens, and static guard allowlist.
- Total tests passing now: 26/26 in `StaticUiContractTests`; Node static contract script passed with exit code 0.
- Layers used: Integration/API (PR 1), Static Java contract (PR 2), Node static behavior/contract (PR 2).
- Approval tests: None — no refactoring-only tasks.
- Pure functions created: 1 (`superItemCommercialPresentationPriceLabel`).

## Tests Run

- Safety net PR 2: `mvn -Dtest=StaticUiContractTests test` → PASS, 26 tests.
- Safety net PR 2: `node src/test/resources/static-ui-contract-tests.mjs` → PASS, exit code 0.
- RED PR 2: `mvn -Dtest=StaticUiContractTests test` → expected FAIL after tests were written first because the HTML input/header/cache token and static JS price behavior did not exist yet.
- RED PR 2: `node src/test/resources/static-ui-contract-tests.mjs` → expected FAIL on missing `#super-item-presentation-price-pesos` / missing exported price label behavior.
- GREEN PR 2: `mvn -Dtest=StaticUiContractTests test` → PASS, 26 tests.
- GREEN PR 2: `node src/test/resources/static-ui-contract-tests.mjs` → PASS, exit code 0.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modified in PR 1 | Added nullable `commercialPresentationPricePesos` column and accessors. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` | Modified in PR 1 | Added nullable price field with positive and max-2-decimal validation. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` | Modified in PR 1 | Exposed nullable price in item responses. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified in PR 1 | Validates label requirement, normalizes scale 2, and clears price with absent presentation. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified in PR 1 | Added Spanish validation label for the price field. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified in PR 1 | Added backend/API tests for Stage 7 price behavior and regressions. |
| `src/main/resources/static/index.html` | Modified in PR 2 | Added price input, price column, and updated app cache token. |
| `src/main/resources/static/js/supermarket.js` | Modified in PR 2 | Added price payload/validation/edit/reset/render support and 10-column render contract. |
| `src/main/resources/static/js/app.js` | Modified in PR 2 | Updated `supermarket.js` import cache token. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified in PR 2 | Extended static contracts for price UI, cache tokens, responsive label, and exact allowlist guard. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified in PR 2 | Extended Node static behavior coverage for price payload/validation/render/edit/reset and static guard. |
| `openspec/changes/super-inventory-stage7-prices/tasks.md` | Modified | Marked tasks 1.1-2.6 complete; left 3.x pending. |
| `openspec/changes/super-inventory-stage7-prices/apply-progress.md` | Modified | Merged PR 1 and PR 2 apply progress and TDD evidence. |

## Deviations

- No backend behavior was touched in PR 2.
- `styles.css` was not changed because existing responsive card table CSS covered the new cell via `data-label`; therefore the CSS cache token remained unchanged.

## Remaining Tasks

- [ ] 3.1 Archive/sync OpenSpec only if archive phase is requested.
- [ ] 3.2 Keep archive PR limited to OpenSpec sync.

## Workload / PR Boundary

- Mode: chained PR slice
- Current work unit: PR 2 UI/static/tests only
- Boundary: starts from local PR 1 backend/API/tests diff; ends with UI capture/edit/reset/render and static contracts for current/reference price only.
- Estimated review budget impact: focused static UI/test diff; CSS and backend changes were avoided for this slice.

## Risks

None identified for PR 2. Static guards now allow only the exact current/reference presentation price surface and continue blocking stores/shops/history/multiple presentations/totals/automation.
