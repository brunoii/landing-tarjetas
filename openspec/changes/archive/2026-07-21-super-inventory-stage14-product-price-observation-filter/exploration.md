## Exploration: super-inventory-stage14-product-price-observation-filter

### Current State

La Etapa 13 está archivada y la especificación viva de `super-inventory` ya soporta observaciones manuales explícitas de precio que opcionalmente pueden sincronizarse con el precio actual/de referencia del producto. El backend ya expone `GET /api/super/price-observations?itemId=&limit=50` y los tests cubren filtrado global y por producto, pero la UI todavía carga solo la lista global reciente con `{ limit: 50 }`.

Esto deja una brecha pequeña de usabilidad después de Etapa 13: el usuario puede registrar y sincronizar precios desde un producto seleccionado, pero no puede inspeccionar rápidamente las observaciones de precio de ese producto desde la fila del producto. El historial de movimientos de stock ya tiene una acción por fila que filtra por producto, así que la Etapa 14 más chica y coherente es replicar esa interacción probada para observaciones de precio sin agregar comparación, gráficos, tiendas, múltiples precios/presentaciones, barcode/OCR/ticket/photo ni alcance de Etapa 15.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — debería ajustar el comportamiento visible para pasar de filtrado MAY a nivel backend a una vista explícita de observaciones por producto, preservando el historial global reciente.
- `src/main/resources/static/index.html` — probablemente necesita título/estado contextual para la tabla de observaciones y/o un control pequeño para volver a recientes.
- `src/main/resources/static/js/supermarket.js` — debería agregar una acción por fila o reutilizar una interacción de producto para llamar `superPriceObservations({ itemId, limit: 50 })`, renderizar la lista filtrada y actualizar título/texto vacío.
- `src/main/resources/static/js/api.js` — probablemente no cambia porque `superPriceObservations(filters = {})` ya soporta query params.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — probablemente no cambia o solo se extiende mínimamente; el filtro backend por producto ya está cubierto.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` — debería fijar el contrato UI product-scoped y mantener exclusiones de Etapa 15/admin/comparison/OCR/photo/ticket.
- `src/test/resources/static-ui-contract-tests.mjs` — debería cubrir el camino behavior-level de UI: pedir historial de precio de un producto envía `itemId`, renderiza solo ese contexto y permite volver a observaciones recientes globales.

### Approaches

1. **Drilldown de observaciones de precio por producto en la tabla existente** — Agregar una acción por fila de historial de precio que recargue la tabla existente con `itemId`, actualice título/estado vacío y ofrezca volver a observaciones recientes.
   - Pros: es la Etapa 14 útil más chica; reutiliza backend/API/tabla existentes; replica el patrón de historial de movimientos; mejora el flujo recién publicado en Etapa 13 sin modelo nuevo.
   - Cons: agrega una acción más en una celda de acciones ya densa; requiere copy claro para no confundirse con comparación o analítica.
   - Effort: Low/Medium.

2. **Observaciones recientes inline bajo cada producto** — Expandir una fila de producto para mostrar una lista corta embebida de sus observaciones.
   - Pros: muy contextual y rápido para el usuario.
   - Cons: más complejidad de DOM/estado, responsive más difícil, diff UI/static más grande y mayor riesgo para el presupuesto de revisión.
   - Effort: Medium.

3. **Iniciar comparación/analítica desde observaciones de precio** — Agregar tendencia por producto, totales, comparación por fuente o comportamiento tipo gráfico.
   - Pros: puede ser valioso más adelante cuando el historial de precios sea más rico.
   - Cons: demasiado grande y conceptualmente fuera de la siguiente Etapa 14 chica; arriesga reabrir tiendas/comercios/comparison/múltiples precios y desplazar la pausa de Etapa 15.
   - Effort: High.

### Recommendation

Avanzar con **Approach 1: drilldown de observaciones de precio por producto en la tabla existente** como `super-inventory-stage14-product-price-observation-filter`.

Es la última slice autónoma pre-Etapa 15 más coherente porque completa el loop de usabilidad de observaciones manuales después de Etapa 13: registrar/sincronizar un precio y luego inspeccionar las observaciones de ese producto sin revisar una tabla global. Usa capacidad backend existente, mantiene intacto el historial append-only y debería poder entregarse como cambio pequeño de UI/static/spec bajo la estrategia forced-chained.

### Risks

- La zona de acciones de la fila de producto ya está cargada; la UI debería usar etiquetas compactas y accesibles, evitando agregar un panel amplio.
- Los guards estáticos deben mantener fuera `comparison`, gráficos, tiendas/comercios, múltiples precios/presentaciones, barcode/OCR/ticket/photo y alcance de Etapa 15.
- Si se reabren tests backend innecesariamente, el cambio puede crecer más que la slice útil mínima; conviene apoyarse en la cobertura existente del filtro API salvo que proposal/spec revele una brecha real.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para Etapa 14 limitada a **filtrado de historial de observaciones de precio por producto en la UI existente**, respaldado por el query param `itemId` ya soportado, preservando observaciones recientes globales y excluyendo explícitamente alcance de Etapa 15.
