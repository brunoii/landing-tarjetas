# Tasks: PRD Etapa 3 — Movimientos

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 900-1,200 |
| Review budget | 800 changed lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend/API/tests → PR 2 UI/static tests → PR 3 verificación/archivo si hace falta |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Contratos backend de movimientos y API transaccional | PR 1 | Base = feature/tracker branch; incluye tests. |
| 2 | Acciones UI, modal, historial y cache tokens | PR 2 | Base = PR 1 branch; incluye tests estáticos. |
| 3 | Evidencia de verificación y limpieza | PR 3 | Base = PR 2 branch solo si PR 2 excede presupuesto. |

## Phase 1: Backend RED

- [x] 1.1 Agregar casos fallidos en `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` para compra, consumo manual, consumo rápido, historial ordenado/filtrado y `ADJUSTMENT` con `quantity=null`.
- [x] 1.2 Agregar tests backend fallidos para stock desconocido, cantidad inválida, 409 enriquecido, sin snapshot/movimiento ante validación o 409, y rollback si es viable.
- [x] 1.3 Agregar tests fallidos de labels/metadata para `quantity` y `allowNegativeStock` en el comportamiento de `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java`.

## Phase 2: Backend GREEN

- [x] 2.1 Modificar `SuperItemStockMovement.java`; agregar DTOs request/response en `src/main/java/com/gentleia/landingtarjetas/supermarket/`, con semántica explícita de ajuste: `quantity=null`.
- [x] 2.2 Modificar `SuperItemRepository.java` para comandos con `PESSIMISTIC_WRITE` y `SuperItemStockMovementRepository.java` para historial reciente con filtro por item.
- [x] 2.3 Modificar `SupermarketService.java` para flujos transaccionales compra/consumo/rápido/ajuste, conflicto antes de mutar, sin movimiento ante fallo, e historial `limit` default 50 max 100.
- [x] 2.4 Agregar `SuperItemMovementController.java` dedicado para `/api/super/movements`; actualizar endpoints de comandos en `SuperItemController.java`.
- [x] 2.5 Modificar `ApiExceptionHandler.java`/modelo de error para preservar metadata enriquecida 409 en conflictos de stock negativo.

## Phase 3: UI RED

- [x] 3.1 Agregar checks fallidos en `StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` para helpers API que preserven body/status/details 409 y nuevos cache tokens.
- [x] 3.2 Agregar checks UI estáticos fallidos para acciones por fila, labels de cantidad y `allowNegativeStock`, submit del modal, reintento con confirmación negativa y panel de historial.

## Phase 4: UI GREEN

- [x] 4.1 Modificar `src/main/resources/static/js/api.js` para lanzar errores API con metadata preservada y agregar helpers de compra/consumo/rápido/historial.
- [x] 4.2 Modificar `index.html`, `js/supermarket.js` y `css/styles.css` para acciones compactas, modal reutilizable, confirmación negativa explícita e historial reciente filtrado.
- [x] 4.3 Actualizar cache-busting tokens a `20260714-super-inventory-stage3-api` y `20260714-super-inventory-stage3-ui`.

## Phase 5: Verification

- [x] 5.1 Ejecutar tests dirigidos: `mvn -Dtest=SupermarketControllerTests,StaticUiContractTests test` y `node src/test/resources/static-ui-contract-tests.mjs`.
- [x] 5.2 Ejecutar `mvn test` completo; verificar que no se introduzcan precios, presentaciones, barcode, OCR, lista sugerida ni filtros avanzados.
