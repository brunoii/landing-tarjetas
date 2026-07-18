# Tasks: Historial manual de observaciones de precio Etapa 10

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 800-1100 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Decision needed before apply | No |
| Delivery strategy | force-chained |
| Chain strategy | stacked-to-main |

Estimated changed lines: 800-1100
Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High
Delivery strategy: force-chained

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend/API/tests append-only | PR 1 | Base `main`; endpoints y contrato persistente. |
| 2 | UI/static/tests mínimos | PR 2 | Base PR 1; cliente, formulario/listado y contratos estáticos. |
| 3 | OpenSpec archive/spec sync | PR 3 | Base PR 2; solo documentación/spec, sin código. |

## Phase 1: PR 1 Backend/API/tests

- [x] 1.1 Crear `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservation.java` con `ManyToOne`, `pricePesos`, `sourceLabel`, `observedDate`, snapshots y `createdAt`.
- [x] 1.2 Crear `SuperItemPriceObservationRepository.java` con `findRecent(Pageable)` y `findRecentByItemId(Long, Pageable)` ordenados por `createdAt desc, id desc`.
- [x] 1.3 Crear `SuperItemPriceObservationRequest.java` y `SuperItemPriceObservationResponse.java` con DTOs planos para precio, fuente, fecha y snapshots.
- [x] 1.4 Modificar `SupermarketService.java`: inyectar repo, agregar `createPriceObservation` y `listPriceObservations`, límites 50/100, validación y trim de `sourceLabel`.
- [x] 1.5 Crear o extender controller para `POST /api/super/items/{id}/price-observations` y `GET /api/super/price-observations`; ajustar etiquetas en `ApiExceptionHandler` solo si aparecen errores nuevos.
- [x] 1.6 Extender `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` para alta válida, inválidos, listado global/por item, límites, snapshot inmutable y no auto-creación desde `POST/PUT /api/super/items`.
- [x] 1.7 Agregar tests backend explícitos para `sourceLabel` con espacios externos, persistencia recortada y rechazo sobre 120 caracteres sin mutaciones colaterales.

## Phase 2: PR 2 UI/static/tests

- [x] 2.1 Modificar `src/main/resources/static/js/api.js` con funciones para `POST /api/super/items/{id}/price-observations` y `GET /api/super/price-observations`.
- [x] 2.2 Modificar `index.html` y `supermarket.js` con formulario/listado manual explícito; no guardar observación al guardar producto.
- [x] 2.3 Implementar payload/validación cliente para precio positivo, fecha no futura y `sourceLabel` recortado antes de enviar.
- [x] 2.4 Agregar prefill desde precio/fuente/fecha actuales, render de recientes y refresh tras alta sin tocar stock, movimientos, barcodes ni listas.
- [x] 2.5 Actualizar `app.js` y tokens de cache solo para assets tocados.
- [x] 2.6 Extender `static-ui-contract-tests.mjs` para formulario explícito, prefill, render, validación, trim de `sourceLabel` y llamadas a endpoints completos.
- [x] 2.7 Extender `StaticUiContractTests.java`: permitir solo tokens precisos de price-observation/history, preservar stock/movement history y mantener bloqueados stores/shops, normalized sources, comparison/charts, OCR, scraping, automation y totals.

## Phase 3: PR 3 OpenSpec archive/spec sync

- [x] 3.1 Tras apply/verify, archivar `openspec/changes/super-inventory-stage10-price-history/` y sincronizar solo specs/docs de `super-inventory` si corresponde.
- [x] 3.2 Verificar que el archive no agregue tareas de código ni amplíe tiendas, fuentes normalizadas, comparación, OCR, scraping, automation o totals.
