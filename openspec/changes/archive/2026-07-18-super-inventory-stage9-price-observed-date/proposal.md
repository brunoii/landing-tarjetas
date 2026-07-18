# Proposal: Fecha observada del precio de referencia

## Intent

Permitir que el usuario registre manualmente una fecha opcional de observación para el precio actual/de referencia de la presentación comercial default, sin convertirlo en historial, timestamp, tienda ni comparación automática.

## Scope

### In Scope
- Campo nullable `commercialPresentationPriceObservedDate` (`LocalDate`) asociado solo a `commercialPresentationPricePesos` en `SuperItem`.
- Fecha manual opcional: el precio MAY existir sin fecha; la fecha MUST existir solo con presentación y precio.
- Rechazo de fechas futuras y limpieza de la fecha al borrar precio o presentación.
- Exposición API/UI como metadata secundaria junto a precio y fuente, preservando payloads legacy y datos no relacionados.

### Out of Scope
- Datetime/timestamp, historial de precios, tienda/comercio, catálogo de fuentes, múltiples precios o presentaciones.
- OCR, lookup externo, automatización de compra/consumo, totales sugeridos, sugerencias persistidas, mezcla de listas o Producto Base.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: ampliar la presentación comercial default para aceptar una fecha manual opcional solo para el precio de referencia existente.

## Approach

Agregar `commercialPresentationPriceObservedDate` como `LocalDate` nullable en entidad y DTOs. Validar en `SupermarketService` que no sea futura y que no quede huérfana sin `commercialPresentationLabel` y `commercialPresentationPricePesos`. Limpiarla a `null` al limpiar precio o presentación. En UI, usar input manual `type="date"`, preservar intención inválida hasta validarla y renderizarla como dato secundario cercano a precio/fuente.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Delta de contrato para fecha observada manual. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modified | Entidad, DTOs, validación y limpieza transaccional. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modified | Etiqueta legible para errores de fecha/formato. |
| `src/main/resources/static/index.html` | Modified | Input opcional de fecha observada. |
| `src/main/resources/static/js/supermarket.js` | Modified | Payload, edición, reset, validación cliente y render secundario. |
| `src/test/java/com/gentleia/landingtarjetas/`, `src/test/resources/static-ui-contract-tests.mjs` | Modified | Contratos backend/UI/static y no mutaciones colaterales. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Confundir fecha con timestamp | Medium | Nombre `ObservedDate` y tipo `LocalDate`; evitar `ObservedAt`. |
| Fecha huérfana descartada silenciosamente en UI | Medium | Preservar payload hasta validación, siguiendo la corrección de Etapa 8. |
| Deriva hacia historial/precios múltiples | Low | Mantener único campo nullable y guards explícitos de alcance. |

## Rollback Plan

Revertir campo, DTOs, validaciones, UI y tests. Al ser nullable, los payloads legacy permanecen válidos; no debe requerirse migración manual adicional sobre H2 con `ddl-auto=update`.

## Dependencies

- Ninguna dependencia externa. `openspec/config.yaml` no existe; aplicar convenciones compartidas y la spec vigente.

## Success Criteria

- [ ] Fecha válida con presentación y precio persiste, se expone y se renderiza como metadata secundaria.
- [ ] Fecha futura o huérfana se rechaza sin modificar el producto persistido.
- [ ] Borrar precio o presentación limpia la fecha.
- [ ] Payloads legacy y cambios de fecha no mutan `checked`, `currentStock`, movimientos, barcodes, lista manual ni lista sugerida.
