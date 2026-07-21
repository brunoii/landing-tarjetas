## Exploration: super-inventory-stage12-reference-price-source-reuse

### Current State

El módulo `supermarket` ya tiene dos caminos distintos para la fuente del precio. En `SuperItem`, el precio actual/de referencia sigue usando `commercialPresentationPriceSourceLabel` como texto libre nullable. En cambio, desde Etapa 11 las observaciones manuales ya pueden reutilizar `SuperPriceSource` mediante `priceSourceId` nullable y conservar `sourceLabel` como snapshot. La especificación viva también refleja esa asimetría: el catálogo reutilizable existe solo para observaciones append-only, mientras el precio de referencia del producto permanece libre.

La UI confirma esa separación. El formulario de observaciones ya carga `GET /api/super/price-sources`, permite seleccionar una fuente reutilizable y hasta crearla inline. El formulario principal de producto todavía expone solo un input textual para `commercialPresentationPriceSourceLabel`. Eso deja duplicación manual y deriva semántica: una misma fuente puede quedar normalizada en observaciones pero escrita a mano y con variantes en el precio actual del producto.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — deberá permitir que el precio actual/de referencia del producto use `priceSourceId` activo o `commercialPresentationPriceSourceLabel` libre, nunca ambos.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — necesita una relación nullable a `SuperPriceSource` para el precio actual, preservando `commercialPresentationPriceSourceLabel` como snapshot/fallback textual.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` / `SuperItemResponse.java` — deberán aceptar/exponer `commercialPresentationPriceSourceId` nullable sin romper payloads legacy.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — deberá resolver exclusividad `commercialPresentationPriceSourceId` XOR `commercialPresentationPriceSourceLabel`, copiar el nombre cuando se use catálogo y limpiar relación/label al limpiar precio o presentación.
- `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` — conviene agregar etiqueta legible para `commercialPresentationPriceSourceId`.
- `src/main/resources/static/js/api.js` — no necesita endpoints nuevos, pero sí contratos actualizados para `/api/super/items`.
- `src/main/resources/static/index.html` — el formulario principal de producto deberá ofrecer selector reutilizable y mantener fallback de texto libre sin duplicar administración completa.
- `src/main/resources/static/js/supermarket.js` — payload, validaciones, precarga, edición y render del precio actual deberán soportar id o texto libre, reutilizando el catálogo ya cargado.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cobertura para create/update/list con fuente reutilizable, texto libre, exclusividad, legacy y no mutaciones colaterales.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — allow-list precisa para `commercialPresentationPriceSourceId` y guards para no abrir tiendas/comercios ni administración amplia.

### Approaches

1. **Reutilizar el catálogo existente también para el precio actual del producto** — Extender `SuperItem` con `commercialPresentationPriceSourceId` nullable y conservar `commercialPresentationPriceSourceLabel` como snapshot/fallback.
   - Pros: reutiliza Stage 11 sin crear otro dominio; elimina duplicación manual; mantiene compatibilidad legacy; alinea precio actual y observaciones con la misma semántica mínima.
   - Cons: agrega una FK nullable nueva sobre `SuperItem` y exige cuidado para mantener exclusividad entre selector y texto libre.
   - Effort: Medium.

2. **Mantener texto libre y solo sugerir fuentes existentes en UI** — No tocar modelo ni contrato backend; apenas ofrecer autocompletado o copia rápida desde el catálogo.
   - Pros: diff menor y casi sin cambios de backend.
   - Cons: NO resuelve el problema conceptual; la fuente del precio actual seguiría sin normalizarse realmente y las variantes de escritura seguirían entrando.
   - Effort: Low/Medium.

3. **Abrir administración completa de fuentes para todo el módulo** — Renombrar, desactivar, quizá migrar/backfillear textos existentes y usar catálogo en precio actual y observaciones.
   - Pros: deja una base más completa para etapas futuras.
   - Cons: se va de la slice mínima, adelanta decisiones de lifecycle/admin y acerca el diseño a Stage 12+ grande en vez de al SIGUIENTE paso seguro.
   - Effort: High.

### Recommendation

La opción más coherente para Etapa 12 es **Approach 1**: reutilizar el catálogo ya existente de `SuperPriceSource` también para el precio actual/de referencia del `SuperItem`, sin tocar todavía administración avanzada de fuentes ni semántica de tienda/comercio.

La forma segura es espejar el patrón de Etapa 11: `commercialPresentationPriceSourceId` nullable + `commercialPresentationPriceSourceLabel` snapshot/fallback. Si llega `commercialPresentationPriceSourceId`, el backend debe cargar una fuente activa y copiar `name` al label textual. Si llega texto libre, el id queda `null`. Si llegan ambos, se rechaza. Si se limpia precio o presentación, se limpian ambos. Así se preserva compatibilidad con payloads legacy y se evita backfill automático sobre productos existentes.

Esta slice es la MENOR continuación útil porque no crea endpoints nuevos de catálogo, no abre CRUD/admin, no toca barcode/OCR/ticket, no entra en tiendas/comercios y aprovecha infraestructura Stage 11 ya publicada. Solo corrige la asimetría más evidente que hoy quedó entre “precio actual del producto” y “observación histórica”.

### Risks

- Sin backfill, los productos legacy seguirán con label libre y `commercialPresentationPriceSourceId=null`; eso es aceptable, pero debe quedar explícito en spec/tests.
- La UI debe dejar clarísimo que para el precio actual se usa **selector o texto libre**, no ambos; si no, reaparece incoherencia de payload.
- Reusar el formulario global de creación de fuentes evita más endpoints, pero también puede generar UX dispersa si el producto intenta crear fuentes desde demasiados lugares; conviene reutilizar un único punto de alta inline.
- Hay que mantener los guards estáticos cerrados para `store`, `shop`, `commerce`, comparación, múltiples precios/presentaciones, OCR y scraping.
- El cambio probablemente siga requiriendo cadena chica (`backend/API/tests` → `UI/static/tests` → `OpenSpec`) para respetar el presupuesto de revisión.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para una Etapa 12 limitada a reutilizar `SuperPriceSource` en el precio actual/de referencia del `SuperItem`, preservando compatibilidad legacy y dejando fuera de alcance administración completa de fuentes, tiendas/comercios, comparación, barcode/OCR/ticket y cualquier trabajo de Etapa 15.
