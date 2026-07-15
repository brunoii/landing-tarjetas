# Archive Report: PRD Etapa 3 — Movimientos

## Resultado

El cambio `super-inventory-stage3-movements` fue validado como listo para archivo: 15/15 tareas completas, verificación PASS y sin issues CRITICAL ni WARNING. El delta aceptado de Etapa 3 se sincronizó en la especificación fuente `openspec/specs/super-inventory/spec.md` preservando comportamientos de Etapa 1 y Etapa 2.

## Resumen de sincronización

| Dominio | Acción | Detalle |
|---------|--------|---------|
| `super-inventory` | Actualizado | 3 requisitos agregados: movimientos de stock, validaciones delta e historial reciente. |
| `super-inventory` | Actualizado | 2 requisitos modificados: snapshot de inventario Etapa 2 y límites explícitos de Etapa 2. |

## Fuente de verdad actualizada

- `openspec/specs/super-inventory/spec.md` — incorpora compras, consumo manual, consumo rápido, historial reciente, validaciones de stock desconocido/negativo y límites de alcance de Etapa 3.

## Evidencia preservada

| Comando | Resultado |
|---------|-----------|
| `mvn -Dtest=SupermarketControllerTests,StaticUiContractTests test` | ✅ BUILD SUCCESS; 57 tests, 0 failures, 0 errors. |
| `node src/test/resources/static-ui-contract-tests.mjs` | ✅ exit 0, sin salida. |
| `mvn test` | ✅ BUILD SUCCESS; 207 tests, 0 failures, 0 errors. |
| `git diff --check` | ✅ exit 0; solo warnings LF-to-CRLF del working tree, sin errores de whitespace. |

## Trazabilidad de artefactos

### OpenSpec

- `proposal.md` ✅
- `specs/super-inventory/spec.md` ✅
- `design.md` ✅
- `tasks.md` ✅ — 15/15 tareas completas.
- `apply-progress.md` ✅
- `verify-report.md` ✅ — verdict PASS.
- `exploration.md` ✅

### Engram

| Artefacto | Observación |
|-----------|-------------|
| Proposal | `#715` |
| Spec delta | `#716` |
| Design | `#717` |
| Tasks | `#719` |
| Apply progress | `#722` |
| Verify report | `#727` |

## Validaciones de archivo

- [x] `tasks.md` no contiene tareas de implementación pendientes.
- [x] `verify-report.md` registra PASS y no contiene issues CRITICAL/WARNING activos.
- [x] La especificación principal fue sincronizada antes de mover el cambio a archivo.
- [x] No se modificó código de implementación durante archive.
- [x] `openspec/config.yaml` no existe en este workspace; no había reglas `rules.archive` adicionales para aplicar.

## Estado final

Carpeta archivada en `openspec/changes/archive/2026-07-14-super-inventory-stage3-movements/`. Este reporte queda como auditoría del cierre del ciclo SDD.

## Nota post-archivo: compatibilidad H2 local

- 2026-07-14: se agregó una corrección de compatibilidad para bases H2 locales persistentes creadas antes de que `super_stock_movements.movement_type` aceptara todos los tipos de movimiento de Etapa 3. La migración de arranque convierte la columna a `VARCHAR(40)` de forma idempotente y limitada a H2.
- Evidencia: `mvn "-Dtest=SupermarketControllerTests,StaticUiContractTests,SuperStockMovementSchemaCompatibilityTests" test` ✅; `node src/test/resources/static-ui-contract-tests.mjs` ✅.
