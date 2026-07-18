# Design: Historial manual de observaciones de precio

## Technical Approach

Agregar una entidad hija append-only para capturar observaciones manuales del precio de referencia de la presentación comercial default de `SuperItem`. El flujo sigue el patrón de `SuperItemStockMovement`: repositorio con `Pageable`, servicio transaccional, DTOs planos y controller REST enfocado. `POST/PUT /api/super/items` seguirá editando solo el precio actual/fuente/fecha del producto y nunca creará ni mutará observaciones.

## Architecture Decisions

| Decisión | Elección | Alternativas consideradas | Rationale |
|---|---|---|---|
| Modelo | Crear `SuperItemPriceObservation` en `super_item_price_observations`, `ManyToOne` a `SuperItem`, `pricePesos`, `sourceLabel`, `observedDate`, snapshot `presentationLabel`/`presentationQuantity`, `createdAt` | Reusar campos actuales o mutar una fila única | La spec exige historia manual append-only y snapshots estables ante cambios posteriores del producto. |
| API | `POST /api/super/items/{id}/price-observations` y `GET /api/super/price-observations?itemId=&limit=50` | Subrecurso GET por producto solamente | Mantiene alta explícita por producto y listado reciente global/per-item como movimientos. |
| Validación | Bean Validation en request más normalización en `SupermarketService` | Validar solo en UI | El backend debe rechazar producto inexistente/inactivo/sin presentación, precio no positivo, fuente >120 y fecha futura sin mutaciones. |
| UI | Formulario/listado mínimo, botón explícito “registrar observación”, prefill opcional desde precio/fuente/fecha actuales | Crear observación al guardar producto | Evita duplicados implícitos y preserva el contrato manual. |
| Alcance | Allow-list solo para price observation/history | Abrir tiendas, comparación, gráficos, OCR o automatización | Stage 10 solo habilita historial manual del precio actual. |

## Data Flow

```
Botón observación ──→ formulario mínimo ──→ POST /items/{id}/price-observations
        │                         │
        └──── prefill opcional desde SuperItem actual
SupermarketService ──→ valida item activo + presentación ──→ guarda snapshot append-only
GET /api/super/price-observations ──→ repo reciente global/per-item ──→ UI lista reciente
```

Cambios posteriores de `commercialPresentationPricePesos`, fuente, fecha o presentación solo actualizan `SuperItem`; no actualizan observaciones existentes. Borrar precio/presentación limpia metadata actual, pero conserva observaciones.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservation.java` | Create | Entidad append-only con precio `precision=12, scale=2`, fuente nullable 120, `LocalDate` nullable, snapshot de presentación y `createdAt`. |
| `SuperItemPriceObservationRepository.java` | Create | `findRecent(Pageable)` y `findRecentByItemId(Long, Pageable)` con `join fetch item`, orden `createdAt desc, id desc`. |
| `SuperItemPriceObservationRequest.java` / `Response.java` | Create | Request `{pricePesos, sourceLabel?, observedDate?}` y response con `id`, `itemId/name`, precio, fuente, fecha, snapshot y `createdAt`. |
| `SupermarketService.java` | Modify | Inyectar repo, `createPriceObservation`, `listPriceObservations`, límite default 50/max 100 y validación/no-colateralidad. |
| `SuperItemController.java` / nuevo `SuperItemPriceObservationController.java` | Modify/Create | POST subrecurso por item y GET global `/api/super/price-observations`. |
| `src/main/resources/static/index.html` | Modify | Sección mínima para observación manual y tabla/lista reciente. |
| `src/main/resources/static/js/api.js`, `supermarket.js`, `app.js` | Modify | Métodos API, payload/validación/render, refresh tras alta y cache bust solo de assets tocados. |
| `StaticUiContractTests.java`, `static-ui-contract-tests.mjs`, `SupermarketControllerTests.java` | Modify | Cobertura backend/UI/static. |

## Interfaces / Contracts

```json
POST /api/super/items/1/price-observations
{ "pricePesos": 1250.50, "sourceLabel": "Ticket proveedor", "observedDate": "2026-07-18" }
```

`observedDate` es date-only `LocalDate`, nullable y no futura. El response incluye `presentationLabelSnapshot` y `presentationQuantitySnapshot`. No se soportan edición/borrado, tienda/comercio, fuentes normalizadas, comparación, charts, múltiples presentaciones, OCR, lookup externo/scraping, automatización ni totales.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Backend/API | Alta válida, inválidos sin persistencia, listado global/per-item con límite, snapshot inmutable, create/update no auto-crean observaciones, no mutar stock/checked/movimientos/barcodes/listas | `SupermarketControllerTests` + asserts de repositorios. |
| UI/static behavior | Formulario explícito, prefill opcional, render recientes, refresh tras alta, validación cliente sin guardar producto | `static-ui-contract-tests.mjs`. |
| Static contract | Abrir solo tokens precisos price-observation/history; mantener bloqueados stores/shops, normalized sources, comparison, charts, multiple presentations, OCR, external lookup/scraping, automation, totals; preservar stock/movement history | `StaticUiContractTests`. |

## Migration / Rollout

No requiere migración manual: tabla nueva aislada con `ddl-auto=update`. Entrega forzada stacked-to-main: PR1 backend/API/tests, PR2 UI/static/tests, PR3 OpenSpec archive/spec sync.

## Open Questions

None.
