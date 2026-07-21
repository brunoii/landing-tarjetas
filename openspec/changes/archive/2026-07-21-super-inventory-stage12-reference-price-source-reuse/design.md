# Design: Super Inventory Stage 12 - reference price source reuse

## Technical Approach

Extender `SuperItem` para que el precio actual/de referencia reutilice el catálogo Stage 11 de `SuperPriceSource` sin romper productos legacy. La implementación espeja el patrón ya usado en `SuperItemPriceObservation`: FK nullable para la fuente reusable más `commercialPresentationPriceSourceLabel` como snapshot/fallback. El backend cerrará el contrato a tres estados válidos de fuente — `{id only}`, `{label only}`, `{neither}` — rechazará `{id + label}`, copiará el nombre activo cuando llegue un id y limpiará ambos campos al limpiar precio o presentación.

## Architecture Decisions

| Decisión | Opción elegida | Alternativas consideradas | Rationale |
|---|---|---|---|
| Modelo persistido | Agregar `@ManyToOne commercialPresentationPriceSource` nullable en `SuperItem` y conservar `commercialPresentationPriceSourceLabel`. | Solo texto libre; derivar label solo por join. | Mantiene compatibilidad legacy, evita backfill y conserva snapshot estable si la fuente reusable cambia después. |
| Validación | Resolver en `SupermarketService` la regla cerrada `{id only}`, `{label only}`, `{neither}` válida y `{id + label}` inválida. | Bean Validation puro o aceptar ambos y priorizar uno. | La regla depende de repositorio y de `active=true`; priorizar silenciosamente sería ambiguo. |
| UX | Reusar el catálogo y el alta inline Stage 11 en el formulario de producto, con selector + texto libre mutuamente excluyentes y ambos opcionales. | Nueva pantalla/admin de fuentes o autocompletar libre. | Cumple la slice mínima, evita nueva superficie y mantiene el cambio divisible. |
| Respuesta API | Exponer `commercialPresentationPriceSourceId` nullable además del label. | Exponer solo label. | La UI necesita rehidratar edición sin adivinar si el origen era reusable o legacy libre. |

## Data Flow

Editar/crear producto:

    Product form ─→ payload(priceSourceId | sourceLabel)
         └─→ SuperItemController ─→ SupermarketService
                ├─ valida presentación/precio/fecha/regla cerrada de fuente
                ├─ si priceSourceId: carga fuente activa y copia name al label
                ├─ si sourceLabel: deja FK null y guarda texto recortado
                ├─ si no llega fuente: deja FK y label en null
                └─ si se limpia precio/presentación: pone FK, label y fecha en null

Crear fuente inline:

    Product form helper ──POST /api/super/price-sources──→ flujo Stage 11 existente
         └─ recarga catálogo activo y selecciona la nueva fuente

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Agregar relación nullable a `SuperPriceSource` para el precio del producto. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` | Modify | Aceptar `Long commercialPresentationPriceSourceId` nullable sin romper payloads legacy. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java` | Modify | Exponer `commercialPresentationPriceSourceId` nullable para edición/render. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRepository.java` | Modify | Mantener listados listos para mapear la nueva relación dentro de transacción; agregar `join fetch` si la lectura lo necesita. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` | Modify | Resolver fuente reusable/libre/opcional, validar la regla cerrada, copiar snapshot y limpiar campos asociados. |
| `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java` | Modify | Registrar etiqueta legible para `commercialPresentationPriceSourceId`. |
| `src/main/resources/static/index.html` | Modify | Reemplazar el input único por selector reusable + fallback libre + ayuda de exclusividad opcional. |
| `src/main/resources/static/js/supermarket.js` | Modify | Payload, validación, edición, reset, render y reuse del refresh/create Stage 11. |
| `src/main/resources/static/js/api.js` | Modify | Sin endpoints nuevos; solo contrato actualizado de `/api/super/items`. |
| `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` | Modify | Cobertura reusable source, free text, no source, legacy null id, invalid dual-source, inactive id y limpieza sin mutar observaciones. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modify | Tokens permitidos para `commercialPresentationPriceSourceId`, exclusividad opcional y guard rails de alcance. |

## Interfaces / Contracts

```json
{
  "commercialPresentationPricePesos": 1250.50,
  "commercialPresentationPriceSourceId": 12,
  "commercialPresentationPriceSourceLabel": null,
  "commercialPresentationPriceObservedDate": "2026-07-21"
}
```

- Request contract: aceptar solo `{id only}`, `{label only}` o `{neither}` entre `commercialPresentationPriceSourceId` y `commercialPresentationPriceSourceLabel`; rechazar `{id + label}`.
- Persistence contract: con id válido, guardar la relación y copiar `SuperPriceSource.name` al label; con texto libre, guardar `commercialPresentationPriceSourceId=null`.
- Optional-source contract: con precio válido y sin fuente, persistir `commercialPresentationPriceSourceId=null` y `commercialPresentationPriceSourceLabel=null`.
- Legacy contract: filas existentes con label libre e id null siguen siendo válidas sin migración ni backfill.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Backend/API | Create/update/list con reusable id, texto libre, legacy null, ambos campos, id inexistente/inactivo, limpieza por borrar precio/presentación, no mutar observaciones. | Extender `SupermarketControllerTests` con H2 `create-drop` y repositorios reales. |
| Static UI | Selector del producto, exclusividad visual opcional, payload correcto, edición de reusable/legacy/sin fuente, refresh tras alta inline. | Extender `StaticUiContractTests` y el allow-list estático existente. |
| Scope guards | Mantener cerrado admin/store/commerce/comparison/OCR/barcode/ticket/photo/Stage 15. | Reusar scans de tokens ya presentes en pruebas estáticas. |

## Migration / Rollout

No migration required. La app usa `spring.jpa.hibernate.ddl-auto=update` y tests `create-drop`, así que Stage 12 solo agrega una columna FK nullable compatible con datos existentes. Entrega recomendada en cadena: PR1 backend/API/tests, PR2 UI/static/tests, PR3 OpenSpec sync.

## Open Questions

- None.
