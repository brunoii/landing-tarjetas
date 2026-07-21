# Tasks: Super Inventory Etapa 11 - fuentes de precio

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-850 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static/tests → PR 3 OpenSpec archive/spec sync |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Estimated changed lines: 650-850
400-line budget risk: High
Chained PRs recommended: Yes
Decision needed before apply: No
Delivery strategy: force-chained
Chain strategy: stacked-to-main

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Persistir y exponer fuentes mínimas, y vincular observaciones. | PR 1 | Base `main`; incluye tests backend/API. |
| 2 | Conectar UI mínima, fallback libre y contratos estáticos. | PR 2 | Base `main` tras PR 1; incluye tests UI/static. |
| 3 | Sincronizar OpenSpec tras verificación. | PR 3 | Base `main` tras PR 2; solo docs/spec/archive. |

## Phase 1: PR 1 Backend/API/tests

- [x] 1.1 Crear `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSource.java` con `name`, `normalizedKey`, `active`, timestamps, unicidad e índices; sin campos de tienda/comercio.
- [x] 1.2 Crear `SuperPriceSourceRepository.java` con listado activo por nombre y búsqueda por clave normalizada para duplicados.
- [x] 1.3 Crear `SuperPriceSourceRequest.java`, `SuperPriceSourceResponse.java` y `SuperPriceSourceController.java` con solo `GET/POST /api/super/price-sources`; evitar CRUD/admin completo.
- [x] 1.4 Modificar `SuperItemPriceObservation.java`, `SuperItemPriceObservationRequest.java`, `SuperItemPriceObservationResponse.java` y `SuperItemPriceObservationRepository.java` para `priceSourceId` nullable, `sourceLabel` snapshot nullable y `left join fetch`.
- [x] 1.5 Modificar `SupermarketService.java`, `SupermarketLimits.java` y, si corresponde, `ApiExceptionHandler.java` para nombre recortado, rechazo de blanco/vacío, duplicado por clave normalizada, fuente inactiva/inexistente y exclusividad `priceSourceId` XOR `sourceLabel`.
- [x] 1.6 Extender `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`: crear/listar, blank/trim, duplicados normalizados, observación con fuente/libre/sin fuente, ambos campos, legacy sin backfill y no mutar producto, stock, movimientos, barcodes ni listas.

## Phase 2: PR 2 UI/static/tests

- [x] 2.1 Modificar `src/main/resources/static/js/api.js` y `app.js` con `superPriceSources()`, `createSuperPriceSource(payload)` y cache token Stage 11.
- [x] 2.2 Modificar `src/main/resources/static/index.html`, `js/supermarket.js` y `css/styles.css` con selector, alta inline mínima y fallback de texto libre; sin edición/borrado/renombrado/desactivación.
- [x] 2.3 Ajustar submit/render de observaciones en `supermarket.js`: enviar solo `priceSourceId` o `sourceLabel`, soportar `sourceLabel` nullable y refrescar fuentes/observaciones tras alta.
- [x] 2.4 Extender `StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` para helpers, selector/alta, fallback, blank/trim/duplicados y allow-list exacta `priceSource`, `price-source(s)`, `SuperPriceSource`, bloqueando store/shop/commerce/comparison/charts/OCR/scraping/automation/totals.

## Phase 3: PR 3 OpenSpec archive/spec sync

- [x] 3.1 Tras verify, sincronizar solo `openspec/specs/super-inventory/spec.md` y artefactos bajo `openspec/changes/super-inventory-stage11-price-sources/`.
- [x] 3.2 Mantener PR 3 docs/spec únicamente: no código, no UI, no tests de runtime; reflejar que PR 1 y PR 2 ya validaron implementación.
