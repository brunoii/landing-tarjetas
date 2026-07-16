# Diseño: Lista sugerida de compras Etapa 5

## Technical Approach

Implementar la delta spec activa de `super-inventory`: una lista sugerida read-only, separada de la lista manual, calculada en backend desde `SuperItem` activo. La regla es estricta: producto activo con `unit`, `habitualObjective`, `currentStock != null` y `currentStock < habitualObjective`; `suggestedQuantity = habitualObjective - currentStock`. La UI consume un endpoint dedicado y renderiza una sección separada; `generatedSuperListText(items)` sigue dependiendo solo de `checked`. No hay persistencia nueva ni mutaciones de `checked`, `currentStock` o movimientos.

## Architecture Decisions

| Decisión | Elección | Alternativas consideradas | Rationale |
|---|---|---|---|
| Fuente de verdad | `SupermarketService.listSuggestedItems()` con `@Transactional(readOnly = true)` | Cálculo frontend; entidad persistida | Cumple la spec de derivación local, evita reglas duplicadas y mantiene sugerencias no confirmables. |
| Endpoint | `GET /api/super/suggested-list` en `SuperSuggestedListController` | Mezclar en `/api/super/items`; comandos por item | Ruta explícita de lectura, separada de CRUD, compras, consumos y ajustes de stock. |
| Consulta | Reusar `SuperItemRepository.findActiveOrderedForList()` y filtrar en servicio | Query dedicada inicial | Ya trae activos ordenados con categoría; el filtro en servicio conserva claridad para esta slice. |
| DTO | `SuperSuggestedItemResponse` sin `checked` | Reusar `SuperItemResponse` | Evita acoplar sugerencias con la lista manual y expone solo el contrato necesario. |

## Data Flow

```text
UI loadSupermarket
  ├─ api.superItems() ───────────────→ tabla/lista manual
  └─ api.superSuggestedList() ───────→ sección sugerida read-only

GET /api/super/suggested-list
  → SupermarketService.listSuggestedItems()
  → findActiveOrderedForList()
  → filter active + unit + habitualObjective + known stock + below objective
  → DTO suggestedQuantity = habitualObjective - currentStock
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedItemResponse.java` | Create | DTO con `itemId`, `name`, `categoryId`, `categoryName`, `unit`, `habitualObjective`, `currentStock`, `suggestedQuantity`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperSuggestedListController.java` | Create | Expone `GET /api/super/suggested-list`. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modify | Agrega cálculo read-only y excluye `currentStock=null`, inactivos, no configurados, en objetivo o sobre objetivo. |
| `src/main/resources/static/js/api.js` | Modify | Agrega `superSuggestedList()` apuntando al nuevo endpoint. |
| `src/main/resources/static/js/supermarket.js` | Modify | Carga sugerencias, renderiza sección separada y mantiene `generatedSuperListText` sin cambios semánticos. |
| `src/main/resources/static/index.html` | Modify | Agrega card `super-suggested-card` antes o después de “Productos marcados”, con `aria-live` y empty state propios. |
| `src/main/resources/static/css/styles.css` | Modify | Estilos mínimos para la card/lista sugerida reutilizando patrones de `super-generated-card`. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Contratos backend de inclusión/exclusión, cantidad sugerida e invariantes de no mutación. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Permitir semántica `suggested` solo para Etapa 5 y conservar bloqueos de precio, tienda, presentación, OCR y automatización. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Validar helper API, render separado y que la lista manual ignore sugerencias. |

## Interfaces / Contracts

```http
GET /api/super/suggested-list -> 200 OK
[
  {
    "itemId": 10,
    "name": "Arroz",
    "categoryId": 4,
    "categoryName": "Almacén",
    "unit": "kg",
    "habitualObjective": 3.000,
    "currentStock": 1.250,
    "suggestedQuantity": 1.750
  }
]
```

Eligibility: item active, `unit` present, `habitualObjective != null`, `currentStock != null`, and `currentStock.compareTo(habitualObjective) < 0`. `currentStock=null` MUST NOT be treated as zero. The endpoint MUST NOT write `checked`, stock, barcode aliases, or `SuperItemStockMovement`, and MUST NOT persist suggestions.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend integration | Eligible/ineligible items, ordering, subtraction, read-only invariants | `MockMvc` + repository assertions in `SupermarketControllerTests`. |
| Static Java contracts | Allowed Stage 5 suggested names; unsupported commerce/OCR automation still blocked | Update `StaticUiContractTests` guard lists precisely. |
| Static Node contracts | `api.superSuggestedList()`, DOM render, no manual-list mixing | Extend `static-ui-contract-tests.mjs` fake API and `generatedSuperListText` cases. |

## Migration / Rollout

No migration required. Delivery should remain stacked-to-main: slice 1 backend DTO/controller/service/tests; slice 2 UI/API/static contracts; slice 3 OpenSpec archive/spec sync after verification. Each slice is reversible because no persisted schema or data changes are introduced.

## Open Questions

None.
