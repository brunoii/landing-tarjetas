# Apply Progress: Fuente manual del precio de referencia Etapa 8

## Status

success

## Scope

- Change: `super-inventory-stage8-price-source`
- Work unit completed this pass: PR 2 UI/static/tests only
- Prior completed work unit preserved: PR 1 backend/API/tests only
- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Mode: Strict TDD

## Completed Tasks

- [x] 1.1 Agregar `ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH = 120` en `SupermarketLimits.java`.
- [x] 1.2 Agregar `commercialPresentationPriceSourceLabel` nullable en `SuperItem.java` como columna `commercial_presentation_price_source_label` de longitud 120, con getters/setters.
- [x] 1.3 Agregar `commercialPresentationPriceSourceLabel` nullable en `SuperItemRequest.java` con `@Size(max=120)` y en `SuperItemResponse.java`.
- [x] 1.4 Actualizar `ApiExceptionHandler.java` con label de validación para `commercialPresentationPriceSourceLabel`.
- [x] 1.5 Actualizar `SupermarketService.java`: trim a `null`, fuente solo con precio válido, limpieza al quitar precio/presentación y preservación de checked, stock, movimientos, barcodes y listas.
- [x] 1.6 Extender `SupermarketControllerTests.java` para legacy null, fuente trimeada, precio sin fuente, fuente inválida/huérfana, limpieza, atomicidad y no mutación colateral.
- [x] 1.7 Ejecutar `mvn -Dtest=SupermarketControllerTests test` como evidencia de PR 1.
- [x] 2.1 Agregar input estático `#super-item-presentation-price-source-label` con `name="commercialPresentationPriceSourceLabel"`, `data-super-limit="priceSourceLabel"` y ayuda secundaria de fuente manual.
- [x] 2.2 Actualizar payload, validación client-side, edición, reset y rechazo de fuente explícita cuando no haya precio o presentación.
- [x] 2.3 Renderizar `commercialPresentationPriceSourceLabel` como texto secundario del precio de referencia, sin columna nueva ni mezcla con listas.
- [x] 2.4 Aplicar cache-busting Etapa 8 para `supermarket.js` en `app.js` y `/js/app.js` en `index.html`.
- [x] 2.5 Actualizar constantes Stage 8 en `StaticUiContractTests.java` y `static-ui-contract-tests.mjs`.
- [x] 2.6 Mantener guards estáticos precisos: se permiten solo tokens exactos de fuente manual/source-label y siguen bloqueados stores/shops/history/multiple prices/totals/automation.
- [x] 2.7 Ejecutar `mvn -Dtest=StaticUiContractTests test` y `node src/test/resources/static-ui-contract-tests.mjs` como evidencia de PR 2.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Tests referenced missing `ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH` | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Over-limit validation uses the new constant plus valid source cases | ✅ Constant colocated with existing presentation limits |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Tests referenced missing entity getter/setter | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Persisted null, non-empty, updated, and cleared source states | ➖ None needed |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Tests asserted missing request/response JSON field | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Legacy null, valid source, price without source, and validation cases | ➖ None needed |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Over-limit test expected Spanish label `Fuente del precio` | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Field label verified through request validation error details | ➖ None needed |
| 1.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Tests described trim, orphan rejection, cleanup, and collateral invariants before service code | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Source with price, price without source, source without price, clear price, clear presentation, and no-collateral mutation paths | ✅ Extracted `normalizeCommercialPresentationPriceSource` beside price normalization |
| 1.6 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ `mvn -Dtest=SupermarketControllerTests test` → 51/51 passing before PR 1 edits | ✅ Six backend tests were written before production changes | ✅ `mvn -Dtest=SupermarketControllerTests test` → 57/57 passing | ✅ Tests cover all assigned backend acceptance scenarios | ✅ Helper methods keep payloads explicit and scoped |
| 1.7 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ Baseline captured before PR 1 edits | ✅ Targeted suite failed at test compile with missing source constant/accessors | ✅ Final targeted suite passed | ✅ 57 tests passing after all cases were added | ➖ None needed |
| 2.1 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Static UI contract | ✅ `mvn -Dtest=StaticUiContractTests test` → 26/26 passing; ✅ `node src/test/resources/static-ui-contract-tests.mjs` → PASS before PR 2 edits | ✅ Tests expected missing input id/name/data-limit/help copy | ✅ Both static suites passed after HTML implementation | ✅ Required id/name/data-limit plus secondary help copy verified | ➖ None needed |
| 2.2 | `static-ui-contract-tests.mjs` | Static UI behavior | ✅ Static UI baseline passed before PR 2 edits | ✅ Tests expected missing `priceSourceLabel` limit, payload field, edit/reset support, and source validation | ✅ `node src/test/resources/static-ui-contract-tests.mjs` → PASS | ✅ Cases cover source with price, blank source, explicit source rejected without price/presentation, create, edit and reset | ✅ Kept payload normalization scoped while preserving explicit invalid source input until validation |
| 2.3 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Static UI behavior/render | ✅ Static UI baseline passed before PR 2 edits | ✅ Tests expected missing source label renderer and row text `Fuente: Ticket proveedor` near the price cell | ✅ Both static suites passed | ✅ Render covers price without source and price with source, without adding a table column | ✅ Extracted `superItemCommercialPresentationPriceHtml` and `superItemCommercialPresentationPriceSourceLabel` |
| 2.4 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Static UI contract | ✅ Static UI baseline passed before PR 2 edits | ✅ Tests expected Stage 8 cache tokens while files still had Stage 7 tokens | ✅ Both static suites passed | ✅ `app.js` import token and `index.html` app token verified together | ➖ None needed |
| 2.5 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Static UI contract | ✅ Static UI baseline passed before PR 2 edits | ✅ Java/Node constants expected Stage 8 token and failed before production tokens changed | ✅ Both static suites passed | ✅ Constants align with changed `app.js` and `index.html` only | ➖ None needed |
| 2.6 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Static guard | ✅ Static UI baseline passed before PR 2 edits | ✅ Guards allowed only exact source-label tokens while retaining unsupported term checks | ✅ Both static suites passed | ✅ Guard scan strips exact source-label tokens and still blocks stores/shops/history/multiple prices/totals/automation terms | ➖ None needed |
| 2.7 | `StaticUiContractTests.java`; `static-ui-contract-tests.mjs` | Verification | ✅ Baseline captured before PR 2 edits | ✅ RED run: `mvn -Dtest=StaticUiContractTests test` failed with 3 expected static-contract failures; `node ...` failed on missing `priceSourceLabel` frontend limit | ✅ Final targeted suites passed | ✅ Final evidence covers HTML, JS behavior, cache tokens and static guards | ➖ None needed |

## Test Summary

- Total PR 1 tests written: 6 backend/API integration tests.
- Total PR 2 test assertions/coverage added: static Java and Node contracts for input, payload, validation, edit/reset, render, cache tokens and guard allow-list.
- Total tests passing for PR 2 targeted suite: 26 Java static tests plus Node static contract script.
- Layers used: API integration for PR 1; Static UI contract/behavior for PR 2.
- Approval tests: None — feature extension, not refactoring-only.
- Pure functions created/extended in PR 2: `superItemPayloadFromValues`, `validateSuperItemPayload`, `superItemCommercialPresentationPriceSourceLabel`, `superItemCommercialPresentationPriceHtml`.

## Tests Run

- `mvn -Dtest=StaticUiContractTests test` — baseline before PR 2 edits: PASS, 26 tests.
- `node src/test/resources/static-ui-contract-tests.mjs` — baseline before PR 2 edits: PASS.
- `mvn -Dtest=StaticUiContractTests test` — RED after tests: FAIL, 3 expected failures for missing source input/cache/render contracts.
- `node src/test/resources/static-ui-contract-tests.mjs` — RED after tests: FAIL, missing `priceSourceLabel` frontend limit.
- `node src/test/resources/static-ui-contract-tests.mjs` — GREEN/final: PASS.
- `mvn -Dtest=StaticUiContractTests test` — GREEN/final: PASS, 26 tests.

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `src/main/resources/static/index.html` | Modified | Added source label input and secondary help copy; updated `/js/app.js` cache token to Stage 8. |
| `src/main/resources/static/js/app.js` | Modified | Updated `./supermarket.js` cache token to Stage 8. |
| `src/main/resources/static/js/supermarket.js` | Modified | Added source label limit, payload capture/cleanup, validation, edit population and secondary price render. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Added Stage 8 token constants, source input contracts, frontend limit expectations and precise static guard allow-list. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added Node static behavior coverage for source payload, validation, edit/reset, render, cache tokens and guard allow-list. |
| `openspec/changes/super-inventory-stage8-price-source/tasks.md` | Modified | Marked assigned tasks 2.1-2.7 complete; preserved 3.x pending. |
| `openspec/changes/super-inventory-stage8-price-source/apply-progress.md` | Modified | Merged PR 1 progress with PR 2 apply progress and TDD evidence. |

## Deviations

None — implementation matches the PR 2 UI/static/tests boundary. Backend behavior was not changed in this pass; existing backend/API PR 1 diff remains as the dependency base.

## Remaining Tasks

- [x] 3.1 Tras verificación, archivar `openspec/changes/super-inventory-stage8-price-source/` y fusionar delta en `openspec/specs/super-inventory/spec.md` solo si se solicita archive.
- [x] 3.2 Mantener PR 3 limitado a OpenSpec archive/spec sync y reportes; no mezclar backend ni UI.

## Risks

- PR 2 builds on the local PR 1 backend/API/tests diff; reviewers should keep this slice stacked after PR 1 to avoid polluted diffs.
- Static UI assertions now intentionally allow exact source-label tokens; future copy changes should avoid store/shop/history/multiple price semantics.

## Next Recommended

None — PR 3 archive/spec sync completed and the SDD cycle is closed.
