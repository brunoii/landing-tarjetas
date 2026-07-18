# Proposal: Fuente manual del precio de referencia

## Intent

Permitir que el usuario registre una etiqueta manual opcional que indique de dónde tomó el precio actual/de referencia de la presentación comercial default, sin convertir esa fuente en tienda, historial ni mecanismo de comparación.

## Scope

### In Scope
- Campo nullable `commercialPresentationPriceSourceLabel` asociado solo a `commercialPresentationPricePesos` en `SuperItem`.
- Trim, límite explícito de longitud (recomendado: 120 caracteres) y rechazo de fuente sin precio.
- Exposición en API/UI como información secundaria y manual junto al precio de referencia.
- Compatibilidad de payloads legacy sin precio/fuente y preservación de inventario, listas, barcodes y movimientos.

### Out of Scope
- Entidad tienda/comercio, catálogo de fuentes, historial, múltiples precios o múltiples presentaciones.
- OCR, lookup externo, automatización de compra/consumo, totales estimados, sugerencias persistidas, mezcla de listas o Producto Base.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: ampliar la presentación comercial default para aceptar una fuente manual opcional solo cuando exista precio de referencia.

## Approach

Agregar un string nullable en `SuperItem`, transportarlo por `SuperItemRequest`/`SuperItemResponse` y validarlo en `SupermarketService` junto con el precio. Si se elimina el precio o la presentación, limpiar la fuente a `null`. En UI, mostrarla como subtexto/ayuda secundaria del precio, evitando copy o tokens que sugieran tiendas, historial, comparación o automatización.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Delta de contrato para fuente manual opcional. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modified | Entidad, DTOs, límites y validación transaccional. |
| `src/main/resources/static/index.html` | Modified | Input opcional de fuente manual del precio. |
| `src/main/resources/static/js/supermarket.js` | Modified | Payload, edición, reset y render secundario. |
| `src/test/java/com/gentleia/landingtarjetas/` | Modified | Contratos backend y UI/static sin mutaciones colaterales. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Derivar hacia tiendas/historial | Medium | Nombres `sourceLabel`/`Fuente` y guards exactos. |
| Fuente huérfana sin precio | Medium | Rechazarla o limpiarla cuando no haya precio. |
| Sobrecarga visual | Low | Render como información secundaria, no columna conceptual nueva. |

## Rollback Plan

Revertir el campo, DTOs, validaciones, UI y tests del cambio. Los payloads previos siguen válidos porque el dato nuevo es nullable y no debe afectar inventario ni listas.

## Dependencies

- Ninguna dependencia externa. `openspec/config.yaml` no existe; aplicar convenciones compartidas y la spec vigente.

## Success Criteria

- [ ] Fuente con precio válido se trimea, persiste, expone y renderiza como información manual secundaria.
- [ ] Fuente sin precio o sobre límite se rechaza sin modificar el producto.
- [ ] Payloads legacy y cambios de precio/presentación no mutan `checked`, `currentStock`, movimientos, barcodes, lista manual ni sugerida.
