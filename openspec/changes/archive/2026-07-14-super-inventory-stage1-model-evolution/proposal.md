# Proposal: Etapa 1 — Evolución del modelo actual

## Intent

Preparar `Lista del Super` para inventario doméstico sin reconstruir el módulo: `super_items` evoluciona hacia Producto Base, conservando datos, endpoints y `checked` como intención manual de compra.

## Scope

### In Scope
- Agregar campos mínimos y compatibles para unidad, objetivo y estado de configuración.
- Tratar productos existentes como pendientes hasta definir unidad/objetivo; no inventar `1 unidad` ni stock inicial.
- Extender DTOs/API de items con campos opcionales y respuestas retrocompatibles.
- Mantener `checked`, categorías y generación manual de lista.
- Preparar el modelo para movimientos/stock futuros sin tabla paralela.

### Out of Scope
- Tabla paralela de Producto Base, renombres masivos o rebuild.
- Movimientos, stock real, lista sugerida, precios, presentaciones, barcode u OCR.
- Rediseño completo de UI; solo ajuste mínimo si evita estados incoherentes.

## Capabilities

### New Capabilities
- `super-inventory`: evolución incremental del producto del super hacia inventario configurable.

### Modified Capabilities
- None: no hay specs OpenSpec vigentes en `openspec/specs/`.

## Approach

Usar `SuperItem`/`super_items` como base. Añadir campos nullable/configurables, mapear existentes como pendientes, preservar API actual y actualizar contratos. No crear nuevas tablas salvo migración versionada aprobada.

## Affected Areas

| Área | Impacto | Descripción |
|------|---------|-------------|
| `SuperItem.java` / `super_items` | Modificado | Unidad, objetivo y configuración pendiente. |
| `SuperItemRequest/Response.java` | Modificado | Campos opcionales retrocompatibles. |
| `SupermarketService.java`, `SuperItemRepository.java` | Modificado | Reglas de configuración y preservación de `checked`. |
| `/api/super/items` | Modificado | GET/POST/PUT amplían payload; PATCH checked y categorías sin cambio funcional. |
| `SupermarketControllerTests.java` | Modificado | Defaults, compatibilidad y migración semántica. |
| `supermarket.js`, `index.html`, `styles.css`, static tests | Posible | Solo si se muestra pendiente/configuración mínima. |
| Nuevos archivos | No previsto | Opcional `db/migration/**` si se aprueba Flyway/Liquibase. |

## Compatibility Strategy

- Columnas nuevas nullable/default seguro; filas existentes quedan “pendientes de configuración”.
- Sin eliminación de campos/endpoints actuales.
- `checked=true` no se convierte en cantidad, stock ni movimiento.

## Tests Planned

- MockMvc: payload antiguo funciona; payload nuevo persiste unidad/objetivo; existentes responden pendientes.
- Servicio/repositorio: `checked` se conserva en updates y desmarcado masivo.
- Contratos estáticos si cambia UI/API JS.
- `mvn test` como verificación de etapa.

## Risks

| Riesgo | Prob. | Mitigación |
|--------|-------|------------|
| Workspace sucio/rama desfasada | Alta | No mezclar cambios; PRs encadenados. |
| `ddl-auto=update` no audita migración | Media | Aprobar Flyway/Liquibase o documentar backfill manual. |
| UI confusa con pendientes | Media | Ajuste mínimo o mantener cambio interno. |

## Decisions Requiring Approval

- Nombres exactos de campos: unidad, objetivo y configurado/pendiente.
- Cambiar `DELETE /api/super/items/{id}` a baja lógica con `active`, o diferirlo.
- Incorporar Flyway/Liquibase ahora o continuar con `ddl-auto=update` controlado.
- Mostrar configuración pendiente en UI o mantenerlo interno.

## Rollback Plan

Revertir el PR. Si hubo columnas nuevas, dejarlas sin uso o aplicar rollback DB aprobado; campos actuales y `checked` quedan intactos.

## Dependencies

- Aprobación de decisiones antes de implementar.
- Preservar cambios locales; no limpiar ni rebasar workspace desde esta etapa.

## Success Criteria

- [x] Productos existentes siguen listándose y generando lista manual.
- [x] No se crea tabla paralela de Producto Base.
- [x] Productos sin unidad/objetivo quedan pendientes, sin cantidad inventada.
- [x] Tests del módulo y contratos relevantes pasan.
