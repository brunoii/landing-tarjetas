## Exploration: super-inventory-stage13-observation-current-price-sync

### Current State

El módulo `supermarket` ya separa dos conceptos válidos pero todavía desconectados: el precio actual/de referencia del `SuperItem` y las observaciones históricas append-only. Desde Etapa 10, `POST /api/super/items/{id}/price-observations` crea una observación manual explícita sin mutar el producto; desde Etapas 11 y 12, tanto la observación como el precio actual del producto pueden reutilizar `SuperPriceSource` con la misma regla cerrada `{id only}`, `{label only}`, `{neither}`.

La UI ya revela la fricción restante. `prefillSuperPriceObservationForm(...)` copia precio, fuente y fecha actuales del producto hacia el formulario de observación, pero `createPriceObservation(...)` no actualiza `SuperItem`. Si el usuario quiere dejar el precio actual alineado con la observación recién tomada, hoy debe hacer dos acciones manuales separadas: editar el producto y luego registrar la observación, o viceversa. La asimetría de fuentes quedó resuelta en Etapa 12; la asimetría de flujo entre “precio vigente” y “registro histórico explícito” sigue abierta.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — deberá permitir una acción explícita que cree observación y MAY sincronizar el precio actual del producto sin volver implícito el guardado genérico.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationRequest.java` — candidato para un flag explícito tipo `updateCurrentReferencePrice` nullable/optional.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — deberá crear la observación append-only y, opcionalmente, sincronizar en la misma transacción `commercialPresentationPricePesos`, fuente y fecha actuales del `SuperItem`.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` / `SuperItemPriceObservationResponse.java` — podrían necesitar metadatos mínimos si la UI debe rehidratar el resultado sincronizado sin ambigüedad.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemPriceObservationController.java` — mismo endpoint explícito o un endpoint dedicado para “registrar observación y aplicar como precio actual”.
- `src/main/resources/static/index.html` — checkbox o CTA explícita en el formulario de observación; sin abrir edición/borrado ni gestión nueva de fuentes.
- `src/main/resources/static/js/supermarket.js` — payload, validación, refresh y feedback para sincronización explícita del precio actual después de registrar la observación.
- `src/main/resources/static/js/api.js` — contrato actualizado para el request explícito; sin tocar barcode/OCR/ticket/photo.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — cobertura para observación sola, observación + sync actual, no colateralidad, compatibilidad legacy y atomicidad.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — allow-list de la nueva semántica explícita y guardas para mantener cerrado Stage 15, administración de fuentes, comparación y automatización implícita.

### Approaches

1. **Observación explícita con sync opcional del precio actual** — Mantener `POST /price-observations` como acción manual explícita, pero permitir un flag opcional para que la misma acción también actualice el precio actual/de referencia del `SuperItem` en la misma transacción.
   - Pros: resuelve la fricción real con el MENOR cambio conceptual; preserva append-only; no convierte `POST/PUT /api/super/items` en historial implícito; reutiliza Stage 11/12 para fuente reusable o libre.
   - Cons: agrega una side effect opcional al flujo de observación y exige tests claros para que siga siendo una acción explícita, no automática.
   - Effort: Medium.

2. **Promover una observación existente a precio actual después de creada** — Agregar una acción separada sobre cada fila del historial para copiar sus datos al `SuperItem` actual.
   - Pros: deja el alta histórica totalmente pura y hace visible el momento de promoción.
   - Cons: suma más UI, más estados y una superficie adicional de acciones por fila; para esta etapa chica es más grande que el problema que resuelve.
   - Effort: Medium/High.

3. **Crear historial automáticamente al guardar el producto** — Cada cambio de precio/fuente/fecha actual generaría una observación append-only.
   - Pros: mantiene sincronía perfecta entre precio actual e historial.
   - Cons: contradice la decisión de Etapa 10 de requerir acción explícita; genera duplicados y efectos colaterales poco transparentes; ensucia el contrato del guardado genérico.
   - Effort: High.

### Recommendation

La siguiente Etapa 13 más coherente es **Approach 1**: extender el flujo explícito de observación para que, de forma opcional y visible, también pueda sincronizar el precio actual/de referencia del producto.

Eso mantiene la base conceptual CORRECTA. El historial sigue naciendo solo desde una acción manual dedicada, pero el usuario deja de hacer doble trabajo cuando justamente quiere que “lo observado recién” pase a ser el precio vigente. La transacción debería crear la observación append-only primero y actualizar el `SuperItem` con los mismos `pricePesos`, `priceSourceId|sourceLabel` y `observedDate` solo si el flag explícito está activo. Si el flag no está activo, el comportamiento Stage 10-12 debe permanecer idéntico.

No recomiendo tocar el guardado genérico del producto ni abrir acciones por fila todavía. Esta slice es más chica, sigue dentro del dominio de precios ya publicado en Stages 7-12, y permanece claramente antes de cualquier trabajo de Stage 15 o de barcode/OCR/ticket/photo.

### Risks

- Si la sync opcional no queda claramente explícita en API/UI, se puede degradar la frontera de Etapa 10 y parecer “historial automático”.
- Hay que mantener atomicidad: si falla la actualización del `SuperItem`, la observación no debe persistirse parcialmente si el usuario pidió sync conjunta.
- El contrato debe seguir aceptando fuentes reutilizables, libres o ninguna fuente, sin reabrir administración de fuentes ni semántica de tienda/comercio.
- La UI puede crecer de más si intenta sumar filtros por fila, edición de observaciones o promociones múltiples; Etapa 13 debe quedarse en un solo formulario explícito.
- Los guards estáticos deben seguir bloqueando comparación, gráficos, múltiples precios/presentaciones, OCR, barcode, ticket, foto y scraping.

### Ready for Proposal

Sí. El orquestador puede continuar con propuesta para una Etapa 13 acotada a **registrar una observación manual de precio y, opcionalmente, sincronizar ese mismo dato como precio actual del producto en la misma acción explícita**, preservando append-only, compatibilidad legacy, fuentes reutilizables/libres existentes y el cierre de alcance previo a Stage 15.
