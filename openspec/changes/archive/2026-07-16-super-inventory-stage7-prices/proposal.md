# Propuesta: Precio actual de presentación default Etapa 7

## Intent

Agregar un precio actual/de referencia, opcional y en pesos, para la presentación comercial default existente de cada `SuperItem`, sin convertirlo en precio por unidad de inventario ni automatizar compras.

## Scope

### In Scope
- Persistir, exponer y editar precio nullable positivo para la presentación default.
- Mostrar el precio como dato informativo de referencia en la UI.
- Mantener intactos `checked`, `currentStock`, sugerencias, movimientos, barcodes y lista manual.

### Out of Scope
- Tiendas/comercios, historial de precios, múltiples presentaciones, lookup externo u OCR.
- Compra/consumo automático o estimación de total de lista sugerida.
- Catálogo paralelo o precio por unidad de inventario.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: permitir precio actual/de referencia nullable, positivo cuando exista y expresado en pesos, asociado solo a la presentación comercial default del `SuperItem` existente.

## Approach

Extender el modelo evolutivo actual con un único campo de precio nullable sobre `SuperItem`; aceptar/exponer el dato en los contratos existentes; validar positividad y semántica en `SupermarketService`; abrir `price` quirúrgicamente en contratos estáticos mientras siguen bloqueados tiendas, historial, múltiples presentaciones y automatizaciones.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Delta para levantar solo precio actual y preservar exclusiones. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modified | Campo, request/response, límites y validación. |
| `src/main/resources/static/` | Modified | Captura y render informativo del precio. |
| `src/test/java/com/gentleia/landingtarjetas/` | Modified | Regresiones API/UI/static contracts y no mutación colateral. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Precio único puede quedar desactualizado | Med | Nombrarlo como actual/de referencia; diferir historial. |
| Derivar totales o compras prematuramente | Med | Prohibir automatización y mantener uso solo informativo. |
| Abrir demasiado los contratos estáticos | Med | Permitir solo precio actual; seguir bloqueando stores/history/presentations. |

## Rollback Plan

Revertir el delta y las modificaciones de campo/contrato/UI/tests; los productos sin precio siguen válidos por nulabilidad. Si hubiera datos persistidos, ignorar o eliminar la columna en migración de reversa.

## Dependencies

- Stage 6A: presentación comercial default opcional ya aceptada en `super-inventory`.
- Sin dependencias externas.

## Success Criteria

- [ ] Productos legacy sin precio siguen funcionando y exponen precio ausente/null.
- [ ] Precio presente debe ser positivo, en pesos, y de referencia para la presentación default.
- [ ] Cambiar precio no muta `checked`, stock, sugerencias, movimientos, barcodes ni lista manual.
