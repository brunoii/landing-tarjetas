## Exploration: super-inventory-stage11-price-sources

### Current State

El módulo `supermarket` mantiene una arquitectura package-by-feature: controladores REST pequeños, `SupermarketService` transaccional, repositorios Spring Data JPA y UI estática en módulos ES. La especificación viva de `super-inventory` ya permite, desde Etapa 10, observaciones manuales append-only de precio con `sourceLabel` libre, fecha observada date-only, snapshot de presentación y listado reciente seguro.

El modelo actual conserva dos superficies de fuente manual: `SuperItem.commercialPresentationPriceSourceLabel` para el precio actual/de referencia, y `SuperItemPriceObservation.sourceLabel` como snapshot libre en cada observación. No existe entidad de fuente, tienda o comercio; tampoco hay Flyway/Liquibase, por lo que los cambios de esquema dependen de Hibernate `ddl-auto=update` en runtime y `create-drop` en tests. `openspec/config.yaml` no existe en este workspace; aplica el estilo OpenSpec existente en español. `main` está limpio y sincronizado con `origin/main`.

Los tests actuales cubren que crear/listar observaciones no muta producto, `checked`, `currentStock`, movimientos, barcodes ni listas; que `POST/PUT /api/super/items` no crea ni muta observaciones; y que los guards estáticos bloquean tiendas/comercios, comparación, gráficos, múltiples presentaciones, OCR, lookup externo, scraping, automatización y totales sugeridos.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — deberá permitir un catálogo mínimo de fuentes de precio sin convertirlo en tienda/comercio, comparación ni automatización.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperPriceSource.java` — nueva entidad candidata para catálogo mínimo (`name`, clave normalizada, `active`, timestamps), sin semántica de tienda.
- `SuperPriceSourceRepository.java` — consultas por activo, orden por nombre y unicidad por clave normalizada.
- `SuperPriceSourceRequest.java` / `SuperPriceSourceResponse.java` — contratos planos para crear/listar fuentes manuales reutilizables.
- `SuperPriceSourceController.java` — endpoints candidatos `GET/POST /api/super/price-sources`.
- `SuperItemPriceObservation.java` — agregar relación nullable `priceSource` y conservar `sourceLabel` como snapshot obligatorio de compatibilidad cuando exista texto.
- `SuperItemPriceObservationRequest.java` / `Response.java` — aceptar `priceSourceId` opcional y devolverlo; preservar `sourceLabel` para legacy y snapshot.
- `SupermarketService.java` — resolver fuente activa, crear fuente, validar duplicados, crear observación con `priceSourceId` o `sourceLabel` libre, y mantener no-colateralidad.
- `ApiExceptionHandler.java` — agregar etiqueta para `priceSourceId` si aparece en validación.
- `src/main/resources/static/js/api.js` — agregar `superPriceSources()` y `createSuperPriceSource(payload)`; extender observaciones con `priceSourceId`.
- `src/main/resources/static/index.html` — agregar selector/alta mínima de fuente de precio junto al formulario de observación, evitando copy de tienda/comercio.
- `src/main/resources/static/js/supermarket.js` — cargar fuentes, renderizar opciones, enviar `priceSourceId` o texto libre, y refrescar fuentes/observaciones tras alta.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — pruebas API de catálogo, unicidad, relación nullable, legacy y no mutaciones colaterales.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — allow-list exacta para `price-sources`/`priceSourceId` y guards reforzados fuera de alcance.

### Approaches

1. **Entidad mínima de fuente con relación nullable y snapshot** — Crear `SuperPriceSource`; nuevas observaciones pueden referenciarla por `priceSourceId`, pero cada observación conserva `sourceLabel` como snapshot. Las observaciones legacy quedan con `priceSourceId=null`.
   - Pros: habilita reutilización real de nombres, mantiene compatibilidad, evita backfill riesgoso, preserva historia si una fuente cambia a futuro, y sigue manual-first.
   - Cons: agrega endpoints/UI/tests nuevos y requiere guards estáticos muy precisos para no abrir tiendas/comparación.
   - Effort: Medium.

2. **Dedupe solo por texto libre sin entidad** — Mantener `sourceLabel` y ofrecer en UI sugerencias derivadas de labels existentes.
   - Pros: menor cambio de backend y cero migración.
   - Cons: no normaliza de verdad, depende de escanear observaciones, mezcla datos históricos con catálogo editable y no resuelve variantes de escritura.
   - Effort: Low/Medium.

3. **Catálogo completo de comercios** — Crear entidad de comercio con edición, inactivación, backfill de observaciones antiguas y uso también en precio actual.
   - Pros: prepara comparación futura por comercio.
   - Cons: demasiado amplio para Etapa 11; adelanta Stage 12+, obliga a decidir identidad comercial, migración/backfill y UX de administración.
   - Effort: High.

### Recommendation

Avanzar con **Approach 1**, pero con una frontera estricta: catálogo mínimo de **fuentes de precio**, no de tiendas/comercios. La entidad debería llamarse en código `SuperPriceSource`, exponer solo crear/listar activos y no incluir dirección, sucursal, geolocalización, scraping, comparación, gráficos, totales ni automatización.

Mantener `sourceLabel` en `SuperItemPriceObservation` como snapshot es obligatorio. La relación `priceSource` debe ser nullable para preservar observaciones existentes y labels libres. No recomiendo backfill automático: sin mecanismo de migración versionada, agrupar labels legacy puede crear fuentes incorrectas por variantes de escritura. La estrategia segura es dejar legacy con `priceSourceId=null` y permitir que nuevas observaciones usen fuente normalizada o texto libre.

Contrato sugerido: `GET /api/super/price-sources`, `POST /api/super/price-sources { name }`, y `POST /api/super/items/{id}/price-observations { pricePesos, priceSourceId?, sourceLabel?, observedDate? }`. El request de observación debería aceptar **una** forma de fuente: `priceSourceId` o `sourceLabel` libre. Si se usa `priceSourceId`, el backend copia `SuperPriceSource.name` a `sourceLabel`; si se usa texto libre, `priceSourceId` queda `null`. El campo actual `commercialPresentationPriceSourceLabel` de `SuperItem` debería permanecer libre en esta etapa; como máximo la UI puede prellenar el texto o facilitar selección para la observación, sin mutar el producto.

### Risks

- `ddl-auto=update` agregará tabla/FK nullable sin backfill; es seguro frente a legacy, pero sigue sin validación de migración versionada.
- El término “comercio” puede empujar el diseño hacia tienda/comparación; mitigación: código/API como `price-source` y guards que sigan bloqueando `store`, `shop`, `commerce`, `comparison`, `chart`, `total`, OCR y scraping.
- La unicidad case-insensitive debe resolverse con clave normalizada (`trim().toLowerCase(Locale.ROOT)`) para no depender de collation de H2; no hacer normalización agresiva de acentos en esta etapa.
- La UI puede superar el presupuesto si agrega administración completa; mantener solo selector + alta mínima inline.
- Si en el futuro se permite renombrar fuentes, el snapshot `sourceLabel` evita que observaciones históricas cambien de significado.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para una Etapa 11 limitada a catálogo mínimo de fuentes de precio reutilizables, relación nullable desde observaciones, `sourceLabel` snapshot preservado, sin backfill automático y sin tocar `checked`, `currentStock`, movimientos, barcodes, lista manual ni lista sugerida.

Split recomendado con `force-chained` / `stacked-to-main`: PR 1 backend/API/tests para catálogo + relación nullable; PR 2 UI/static/tests para selector/alta mínima y guards; PR 3 OpenSpec archive/spec sync. Si PR 2 excede presupuesto, recortar a backend/API + lectura de fuentes y diferir alta inline de UI.
