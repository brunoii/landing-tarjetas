# Tasks: Fecha observada del precio de referencia

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 520-760 |
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
| 1 | Persistir y validar fecha date-only en API | PR 1 | Base `main`; incluye tests backend. |
| 2 | Exponer fecha en UI y contratos estáticos | PR 2 | Base `main` después de PR 1; incluye tests Node/Java. |
| 3 | Sincronizar OpenSpec | PR 3 | Base `main` después de PR 2; solo docs/spec. |

## Phase 1: PR 1 Backend/API/tests

- [x] 1.1 Agregar `LocalDate commercialPresentationPriceObservedDate` nullable en `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` con columna `commercial_presentation_price_observed_date` y accesores.
- [x] 1.2 Incluir `commercialPresentationPriceObservedDate` en `SuperItemRequest.java` y `SuperItemResponse.java`, preservando payloads legacy con `null`/ausente.
- [x] 1.3 Actualizar `SupermarketService.applyCommercialPresentation` para normalizar, rechazar fecha futura, bloquear fecha sin presentación/precio y limpiar fuente+fecha al borrar precio o presentación.
- [x] 1.4 Actualizar `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` con label “Fecha observada del precio” y detalle de formato date-only `YYYY-MM-DD` cuando corresponda.
- [x] 1.5 Extender `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` para persistencia/exposición, legacy null, fecha futura/huérfana sin mutar, limpieza y no mutación de `checked`, stock, movimientos, barcodes ni listas.

## Phase 2: PR 2 UI/static/tests

- [x] 2.1 Agregar en `src/main/resources/static/index.html` el input `id="super-item-presentation-price-observed-date"`, `name="commercialPresentationPriceObservedDate"`, `type="date"` junto a la fuente.
- [x] 2.2 Actualizar `src/main/resources/static/js/supermarket.js` para payload, validación cliente, edit/reset y preservación de fecha huérfana hasta `validateSuperItemPayload`, siguiendo la lección de Etapa 8.
- [x] 2.3 Renderizar `Observado: YYYY-MM-DD` como texto secundario junto a precio/fuente, sin formato local ni timezone.
- [x] 2.4 Actualizar cache bust Stage 9 en `index.html` y `js/app.js`; actualizar constantes equivalentes en `StaticUiContractTests.java` y `static-ui-contract-tests.mjs`.
- [x] 2.5 Ampliar contratos estáticos para permitir solo tokens date-only precisos y bloquear semántica de historial de precios, preservando tokens/features existentes de historial de stock/movimientos (`history`, `super-movement-history`).
- [x] 2.6 Extender `src/test/resources/static-ui-contract-tests.mjs` para input date, payload válido, fecha futura, fecha huérfana preservada hasta validación, edit/reset y render secundario.

## Phase 3: PR 3 OpenSpec archive/spec sync

- [x] 3.1 Ejecutar solo sincronización documental de archive/spec: mover cambio activo y fusionar delta en `openspec/specs/super-inventory/spec.md` si apply+verify pasan.
- [x] 3.2 Mantener PR 3 sin cambios de código, tests productivos ni assets; solo OpenSpec/auditoría.
