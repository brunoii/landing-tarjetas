# Design: Etapa 1 — Evolución del modelo actual

## Technical Approach

Evolucionar `SuperItem`/`super_items` como base del futuro Producto Base, sin tabla paralela. La etapa agrega configuración opcional de inventario (`unit`, `habitualObjective`) y expone `configured` como estado derivado. Las filas existentes quedan pendientes porque ambos campos nacen `null`; `checked` sigue siendo intención manual de compra y no se transforma en stock, cantidad ni movimiento.

## Architecture Decisions

| Decisión | Elección | Alternativas consideradas | Rationale |
|---|---|---|---|
| Campos de configuración | `unit: String?` (`unit`, máx. 40); `habitualObjective: BigDecimal?` (`habitual_objective`, precision 10 scale 3); `configured: boolean` solo en respuesta/derivado | Defaults como `unidad`/`1`; enum de unidades; estado persistido | Nullable evita inventar datos; `BigDecimal` permite kg/l/unidades sin stock real; derivar evita desincronización. |
| Validación | Si `habitualObjective` viene informado, MUST ser `> 0`; `unit` se trimmea a `null`; producto configurado solo si ambos existen | Permitir objetivo sin unidad; completar unidad por defecto | La configuración parcial debe seguir siendo pendiente, no falsa precisión. |
| Compatibilidad API | `SuperItemRequest` acepta campos nuevos opcionales; payload viejo sigue creando/actualizando pendiente. `SuperItemResponse` agrega `unit`, `habitualObjective`, `configured` sin quitar campos actuales | Nuevo endpoint de configuración | Extender el contrato actual reduce cambios y conserva `/api/super/items`. |
| Borrado de productos | Cambiar `DELETE /api/super/items/{id}` a baja lógica con `active=false` desde Stage 1 | Diferir hasta movimientos | Ya existe `active`; hacerlo ahora prepara FKs futuras sin romper el 204 ni el listado activo. |
| Migración | Mantener `ddl-auto=update` en Stage 1; agregar columnas nullable, sin backfill. No introducir Flyway/Liquibase en este PR | Incorporar Flyway ahora | No hay mecanismo versionado actual; columnas nullable son seguras. Flyway/Liquibase debe ser un PR separado antes de movimientos/stock reales. |
| UI | Ajuste mínimo: agregar unidad/objetivo opcionales al formulario y una columna/indicador “Configuración” en la tabla | Cambio interno sin UI | Sin UI no habría forma normal de completar configuración; el indicador evita que pendientes parezcan productos listos. |

## Data Flow

```text
UI formulario ──payload opcional──> SuperItemController
      └──────────── GET lista <──── SupermarketService ──> SuperItemRepository
                                     │
                                     └─ deriva configured = unit != null && habitualObjective != null
```

La generación manual de lista continúa leyendo solo `checked=true`.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` | Modify | Agregar `unit`, `habitualObjective`, getters/setters y helper derivado. |
| `SuperItemRequest.java` / `SuperItemResponse.java` | Modify | Extender contrato con campos opcionales y `configured`. |
| `SupermarketLimits.java` | Modify | Agregar `ITEM_UNIT_MAX_LENGTH = 40`. |
| `SupermarketService.java` | Modify | Trim/validación, preservar `checked`, baja lógica en delete. |
| `SuperItemRepository.java` | Modify | Mantener consultas activas; revisar helpers para baja lógica. |
| `ApiExceptionHandler.java` | Modify | Etiquetas `unit` y `habitualObjective`. |
| `index.html`, `supermarket.js`, `styles.css` | Modify | Inputs mínimos e indicador responsive de configuración. |
| `SupermarketControllerTests.java`, `StaticUiContractTests.java`, `static-ui-contract-tests.mjs` | Modify | Contratos backend/UI nuevos. |

## Interfaces / Contracts

```json
{
  "name": "Leche",
  "categoryId": 1,
  "checked": true,
  "notes": "Sin lactosa",
  "unit": "litro",
  "habitualObjective": 2.000
}
```

Respuesta mantiene los campos actuales y agrega `unit`, `habitualObjective`, `configured`. Los campos fuera de etapa (`stock`, `movements`, `price`, `barcode`, `ocr`) no se modelan ni se persisten como contrato soportado.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Backend contract | Payload viejo, payload nuevo, respuesta pendiente/configurada, objetivo inválido | MockMvc en `SupermarketControllerTests`. |
| Service/repository | `checked` no cambia al configurar; `uncheck-all` no toca configuración; delete deja `active=false` y oculta del listado | Assertions JPA/MockMvc. |
| Static UI | Límites, payload JS, validación, columna `Configuración`, lista manual sin objetivo/stock | `StaticUiContractTests` y `static-ui-contract-tests.mjs`. |
| Full | Regresión del proyecto | `mvn test`. |

## Migration / Rollout

Hibernate agregará `unit` y `habitual_objective` como columnas nullable. No hay backfill: productos existentes responderán `configured=false`. Rollout por PR encadenado Stage 1; antes de stock/movimientos, abrir decisión separada para migraciones versionadas.

## Resolved Questions

- [x] Vocabulario UI resuelto con el texto implementado y verificado: campos “Unidad opcional” y “Objetivo habitual opcional”, columna “Configuración” y estados “Configurado”/“Pendiente”.
