# Design: Etapa 2 — Inventario del Super

## Technical Approach

Extender `SuperItem` como snapshot de inventario liviano y mantener la mutación de stock fuera del update genérico. `currentStock` será lectura rápida nullable; `quickQuantity` será dato opcional para compra manual. Cada cambio de stock pasará por `POST /api/super/items/{id}/stock-adjustments`, que registrará un hecho interno mínimo y actualizará el snapshot, sin exponer historial visible.

## Architecture Decisions

| Decisión | Elección | Alternativas consideradas | Rationale |
|---|---|---|---|
| Campos de item | `currentStock: BigDecimal?` → `current_stock`; `quickQuantity: BigDecimal?` → `quick_quantity`; ambos `precision=10, scale=3` | `double`; enteros; stock default `0` | `BigDecimal` mantiene coherencia con `habitualObjective`; nullable diferencia stock desconocido de cero real. |
| Ajuste auditable | Crear `SuperItemStockMovement` / `super_stock_movements` con `id`, `item`, `movementType=ADJUSTMENT`, `previousStock`, `resultingStock`, `createdAt` | Solo columna snapshot; historial completo | Es el mínimo movement-first sin UI/API de historial. |
| Endpoint | `POST /api/super/items/{id}/stock-adjustments` con `{ "currentStock": 5.000 }`; respuesta `SuperItemResponse` | `PUT /items/{id}`; PATCH genérico | Evita que el update de producto mute stock y deja una frontera clara. |
| Validación decimal | `@Digits(integer=7, fraction=3)`. `habitualObjective` y `quickQuantity` MUST ser `> 0`; `currentStock` ajustado MUST ser `>= 0`; más de 3 decimales se rechaza | Redondear silenciosamente | Rechazar evita inventar precisión y conserva contratos previsibles. |
| Contrato genérico | `SuperItemRequest` acepta `quickQuantity`; si trae `currentStock` no nulo, se rechaza con 400 y no se modifica stock | Ignorar campo desconocido | La spec exige rechazo explícito de mutación genérica de stock. |
| UI | Stock desconocido: “Sin cargar”; stock cero: `0 {unit?}`; stock positivo: valor + unidad si existe. `quickQuantity` se edita en formulario de producto. La lista manual solo muestra cantidad cuando `quickQuantity` existe | Derivar cantidad desde stock/objetivo | Preserva compra manual y evita lista sugerida automática. |

## Data Flow

```text
Producto create/update ──> SuperItemRequest ──> SupermarketService ──> super_items
                         └─ rechaza currentStock no nulo

Ajuste stock UI/API ──> StockAdjustmentRequest ──> guarda movement interno
                                             └──> actualiza SuperItem.currentStock
```

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Agregar `currentStock`, `quickQuantity`, getters/setters. |
| `SuperItemStockMovement.java`, `SuperItemStockMovementRepository.java` | Create | Persistir ajuste interno mínimo. |
| `SuperItemRequest.java`, `SuperItemResponse.java`, `SuperItemStockAdjustmentRequest.java` | Modify/Create | DTOs de item, respuesta y ajuste enfocado. |
| `SupermarketService.java`, `SuperItemController.java`, `ApiExceptionHandler.java` | Modify | Validación, endpoint de ajuste, etiquetas de error. |
| `index.html`, `js/api.js`, `js/app.js`, `js/supermarket.js`, `css/styles.css` | Modify | UI de stock/cantidad rápida y cache busting. |
| `SupermarketControllerTests.java`, `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Modify | Contratos backend y estáticos. |

## Interfaces / Contracts

`SuperItemResponse` agrega: `currentStock`, `quickQuantity`. `SuperItemRequest` agrega `quickQuantity`; `currentStock` solo se permite ausente/null y se rechaza si intenta mutar stock. `SuperItemStockAdjustmentRequest`: `currentStock` requerido, decimal `0..9999999.999`.

La lista generada sigue usando `checked=true`; formato sugerido cuando hay cantidad explícita: `- Leche (2 litro) — nota`.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Backend | Defaults null, quickQuantity válida/inválida, rechazo de currentStock en update genérico, ajuste con cero/positivo, movimiento interno, preservación de checked/uncheck-all | MockMvc + asserts JPA. |
| Static UI | Payload sin stock genérico, helper de ajuste, render “Sin cargar” vs `0`, lista con quickQuantity, out-of-scope sin history/price/barcode/OCR/suggested | `StaticUiContractTests` + Node contract. |
| Regression | Todo el proyecto | `mvn test`. |

## Migration / Rollout

`ddl-auto=update` agregará columnas nullable y la tabla interna. No hay backfill: productos existentes responden `currentStock=null` y `quickQuantity=null`. Revertir código deja columnas/tabla sin uso; Hibernate no las elimina. Cache busting: usar `20260714-super-inventory-stage2-api` y `20260714-super-inventory-stage2-ui` en importaciones/enlaces afectados.

## Open Questions

- Ninguna bloqueante.

## Out of Scope

Sin historial visible, endpoints de movimientos, consumo, compras reales, precios, presentaciones comerciales, barcode, OCR, lista sugerida automática, enum de unidades ni migraciones versionadas.
