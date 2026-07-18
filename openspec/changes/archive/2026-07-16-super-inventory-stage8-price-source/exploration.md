## Exploration: super-inventory-stage8-price-source

### Current State

`main` está en la rama `main` y el cambio activo no tenía artefactos OpenSpec previos. La especificación vigente de `super-inventory` ya permite una única presentación comercial default opcional y un único precio actual/de referencia nullable en pesos asociado a esa presentación. También mantiene explícitamente fuera de contrato tiendas/comercios como entidad, historial de precios, múltiples presentaciones, OCR, lookup externo, automatización de compra/consumo, totales de lista sugerida y sugerencias persistidas.

El modelo actual concentra la evolución en `SuperItem`: `commercialPresentationLabel`, `commercialPresentationQuantity` y `commercialPresentationPricePesos` viven en la misma entidad, viajan por `SuperItemRequest`/`SuperItemResponse`, se validan en `SupermarketService.applyCommercialPresentation`, y la UI estática los edita/renderiza desde el formulario/listado existente. Los tests de backend ya cubren payload legacy sin precio, precio válido/inválido y no mutación de `checked`, `currentStock`, movimientos, sugerencias ni barcodes. Los contratos estáticos permiten solo tokens exactos del precio de referencia y siguen bloqueando términos como `store`, `shop`, `shops`, `prices`, `presentations`, lookup externo y automatizaciones.

Nota de entorno SDD: existe `openspec/specs/super-inventory/spec.md`, pero no se encontró `openspec/config.yaml` en el workspace actual.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — requiere delta para permitir solo una fuente/etiqueta manual opcional asociada al precio actual/de referencia y reforzar que no crea tiendas, historial ni múltiples precios.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — punto más seguro para agregar un campo nullable de texto libre en el `SuperItem` existente, sin tabla nueva.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` / `SuperItemResponse.java` — contratos compatibles para aceptar/exponer la fuente opcional junto al precio actual.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` — conviene centralizar un límite explícito para la fuente, por ejemplo `ITEM_PRICE_SOURCE_LABEL_MAX_LENGTH`.
- `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` — agregar etiqueta de validación para el nuevo campo.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — trim, validación de dependencia con `commercialPresentationPricePesos`, limpieza al borrar presentación/precio y preservación de inventario/listas/barcodes.
- `src/main/resources/static/index.html` — input manual opcional para la fuente del precio; evitar copy que sugiera entidad de tienda o automatización.
- `src/main/resources/static/js/supermarket.js` — payload, validación, edición/reset y render informativo de la fuente; no incluirla en lista manual ni sugerida.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — compatibilidad, persistencia, validaciones y no mutación colateral.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — abrir solo tokens exactos de fuente manual del precio y mantener cerrados stores/shops/history/multiple prices/totals/automation.

### Approaches

1. **Fuente textual nullable en `SuperItem`** — Agregar `commercialPresentationPriceSourceLabel` como texto libre nullable asociado al precio actual/de referencia existente.
   - Pros: slice mínima; manual-first; retrocompatible; no crea entidad de tienda; encaja con el patrón de Stage 6/7; fácil de validar y testear.
   - Cons: no normaliza comercios; puede haber variantes de escritura; no conserva historial.
   - Effort: Medium

2. **Campo genérico `priceSource` sin sufijo `Label`** — Agregar un string más corto para la fuente del precio.
   - Pros: nombre más breve en API/UI.
   - Cons: menos explícito; puede confundirse con proveedor técnico, origen automático o entidad futura; menor alineación con `commercialPresentationPricePesos`.
   - Effort: Medium

3. **Entidad `Store`/`Shop` o catálogo de fuentes** — Crear tabla de comercios y referenciarla desde el precio.
   - Pros: normalización y base para comparación futura.
   - Cons: fuera de alcance; abre CRUD, identidad, migraciones y reglas de comparación; contradice el límite manual-first de la etapa.
   - Effort: High

4. **Historial de precios con fuente por observación** — Crear observaciones de precio con fecha/fuente.
   - Pros: modelo más honesto para precios cambiantes.
   - Cons: introduce múltiples precios/historial y supera la siguiente slice segura.
   - Effort: High

### Recommendation

Avanzar con **Approach 1: fuente textual nullable en `SuperItem`**. Nombre recomendado de contrato Java/JSON: `commercialPresentationPriceSourceLabel`; columna sugerida: `commercial_presentation_price_source_label`; límite sugerido: 120 caracteres para reutilizar una política similar a la presentación comercial sin aceptar textos largos.

Reglas recomendadas: la fuente es opcional y manual; si existe, se trimea y requiere `commercialPresentationPricePesos` presente; un precio puede existir sin fuente; borrar la presentación o el precio debe dejar la fuente en `null`; payloads legacy sin fuente/precio siguen válidos. La UI debería mostrarla como texto secundario junto a “Precio ref.” o en una columna/celda mínima, evitando nombres como tienda/comercio persistido, historial o comparación.

Split recomendado por `force-chained` / `stacked-to-main` y presupuesto 800 líneas:
- PR 1 backend/API/tests: entidad, request/response, service validation, labels y `SupermarketControllerTests`.
- PR 2 UI/static/tests: formulario/render/helper/guards/cache tokens y Node contract.
- PR 3 OpenSpec archive/spec sync: delta, verificación y archivo si las fases siguientes lo confirman.

### Risks

- Usar palabras como `store`/`shop` en código o UI rompería los guards actuales o empujaría a una entidad de comercio; mitigación: usar `sourceLabel`/`Fuente` como texto libre manual.
- Si se permite fuente sin precio, queda un dato comercial huérfano; mitigación: rechazar fuente cuando no hay `commercialPresentationPricePesos` o limpiarla al borrar el precio.
- Agregar una columna nueva a la tabla puede aumentar diff y carga visual; mitigación: renderizar la fuente como subtexto del precio antes de crear más columnas.
- Los contratos estáticos tienen allowlists quirúrgicas para precio; hay que actualizar reemplazos/guards exactos sin abrir `prices`, historial, tiendas, múltiples presentaciones ni totales.
- `openspec/config.yaml` no está presente; las siguientes fases deben apoyarse en `openspec/specs/` y las convenciones compartidas hasta restaurarlo.

### Ready for Proposal

Yes — listo para propuesta. El mensaje al usuario/orquestador: Stage 8 puede agregar una etiqueta manual opcional de fuente para el precio actual de la presentación default, pero debe seguir siendo un string nullable en `SuperItem`, no una tienda, historial, múltiples precios ni automatización.
