# Design: PRD Etapa 3 — Movimientos

## Technical Approach

Extender la base de Etapa 2 sin reconstruir el módulo: `SupermarketService` seguirá concentrando reglas de dominio y agregará comandos transaccionales para compra, consumo, consumo rápido e historial reciente. `SuperItem.currentStock=null` seguirá significando stock desconocido; los movimientos delta no lo inicializan.

## Architecture Decisions

| Tema | Decisión | Rationale |
|------|----------|-----------|
| Persistencia | Mantener `SuperItemStockMovement` y tabla `super_stock_movements`; ampliar `MovementType` a `ADJUSTMENT`, `PURCHASE`, `CONSUMPTION`, `QUICK_CONSUMPTION`. | Evita migración destructiva con `ddl-auto=update` y conserva ajustes de Etapa 2. |
| Campos de movimiento | Agregar columnas nullable `quantity` (`BigDecimal 10,3`), `notes` (`500`), `source` (`40`), manteniendo `previousStock`, `resultingStock`, `createdAt`. | Las filas históricas `ADJUSTMENT` quedan válidas; compras/consumos no dependen de inferir cantidad. `source` permite distinguir `MANUAL`/`QUICK` sin bloquear datos previos. |
| Stock desconocido | Compra, consumo manual y rápido rechazan `currentStock=null` con `400` y mensaje que pida ajuste inicial. Ajuste absoluto sigue permitiendo inicializar en `0+`. | No convertir `null` en cero protege productos migrados. |
| Stock negativo | Solo `CONSUMPTION` y `QUICK_CONSUMPTION` pueden dejar negativo si `allowNegativeStock=true`; compra y ajuste no generan negativo. | Limita el riesgo al flujo que lo necesita y preserva ajuste no negativo. |
| Concurrencia | Usar `@Transactional` y lectura con bloqueo pesimista de `SuperItem` para todo comando de stock, incluido ajuste. | Snapshot y movimiento se calculan sobre la misma versión bloqueada y se confirman o revierten juntos. |

## Data Flow

```text
UI action -> api.js -> SuperItemController -> SupermarketService
  -> find active item with PESSIMISTIC_WRITE
  -> validate stock/quantity/negative confirmation
  -> set currentStock + save SuperItemStockMovement
  -> return SuperItemResponse or movement history
```

El conflicto negativo se calcula antes de mutar `item.currentStock` y antes de guardar movimiento.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `SuperItemStockMovement.java` | Modify | Nuevos tipos y campos nullable; constructores/factory por tipo. |
| `SuperItemStockMovementRepository.java` | Modify | Consultas `findTop...OrderByCreatedAtDesc` con filtro opcional por `itemId`. |
| `SuperItemRepository.java` | Modify | Método bloqueante para comandos de stock. |
| `SupermarketService.java` | Modify | Comandos purchase/consume/quick/history y ajuste usando la misma ruta transaccional. |
| `SuperItemController.java` | Modify | Endpoints nuevos bajo `/api/super/items` y `/api/super/movements`. |
| `src/main/java/.../supermarket/*Request.java` | Create | DTOs de compra/consumo con validación positiva y confirmación. |
| `SuperItemStockMovementResponse.java` | Create | Respuesta plana de historial. |
| `api.js`, `supermarket.js`, `index.html`, `styles.css` | Modify | Helpers, acciones de fila, modal/panel e historial mínimo. |
| Tests | Modify | Contratos backend y UI estática. |

## Interfaces / Contracts

- `POST /api/super/items/{id}/purchases` body `{ "quantity": 1.000, "notes": "opcional" }` -> `200 SuperItemResponse`.
- `POST /api/super/items/{id}/consumptions` body `{ "quantity": 1.000, "notes": "opcional", "allowNegativeStock": false }` -> `200 SuperItemResponse` o `409`.
- `POST /api/super/items/{id}/quick-consumptions` body `{ "allowNegativeStock": false }` -> usa `quickQuantity`.
- `GET /api/super/movements?itemId=1&limit=50` -> recientes primero; `limit` default 50, máximo 100.
- Historial item: `{ id, itemId, itemName, movementType, quantity, previousStock, resultingStock, notes, source, createdAt }`.
- Conflicto negativo:
```json
{
  "timestamp": "...",
  "status": 409,
  "error": "El consumo dejaría stock negativo. Confirme para continuar.",
  "details": ["Reintente con allowNegativeStock=true para confirmar."],
  "itemId": 1,
  "itemName": "Leche",
  "currentStock": 1.000,
  "quantity": 2.000,
  "resultingStock": -1.000,
  "movementType": "CONSUMPTION"
}
```

## UI Minimal Flow

Agregar acciones compactas por fila: `Compra`, `Consumir`, `Rápido`, `Historial`, además de editar/eliminar. Compra/consumo abren modal reutilizando estilos `.modal-*`; rápido ejecuta directo. Si llega `409`, mostrar confirmación con datos del conflicto y reintentar con `allowNegativeStock=true`. El historial se muestra en un panel bajo la tabla, filtrado por producto cuando se abre desde una fila.

## Cache Busting

Subir tokens a `20260714-super-inventory-stage3-api` para imports de `api.js` y `20260714-super-inventory-stage3-ui` para `index.html`/`app.js`/`supermarket.js`, actualizando `StaticUiContractTests` y `static-ui-contract-tests.mjs`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend | Tipos, cantidades, `null`, negativo, historial, ajuste compatible, no movimiento en `checked`. | `SupermarketControllerTests` con MockMvc y repositorios reales H2. |
| Atomicidad/concurrencia | Sin movimiento ni snapshot ante conflicto/validación; lectura bloqueante en comandos. | Tests de no-mutación y verificación de método bloqueante en repositorio. |
| UI estática | Helpers API, modal, confirmación negativa, historial, cache tokens. | `StaticUiContractTests` y Node static contract. |

## Migration / Rollout

No requiere migración manual. Hibernate agregará columnas nullable; filas `ADJUSTMENT` previas se muestran sin `quantity` como “Ajuste a {resultingStock}”. Slicing recomendado: PR 1 backend/API/tests; PR 2 UI/cache/tests. No introducir precios, presentaciones, barcode, OCR, lista sugerida automática ni filtros avanzados.

## Open Questions

None.
