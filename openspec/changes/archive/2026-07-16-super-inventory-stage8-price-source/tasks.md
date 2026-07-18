# Tasks: Fuente manual del precio de referencia Etapa 8

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-850 cambio completo; PRs apilados planificados bajo 800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static/tests → PR 3 OpenSpec archive/spec sync si aplica |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Estimated changed lines: 650-850 cambio completo; PRs apilados planificados bajo 800
400-line budget risk: High
Chained PRs recommended: Yes
Decision needed before apply: No
Delivery strategy: force-chained
Chain strategy: stacked-to-main

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Agregar fuente manual al contrato backend/API | PR 1 | Base `main`; incluir pruebas backend. |
| 2 | Capturar, validar y renderizar fuente en UI | PR 2 | Base `main` luego de PR 1; incluir static tests. |
| 3 | Sincronizar OpenSpec aceptado | PR 3 | Base `main` luego de PR 2; solo docs/spec/archive. |

## Phase 1: Backend/API/tests

- [x] 1.1 Agregar `ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH = 120` en `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java`.
- [x] 1.2 Agregar `commercialPresentationPriceSourceLabel` nullable en `SuperItem.java` como columna `commercial_presentation_price_source_label` de longitud 120, con getters/setters.
- [x] 1.3 Agregar `commercialPresentationPriceSourceLabel` nullable en `SuperItemRequest.java` con `@Size(max=120)` y en `SuperItemResponse.java`.
- [x] 1.4 Actualizar `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` con label de validación para `commercialPresentationPriceSourceLabel`.
- [x] 1.5 Actualizar `SupermarketService.java`: trim a `null`, fuente solo con precio válido, limpieza al quitar precio/presentación y preservación de checked, stock, movimientos, barcodes y listas.
- [x] 1.6 Extender `SupermarketControllerTests.java` para legacy null, fuente trimeada, precio sin fuente, fuente inválida/huérfana, limpieza, atomicidad y no mutación colateral.
- [x] 1.7 Ejecutar `mvn -Dtest=SupermarketControllerTests test` como evidencia de PR 1.

## Phase 2: UI/static/tests

- [x] 2.1 Agregar en `src/main/resources/static/index.html` input `#super-item-presentation-price-source-label`, `name="commercialPresentationPriceSourceLabel"`, `data-super-limit="priceSourceLabel"` y ayuda secundaria sin tokens de tienda/historial.
- [x] 2.2 Actualizar `src/main/resources/static/js/supermarket.js` para payload, validación client-side, edición, reset y limpieza de fuente cuando no haya precio/presentación.
- [x] 2.3 Renderizar `commercialPresentationPriceSourceLabel` como texto secundario del precio, sin columna nueva, totales ni mezcla con lista manual/sugerida.
- [x] 2.4 Aplicar cache-busting Etapa 8: actualizar token de `./supermarket.js` en `app.js` si cambia el módulo y token de `/js/app.js` en `index.html` si cambia `app.js`.
- [x] 2.5 Actualizar constantes Stage 8 en `StaticUiContractTests.java` y `static-ui-contract-tests.mjs` para los tokens de cache afectados.
- [x] 2.6 Mantener guards estáticos precisos: permitir solo tokens de fuente manual/source-label y seguir bloqueando stores/shops/history/multiple prices/totals/automation.
- [x] 2.7 Ejecutar `mvn -Dtest=StaticUiContractTests test` y `node src/test/resources/static-ui-contract-tests.mjs` como evidencia de PR 2.

## Phase 3: OpenSpec sync

- [x] 3.1 Tras verificación, archivar `openspec/changes/super-inventory-stage8-price-source/` y fusionar delta en `openspec/specs/super-inventory/spec.md` solo si se solicita archive.
- [x] 3.2 Mantener PR 3 limitado a OpenSpec archive/spec sync y reportes; no mezclar backend ni UI.
