# Apply Progress: Super Inventory Etapa 11 - fuentes de precio

## Status

success — PR 1 backend/API/tests, PR 2 UI/static/tests y PR 3 OpenSpec spec sync/doc-only completados para tareas 1.1 a 3.2. Queda pendiente la verificación formal de PR 3 antes de archivar.

## Workload / PR Boundary

| Field | Value |
|-------|-------|
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |
| Current work unit | PR 3 OpenSpec spec sync/doc-only |
| Boundary | Sincronización de la especificación viva `openspec/specs/super-inventory/spec.md`, `tasks.md` y este progreso; sin código Java, UI, CSS, JS ni tests de runtime. |
| Base expectation | Base `main` después de PR 2; este PR cierra la sincronización documental previa al verify/archive final. |
| Dependency diagram | `main` → PR 1 backend/API/tests → PR 2 UI/static/tests → 📍 PR 3 OpenSpec spec sync/doc-only |

## Completed Tasks

- [x] 1.1 Crear `SuperPriceSource` con `name`, `normalizedKey`, `active`, timestamps, unicidad e índices; sin campos de tienda/comercio.
- [x] 1.2 Crear `SuperPriceSourceRepository` con listado activo por nombre y búsqueda por clave normalizada.
- [x] 1.3 Crear DTOs y `SuperPriceSourceController` con solo `GET/POST /api/super/price-sources`.
- [x] 1.4 Extender observaciones con `priceSourceId` nullable, snapshot `sourceLabel` y `left join fetch`.
- [x] 1.5 Agregar lógica de servicio/límites/errores para trim, blanco, duplicado normalizado, fuente inactiva/inexistente y exclusividad `priceSourceId` XOR `sourceLabel`.
- [x] 1.6 Extender `SupermarketControllerTests` para creación/listado, validaciones, observaciones con fuente/libre/sin fuente, legacy nullable y no mutación colateral.
- [x] 2.1 Agregar helpers frontend `superPriceSources()` y `createSuperPriceSource(payload)` y avanzar cache token a Stage 11.
- [x] 2.2 Agregar selector de fuente, alta inline mínima, fallback libre y estilos mínimos sin controles admin de fuente.
- [x] 2.3 Ajustar payload/render de observaciones para enviar solo `priceSourceId` o `sourceLabel`, soportar `sourceLabel` nullable y refrescar fuentes/observaciones tras alta inline.
- [x] 2.4 Extender contratos estáticos Java/Node para helpers, UI, fallback, trim/blank, flujo de alta y allow-list exacta de fuentes.
- [x] 3.1 Sincronizar la especificación viva `openspec/specs/super-inventory/spec.md` con el comportamiento aceptado de Etapa 11 y mantener artefactos del cambio coherentes.
- [x] 3.2 Mantener el slice PR 3 como docs/spec únicamente y preservar la evidencia de validación de PR 1 y PR 2 sin rerun innecesario.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline (`mvn -Dtest=SupermarketControllerTests test`) | ✅ Test referenced missing `SuperPriceSource` entity/limit/getter and failed compilation | ✅ 72/72 after entity implementation | ✅ normalized key, active flag, timestamps and no commercial fields covered | ✅ 72/72 after cleanup |
| 1.2 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline | ✅ Test referenced missing `SuperPriceSourceRepository` and failed compilation | ✅ 72/72 after repository implementation | ✅ active listing and duplicate normalized lookup covered | ✅ 72/72 after cleanup |
| 1.3 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline | ✅ `GET/POST /api/super/price-sources` tests written before controller/DTOs | ✅ 72/72 after controller/DTO implementation | ✅ create, list, blank, long, duplicate and response shape covered | ✅ 72/72 after cleanup |
| 1.4 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline | ✅ Tests asserted `priceSourceId`, nullable relation and legacy rows before relation existed | ✅ 72/72 after observation model/repository changes | ✅ reusable source, free text, no source and legacy nullable paths covered | ✅ 72/72 after cleanup |
| 1.5 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline | ✅ Validation tests for XOR, inactive/missing source and duplicate source written first | ✅ 72/72 after service/lifecycle implementation | ✅ conflict, bad request, not found, trim and no product mutation covered | ✅ 72/72 after cleanup |
| 1.6 | `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | API integration | ✅ 67/67 baseline | ✅ Five backend/API tests added before production implementation | ✅ 72/72 after implementation | ✅ 5 tests cover 15+ behavior branches and edge cases | ✅ 72/72 after replacing brittle JSON parsing with repository lookup |
| 2.1 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static UI contract | ✅ 26/26 baseline (`mvn -Dtest=StaticUiContractTests test`) | ✅ Tests expected Stage 11 cache tokens and missing API helpers first; command failed as expected | ✅ 26/26 after helpers/tokens implementation | ✅ GET and POST helper tokens plus direct import cache checks covered | ✅ 26/26 after app fake DOM compatibility cleanup |
| 2.2 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static UI contract | ✅ 26/26 baseline | ✅ Tests expected missing selector, inline create form, limit wiring and styles first | ✅ 26/26 after HTML/CSS/UI implementation | ✅ selector, create form, source list options, blank create validation and no admin controls covered | ✅ 26/26 after responsive CSS and fake DOM cleanup |
| 2.3 | `static-ui-contract-tests.mjs` | Static UI contract | ✅ 26/26 baseline | ✅ Tests expected `priceSourceId` payload precedence, nullable `sourceLabel`, source refresh and observation refresh before implementation | ✅ 26/26 after submit/render implementation | ✅ reusable source, free-text fallback, blank/no-source and inline creation refresh paths covered | ✅ 26/26 after refresh call expectation cleanup |
| 2.4 | `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Static UI contract | ✅ 26/26 baseline | ✅ Contracts added for helpers, selector/create UI, fallback, trim/blank and Stage 11 allow-list first; command failed | ✅ 26/26 targeted static UI tests and 250/250 full suite | ✅ Java + Node contracts exercise multiple UI/API/static paths and forbidden semantics scan | ✅ Static allow-list narrowed to precise source tokens and full suite remained green |
| 3.1 | N/A | OpenSpec documentation/spec sync | N/A — docs/spec only | N/A — no runtime behavior or production code in this slice | ✅ Live spec synced to accepted Stage 11 behavior | N/A — delta-to-live spec sync has a single intended output | ✅ Scope checked against Stage 11 boundary |
| 3.2 | N/A | OpenSpec documentation/spec sync | N/A — docs/spec only | N/A — no runtime behavior or production code in this slice | ✅ PR 1/PR 2 validation evidence preserved without rerun | N/A — documentation-only status update | ✅ Tasks/progress updated without touching source/test files |

## Tests Run

| Command | Result | Notes |
|---------|--------|-------|
| `mvn -Dtest=SupermarketControllerTests test` | PASS — 72 tests | PR 1 preserved evidence; not rerun in PR 3. |
| `mvn -Dtest=StaticUiContractTests test` | PASS — 26 tests | PR 2 preserved evidence; not rerun in PR 3. |
| `mvn test` | PASS — 250 tests | PR 2 full-suite evidence preserved; not rerun in PR 3 because this slice is docs/spec only. |
| `openspec --version` | WARNING — unavailable | OpenSpec CLI is not installed or not on PATH in this environment; non-blocking per PR 3 constraint. |

## Files Changed

| File | Action | Notes |
|------|--------|-------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSource.java` | Created | PR 1: Minimal source catalog entity with normalized unique key, active flag and timestamps. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSourceRepository.java` | Created | PR 1: Active listing, normalized duplicate lookup and active id resolution. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSourceRequest.java` | Created | PR 1: Name-only request with existing validation style. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSourceResponse.java` | Created | PR 1: Minimal response without commercial/store/comparison fields. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSourceController.java` | Created | PR 1: Only `GET` and `POST` endpoints under `/api/super/price-sources`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservation.java` | Modified | PR 1: Added nullable FK to `SuperPriceSource` while preserving `sourceLabel` snapshot. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` | Modified | PR 1: Accepts nullable `priceSourceId`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationResponse.java` | Modified | PR 1: Exposes nullable `priceSourceId`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRepository.java` | Modified | PR 1: Adds `left join fetch observation.priceSource`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modified | PR 1: Creates/lists sources and resolves observation source validation/snapshot rules. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` | Modified | PR 1: Adds `PRICE_SOURCE_NAME_MAX_LENGTH = 120`. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified | PR 1: Adds Spanish label for `priceSourceId`. |
| `src/main/resources/static/js/api.js` | Modified | PR 2: Adds `GET/POST /api/super/price-sources` helpers. |
| `src/main/resources/static/js/app.js` | Modified | PR 2: Updates Stage 11 cache-busting tokens for API/supermarket imports. |
| `src/main/resources/static/index.html` | Modified | PR 2: Adds minimal price-source selector and inline create form while preserving free-text fallback. |
| `src/main/resources/static/js/supermarket.js` | Modified | PR 2: Loads/renders source list, creates sources inline, builds exclusive observation payloads, supports nullable source labels. |
| `src/main/resources/static/css/styles.css` | Modified | PR 2: Adds minimal responsive layout for inline source creation. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | PR 2: Extends static contract for Stage 11 tokens/helpers/UI and forbidden semantics allow-list. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | PR 2: Extends Node static behavior coverage for payloads, source create/refresh, selector, fallback and allow-list. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modified | PR 1: Backend/API TDD coverage preserved. |
| `openspec/specs/super-inventory/spec.md` | Modified | PR 3: Synced live spec with accepted Stage 11 price-source behavior. |
| `openspec/changes/super-inventory-stage11-price-sources/tasks.md` | Modified | PR 3: Marks tasks 3.1-3.2 complete; cumulative 12/12. |
| `openspec/changes/super-inventory-stage11-price-sources/apply-progress.md` | Modified | PR 3: Merged cumulative progress through PR 3, preserving prior PR 1/PR 2 evidence. |

## PR 3 Changed Files Only

| File | Action | Notes |
|------|--------|-------|
| `openspec/specs/super-inventory/spec.md` | Modified | Added price-source requirement and updated Stage 11 limits/observation requirements. |
| `openspec/changes/super-inventory-stage11-price-sources/tasks.md` | Modified | Marked tasks 3.1 and 3.2 complete. |
| `openspec/changes/super-inventory-stage11-price-sources/apply-progress.md` | Modified | Recorded PR 3 docs/spec-only completion and preserved prior validation evidence. |

## Deviations

None — PR 3 stayed docs/spec only and did not modify Java, static UI, runtime tests, commits, branches, PRs, or archive location.

## Remaining Tasks

None in `tasks.md` — 12/12 tasks are complete. Next phase should verify PR 3 and then archive only after verification passes.

## Risks

- OpenSpec CLI is unavailable in this environment, so CLI validation could not be run during PR 3 apply.
- `git status` still includes cumulative PR 1 + PR 2 source/test changes and untracked OpenSpec/source files from earlier slices; PR 3 preparation must stage only the docs/spec files intended for this slice.
