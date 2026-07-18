# Exploration: super-inventory-stage9-price-observed-date

### Current State

La evolución comercial del módulo Supermarket está concentrada en `SuperItem`: una única presentación comercial default nullable (`commercialPresentationLabel`, `commercialPresentationQuantity`), un único precio actual/de referencia nullable (`commercialPresentationPricePesos`) y una única fuente manual nullable (`commercialPresentationPriceSourceLabel`). Esos campos viajan por `SuperItemRequest`/`SuperItemResponse`, se validan en `SupermarketService.applyCommercialPresentation`, se renderizan/editar desde `src/main/resources/static/js/supermarket.js` e `index.html`, y están protegidos por `SupermarketControllerTests`, `StaticUiContractTests` y `static-ui-contract-tests.mjs`.

La especificación viva `openspec/specs/super-inventory/spec.md` todavía limita explícitamente Etapa 8 a una sola presentación default, un solo precio de referencia en pesos y una sola fuente manual opcional; además prohíbe tienda/comercio, catálogo de fuentes, historial de precios, múltiples precios/presentaciones, OCR, lookup externo, automatización, totales de lista sugerida, sugerencias persistidas, mezcla de listas y Producto Base/catálogo paralelo. `openspec/config.yaml` no existe en este workspace, por lo que aplican las convenciones compartidas y el estilo existente de la spec en español.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — fuente viva que deberá modificarse en la etapa de spec/archive para permitir una fecha observada manual del precio sin historial.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — agregar un único campo nullable `LocalDate` en la misma entidad, sin tabla nueva.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` — aceptar el campo opcional en el contrato genérico retrocompatible.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` — exponer el campo nullable junto al precio/fuente.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — validar dependencia con presentación/precio, limpiar al borrar precio/presentación y preservar inventario/listas/barcodes.
- `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` — agregar etiqueta de validación legible para el nuevo campo y aprovechar el manejo existente de `InvalidFormatException`.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cubrir legacy null, fecha válida, fecha huérfana/inválida, limpieza y no mutaciones colaterales.
- `src/main/resources/static/index.html` — agregar un input `type="date"` manual y opcional junto al precio de referencia.
- `src/main/resources/static/js/supermarket.js` — payload, validación cliente, render secundario, edición/reset y cache-busting del módulo.
- `src/main/resources/static/js/app.js` e `index.html` — actualizar tokens de cache para la slice UI si se toca `supermarket.js`.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` — extender contratos estáticos, fixtures, payloads y allow-list de términos exactos.

### Approaches

1. **Fecha nullable del precio actual en `SuperItem`** — Agregar `commercialPresentationPriceObservedDate` como `LocalDate` nullable asociado solo a `commercialPresentationPricePesos`.
   - Pros: mantiene el modelo incremental actual, no introduce historial ni entidades nuevas, preserva payloads legacy, es fácil de testear y evita ambigüedad de timezone.
   - Cons: solo conserva la última fecha observada; sobrescribir precio/fecha no deja auditoría histórica.
   - Effort: Low/Medium.

2. **Timestamp observado (`commercialPresentationPriceObservedAt`)** — Guardar instante/fecha-hora de observación.
   - Pros: más preciso si en el futuro se registran capturas operativas.
   - Cons: contradice la intención de fecha manual, abre preguntas de timezone/hora, y en el código actual el sufijo `At` se usa para `Instant` (`createdAt`, `updatedAt`).
   - Effort: Medium.

3. **Registro histórico o entidad de observación de precio** — Crear filas separadas por observación.
   - Pros: auditabilidad real.
   - Cons: está explícitamente fuera de contrato porque introduce historial de precios y una colección de precios/observaciones.
   - Effort: High.

### Recommendation

Avanzar con **Approach 1**. Nombre recomendado de Java/JSON: `commercialPresentationPriceObservedDate`; columna sugerida: `commercial_presentation_price_observed_date`; tipo recomendado: `java.time.LocalDate`. Evitar `ObservedAt` en esta etapa porque sugiere fecha-hora y en el proyecto `createdAt`/`updatedAt` son `Instant`.

Reglas recomendadas: el campo es opcional y manual; si existe, MUST requerir `commercialPresentationPricePesos` y `commercialPresentationLabel`; el precio MAY existir sin fecha; al limpiar precio o presentación, la fecha MUST quedar `null`; payloads legacy sin presentación/precio/fuente/fecha MUST seguir válidos. Recomendación adicional segura: rechazar fechas futuras con `@PastOrPresent` o validación equivalente para mantener semántica de “observado”, usando una fecha claramente futura en tests para evitar fragilidad.

Guardas fuera de alcance: no crear tabla de historial, tienda/comercio, catálogo de fuentes, múltiples precios, múltiples presentaciones, OCR, lookup externo, automatización, totales sugeridos, sugerencias persistidas, mezcla con lista manual ni Producto Base/catálogo paralelo. La lista sugerida debe seguir sin exponer precio/fuente/fecha, y actualizar esta fecha no debe mutar `checked`, `currentStock`, movimientos, barcodes, lista manual ni sugerencias.

Split recomendado por `force-chained` / `stacked-to-main` y presupuesto 800 líneas:
- PR 1: backend/API/tests (`SuperItem`, request/response, service, handler, `SupermarketControllerTests`).
- PR 2: UI/static/tests (`index.html`, `app.js`, `supermarket.js`, contratos estáticos y fixtures), apilado sobre PR 1.
- PR 3: OpenSpec archive/spec sync solamente, luego de apply/verify.

### Risks

- Un nombre con sufijo `At` puede empujar a datetime/timezone; mitigación: usar `commercialPresentationPriceObservedDate` y `LocalDate`.
- La UI debe preservar el patrón corregido en Etapa 8: no descartar una fecha huérfana antes de validar, para que el cliente pueda rechazar intención inválida en vez de silenciarla.
- Los contratos estáticos tienen allow-list de tokens comerciales; el nuevo campo deberá agregarse de forma exacta sin abrir términos genéricos como `prices`, `history`, `store` o `shop`.
- `ddl-auto=update` agregará columna nullable en runtime; no hay Flyway/Liquibase, así que no debe requerirse migración manual para legacy.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para una slice Stage 9 limitada a `commercialPresentationPriceObservedDate` nullable, manual, asociada solo al precio de referencia existente y sin historial ni entidades nuevas.
