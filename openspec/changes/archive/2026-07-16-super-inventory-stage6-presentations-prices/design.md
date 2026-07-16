# Diseño: Presentación comercial default Etapa 6A

## Technical Approach

Extender `SuperItem` con una única presentación comercial default nullable, sin tablas nuevas ni precios. El contrato existente de `/api/super/items` aceptará y devolverá los metadatos opcionales; la UI los capturará en el formulario actual de producto y los mostrará como información comercial. La implementación debe preservar inventario, lista manual, sugerencias, movimientos y barcodes: presentación describe cómo se compra, no ejecuta compras ni recalcula stock.

## Architecture Decisions

| Opción | Tradeoff | Decisión |
|--------|----------|----------|
| Campos nullable en `SuperItem` vs tabla `presentations` | La tabla nueva prepara múltiples presentaciones, pero contradice Etapa 6A. | Usar `commercialPresentationLabel` y `commercialPresentationQuantity` nullable en `SuperItem`. |
| Reusar `unit` para interpretar cantidad vs guardar otra unidad | Otra unidad abre conversiones y precios prematuros. | `commercialPresentationQuantity` siempre se expresa en `unit`; si hay cantidad y falta `unit`, se rechaza. |
| Endpoint dedicado vs contrato actual de producto | Endpoint dedicado aísla operaciones, pero agrega superficie innecesaria. | Mantener `POST/PUT /api/super/items`; presentación no tiene endpoint propio ni comandos de stock. |
| Guard static actual vs abrir presentación completa | El guard actual bloquea `presentation`; abrir todo podría habilitar precio/tienda por accidente. | Actualizar el guard para permitir solo presentación y seguir bloqueando `price`, `prices`, `store`, tiendas y automatizaciones. |

## Data Flow

```
index.html form ─→ supermarket.js payload ─→ api.js /api/super/items
        └──────── render item response ← SuperItemResponse ← SupermarketService
```

`SupermarketService.createItem/updateItem` normaliza campos de presentación después de validar el request. No llama a `adjustItemStock`, `applyStockMovement`, repositorios de movimientos, sugerencias ni aliases de barcode.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` | Modify | Agregar `ITEM_PRESENTATION_LABEL_MAX_LENGTH` (120) para backend/UI. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Agregar columnas nullable `commercial_presentation_label` y `commercial_presentation_quantity` (`precision=10, scale=3`) con getters/setters. |
| `SuperItemRequest.java` | Modify | Agregar `commercialPresentationLabel` con `@Size` y `commercialPresentationQuantity` con `@Digits`/`@DecimalMin`. |
| `SuperItemResponse.java` | Modify | Exponer ambos campos nullable en list/create/update. |
| `SupermarketService.java` | Modify | Agregar `applyCommercialPresentation`: trim a null, validar cantidad positiva, cantidad requiere `unit`, cantidad sin label rechaza, ausencia limpia ambos. |
| `src/main/resources/static/index.html` | Modify | Agregar inputs `#super-item-presentation-label` y `#super-item-presentation-quantity`; agregar columna/label “Presentación”; actualizar solo los tokens de `/css/styles.css` y `/js/app.js` cuando cambien esos assets. |
| `src/main/resources/static/js/supermarket.js` | Modify | Extender `SUPER_FIELD_LIMITS`, payload/validación/render/edit/reset; no incluir precio/tienda ni mutaciones extra. |
| `src/main/resources/static/js/app.js` | Modify | Actualizar solo los import tokens de `./api.js` y `./supermarket.js` cuando cambien esos módulos; preservar tokens de módulos no tocados. |
| `src/main/resources/static/css/styles.css` | Modify | Solo ajustes mínimos para badge/texto de presentación si la tabla lo requiere. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Tests de legacy null, presentación válida, inválida sin mutación, y preservación de checked/stock/movimientos/barcodes/sugerencias. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Permitir quirúrgicamente `presentation` en contratos de super UI y cache tokens, manteniendo bloqueados precios, tiendas, múltiples presentaciones y automatizaciones. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modify | Contratos UI/API/static; permitir presentación y mantener bloqueados `price`, `prices`, `store`. |

## Interfaces / Contracts

```json
{
  "commercialPresentationLabel": "Pack x 6",
  "commercialPresentationQuantity": 6.000
}
```

Ambos campos son opcionales y nullable. Si `commercialPresentationLabel` está ausente/blanco, la presentación queda `null` y la cantidad debe quedar ausente. Si `commercialPresentationQuantity` existe, debe ser positiva y `unit` debe existir.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend | DTO/entity/service invariants | `mvn -Dtest=SupermarketControllerTests test` con casos nuevos y regresiones existentes. |
| Java Static UI | HTML/module cache contracts and forbidden terms | `mvn -Dtest=StaticUiContractTests test`; actualizar allowlist de `presentation` sin abrir `price`, `prices`, `store`, tiendas ni automatización. |
| Node Static UI | Payload, render, guards, tokens | `node src/test/resources/static-ui-contract-tests.mjs`; validar `index.html` CSS/app tokens y `app.js` api/supermarket import tokens. |
| Integration | No collateral mutations | Asserts sobre `checked`, `currentStock`, `SuperItemStockMovement`, suggested list y barcode alias antes/después. |

## Migration / Rollout

No migration required. Hibernate `ddl-auto=update` agregará columnas nullable; productos existentes responderán `null`. Entrega recomendada stacked-to-main: PR 1 backend/API/tests, PR 2 UI/static/tests, PR 3 OpenSpec archive si aplica.

## Open Questions

None.
