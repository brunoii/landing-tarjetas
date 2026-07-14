## Exploración: PRD Etapa 2 — Inventario

### Estado Actual
La Etapa 1 está integrada en `main` en `4a582fe` con el workspace local limpio. El módulo del super conserva `SuperItem` como Producto Base evolutivo y ya soporta `unit` opcional, `habitualObjective` opcional y estado derivado `configured`. Los endpoints actuales siguen siendo `/api/super/items`, `PATCH /api/super/items/{id}/checked` y `POST /api/super/items/uncheck-all`; `checked` continúa siendo solo intención manual de compra. La UI estática permite crear/editar unidad y objetivo, renderiza `Configurado`/`Pendiente` y genera la lista manual estrictamente desde productos marcados.

La especificación principal de OpenSpec todavía describe solo Etapa 1 y declara explícitamente stock/movimientos fuera de alcance. El PRD abre una tensión para Etapa 2: pide `Stock Actual`, pero el principio de producto indica que el stock debería cambiar solo mediante movimientos, mientras que Etapa 2 excluye Historial/Movimientos.

### Áreas Afectadas
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — requeriría campos de snapshot de stock y cantidad rápida, o un stock denormalizado alimentado por comandos internos con forma de movimiento.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` / `SuperItemResponse.java` — expondrían campos de Etapa 2 manteniendo compatibilidad con payloads anteriores.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — concentraría validación, reglas de mutación de stock y preservación de `checked` como intención manual/excepcional de compra.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemController.java` — probablemente necesita un endpoint enfocado para ajuste de stock, evitando ediciones arbitrarias desde el update genérico del producto.
- `src/main/resources/static/js/api.js` — agregaría helpers para cualquier endpoint enfocado de stock/cantidad.
- `src/main/resources/static/js/supermarket.js` — renderizaría stock/unidad/objetivo, incluiría cantidad rápida en payloads y generaría la lista manual con cantidad cuando exista.
- `src/main/resources/static/index.html` / `src/main/resources/static/css/styles.css` — sumarían inputs/columnas/acciones mínimas para stock actual, cantidad rápida y compra excepcional sin rediseñar el módulo.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cubriría contratos backend de defaults, validación, mutación de stock, cantidad rápida e independencia de la lista manual.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — cubrirían contratos estáticos/JS de payloads, render, generación de lista, cache-busting y límites de alcance.
- `openspec/specs/super-inventory/spec.md` — necesita un delta de Etapa 2 que modifique el fuera de alcance de Etapa 1 y agregue comportamiento explícito de inventario.

### Enfoques
1. **Movimiento interno mínimo, sin historial visible** — Agregar una escritura interna append-only con forma de movimiento para cambios de stock y actualizar un snapshot `currentStock` en `SuperItem`, sin exponer todavía historial/listado de movimientos.
   - Pros: Es lo más alineado con movement-first; una Etapa 3 podría exponer historial sin inventar datos después; evita ediciones arbitrarias de stock.
   - Contras: Más trabajo backend que una columna simple; requiere aclarar que Historial/Movimientos UI/API sigue fuera de alcance.
   - Esfuerzo: Medio

2. **Snapshot transitorio de stock** — Agregar `currentStock` nullable directamente a `SuperItem` y permitir ediciones controladas como baseline temporal hasta que exista historial de movimientos.
   - Pros: Implementación más chica; migración simple con columnas nullable; podría entrar en una división backend/UI acotada.
   - Contras: Viola parcialmente movement-first; la conversión posterior a movimientos requiere semántica especial de migración y puede perder intención auditiva.
   - Esfuerzo: Bajo/Medio

3. **Diferir stock e implementar solo compra rápida/excepcional** — Mantener stock fuera hasta que existan movimientos; implementar cantidad rápida y compra excepcional mediante el `checked` existente.
   - Pros: No viola movement-first; bajo riesgo sobre productos existentes.
   - Contras: No satisface el alcance `Stock Actual` de Etapa 2; probablemente no alcance para el PRD.
   - Esfuerzo: Bajo

### Recomendación
Usar el Enfoque 1 como slice incremental seguro: implementar stock mediante un comando backend mínimo que registre hechos internos con forma de movimiento y actualice `currentStock`, sin construir pantallas ni endpoints de consulta de historial en Etapa 2. Tratar `checked=true` como la `Compra Excepcional`/intención manual ya existente y agregar `quickQuantity` como cantidad usada cuando un producto marcado aparece en la lista generada. Reutilizar `unit` y `habitualObjective` de Etapa 1; no duplicarlos.

División recomendada bajo el presupuesto de revisión de 800 líneas:
- Slice 1: modelo/API/tests backend para `currentStock` nullable, `quickQuantity`, validación y semántica de comando enfocado de stock.
- Slice 2: UI/contratos estáticos para mostrar stock/objetivo/unidad, cantidad rápida, compra excepcional mediante el flujo `checked` existente y texto de cantidad en la lista generada.

Se recomiendan PRs encadenados si la estimación supera el presupuesto; usar la estrategia solicitada `feature-branch-chain`.

### Riesgos
- Los productos existentes tienen stock desconocido. Deberían quedar como `currentStock=null` / “stock sin cargar”, no `0`, porque cero representa un estado real de falta de stock.
- Hibernate `ddl-auto=update` sigue siendo el mecanismo de migración; columnas nullable son seguras, pero una tabla interna de movimientos aumenta el riesgo de rollout sin Flyway/Liquibase.
- Permitir stock en updates genéricos de producto debilitaría movement-first; el stock debería cambiar por un comando enfocado aunque el historial no sea visible todavía.
- `checked` no debe convertirse en stock ni en semántica de lista sugerida. Debe seguir siendo intención manual/excepcional de compra.
- El ancho de tabla UI y los tokens de cache estático requerirán actualización cuidadosa porque los contratos estáticos de Etapa 1 son estrictos.

### Listo para Propuesta
Sí — proponer Etapa 2 como “snapshot de inventario más compra excepcional rápida”, dejando explícito que las mutaciones de stock tienen forma interna de movimiento pero que la UI/API de historial de movimientos se difiere. La propuesta debería hacer `currentStock` nullable para productos existentes, reutilizar `unit`/`habitualObjective` y evitar lógica automática de lista sugerida.
