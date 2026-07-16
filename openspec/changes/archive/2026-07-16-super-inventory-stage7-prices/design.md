# Diseño: Precio actual de presentación default Etapa 7

## Technical Approach

Extender el `SuperItem` existente con un único precio actual/de referencia nullable en pesos para la presentación comercial default. El precio viajará por el contrato actual `POST/PUT/GET /api/super/items`, validado en `SupermarketService`, y se renderizará como información comercial editable. No crea tiendas, historial, múltiples presentaciones, totales, automatización, lookup externo ni catálogo paralelo; tampoco muta `checked`, `currentStock`, sugerencias, movimientos, barcodes ni lista manual.

## Architecture Decisions

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Campo nullable en `SuperItem` vs tabla de precios | Tabla nueva prepara historial/tiendas, pero abre alcance prohibido. | Agregar `commercialPresentationPricePesos` nullable en `SuperItem`. |
| Precio de presentación vs precio por unidad de inventario | Precio por unidad requeriría conversión y totales. | El precio pertenece solo a la presentación default y se muestra como referencia. |
| Reusar `/api/super/items` vs endpoint dedicado | Endpoint dedicado agrega superficie sin comando independiente. | Mantener create/update/list existentes. |
| Abrir static guard de precio | Abrir demasiado habilita tiendas/historial. | Permitir solo tokens exactos del precio actual; mantener bloqueados stores/shops/history de precio/automation. |

## Data Flow

```
index.html price input ─→ supermarket.js payload ─→ api.js /api/super/items
        └──────── render informative price ← SuperItemResponse ← SupermarketService
```

`createItem/updateItem` aplican inventario y presentación como hoy, luego validan el precio. No deben llamar stock commands, movement repositories, suggested-list persistence ni barcode aliases.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Agregar columna nullable `commercial_presentation_price_pesos` `BigDecimal` (`precision=12, scale=2`) con getters/setters. |
| `SuperItemRequest.java` | Modify | Agregar `commercialPresentationPricePesos` con `@Digits(integer=10, fraction=2)` y `@DecimalMin(value="0.0", inclusive=false)`. |
| `SuperItemResponse.java` | Modify | Exponer el campo nullable en create/update/list. |
| `SupermarketService.java` | Modify | Agregar validación: precio `null` permitido; si existe debe ser positivo y requerir `commercialPresentationLabel` no blanco. No modificar stock/checked/movimientos. |
| `src/main/resources/static/index.html` | Modify | Agregar input `#super-item-presentation-price-pesos` (`type=number`, `min=0.01`, `step=0.01`) y columna “Precio ref.”; ajustar `colspan` y cache tokens si cambia HTML/CSS/app. |
| `src/main/resources/static/js/supermarket.js` | Modify | Incluir payload, validación, edit/reset y render `superItemCommercialPresentationPriceLabel`; no incluir precio en lista manual ni sugerencias. |
| `src/main/resources/static/js/app.js` | Modify | Actualizar import token de `./supermarket.js` si cambia ese módulo; preservar tokens no tocados. |
| `src/main/resources/static/css/styles.css` | Modify | Ajustes mínimos de tabla/badge/precio si la columna lo requiere; actualizar token en `index.html` solo si cambia CSS. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Tests legacy null, precio válido, precio inválido sin mutación, y no mutación de checked/stock/sugerencias/movimientos/barcodes/lista manual. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Permitir solo precio actual de presentación; seguir bloqueando `prices`, `priceHistory`, tiendas/comercios, múltiples presentaciones, lookup externo y automatización. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Cubrir payload/render/validación/cache tokens y mantener guards equivalentes. |

## Interfaces / Contracts

```json
{
  "commercialPresentationLabel": "Pack x 6",
  "commercialPresentationQuantity": 6.000,
  "commercialPresentationPricePesos": 1250.50
}
```

`commercialPresentationPricePesos` es opcional y nullable. Si está presente, MUST ser `BigDecimal` positivo en pesos, con hasta 2 decimales, y requerir presentación default existente. `commercialPresentationQuantity` conserva su semántica actual y no es obligatoria para cargar precio.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend/API | Persistencia, null legacy, validación positiva, precio sin presentación y no mutación colateral | `mvn -Dtest=SupermarketControllerTests test`. |
| Static Java | HTML/API/static guards/cache tokens | `mvn -Dtest=StaticUiContractTests test`. |
| Static Node | Payload, labels, render, generated list unchanged | `node src/test/resources/static-ui-contract-tests.mjs`. |

## Migration / Rollout

No migration required beyond nullable column addition by Hibernate `ddl-auto=update`; legacy rows return `null`. Stacked-to-main slices: PR 1 backend/API/tests, PR 2 UI/static/tests, PR 3 OpenSpec archive/sync if applicable.

## Open Questions

None.
