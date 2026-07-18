# Proposal: Historial manual de observaciones de precio Etapa 10

## Intent

Permitir registrar observaciones manuales de precio en el tiempo para productos del super, sin convertir el guardado genérico del producto en un historial automático ni introducir comercios, comparación o automatización.

## Scope

### In Scope
- Nueva entidad/tabla append-only de observaciones asociada a `SuperItem` y a su presentación comercial default.
- Alta solo por acción/endpoint manual explícito con precio positivo en pesos, fuente opcional, fecha observada opcional date-only, snapshot de presentación y `createdAt`.
- Consulta de observaciones recientes globales y filtrables por producto con límite seguro.
- UI mínima de formulario/listado que puede prellenar precio/fuente/fecha actuales, persistiendo solo al enviar observación.

### Out of Scope
- Edición/borrado, tienda/comercio, fuentes normalizadas, comparación, gráficos, múltiples presentaciones, OCR, lookup externo, scraping, automatización, totales sugeridos, sugerencias persistidas, mezcla de listas y Producto Base/catálogo.
- Mutaciones colaterales de `checked`, `currentStock`, movimientos, barcodes, lista manual o lista sugerida.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: habilita observaciones manuales append-only de precio y ajusta el límite de Etapa 10 sin abrir los demás fuera de alcance.

## Approach

Seguir el patrón de `SuperItemStockMovement`: entidad hija `SuperItemPriceObservation`, repositorio con consultas recientes, servicio transaccional y endpoints `POST /api/super/items/{id}/price-observations` y `GET /api/super/price-observations?itemId=&limit=50`. Validar item activo, presentación default existente, precio positivo, fuente recortada y fecha no futura. Copiar snapshot de presentación para no reinterpretar observaciones viejas si cambia la presentación actual. No crear observaciones desde `POST/PUT /api/super/items`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Delta para permitir solo esta historia manual. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/*PriceObservation*` | New | Entidad, repositorio y DTOs. |
| `SupermarketService` / controller | Modified | Alta/listado explícitos y validaciones. |
| `src/main/resources/static/*` | Modified | Formulario/lista manual y llamadas API. |
| `src/test/**` | Modified | Cobertura API, UI estática y guardas de no colateralidad. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Abrir accidentalmente comercios o múltiples precios | Medium | Allow-list estricta y tests negativos. |
| Duplicados por alta implícita | Medium | Prohibir auto-creación desde create/update. |
| UI excede presupuesto | Medium | Mantener formulario/listado mínimo. |

## Rollback Plan

Revertir la entidad/endpoints/UI y restaurar la delta spec para que `super-inventory` vuelva al contrato de precio/fuente/fecha actual sin historial. Las filas nuevas quedan aisladas en una tabla hija sin afectar productos, stock ni listas.

## Dependencies

- Sin dependencias externas; usa JPA/Hibernate `ddl-auto` existente.

## Success Criteria

- [ ] Solo una acción manual explícita crea observaciones de precio.
- [ ] El listado reciente funciona globalmente y por producto con límite seguro.
- [ ] Precio/fuente/fecha actuales se preservan y no generan historial automático.
- [ ] No hay mutaciones en stock, `checked`, movimientos, barcodes ni listas.
