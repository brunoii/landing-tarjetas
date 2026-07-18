# Apply Progress: Historial manual de observaciones de precio Etapa 10

## Status

- Change: `super-inventory-stage10-price-history`
- Mode: Strict TDD
- Artifact store: hybrid
- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Current work unit completed in this batch: PR 3 OpenSpec archive/spec sync only
- Completed tasks: 16/16

## Completed Tasks

- [x] 1.1-1.7 Backend/API/tests append-only price observations completed.
- [x] 2.1 Agregar funciones API de observaciones de precio en `api.js`.
- [x] 2.2 Agregar formulario/listado manual explícito en `index.html` y `supermarket.js` sin auto-crear observaciones al guardar producto.
- [x] 2.3 Implementar payload/validación cliente: precio positivo, fecha no futura y `sourceLabel` recortado a 120 antes de enviar.
- [x] 2.4 Agregar prefill desde precio/fuente/fecha actuales, render de recientes y refresh tras alta sin tocar stock, movimientos, barcodes ni listas.
- [x] 2.5 Actualizar tokens de cache solo para assets tocados (`api.js`, `supermarket.js`, `app.js`, `index.html`) y constantes Java/Node.
- [x] 2.6 Extender `static-ui-contract-tests.mjs` para formulario explícito, prefill, render, validación, trim y endpoints completos.
- [x] 2.7 Extender `StaticUiContractTests.java` para permitir solo tokens precisos de observaciones de precio y mantener bloqueos de alcance.
- [x] 3.1 Archivar `openspec/changes/super-inventory-stage10-price-history/` y sincronizar solo specs/docs de `super-inventory`.
- [x] 3.2 Verificar que el archive no agregue tareas de código ni amplíe tiendas, fuentes normalizadas, comparación, OCR, scraping, automation o totals.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Integration/API | ✅ 62/62 baseline | ✅ Compile RED por repositorio inexistente | ✅ 67/67 final | ✅ Alta válida verifica entidad/snapshots/createdAt | ✅ Entidad mínima |
| 1.2 | same | Integration/API | ✅ 62/62 baseline | ✅ Tests exigieron repo/orden | ✅ 67/67 final | ✅ Global/per-item/default 50/max 100/order | ✅ `join fetch item` |
| 1.3 | same | Integration/API | ✅ 62/62 baseline | ✅ Tests exigieron DTOs planos | ✅ 67/67 final | ✅ Request/response precio/fuente/fecha/snapshots | ✅ Sin campos fuera de alcance |
| 1.4 | same | Integration/API | ✅ 62/62 baseline | ✅ Tests exigieron servicio inexistente | ✅ 67/67 final | ✅ Válidos, inválidos, trim, fecha futura, límites | ✅ Helpers acotados |
| 1.5 | same | Integration/API | ✅ 62/62 baseline | ✅ Endpoints inexistentes | ✅ 67/67 final | ✅ POST subrecurso y GET global | ✅ Controller dedicado |
| 1.6 | same | Integration/API | ✅ 62/62 baseline | ✅ Tests primero | ✅ 67/67 final | ✅ Alta, inválidos, listado, snapshot, no auto-creación | ✅ Assertions de comportamiento |
| 1.7 | same | Integration/API | ✅ 62/62 baseline | ✅ Tests sourceLabel primero | ✅ 67/67 final | ✅ Trim y rechazo >120 sin colateralidad | ✅ Validación post-trim en servicio |
| 2.1 | `src/test/resources/static-ui-contract-tests.mjs`, `StaticUiContractTests.java` | Static UI/API contract | ✅ `StaticUiContractTests` 26/26 and Node static script baseline PASS | ✅ Tests exigieron `createSuperItemPriceObservation`/`superPriceObservations` inexistentes y fallaron | ✅ Node static script PASS; `mvn -Dtest=StaticUiContractTests test` PASS 26/26 | ✅ POST and GET endpoint calls plus payload body assertions | ✅ API helpers mirror existing `withQuery`/`request` style |
| 2.2 | same | Static UI behavior | ✅ Existing static UI suite baseline PASS | ✅ Tests exigieron form/list selectors and behavior absent | ✅ Node static script PASS; `StaticUiContractTests` PASS | ✅ Form markup, explicit submit, no product-save observation calls | ✅ Minimal section using existing card/form/table patterns |
| 2.3 | same | Static UI behavior | ✅ Existing static UI suite baseline PASS | ✅ Payload/validation tests failed before functions existed | ✅ Node static script PASS; `StaticUiContractTests` PASS | ✅ Positive/zero/negative price, invalid/future date, blank and overlong source trim | ✅ Pure payload and validation helpers extracted |
| 2.4 | same | Static UI behavior | ✅ Existing static UI suite baseline PASS | ✅ Tests expected prefill/render/refresh paths absent | ✅ Node static script PASS; `StaticUiContractTests` PASS | ✅ Prefill from selected item, recent row render, refresh after create, and no stock/list mutation calls | ✅ Render and prefill helpers isolated from stock/movement flows |
| 2.5 | `static-ui-contract-tests.mjs`, `StaticUiContractTests.java` | Static cache contract | ✅ Existing cache contract baseline PASS | ✅ Tests expected Stage 10 cache tokens and failed on Stage 9/Stage 5 tokens | ✅ Node static script PASS; `StaticUiContractTests` PASS | ✅ Index/app imports and direct API import scanner verify only touched assets changed | ✅ Tokens centralized in Java/Node constants |
| 2.6 | `src/test/resources/static-ui-contract-tests.mjs` | Static UI behavior | ✅ Node static script baseline PASS | ✅ Node tests failed before UI/API helpers existed | ✅ `node src/test/resources/static-ui-contract-tests.mjs` PASS | ✅ Endpoints, payload, validation, trim, prefill, render, refresh, and separation assertions | ✅ Assertions kept behavior-focused; no CSS class coupling added |
| 2.7 | `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Static contract | ✅ `mvn -Dtest=StaticUiContractTests test` baseline PASS 26/26 | ✅ Java static tests failed on missing allowed price-observation tokens/cache updates | ✅ `mvn -Dtest=StaticUiContractTests test` PASS 26/26 | ✅ Allows precise price-observation tokens while preserving stock/movement history and unsupported-term blocks | ✅ Allow-list remains narrow; stores/shops/comparison/charts/OCR/scraping/automation/totals still blocked |

## Test Summary

- Total tests written/extended: static Node assertions and Java static contract assertions for PR 2 behavior and tokens.
- Total tests passing: `StaticUiContractTests` 26/26; Node static UI contract script PASS.
- Layers used: Static UI contract/behavior (2 files); Backend/API regression not run because backend behavior was not touched in PR 2.
- Approval tests: Existing static suites acted as safety net for static assets; no pure refactoring task.
- Pure functions created: 4 (`superPriceObservationPayloadFromValues`, `validateSuperPriceObservationPayload`, `superPriceObservationPresentationLabel`, `superPriceObservationRowHtml`).

## Tests Run

- `mvn -Dtest=StaticUiContractTests test` — baseline before PR 2 production changes: PASS, 26 tests.
- `node src/test/resources/static-ui-contract-tests.mjs` — baseline before PR 2 production changes: PASS.
- `mvn -Dtest=StaticUiContractTests test` — RED after tests first: FAIL on missing Stage 10 UI/API contracts.
- `node src/test/resources/static-ui-contract-tests.mjs` — RED after tests first: FAIL on missing explicit observation UI/API contracts.
- `node src/test/resources/static-ui-contract-tests.mjs` — GREEN/final PR 2 evidence: PASS.
- `mvn -Dtest=StaticUiContractTests test` — GREEN/final PR 2 evidence: PASS, 26 tests.

## Files Changed

| File | Action | Summary |
|------|--------|---------|
| `src/main/resources/static/js/api.js` | Modified | Added explicit POST/GET price-observation API helpers. |
| `src/main/resources/static/js/supermarket.js` | Modified | Added explicit manual observation form handling, validation, prefill, recent render and refresh after create. |
| `src/main/resources/static/index.html` | Modified | Added minimal manual price observation section/table and updated app cache token. |
| `src/main/resources/static/js/app.js` | Modified | Updated cache tokens for touched `api.js` and `supermarket.js`. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Added static behavior coverage for endpoints, form, validation, trim, prefill, render, refresh and separation. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Added static contract coverage and narrow allow-list for price-observation tokens/cache updates. |
| `openspec/changes/super-inventory-stage10-price-history/tasks.md` | Modified | Marked assigned tasks 2.1-2.7 complete. |
| `openspec/changes/super-inventory-stage10-price-history/apply-progress.md` | Modified | Merged PR 1 and PR 2 progress/TDD evidence. |
| `openspec/specs/super-inventory/spec.md` | Modified in PR 3 | Merged the accepted Stage 10 ADDED/MODIFIED requirements into the live super-inventory spec. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/tasks.md` | Modified in PR 3 | Marked tasks 3.1 and 3.2 complete after archive/spec sync. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/apply-progress.md` | Modified in PR 3 | Closed PR 3 archive/spec sync progress and next step state. |
| `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/archive-report.md` | Added in PR 3 | Recorded archive validation, Engram traceability, risks and publication boundary. |

## Deviations

- None — implementation matches the PR 2 UI/static/tests slice of `design.md`.
- Backend regression `SupermarketControllerTests` was not run because no backend Java behavior was touched in PR 2.
- PR 3 was limited to OpenSpec archive/spec sync. Backend, UI/static code, productive tests and assets were not changed in this batch.

## Remaining Tasks

- [x] 3.1 Tras apply/verify, archivar `openspec/changes/super-inventory-stage10-price-history/` y sincronizar solo specs/docs de `super-inventory` si corresponde.
- [x] 3.2 Verificar que el archive no agregue tareas de código ni amplíe tiendas, fuentes normalizadas, comparación, OCR, scraping, automation o totals.

## Next Recommended

None — PR 3 archive/spec sync completed and the SDD cycle is closed.

## Workload / PR Boundary

- Mode: chained PR slice.
- Current work unit completed in this batch: PR 3 OpenSpec archive/spec sync only.
- Boundary: starts from verified PR 1 backend/API/tests and PR 2 UI/static/tests evidence, ends with live spec sync, archive move and archive reporting.
- Out of scope for this apply: backend behavior changes, UI/static changes, productive tests, assets, commits, push, PR creation or merge.
- Review budget note: PR 3 is scoped to OpenSpec/audit artifacts only and should not include backend/UI diffs in its review boundary.
