# Design: Super Inventory Etapa 11 - fuentes de precio

## Technical Approach

Agregar un catálogo mínimo `SuperPriceSource` dentro del paquete `supermarket`, usado solo por observaciones manuales de precio. El producto conserva su fuente libre de precio de referencia; la observación gana una FK nullable y mantiene siempre `sourceLabel` como snapshot. La UI suma selector e incorporación inline, pero conserva texto libre y no incorpora administración completa ni semántica de tienda/comercio.

## Architecture Decisions

| Decisión | Opción elegida | Alternativas consideradas | Rationale |
|---|---|---|---|
| Catálogo mínimo | Nueva entidad `SuperPriceSource` con `name`, `normalizedKey`, `active`, `createdAt`, `updatedAt`. | Reusar categorías o texto libre únicamente. | Evita duplicados sin mezclar dominios ni perder compatibilidad legacy. |
| Normalización | `trim().toLowerCase(Locale.ROOT)` y unicidad por `normalizedKey` sobre fuentes existentes. | Unicidad case-sensitive o por nombre visible. | Cubre espacios/mayúsculas y evita duplicados aunque más adelante una fuente esté inactiva. |
| Observación | `price_source_id` nullable + `sourceLabel` snapshot siempre. | Derivar nombre por join en lectura. | Preserva histórico si el catálogo cambia y permite legacy sin backfill. |
| Validación | `SupermarketService` resuelve exclusividad `priceSourceId` XOR `sourceLabel`; fuente inexistente/inactiva se rechaza. | Validar solo con Bean Validation. | La regla depende de repositorios y del estado activo. |
| UI | Selector mínimo, alta inline y fallback de texto libre. | Pantalla/admin CRUD de fuentes. | Cumple reutilización mínima sin abrir edición, borrado, tiendas ni comparación. |

## Data Flow

Crear fuente:

    UI inline ──POST /api/super/price-sources──→ Controller ─→ SupermarketService
        └── reload/select active sources ←────── Repository/SuperPriceSource

Crear observación:

    Form ─→ payload(priceSourceId | sourceLabel) ─→ SupermarketService
        ├─ valida item activo + presentación + precio/fecha
        ├─ si priceSourceId: carga fuente activa y copia name a sourceLabel
        └─ guarda SuperItemPriceObservation append-only sin mutar producto/stock/listas

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/.../supermarket/SuperPriceSource.java` | Create | Entidad mínima con clave normalizada única, activo y timestamps. |
| `SuperPriceSourceRepository.java` | Create | `findByActiveTrueOrderByNameAsc`, `findByNormalizedKey`, validación de duplicados. |
| `SuperPriceSourceRequest.java`, `SuperPriceSourceResponse.java` | Create | DTOs de alta/listado sin metadatos comerciales. |
| `SuperPriceSourceController.java` | Create | `GET/POST /api/super/price-sources`. |
| `SuperItemPriceObservation.java` | Modify | FK nullable a `SuperPriceSource`, índice opcional por `price_source_id`; `sourceLabel` se preserva. |
| `SuperItemPriceObservationRequest/Response.java` | Modify | Agregar `priceSourceId` nullable y exponerlo en respuesta. |
| `SuperItemPriceObservationRepository.java` | Modify | `left join fetch observation.priceSource` en listados recientes. |
| `SupermarketService.java` | Modify | Inyectar repositorio, crear/listar fuentes, normalizar nombre y validar observación. |
| `SupermarketLimits.java` | Modify | Agregar límite `PRICE_SOURCE_NAME_MAX_LENGTH = 120` y alinear UI. |
| `api.js`, `supermarket.js`, `index.html`, `styles.css`, `app.js` | Modify | Helpers API, selector/alta inline, fallback libre y cache tokens Stage 11. |
| `SupermarketControllerTests.java`, `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Modify | Cobertura backend, UI y guards de alcance. |

## Interfaces / Contracts

- `GET /api/super/price-sources` devuelve solo fuentes activas: `id`, `name`, `active`, `createdAt`, `updatedAt`; no dirección, ubicación, tienda, comercio, métricas ni comparación.
- `POST /api/super/price-sources { "name": "Ticket proveedor" }` recorta nombre, persiste `active=true`, calcula `normalizedKey` y rechaza duplicado normalizado.
- `POST /api/super/items/{id}/price-observations` acepta `priceSourceId` o `sourceLabel`, nunca ambos. Con `priceSourceId`, guarda FK y copia `SuperPriceSource.name` a `sourceLabel`; con texto libre o sin fuente, guarda `priceSourceId=null`.
- No backfill: observaciones existentes continúan con `priceSourceId=null`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend/API | Crear/listar fuentes activas, duplicado normalizado, observación con fuente, fuente libre, ambos campos, fuente inexistente/inactiva, legacy sin backfill, no mutar producto/stock/movimientos/barcodes/listas. | Extender `SupermarketControllerTests` con H2 create-drop y repositorios reales. |
| Static UI | API helpers, selector, alta inline, fallback libre, tokens Stage 11. | Extender JUnit static y `static-ui-contract-tests.mjs`. |
| Scope guards | Permitir solo tokens precisos `priceSource`, `price-source(s)`, `SuperPriceSource`; mantener bloqueados store/shop/commerce/comparison/charts/OCR/scraping/automation/totals. | Ajustar allowlist del scan existente sin abrir términos comerciales. |

## Migration / Rollout

No se requiere migración versionada ni backfill; Hibernate actualiza H2 local y tests usan `create-drop`. Entrega forzada stacked-to-main: PR1 backend/API/tests, PR2 UI/static/tests, PR3 OpenSpec archive/spec sync. Rollback: revertir código y artefactos; observaciones legacy siguen válidas.

## Open Questions

- None.
