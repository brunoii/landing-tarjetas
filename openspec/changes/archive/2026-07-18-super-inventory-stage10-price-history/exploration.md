## Exploration: super-inventory-stage10-price-history

### Current State

El módulo `supermarket` usa arquitectura package-by-feature con controladores REST, un `SupermarketService` transaccional y entidades JPA bajo `com.gentleia.landingtarjetas.supermarket`. `SuperItem` concentra hoy categoría, `checked`, configuración de inventario, `currentStock`, cantidad rápida, una única presentación comercial default nullable, un único precio actual/de referencia nullable en pesos, una fuente manual opcional y una fecha observada manual date-only opcional. Los movimientos de stock ya usan una entidad hija append-only (`SuperItemStockMovement`) con repositorio, endpoint de consulta reciente filtrable y UI de historial, lo que sirve como patrón para una historia de precios sin tocar stock.

La especificación viva `openspec/specs/super-inventory/spec.md` todavía limita Etapa 9 a precio/fuente/fecha actual y prohíbe historial de precios, tiendas/comercios, múltiples precios/presentaciones, OCR, lookup externo, automatización, sugerencias persistidas, total sugerido, mezcla con lista manual y Producto Base/catálogo. `openspec/config.yaml` no existe en este workspace; aplica el estilo OpenSpec existente en español. La rama `main` está limpia y sincronizada con `origin/main` según `git status --short --branch`.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — deberá modificarse para permitir observaciones manuales append-only de precio en Etapa 10 y retirar solo la prohibición específica de historial de precios dentro de este límite.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservation.java` — nueva entidad pequeña asociada a `SuperItem`, con precio observado, fuente manual opcional, fecha observada opcional/date-only, snapshot de presentación default y `createdAt`.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRepository.java` — consultas recientes globales y filtradas por producto, siguiendo el patrón de `SuperItemStockMovementRepository`.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` / `SuperItemPriceObservationResponse.java` — contrato plano para crear/listar observaciones sin aceptar tienda, presentación alternativa ni mutaciones de inventario.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationController.java` o `SuperItemController.java` — endpoints recomendados: `POST /api/super/items/{id}/price-observations` y `GET /api/super/price-observations?itemId=&limit=50`.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — validar item activo, presentación default existente, precio positivo, fuente recortada, fecha no futura, append-only explícito, listado reciente y no mutaciones colaterales.
- `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` — etiquetas legibles para campos nuevos como `pricePesos`, `sourceLabel` y `observedDate` si se usan nombres de request específicos.
- `src/main/resources/static/js/api.js` — agregar llamadas `superPriceObservations(filters)` y `createSuperPriceObservation(itemId, payload)`.
- `src/main/resources/static/index.html` — agregar una sección/formulario manual mínimo para registrar observación y una tabla/lista de historial de precios, separada del formulario genérico de producto y del historial de movimientos.
- `src/main/resources/static/js/supermarket.js` — payload/validación/render de observaciones, carga reciente filtrable por producto, acción explícita desde una fila o selector, y cache-busting.
- `src/main/resources/static/styles.css` — estilos mínimos para la nueva sección si no alcanza con clases existentes.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cobertura API de creación/listado, validaciones, no auto-creación desde `POST/PUT /api/super/items` y no mutaciones de `checked`, stock, movimientos, barcodes, lista manual ni sugerida.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` — contratos estáticos/UI para endpoints, tokens permitidos de observaciones de precio, render, submit explícito, guards anti OCR/store/shop/múltiples presentaciones/automatización y preservación del historial de movimientos existente.

### Approaches

1. **Observaciones append-only explícitas en tabla hija** — Crear una entidad `SuperItemPriceObservation` y registrar filas solo cuando el usuario ejecuta una acción manual dedicada.
   - Pros: respeta manual-first, evita efectos colaterales del guardado genérico del producto, permite auditoría mínima, sigue el patrón de movimientos, preserva precio/fuente/fecha actuales y mantiene fuera tiendas, catálogos y múltiples presentaciones.
   - Cons: el usuario debe hacer una acción separada; si también quiere cambiar el precio actual deberá editar el producto en otro paso o en una etapa posterior.
   - Effort: Medium.

2. **Crear historial automáticamente al guardar precio actual** — Cada `POST/PUT /api/super/items` con cambios de precio/fuente/fecha crea una fila histórica.
   - Pros: captura cambios sin una pantalla adicional y mantiene el historial alineado con el precio actual.
   - Cons: convierte el endpoint genérico en productor de historial oculto, genera duplicados por ediciones/correcciones, exige reglas de deduplicación, sorprende a tests y usuarios, y dificulta preservar la semántica “solo por acción explícita”.
   - Effort: Medium/High.

3. **Normalizar comercios/fuentes desde ahora** — Crear fuente/comercio como entidad asociada a cada observación.
   - Pros: prepara comparación por comercio y reportes posteriores.
   - Cons: adelanta Stage 11, abre identidad de comercio antes de validar UX/datos reales y aumenta mucho el alcance.
   - Effort: High.

### Recommendation

Avanzar con **Approach 1** como slice segura de Stage 10: historial manual append-only, product-scoped, asociado al `SuperItem` existente y a su presentación comercial default actual, sin crear entidades de tienda/comercio ni tocar `checked`, `currentStock`, movimientos, barcodes, lista manual o lista sugerida.

Modelo recomendado: `SuperItemPriceObservation` con `item` obligatorio, `pricePesos` obligatorio y positivo (`precision=12, scale=2`), `sourceLabel` nullable y recortado hasta 120 caracteres, `observedDate` nullable `LocalDate` no futura, `commercialPresentationLabelSnapshot` obligatorio copiado desde el item, `commercialPresentationQuantitySnapshot` nullable y `createdAt` automático. El snapshot de presentación es más seguro que leer siempre la presentación actual porque evita que un cambio futuro de “Pack x 6” a “Botella” reinterprete observaciones viejas; no crea múltiples presentaciones porque no es una entidad seleccionable ni editable.

API recomendada: `POST /api/super/items/{id}/price-observations` crea una observación explícita y rechaza items sin presentación default; `GET /api/super/price-observations?itemId=&limit=50` lista recientes con límite normalizado como movimientos. El request NO debe incluir tienda, comercio, barcode, stock, presentación alternativa ni flags de compra. No crear entradas automáticas desde `createItem`/`updateItem`: el guardado de precio actual debe seguir siendo solo metadata actual/de referencia. La UI puede prellenar el formulario de observación con precio/fuente/fecha actuales para reducir fricción, pero la persistencia histórica solo ocurre al enviar el formulario dedicado.

Pruebas necesarias: API create/list/filter/limit, validación de precio/fuente/fecha, rechazo sin presentación, no mutación colateral, no auto-creación por `POST/PUT /api/super/items`, preservación de observaciones al limpiar precio/presentación actual, y static/Node tests que distingan `price-observations` de `super-movement-history`. Guardas fuera de alcance: edit/delete de observaciones, gráficos, comparación por comercio, entidad tienda/comercio, múltiples presentaciones, Producto Base, OCR, scraping, lookup externo, automatización de compra/consumo, totales sugeridos y mezcla con lista manual.

Split recomendado con `force-chained` / `stacked-to-main` y presupuesto 800 líneas: PR 1 backend/API/tests; PR 2 UI/static/tests y cache-busting; PR 3 OpenSpec archive/spec sync. Si la estimación de PR 2 excede el presupuesto, achicar Stage 10 a formulario + tabla reciente product-scoped sin panel global, sin estilos nuevos complejos y sin acciones de edición/eliminación.

### Risks

- La suite estática hoy bloquea `priceHistory`, `price-history` e “historial de precios”; Stage 10 debe cambiar esos guards con allow-list precisa para observaciones de precio sin abrir stores/shops/múltiples precios.
- Sin snapshot de presentación, el historial podría quedar semánticamente incorrecto cuando cambie la presentación default; con snapshot hay más campos, pero el dato histórico es más fiable.
- Crear historia automáticamente desde el guardado actual produciría duplicados y efectos colaterales no obvios; debe mantenerse append-only explícito.
- No hay Flyway/Liquibase; la tabla nueva dependerá de `ddl-auto=update` en runtime y `create-drop` en tests, como el resto del módulo.
- La UI puede crecer rápido si intenta resolver filtros, edición, borrado o comparación; mantener una slice mínima evita romper el presupuesto de revisión.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para Stage 10 limitada a observaciones manuales append-only de precio sobre el `SuperItem` existente, con fuente/date-only opcionales, snapshot de presentación default y endpoints/UI explícitos, sin automatismos ni normalización de comercios.
