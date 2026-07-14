# Tasks: Etapa 2 — Inventario del Super

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 650-800 |
| Review budget | 800 changed lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend → PR 2 UI → PR 3 verification polish |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend snapshot, movement and endpoint | PR 1 | base = feature/tracker branch; tests included. |
| 2 | UI mínima de stock y cantidad rápida | PR 2 | base = PR 1 branch; sin historial visible. |
| 3 | Verificación final y ajustes menores | PR 3 | base = PR 2 branch; solo evidencias/polish. |

## Phase 1: Backend RED

- [x] 1.1 En `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, agregar tests fallidos para `currentStock=null`, `quickQuantity` válida/inválida y rechazo de `currentStock` en create/update genérico.
- [x] 1.2 En el mismo archivo, agregar test fallido de `POST /api/super/items/{id}/stock-adjustments`: establece stock absoluto, no delta, y persiste movimiento + snapshot atómicamente.

## Phase 2: Backend GREEN

- [x] 2.1 Modificar `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` con `currentStock` y `quickQuantity`; crear `SuperItemStockMovement.java` y `SuperItemStockMovementRepository.java`.
- [x] 2.2 Modificar `SuperItemRequest.java`, `SuperItemResponse.java`, crear `SuperItemStockAdjustmentRequest.java` y agregar labels en `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java`.
- [x] 2.3 Modificar `SupermarketService.java`: validar decimales, rechazar stock genérico, guardar movimiento interno y snapshot en una misma transacción.
- [x] 2.4 Modificar `SuperItemController.java` con `POST /api/super/items/{id}/stock-adjustments` que devuelve `SuperItemResponse`.

## Phase 3: UI RED/GREEN

- [x] 3.1 En `StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs`, agregar tests fallidos para payload sin stock genérico, helper de ajuste, “Sin cargar” vs `0`, lista con `quickQuantity` y ausencia de historial/precio/barcode/OCR/sugeridas.
- [x] 3.2 Modificar `src/main/resources/static/js/api.js`, `js/supermarket.js`, `index.html` y `css/styles.css`: mostrar stock, permitir setear stock actual por producto y editar `quickQuantity` sin historial visible.
- [x] 3.3 Actualizar cache busting en `index.html`, `js/app.js` y `js/supermarket.js` a `20260714-super-inventory-stage2-api/ui`.

## Phase 4: Verification

- [x] 4.1 Ejecutar `mvn -Dtest=SupermarketControllerTests test` y corregir fallos de contrato backend.
- [x] 4.2 Ejecutar `mvn -Dtest=StaticUiContractTests test` y `node src/test/resources/static-ui-contract-tests.mjs`.
- [x] 4.3 Ejecutar `mvn test`; confirmar que no hay lista sugerida, historial visible ni mutación de stock fuera del endpoint enfocado.
