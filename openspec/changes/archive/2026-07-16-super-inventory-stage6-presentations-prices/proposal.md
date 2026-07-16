# Propuesta: Presentación comercial default Etapa 6A

## Intent

Preparar precios futuros sin introducir precios todavía: describir cómo se compra habitualmente un `SuperItem` frente a la unidad usada para controlar inventario, manteniendo intactos stock, lista manual, sugerencias, movimientos y barcodes.

## Scope

### In Scope
- Metadatos opcionales/nullable de presentación comercial default sobre el `SuperItem` existente.
- Contratos retrocompatibles para crear, editar y consultar la presentación cuando exista.
- Validaciones y UI mínimas para etiqueta de presentación y cantidad contenida expresada en la unidad de inventario.

### Out of Scope
- Precios, tiendas, historial de precios y comparación por comercio.
- Múltiples presentaciones por producto, catálogo paralelo o Producto Base nuevo.
- Compra automática, conversión de sugerencias, mutación de `checked`, `currentStock`, movimientos o alias de barcode.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: permite presentación comercial default opcional en productos existentes y mantiene fuera de contrato precios, tiendas y automatizaciones.

## Approach

Extender el modelo evolutivo actual: `SuperItem` sigue siendo la identidad funcional. La presentación será un dato comercial nullable, por ejemplo una etiqueta visible y una cantidad contenida positiva expresada en la unidad de inventario. Backend conserva la validación y la UI solo captura/renderiza el dato; ninguna operación de presentación dispara stock, sugerencias, movimientos ni barcodes.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Delta para levantar solo presentación default. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem*.java` | Modified | Campos, request/response y límites. |
| `SupermarketService.java` | Modified | Validación sin mutaciones colaterales. |
| `src/main/resources/static/` | Modified | Formulario/render mínimo y contratos estáticos. |
| `SupermarketControllerTests.java` | Modified | Retrocompatibilidad y no mutación. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Confundir presentación con precio | Med | Mantener `price`/`store` bloqueados. |
| Datos existentes sin presentación | Low | Campos nullable y respuestas compatibles. |
| UI supera presupuesto | Med | Mantener slice mínima y usar PRs encadenados. |

## Rollback Plan

Revertir el delta, cambios de modelo/DTO/UI/tests y dejar campos nuevos sin uso; al ser nullable, productos existentes permanecen válidos.

## Dependencies

- Ninguna externa. Usa `ddl-auto=update` actual para columnas nullable.

## Success Criteria

- [ ] Productos viejos siguen creando/listando sin presentación.
- [ ] Presentación opcional se guarda y consulta sin afectar `checked`, `currentStock`, sugerencias, movimientos ni barcodes.
- [ ] Precios, tiendas y automatizaciones siguen fuera de contrato.
