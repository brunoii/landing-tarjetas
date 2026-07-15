## Exploración: PRD Etapa 3 — Movimientos

### Estado Actual
`main` está limpio en `4ae6ebb` e incluye Etapa 1 y Etapa 2. El módulo del super conserva `SuperItem` como producto base evolutivo, con `unit`, `habitualObjective`, `currentStock` nullable y `quickQuantity` opcional. `checked` sigue siendo intención manual de compra y no modifica stock.

La Etapa 2 ya preparó una base movement-first mínima: `POST /api/super/items/{id}/stock-adjustments` ajusta `currentStock` absoluto dentro de una transacción y guarda un `SuperItemStockMovement` en `super_stock_movements` con `movementType=ADJUSTMENT`, `previousStock`, `resultingStock` y `createdAt`. No hay historial visible, compras, consumos, consumo rápido ni lista sugerida automática. La UI actual mezcla edición de producto con ajuste de stock: el formulario guarda producto y, si se informa stock, llama un endpoint separado de ajuste.

### Áreas Afectadas
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovement.java` — debe evolucionar de ajuste interno a hecho completo de movimiento de stock.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemStockMovementRepository.java` — necesita consultas ordenadas para historial y, posiblemente, filtros por producto.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — concentrará compras, consumos, consumo rápido, ajustes, validación de stock desconocido/negativo y escritura transaccional de movimiento + snapshot.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemController.java` — mantendrá ajuste y expondrá comandos nuevos o delegará a un controlador de movimientos.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` / `SuperItemResponse.java` — `currentStock` podría admitir resultado negativo confirmado; la respuesta debe seguir mostrando snapshot actual.
- Nuevos DTOs backend — requests/responses para compra, consumo, consumo rápido y listado de movimientos.
- `src/main/resources/static/js/api.js` — helpers para comandos de compra/consumo/consumo rápido y consulta de historial.
- `src/main/resources/static/js/supermarket.js` — acciones de movimiento, confirmación de stock negativo, render de historial y eliminación de semánticas fuera de etapa anterior en tests.
- `src/main/resources/static/index.html` / `src/main/resources/static/css/styles.css` — controles mínimos para movimientos e historial sin rediseñar la pantalla.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — contratos de API, persistencia de movimientos, compatibilidad de ajustes previos y validaciones transaccionales.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — contratos de UI/API estática, términos ahora soportados y flujos de confirmación.
- `openspec/specs/super-inventory/spec.md` — requiere delta de Etapa 3 que modifique los límites de Etapa 2 y agregue compras, consumos e historial visible.

### Enfoques
1. **Extender `SuperItemStockMovement` sin renombrar** — Mantener entidad/tabla actual y agregar tipos `PURCHASE`, `CONSUMPTION`, `QUICK_CONSUMPTION` junto a `ADJUSTMENT`, más campos nullable compatibles como `quantityDelta`, `notes` y/o `source` si el diseño los necesita.
   - Pros: Preserva filas de Etapa 2 sin migración destructiva; respeta `ddl-auto=update`; permite exponer historial inmediatamente; bajo riesgo de datos.
   - Cons: El nombre queda menos genérico que `SuperInventoryMovement`; algunos campos nuevos deben ser nullable por compatibilidad.
   - Esfuerzo: Medio

2. **Renombrar/generalizar a `SuperInventoryMovement` y tabla nueva** — Crear entidad/tabla nueva más limpia y migrar o copiar ajustes existentes.
   - Pros: Modelo semánticamente más claro para largo plazo; separa el concepto de movimiento del nombre inicial de Etapa 2.
   - Cons: Con Hibernate `ddl-auto=update` no hay rename seguro; se pueden perder/duplicar ajustes existentes; requiere backfill o migración manual que el proyecto aún no tiene.
   - Esfuerzo: Alto

3. **Crear endpoints sin enriquecer historial** — Usar `currentStock` y registrar solo `previousStock/resultingStock/type`, sin campos de cantidad ni metadata adicional.
   - Pros: Implementación corta y compatible con la tabla actual.
   - Cons: Historial menos explicativo; compras/consumos dependen de inferir cantidad desde snapshot; peor para auditoría y UX.
   - Esfuerzo: Bajo/Medio

### Recomendación
Usar el Enfoque 1: extender mínimamente `SuperItemStockMovement` y conservar `super_stock_movements`. No renombrar en Etapa 3 porque la tabla ya contiene ajustes reales de Etapa 2 y no hay migraciones versionadas; una “limpieza” de nombres ahora compraría riesgo sin aportar valor funcional.

Plan API sugerido:
- `POST /api/super/items/{id}/purchases` con `{ quantity, notes? }`: requiere cantidad positiva y stock conocido; suma al snapshot y registra `PURCHASE`.
- `POST /api/super/items/{id}/consumptions` con `{ quantity, notes?, allowNegativeStock?: false }`: requiere cantidad positiva y stock conocido; si el resultado es negativo y no hay confirmación explícita, responder `409 Conflict` con datos suficientes para que la UI confirme; con confirmación, registra `CONSUMPTION` y permite snapshot negativo.
- `POST /api/super/items/{id}/quick-consumptions` con `{ allowNegativeStock?: false }`: usa `quickQuantity`; rechaza si no existe; aplica la misma regla de stock negativo y registra `QUICK_CONSUMPTION`.
- Mantener `POST /api/super/items/{id}/stock-adjustments` para ajustes absolutos no negativos y registrar `ADJUSTMENT`; las filas existentes siguen válidas aunque no tengan campos nuevos.
- `GET /api/super/movements?itemId=&limit=50` para historial reciente, con respuesta plana: producto, tipo, cantidad/delta cuando exista, stock anterior, stock resultante, notas y fecha.

Reglas de dominio recomendadas:
- `currentStock=null` sigue significando stock desconocido. Compras y consumos delta no deberían inventar un stock inicial; pedir primero un ajuste explícito.
- Ajuste manual conserva semántica absoluta; compra/consumo son deltas.
- Todo método que cambie stock debe guardar movimiento y snapshot en la misma transacción.
- Para evitar carreras entre dos consumos/compras simultáneas, agregar lectura bloqueante u optimista del item en comandos de stock; el ajuste actual no lo necesita para UX local, pero Stage 3 ya depende de historial correcto.

UI mínima sin sobreconstruir:
- En la tabla, agregar acciones compactas: “Compra”, “Consumir”, “Rápido” y “Historial”, evitando convertir el formulario principal en un panel complejo.
- Usar un panel/modal simple para ingresar cantidad/notas de compra o consumo.
- Para consumo rápido, un botón directo que usa `quickQuantity`; si produciría stock negativo, mostrar confirmación antes de reintentar con `allowNegativeStock=true`.
- Mostrar un historial reciente como sección/panel único, filtrable por producto si se abre desde una fila. No implementar lista sugerida automática ni precios/presentaciones en esta etapa.

Migración y compatibilidad:
- Conservar `super_stock_movements`; agregar columnas nullable para nueva metadata. Las filas de Etapa 2 quedan como `ADJUSTMENT` con `previousStock/resultingStock` y se pueden mostrar en historial.
- Si se agrega `quantityDelta`, no hacerlo `NOT NULL` hasta tener backfill; para ajustes previos se puede mostrar “Ajuste a X” en lugar de cantidad.
- `currentStock` ya no debe asumirse siempre no negativo si se acepta consumo negativo confirmado; limitar esa posibilidad a endpoints de consumo, no a ajuste manual.

Slicing recomendado bajo presupuesto de 800 líneas:
- PR 1: backend modelo/DTO/API/tests para tipos de movimiento, compras, consumos, consumo rápido, listado y compatibilidad de ajustes.
- PR 2: UI mínima, helpers API, confirmación de negativo, historial visible y contratos estáticos.
- Usar `feature-branch-chain` si la estimación de cambios supera el presupuesto.

### Riesgos
- Permitir negativo confirmado cambia la semántica previa de `currentStock >= 0`; debe quedar acotado a consumo y reflejado en spec/tests/UI.
- Sin bloqueo u optimismo, dos consumos simultáneos pueden calcular desde el mismo stock y dejar historial inconsistente.
- `currentStock=null` no debe convertirse silenciosamente en cero; hacerlo contaminaría inventario existente.
- Renombrar entidad/tabla con `ddl-auto=update` puede abandonar los ajustes ya registrados.
- La UI actual llama ajuste de stock como efecto secundario del formulario de producto; Stage 3 debería separar acciones de movimiento para que “todo cambio de stock genera movimiento” sea visible y entendible.

### Listo para Propuesta
Sí — avanzar con una propuesta de Etapa 3 que extienda la tabla actual de movimientos, mantenga ajustes compatibles, agregue comandos enfocados para compra/consumo/consumo rápido y exponga un historial reciente sin introducir lista sugerida, precios, presentaciones, barcode ni OCR.
