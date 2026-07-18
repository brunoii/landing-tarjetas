# Apply Progress: super-inventory-stage9-price-observed-date

## Status

Apply complete for PR 1 backend/API/tests, PR 2 UI/static/tests and PR 3 OpenSpec archive/spec sync.

## Workload / PR Boundary

- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Current work unit completed in this batch: PR 3 OpenSpec archive/spec sync
- Boundary: OpenSpec live spec sync, archive move and archive reporting only.
- Base dependency: PR 1 backend/API/tests and PR 2 UI/static/tests already verified PASS and retained in this merged progress artifact.
- Out of scope for this batch: backend code, UI/static code, productive tests, assets, commit/push/PR/merge.

## Completed Tasks

- [x] 1.1 Agregar `LocalDate commercialPresentationPriceObservedDate` nullable en `SuperItem` con columna `commercial_presentation_price_observed_date` y accesores.
- [x] 1.2 Incluir `commercialPresentationPriceObservedDate` en `SuperItemRequest` y `SuperItemResponse`, preservando payloads legacy con `null`/ausente.
- [x] 1.3 Actualizar `SupermarketService.applyCommercialPresentation` para normalizar, rechazar fecha futura, bloquear fecha sin presentación/precio y limpiar fuente+fecha al borrar precio o presentación.
- [x] 1.4 Actualizar `ApiExceptionHandler` con label “Fecha observada del precio” y detalle de formato date-only `YYYY-MM-DD`.
- [x] 1.5 Extender `SupermarketControllerTests` para persistencia/exposición, legacy null, fecha futura/huérfana sin mutar, limpieza y no mutación de `checked`, stock, movimientos, barcodes ni listas.
- [x] 2.1 Agregar en `index.html` el input `id="super-item-presentation-price-observed-date"`, `name="commercialPresentationPriceObservedDate"`, `type="date"` junto a la fuente.
- [x] 2.2 Actualizar `supermarket.js` para payload, validación cliente, edit/reset y preservación de fecha huérfana hasta `validateSuperItemPayload`.
- [x] 2.3 Renderizar `Observado: YYYY-MM-DD` como texto secundario junto a precio/fuente, sin formato local ni timezone.
- [x] 2.4 Actualizar cache bust Stage 9 en `index.html` y `js/app.js`, más constantes equivalentes en tests Java/Node.
- [x] 2.5 Ampliar contratos estáticos para permitir solo tokens date-only precisos y bloquear semántica de historial de precios, preservando `history` y `super-movement-history`.
- [x] 2.6 Extender `static-ui-contract-tests.mjs` para input date, payload válido, fecha futura, fecha huérfana preservada hasta validación, edit/reset y render secundario.
- [x] 3.1 Ejecutar solo sincronización documental de archive/spec: mover cambio activo y fusionar delta en `openspec/specs/super-inventory/spec.md` si apply+verify pasan.
- [x] 3.2 Mantener PR 3 sin cambios de código, tests productivos ni assets; solo OpenSpec/auditoría.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API + repository assertions | ✅ Baseline 57/57 passing before production edits | ✅ Tests referenced missing `SuperItem` getter/setter; compile failed first | ✅ `mvn -Dtest=SupermarketControllerTests test`: 62/62 passing | ✅ Legacy null, persist, update, clear, and no-collateral paths assert entity date state | ✅ Kept field nullable and date-only `LocalDate`; tests green after refactor |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API JSON | ✅ Baseline 57/57 passing before production edits | ✅ Tests asserted request/response JSON field before DTO support existed | ✅ `mvn -Dtest=SupermarketControllerTests test`: 62/62 passing | ✅ Create, list, update, omission, and malformed payload coverage | ✅ Preserved flat DTO pattern; tests green after refactor |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API + repository assertions | ✅ Baseline 57/57 passing before production edits | ✅ Tests asserted future/orphan rejection and cleanup before service support existed | ✅ `mvn -Dtest=SupermarketControllerTests test`: 62/62 passing | ✅ Covered valid date, price without date, future date, date without price/presentation, clearing price, clearing presentation | ✅ Extracted date normalization helper; tests green after refactor |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API error contract | ✅ Baseline 57/57 passing before production edits | ✅ Malformed date test expected field label/date-only detail before handler support existed | ✅ `mvn -Dtest=SupermarketControllerTests test`: 62/62 passing | ✅ Invalid semantic date and invalid JSON date format exercise different error paths | ✅ Added nested `InvalidFormatException` lookup; tests green after refactor |
| 1.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API + repository assertions | ✅ Baseline 57/57 passing before production edits | ✅ Added backend contract tests before production support | ✅ `mvn -Dtest=SupermarketControllerTests test`: 62/62 passing | ✅ 5 new tests plus one strengthened existing test cover legacy, happy path, invalids, cleanup, and no collateral mutation | ✅ Stabilized dynamic dates in tests; final run still green |
| 2.1 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static UI contract | ✅ Baseline `StaticUiContractTests`: 26/26 and Node contract PASS before UI edits | ✅ Tests required the date input id/name/type before markup existed | ✅ `mvn -Dtest=StaticUiContractTests test`: 26/26 and Node PASS | ✅ Java and Node both assert id/name/type plus Spanish help copy | ✅ Kept the input near the existing source field without adding a table column |
| 2.2 | `static-ui-contract-tests.mjs` | Unit/static JS behavior | ✅ Same static safety net before production edits | ✅ Tests required payload field, orphan-date preservation, future-date rejection, edit/reset behavior before JS support existed | ✅ Node contract PASS after implementation | ✅ Valid date, orphan date, missing presentation, malformed date, future date, price-without-date, create/update/edit/reset paths | ✅ Compared local date-only values without using render/local timezone formatting |
| 2.3 | `static-ui-contract-tests.mjs`, `StaticUiContractTests.java` | Unit/static render contract | ✅ Same static safety net before production edits | ✅ Tests required `Observado: 2026-07-18` render and exported label before production function existed | ✅ Node contract PASS and `StaticUiContractTests` PASS | ✅ Null date returns empty label; source+date renders both secondary texts; table row shows observed date | ✅ Reused the existing secondary `<small class="super-fuente-precio">` pattern |
| 2.4 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static cache contract | ✅ Same static safety net before production edits | ✅ Tests expected Stage 9 token before `index.html`/`app.js` were updated | ✅ `mvn -Dtest=StaticUiContractTests test`: 26/26 and Node PASS | ✅ Java and Node assert `index.html` app token and `app.js` supermarket import token | ➖ None needed — token-only structural change |
| 2.5 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static scope guard | ✅ Same static safety net before production edits | ✅ Tests blocked datetime/timestamp/ObservedAt and price-history tokens while requiring movement history tokens | ✅ Java/Node static contracts PASS | ✅ Negative guards plus positive assertions for `data-super-action="history"` and `super-movement-history` | ✅ Extended allowlist narrowly for observed-date tokens only |
| 2.6 | `static-ui-contract-tests.mjs` | Node static UI behavior | ✅ Same static safety net before production edits | ✅ Node behavior tests failed on missing input/payload/render/edit/reset support | ✅ Node contract PASS after implementation | ✅ Payload, validation, render, DOM setup, create/update, edit and reset cases | ✅ Fake DOM updated only for the new input field |

## Test Summary

- Total PR 2 tests written/extended: Java static contract assertions and Node static UI behavior assertions for the Stage 9 date field.
- Total targeted tests passing: `StaticUiContractTests` 26/26 and Node static UI contract PASS.
- Full Maven regression: `mvn test` PASS, 240 tests.
- Layers used: Static UI contract, Node unit/static behavior, full Maven regression.
- Approval tests: Static contract safety net for existing UI/static files before modifying them.
- Pure functions created: 3 small JS helpers/exports around date-only validation and observed-date secondary label.

## Tests Run

- `mvn -Dtest=StaticUiContractTests test` — baseline before PR 2 production edits: PASS, 26 tests.
- `node src/test/resources/static-ui-contract-tests.mjs` — baseline before PR 2 production edits: PASS.
- `mvn -Dtest=StaticUiContractTests test; node src/test/resources/static-ui-contract-tests.mjs` — RED after tests first: FAIL, missing Stage 9 input/token/render behavior.
- `mvn -Dtest=StaticUiContractTests test; node src/test/resources/static-ui-contract-tests.mjs` — GREEN after implementation: PASS.
- `git diff --check` — PASS; Git emitted LF→CRLF working-tree warnings only.
- `mvn test` — PASS, 240 tests, 0 failures, 0 errors, 0 skipped.

## Files Changed

| File | Action | What changed |
|------|--------|--------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modified in PR 1 | Added nullable `LocalDate commercialPresentationPriceObservedDate` column and accessors. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` | Modified in PR 1 | Added nullable request `LocalDate` field. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` | Modified in PR 1 | Added nullable response `LocalDate` field emitted as date-only JSON. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified in PR 1 | Added observed-date validation, future-date rejection, orphan-date rejection, and cleanup with price/presentation. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified in PR 1 | Added Spanish field label and date-only format detail for malformed `LocalDate` JSON. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified in PR 1 | Added backend/API coverage for date persistence, legacy null, invalids, cleanup, and no collateral mutation. |
| `src/main/resources/static/index.html` | Modified in PR 2 | Added date input with required id/name/type near source and bumped app token to Stage 9. |
| `src/main/resources/static/js/app.js` | Modified in PR 2 | Bumped `supermarket.js` import token to Stage 9. |
| `src/main/resources/static/js/supermarket.js` | Modified in PR 2 | Added observed-date payload, client validation, edit/reset capture, and secondary render label. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified in PR 2 | Added Stage 9 token checks, input contract, observed-date token allowlist, and price-history blockers. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified in PR 2 | Added Node contract coverage for payload, validation, render, DOM create/update/edit/reset, and cache tokens. |
| `openspec/specs/super-inventory/spec.md` | Modified in PR 3 | Merged the accepted Stage 9 MODIFIED requirements into the live super-inventory spec. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/tasks.md` | Modified in PR 3 | Marked tasks 3.1 and 3.2 complete after archive/spec sync. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/apply-progress.md` | Modified in PR 3 | Closed PR 3 archive/spec sync progress and next step state. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/archive-report.md` | Added in PR 3 | Recorded archive validation, Engram traceability, risks and publication boundary. |

## Deviations

None — PR 3 was limited to OpenSpec archive/spec sync. Backend, UI/static code, productive tests and assets were not changed in this batch.

## Remaining Tasks

- [x] 3.1 Ejecutar solo sincronización documental de archive/spec: mover cambio activo y fusionar delta en `openspec/specs/super-inventory/spec.md` si apply+verify pasan.
- [x] 3.2 Mantener PR 3 sin cambios de código, tests productivos ni assets; solo OpenSpec/auditoría.

## Next Recommended

None — PR 3 archive/spec sync completed and the SDD cycle is closed.

## Issues Found

None.
