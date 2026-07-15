# Proposal: PRD Etapa 3 — Movimientos

## Intent

Habilitar movimientos reales de inventario sin reconstruir el módulo: compras, consumos, consumo rápido e historial visible deben apoyarse en `currentStock` y movimientos auditables.

## Scope

### In Scope
- Extender `SuperItemStockMovement` y `super_stock_movements`; no renombrar ni reemplazar la entidad/tabla.
- Agregar comandos manuales de compra y consumo, más consumo rápido usando `quickQuantity`.
- Mantener el ajuste existente como comando absoluto, compatible y no negativo.
- Mostrar historial reciente de movimientos, con filtro mínimo por producto.
- Garantizar que todo comando que cambia stock guarde movimiento y snapshot en la misma transacción.
- Requerir confirmación explícita de stock negativo solo en flujos de consumo.
- Mantener `currentStock=null` como stock desconocido; compra/consumo deben exigir inicialización previa mediante ajuste.

### Out of Scope
- Precios, supermercados, presentaciones comerciales, barcode y OCR.
- Lista sugerida automática.
- Filtros avanzados fuera de historial reciente/filtro por producto.
- Renombrar la tabla o entidad de movimientos.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `super-inventory`: agrega movimientos operativos, historial visible y reglas de stock conocido/negativo sobre el inventario existente.

## Approach

Extender la base movement-first de Etapa 2. Incorporar tipos `PURCHASE`, `CONSUMPTION` y `QUICK_CONSUMPTION`, con metadata nullable compatible. Centralizar en `SupermarketService` la validación, cálculo, persistencia transaccional y conflicto cuando un consumo requiera confirmación negativa. La UI agregará acciones compactas e historial mínimo sin rediseñar.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modified | Entidad, repositorio, servicio, controladores y DTOs. |
| `src/main/resources/static/` | Modified | Acciones de compra/consumo/rápido, confirmación negativa e historial. |
| `src/test/java/com/gentleia/landingtarjetas/` | Modified | Contratos API, UI estática, atomicidad y compatibilidad de ajustes. |
| `openspec/specs/super-inventory/spec.md` | Modified | Delta de Etapa 3 sobre límites y movimientos. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Historial inconsistente por comandos concurrentes | Med | Usar bloqueo u optimismo en comandos de stock. |
| Confundir `null` con cero | Med | Validar stock inicializado antes de compra/consumo. |
| Stock negativo fuera de consumo | Low | Confirmación solo para `CONSUMPTION` y `QUICK_CONSUMPTION`. |

## Rollback Plan

Revertir endpoints, UI y columnas nullable nuevas; conservar `super_stock_movements` y ajustes `ADJUSTMENT` existentes, que siguen siendo compatibles.

## Dependencies

- Etapas 1 y 2 de `super-inventory` ya aplicadas.

## Success Criteria

- [ ] Compra, consumo y consumo rápido actualizan stock y registran movimiento atómicamente.
- [ ] Ajuste absoluto existente sigue funcionando y genera `ADJUSTMENT` compatible.
- [ ] Stock desconocido bloquea compra/consumo hasta ajuste inicial.
- [ ] Consumo negativo requiere confirmación explícita y queda visible en historial.
