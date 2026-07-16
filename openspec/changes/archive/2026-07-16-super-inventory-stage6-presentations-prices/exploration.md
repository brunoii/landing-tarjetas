## Exploration: super-inventory-stage6-presentations-prices

### Current State

`main` está limpio y contiene la Etapa 5 archivada: la lista sugerida se calcula en backend con `GET /api/super/suggested-list`, se muestra separada de la lista manual y no muta `checked`, stock, movimientos ni barcodes.

La especificación vigente de `super-inventory` todavía limita explícitamente la Etapa 5 a sugerencias read-only y mantiene fuera de contrato precios, tiendas y presentaciones comerciales. El modelo actual de `SuperItem` tiene `unit`, `habitualObjective`, `currentStock` y `quickQuantity`, pero no tiene campos de presentación comercial ni precio. Los contratos estáticos también bloquean términos como `price`, `store` y `presentation` en la UI del super.

La próxima frontera segura no debería abrir precios completos todavía. Antes de registrar precios hace falta una base mínima para saber qué se compra comercialmente frente a qué unidad se controla en inventario; si no, el precio queda ambiguo entre unidad de stock, paquete, botella, pack, tienda o fecha.

### Affected Areas
- `openspec/specs/super-inventory/spec.md` — requiere un delta que levante solo la prohibición de presentaciones comerciales mínimas y mantenga precios/tiendas fuera de alcance si se acepta la slice pequeña.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — lugar más conservador para agregar metadatos opcionales de presentación sobre el producto existente, sin crear Producto Base paralelo.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` / `SuperItemResponse.java` — contratos compatibles para aceptar y exponer presentación opcional.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — validación transaccional y preservación de stock/movimientos al configurar presentación.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` — límites explícitos para etiquetas/campos de presentación.
- `src/main/resources/static/js/supermarket.js` / `index.html` / `css/styles.css` — formulario, tabla y render de la presentación sin mezclarlo con lista manual, sugerencias ni movimientos.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cobertura de retrocompatibilidad, validación y no mutación de stock/checked/movimientos.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` — abrir `presentation` de forma limitada y seguir bloqueando `price`, `store`, automatización y lookup externo.

### Approaches
1. **Prerequisito: presentación comercial default sin precios** — Agregar metadatos opcionales al `SuperItem` existente para describir cómo se compra normalmente el producto (por ejemplo, etiqueta de presentación y cantidad contenida expresada en la unidad de inventario), sin precio ni tienda.
   - Pros: Slice pequeña; conserva el modelo evolutivo; prepara precios sin ambigüedad; no toca stock ni movimientos; mantiene backend como source of truth.
   - Cons: Solo cubre una presentación default por producto; múltiples presentaciones quedan para una etapa posterior.
   - Effort: Medium

2. **Presentaciones y precio actual en una sola etapa** — Agregar presentación default más campos de precio directo en el producto.
   - Pros: Entrega valor visible más rápido para estimar compras.
   - Cons: Mezcla dos conceptos; precio sin tienda/fecha/historial envejece rápido; aumenta scope y riesgo de romper los guards de precio/tienda.
   - Effort: High

3. **Entidad de presentaciones múltiples con precios por tienda** — Crear un submodelo de presentaciones por producto y precios asociados.
   - Pros: Modelo más completo para barcodes por presentación, historial de precios y comparación por tienda.
   - Cons: Demasiado grande para la próxima slice; requiere varias pantallas/contratos; puede exceder el presupuesto de revisión.
   - Effort: High

### Recommendation

Avanzar con **Approach 1: presentación comercial default sin precios** como Stage 6A, aunque el candidato se llame `super-inventory-stage6-presentations-prices`. La propuesta debería aclarar que esta etapa prepara el terreno para precios, pero no registra precios todavía.

Regla mínima recomendada: la presentación comercial es nullable/opcional, retrocompatible y solo describe la compra habitual del producto existente. Configurar o editar presentación MUST NOT modificar `checked`, `currentStock`, lista sugerida, movimientos ni alias de barcode. Los precios, tiendas, historial de precios, múltiples presentaciones, automatización de compra y conversión automática de sugerencias deben quedar fuera de alcance.

### Risks
- Abrir precios junto con presentaciones puede introducir semántica incompleta: precio por unidad de stock vs. precio por pack vs. precio por tienda/fecha.
- Una presentación default puede quedarse corta para productos con múltiples formatos, pero es más segura que diseñar todo el catálogo de presentaciones ahora.
- Los tests estáticos bloquean `presentation`; habrá que abrir ese término de forma quirúrgica y mantener bloqueados `price`, `store` y automatizaciones.
- La UI del super ya está cargada; sumar columnas/campos puede aumentar líneas de cambio y justificar el split chained.
- Hibernate `ddl-auto=update` sigue siendo el mecanismo de schema evolution; campos nuevos deben ser nullable para productos existentes.

### Ready for Proposal

Yes — listo para propuesta si el scope se reduce a presentaciones comerciales default sin precios. El mensaje al usuario/orquestador: la etapa segura no es “presentaciones y precios” completa; primero conviene modelar la presentación de compra sobre el `SuperItem` existente, preservar todo el comportamiento actual y dejar precios/tiendas para una etapa posterior.
