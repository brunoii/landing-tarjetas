# Design: Fecha observada del precio de referencia

## Technical Approach

Extender el mismo eje de Etapas 7/8: un único `SuperItem` conserva presentación, precio y fuente, y suma una fecha manual nullable asociada solo al precio de referencia. La API expondrá `commercialPresentationPriceObservedDate` como date-only JSON (`YYYY-MM-DD`) mediante `LocalDate`; la UI usará `input type="date"` y mantendrá la fecha tipeada en el payload hasta que `validateSuperItemPayload` la rechace si queda huérfana.

## Architecture Decisions

| Decisión | Elección | Alternativas consideradas | Rationale |
|---|---|---|---|
| Modelo | Agregar `SuperItem.commercialPresentationPriceObservedDate` con `@Column(name="commercial_presentation_price_observed_date") LocalDate` nullable | Nueva entidad/tabla de historial | La spec limita Etapa 9 a un único dato manual; una tabla introduciría historial y múltiples precios. |
| Contrato API | Agregar el campo al record `SuperItemRequest` y `SuperItemResponse` | DTO separado para metadata de precio | El contrato actual ya es plano y retrocompatible; payloads legacy omiten el campo y reciben `null`. |
| Validación | Centralizar en `SupermarketService.applyCommercialPresentation` y helpers | Solo Bean Validation | Las dependencias entre presentación, precio, fuente y fecha requieren validar después de normalizar strings/precio. |
| UI | `id="super-item-presentation-price-observed-date"`, `name="commercialPresentationPriceObservedDate"`, `type="date"` | Datetime o texto libre | Date-only evita timestamp/zona horaria y coincide con `LocalDate`. |
| Render | Mostrar `Observado: YYYY-MM-DD` como `<small>` junto a precio/fuente | Formato local ambiguo | Mantiene la semántica date-only del API y evita cambios de zona; estilo consistente con metadata secundaria existente. |

## Data Flow

```
Formulario type=date ──→ superItemPayloadFromValues ──→ validateSuperItemPayload
        │                            │                          │
        └──── valor inválido/orfandad se preserva hasta error ──┘
POST/PUT /api/super/items ──→ SuperItemRequest(LocalDate) ──→ SupermarketService ──→ SuperItem
GET /api/super/items ──→ SuperItemResponse(LocalDate) ──→ render precio/fuente/fecha
```

Reglas: fecha requiere presentación y precio; precio puede existir sin fecha; fecha futura se rechaza; borrar precio o presentación limpia fuente y fecha. Jackson debe aceptar/emitar JSON date-only para `LocalDate`; formato inválido debe responder 400 con etiqueta legible desde `ApiExceptionHandler`.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Campo `LocalDate`, columna nullable y getter/setter. |
| `SuperItemRequest.java` / `SuperItemResponse.java` | Modify | Incluir `commercialPresentationPriceObservedDate`. |
| `SupermarketService.java` | Modify | Normalizar/validar fecha, rechazar futura y limpiar al borrar precio/presentación. |
| `ApiExceptionHandler.java` | Modify | Label “Fecha observada del precio” para errores de binding/formato. |
| `src/main/resources/static/index.html` | Modify | Input date opcional junto a fuente. |
| `src/main/resources/static/js/supermarket.js` | Modify | Payload, validación cliente, edición/reset y render secundario. |
| `src/main/resources/static/js/app.js` / `index.html` | Modify | Cache bust solo para assets afectados con token Stage 9 UI. |
| `src/test/java/.../SupermarketControllerTests.java` | Modify | Persistencia, null legacy, inválidos, limpieza y no mutaciones. |
| `src/test/java/.../StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs` | Modify | Contratos estáticos/UI. |
| `openspec/specs/super-inventory/spec.md` | Modify en PR3 | Sync al archivar. |

## Interfaces / Contracts

```json
{
  "commercialPresentationPriceObservedDate": "2026-07-18"
}
```

No se aceptan `ObservedAt`, datetime, timestamp, tienda, historial de precios, múltiples precios, totales ni automatización como contrato soportado; el historial operativo de stock/movimientos existente se preserva.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Backend/API | `LocalDate` persiste/expone, legacy `null`, fecha futura/huérfana no muta, limpieza por precio/presentación | `SupermarketControllerTests` con MockMvc y asserts de repositorio. |
| UI unit/static | Payload conserva fecha huérfana hasta validación, fecha futura cliente, edición/reset, render `Observado: YYYY-MM-DD` | `static-ui-contract-tests.mjs`. |
| Static contract | Solo tokens precisos `commercialPresentationPriceObservedDate`, id/name recomendados; bloquear `datetime`, `timestamp`, `store/shop`, historial de precios, múltiples precios, totales y automatización, preservando historial operativo existente | `StaticUiContractTests` con allowlist de tokens date-only. |

## Migration / Rollout

No migration required: campo nullable en tabla existente; H2 `ddl-auto=update` agregará la columna. Entrega forzada stacked-to-main: PR1 backend/API/tests, PR2 UI/static/tests, PR3 OpenSpec archive/spec sync.

## Open Questions

None.
