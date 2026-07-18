# Diseño: Fuente manual del precio de referencia Etapa 8

## Technical Approach

Extender el `SuperItem` existente con una etiqueta manual nullable para la fuente del precio actual/de referencia de la presentación comercial default. El dato viaja por `POST/PUT/GET /api/super/items`, se valida en `SupermarketService` junto al precio y se muestra como texto secundario cercano al precio. No crea tiendas, historial, catálogos, múltiples precios/presentaciones, totales ni automatización; tampoco muta `checked`, `currentStock`, movimientos, barcodes, lista manual ni lista sugerida.

## Architecture Decisions

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Columna nullable vs tabla de fuentes | Tabla nueva habilita catálogo/tiendas fuera de alcance. | Agregar `commercialPresentationPriceSourceLabel` en `SuperItem`. |
| Límite 120 vs texto libre | Texto libre aumenta riesgo visual y de contrato. | Usar 120 caracteres, igual que `commercialPresentationLabel`, suficiente para etiquetas manuales. |
| Fuente independiente vs dependiente del precio | Independiente permitiría datos huérfanos. | La fuente requiere precio; el precio puede existir sin fuente. |
| Render como columna nueva vs subtexto | Columna nueva sugiere entidad conceptual. | Renderizar en la celda de precio como texto secundario. |

## Data Flow

```
input fuente ─→ supermarket.js payload ─→ SuperItemRequest
       └── render secundario ← SuperItemResponse ← SupermarketService ← SuperItem
```

`createItem/updateItem` aplican presentación/precio/fuente dentro del flujo actual. Si se limpia precio o presentación, la fuente enviada como vacía se normaliza a `null`; si llega fuente no vacía sin precio válido, se rechaza antes de persistir.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` | Modify | Agregar `ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH = 120`. |
| `SuperItem.java` | Modify | Agregar `@Column(name="commercial_presentation_price_source_label", length=120)` y getters/setters. |
| `SuperItemRequest.java` | Modify | Agregar `String commercialPresentationPriceSourceLabel` con `@Size(max=120)`. |
| `SuperItemResponse.java` | Modify | Exponer `commercialPresentationPriceSourceLabel` nullable. |
| `SupermarketService.java` | Modify | Trim a null, validar fuente solo con precio, limpiar al limpiar precio/presentación y preservar invariantes colaterales. |
| `src/main/resources/static/index.html` | Modify | Agregar input recomendado `id="super-item-presentation-price-source-label"`, `name="commercialPresentationPriceSourceLabel"`, `data-super-limit="priceSourceLabel"`; actualizar token de `app.js` solo si cambia HTML. |
| `src/main/resources/static/js/supermarket.js` | Modify | Payload/edit/reset/validación/render: fuente como subtexto de precio; actualizar import de `api.js` solo si cambia API. |
| `src/main/resources/static/js/app.js` | Modify | Actualizar token de `./supermarket.js` si cambia ese módulo. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Cubrir persistencia, trim, validación, limpieza y no mutación colateral. |
| `StaticUiContractTests.java` / `static-ui-contract-tests.mjs` | Modify | Permitir tokens exactos de fuente manual y mantener bloqueos de tiendas, historial, múltiples precios, totales y automatización. |

## Interfaces / Contracts

```json
{
  "commercialPresentationLabel": "Pack x 6",
  "commercialPresentationPricePesos": 1250.50,
  "commercialPresentationPriceSourceLabel": "Ticket mayorista"
}
```

`commercialPresentationPriceSourceLabel` es opcional, nullable, trimeado y de máximo 120 caracteres. `null`, ausente o blanco significan sin fuente. Fuente no vacía con precio ausente/no positivo o presentación ausente debe fallar con error de validación sin modificar el producto. Precio válido sin fuente sigue permitido.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend/API | Legacy null, fuente válida trimeada, precio sin fuente, fuente sin precio, exceso de 120, limpieza por precio/presentación, atomicidad sin mutación | `mvn -Dtest=SupermarketControllerTests test`. |
| Static Java | HTML, cache tokens, términos permitidos/prohibidos | `mvn -Dtest=StaticUiContractTests test`. |
| Static Node | Payload, validate, edit/reset, render secundario, lista manual/sugerida sin fuente | `node src/test/resources/static-ui-contract-tests.mjs`. |

## Migration / Rollout

No requiere migración manual: Hibernate `ddl-auto=update` agrega columna nullable y filas legacy responden `null`. Slicing forzado stacked-to-main: PR1 backend/API/tests, PR2 UI/static/tests, PR3 OpenSpec archive/spec sync.

## Open Questions

None.
