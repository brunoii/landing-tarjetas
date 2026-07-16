## Exploration: super-inventory-stage5-suggested-list

### Current State

`main` está limpio e incluye la Etapa 4 archivada. La especificación vigente de `super-inventory` conserva `SuperItem` como base progresiva del Producto Base, con categorías, `checked` manual, `unit`, `habitualObjective`, `currentStock` nullable, `quickQuantity`, movimientos inmutables e alias locales de barcode.

La lista generada actual sigue siendo manual: `generatedSuperListText(items)` usa solo productos con `checked=true` y puede mostrar `quickQuantity + unit`, pero no deriva compras desde stock u objetivo. Backend y UI ya tienen los datos mínimos para una lista sugerida: producto configurado, stock conocido y objetivo habitual. La Etapa 4 dejó explícitamente fuera la lista sugerida automática, y los contratos estáticos todavía bloquean términos `suggested`/`suggested-list` en `supermarket.js`.

No se encontró `openspec/config.yaml`; la fuente OpenSpec real para esta exploración fue `openspec/specs/super-inventory/spec.md` más archivos archivados de etapas previas.

### Affected Areas
- `openspec/specs/super-inventory/spec.md` — necesita un delta que levante parcialmente la prohibición de lista sugerida automática sin abrir precios, presentaciones, tiendas, OCR ni compras automáticas.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — lugar natural para calcular sugerencias desde datos persistidos, manteniendo backend como source of truth.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRepository.java` — puede reutilizar `findActiveOrderedForList()` o agregar una consulta enfocada si conviene evitar cálculo innecesario.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/` — probable DTO/controlador nuevo para exponer sugerencias sin mutar `SuperItem`.
- `src/main/resources/static/js/api.js` — helper para consultar la lista sugerida.
- `src/main/resources/static/js/supermarket.js` — render separado para sugerencias; debe mantener `generatedSuperListText` manual basado en `checked`.
- `src/main/resources/static/index.html` / `css/styles.css` — sección o card mínima para sugerencias, separada de “Productos marcados”.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — contratos de sugerencias, exclusión de stock desconocido/no configurado y no mutación de stock/checked/movimientos.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — actualizar bloqueos para permitir solo la semántica sugerida de Stage 5 y seguir bloqueando precios/OCR/presentaciones/automatización.

### Approaches
1. **Backend-derived read-only suggestions** — Agregar `GET /api/super/suggested-list` o equivalente que devuelva productos activos con `unit`, `habitualObjective` y `currentStock` conocido cuando `currentStock < habitualObjective`; `suggestedQuantity = habitualObjective - currentStock`.
   - Pros: Respeta backend como source of truth; no persiste estado nuevo; usa fundamentos ya existentes; separa lista sugerida de `checked`; fácil de testear.
   - Cons: Requiere endpoint/DTO/UI nuevos y ajustar guards estáticos.
   - Effort: Medium

2. **Frontend-only derived suggestions from `/api/super/items`** — Calcular sugerencias en `supermarket.js` con los items ya cargados.
   - Pros: Menos backend; entrega rápida.
   - Cons: Viola la restricción de backend como source of truth; duplica reglas de dominio; peor para tests de contrato y futuros consumidores.
   - Effort: Low

3. **Persisted suggestion/checklist model** — Crear entidad propia de sugerencias o lista sugerida confirmable.
   - Pros: Puede soportar flujo futuro de aceptación, historial o planificación de compras.
   - Cons: Demasiado grande para la próxima slice; mezcla sugerencia con intención manual y puede abrir compras automáticas antes de tiempo.
   - Effort: High

### Recommendation

Avanzar con **Backend-derived read-only suggestions** como Etapa 5. No hace falta una etapa previa más chica: Etapas 1-4 ya dejaron los campos, movimientos y barcode aislados; la próxima frontera segura es leer `currentStock` vs `habitualObjective` sin modificar stock, `checked` ni movimientos.

Regla mínima recomendada: sugerir solo productos activos, configurados (`unit` + `habitualObjective`) y con `currentStock` conocido menor al objetivo. Excluir stock desconocido y productos no configurados en esta primera slice, o mostrarlos solo como explicación secundaria si no agranda demasiado el alcance. La lista manual marcada debe permanecer independiente.

### Risks
- La especificación vigente todavía prohíbe la lista sugerida automática; el delta debe modificar ese límite con precisión.
- `currentStock=null` no debe interpretarse como cero, porque generaría compras falsas.
- Si la UI mezcla “marcados” y “sugeridos”, se rompe la intención histórica de `checked` como lista manual.
- Los contratos estáticos bloquean `suggested`; hay que abrir ese guard solo para Stage 5 y mantener cerrados precios, presentaciones, tiendas, OCR, lookup externo y automatización.
- Una lista persistida/confirmable probablemente excede el presupuesto; conviene dejar aceptación/merge con lista manual para otra etapa.

### Ready for Proposal

Yes — proponer Stage 5 como lista sugerida derivada, read-only y separada de la lista manual. El mensaje al usuario: el sistema puede empezar a sugerir compras cuando tiene objetivo y stock conocido, pero todavía no debe marcar productos, crear movimientos, registrar compras ni manejar precios/presentaciones.
