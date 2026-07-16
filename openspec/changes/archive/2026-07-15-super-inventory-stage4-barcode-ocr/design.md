# Design: Etapa 4 — Alias local de barcode

## Technical Approach

Implementar una primera slice manual-first: el barcode es un alias local de identificación sobre `SuperItem`, no una fuente de stock ni un catálogo externo. El flujo será: ingresar/pegar código → buscar alias activo → si existe, devolver/resaltar el item; si no existe, asociarlo manualmente a un `SuperItem` activo. OCR, lookup externo, precios, tiendas, presentaciones, compras automáticas y lista sugerida quedan diferidos.

## Architecture Decisions

| Área | Decisión | Alternativas / rationale |
|------|----------|--------------------------|
| Modelo | Crear `SuperItemBarcodeAlias` con `code` texto, `format` texto opcional, `item`, `active`, `activeCode`, `createdAt`, `updatedAt`. | Evita agregar un único `barcode` a `SuperItem` y soporta múltiples códigos sin Producto Base paralelo. `code` nunca se parsea como número, preservando ceros iniciales. |
| Unicidad activa | `activeCode = normalizedCode` solo cuando `active=true`; `activeCode=null` al desactivar. Constraint único sobre `active_code`. | `(code, active)` bloquearía múltiples alias inactivos. Índices parciales no son portables por JPA. H2 permite múltiples `NULL` en unique y cubre la regla de “un activo por código”. |
| Semántica de remove | Remover = soft deactivate; no hard delete por defecto. | Mantiene historial auxiliar y permite re-asociar el mismo código después. El lookup siempre filtra `active=true`. |
| API | Agregar controlador de alias, manteniendo `SupermarketService` como fachada del módulo. | Sigue el patrón actual de controladores finos + servicio transaccional único. |
| Cámara | No implementar cámara en esta slice. | `BarcodeDetector/getUserMedia` no puede ser dependencia del flujo; puede entrar luego como mejora progresiva sin cambiar API. |

## Data Flow

```text
Manual input ──GET /api/super/barcode-aliases?code=...──> service
   │                                                     │
   │                                  active alias? ──yes┴──> SuperItemResponse + alias metadata
   │                                                     │
   └── not found ──POST /api/super/items/{id}/barcode-aliases──> alias active
```

Las operaciones de alias no llaman a `adjustItemStock`, `purchaseItemStock`, `consumeItemStock`, `quickConsumeItemStock` ni a `stockMovementRepository.save(...)`; solo leen o actualizan la tabla de alias.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemBarcodeAlias.java` | Create | Entidad JPA con lifecycle timestamps, `code`, `format`, `active`, `activeCode` único y FK a `super_items`. |
| `SuperItemBarcodeAliasRepository.java` | Create | Lookup activo por `activeCode`, duplicado activo y búsqueda por id/item. |
| `SuperItemBarcodeAliasRequest/Response.java`, `SuperBarcodeLookupResponse.java` | Create | DTOs mínimos para attach/lookup/remove. |
| `SupermarketService.java` | Modify | Métodos `lookupBarcodeAlias`, `attachBarcodeAlias`, `deactivateBarcodeAlias`; normalización trim-only, validación nonblank/max length, item activo. |
| `SuperItemBarcodeAliasController.java` | Create | `GET /api/super/barcode-aliases?code=...`, `POST /api/super/items/{itemId}/barcode-aliases`, `DELETE /api/super/items/{itemId}/barcode-aliases/{aliasId}`. |
| `src/main/resources/static/js/api.js` | Modify | Helpers API y cache token nuevo. |
| `src/main/resources/static/js/supermarket.js`, `index.html`, `css/styles.css` | Modify | Panel manual para buscar/asociar/remover alias; resaltar fila encontrada; sin cámara. |
| `src/test/**` | Modify | Contratos backend, H2/schema y UI estática. |

## Interfaces / Contracts

- `GET /api/super/barcode-aliases?code=0075012345678` → `200 { found, code, aliasId?, format?, item? }`; not found devuelve `found:false`, no 404 funcional.
- `POST /api/super/items/{itemId}/barcode-aliases` body `{ "code": "0075012345678", "format": "EAN_13" }` → `201` alias; duplicado activo → `409`.
- `DELETE /api/super/items/{itemId}/barcode-aliases/{aliasId}` → `204`; lookup futuro ignora alias desactivado.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend | lookup, attach, duplicate active, deactivate, leading zeros, inactive item/alias, stock/checked/movement invariants | `SupermarketControllerTests` + repository assertions. |
| H2/JPA | unique `active_code`, inactive duplicates, no enum-like format column | Focused H2 schema test, learning from Stage 3 `movement_type`: avoid enums and verify persistent-schema behavior. |
| UI/static | manual input, no numeric coercion, API helpers, row highlight, remove, fallback without camera | `StaticUiContractTests` + Node contract. Update blockers to allow barcode terms only for local alias flow while still rejecting `ocr`, `price/prices`, `suggested*`, `OpenFoodFacts`, `Tesseract`, `getUserMedia`, `BarcodeDetector`. |

## Migration / Rollout

No manual data migration expected: `super_item_barcode_aliases` is new. With `ddl-auto=update`, do not rely on later constraint evolution; if local H2 shows an existing partial table, add an explicit compatibility runner before widening behavior.

Review slicing: backend/API/tests first, UI/static contracts second if diff approaches 400 changed lines. Rollback is safe by reverting alias files/endpoints/UI; stock, checked and movements remain untouched.

## Open Questions

None.
