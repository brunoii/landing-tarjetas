# Archive Report: Fecha observada del precio de referencia Etapa 9

## Status

success

## Change

- Change: `super-inventory-stage9-price-observed-date`
- Proyecto: `landing-tarjetas`
- Artifact store: hybrid (`openspec` + Engram)
- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Work unit: PR 3 OpenSpec archive/spec sync only
- Archived location: `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/`

## Executive Summary

Se fusionaron los dos requisitos MODIFIED aceptados de Etapa 9 en `openspec/specs/super-inventory/spec.md` y se archivó el cambio completo bajo la convención `YYYY-MM-DD-{change}`. El cierre mantuvo el límite de publicación de PR 3: solo OpenSpec/spec/archive/reportes, sin tocar backend, UI, tests productivos ni assets.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `super-inventory` | Updated | 0 added, 2 modified, 0 removed requirements. Se actualizaron `Límites explícitos de Etapa 2` y `Presentación comercial default opcional`. |

## Requirements Merged

- `Límites explícitos de Etapa 2`: ahora limita Etapa 9 a una presentación default nullable, precio actual/de referencia nullable, fuente manual opcional y fecha observada manual opcional date-only, ambas asociadas solo a ese precio; mantiene fuera de contrato datetime/timestamp, tiendas/comercios, historial, múltiples precios/presentaciones, OCR, lookup externo, automatización, sugerencias persistidas, totales sugeridos, mezcla con lista manual y Producto Base/catálogo paralelo.
- `Presentación comercial default opcional`: ahora permite `commercialPresentationPriceObservedDate` nullable, manual y date-only/`LocalDate` solo asociada al precio; rechaza fecha futura o huérfana, acepta precio sin fecha, limpia fuente y fecha al quitar precio o presentación, y preserva comportamiento sin mutaciones colaterales.

## Archive Contents

- `exploration.md` ✅
- `proposal.md` ✅
- `specs/super-inventory/spec.md` ✅
- `design.md` ✅
- `tasks.md` ✅ (13/13 tareas completas)
- `apply-progress.md` ✅
- `verify-report.md` ✅
- `archive-report.md` ✅

## Source of Truth Updated

- `openspec/specs/super-inventory/spec.md`

## Engram Traceability

| Artifact | Observation ID | Topic key |
|----------|----------------|-----------|
| Exploration | `903` | `sdd/super-inventory-stage9-price-observed-date/explore` |
| Proposal | `904` | `sdd/super-inventory-stage9-price-observed-date/proposal` |
| Spec delta | `905` | `sdd/super-inventory-stage9-price-observed-date/spec` |
| Design | `906` | `sdd/super-inventory-stage9-price-observed-date/design` |
| Verify report | `907` | `sdd/super-inventory-stage9-price-observed-date/verify-report` |
| Tasks | `908` | `sdd/super-inventory-stage9-price-observed-date/tasks` |

## Validation Run

| Check | Result | Evidence |
|-------|--------|----------|
| Task completion gate | PASS | `tasks.md` quedó con 13/13 tareas marcadas `[x]`; las tareas 3.1 y 3.2 se completaron en este cierre documental. |
| Verification gate | PASS | `verify-report.md` declara PASS para PR 2, con PR 1 previamente verificado, y `CRITICAL: None`, `WARNING: None`. |
| OpenSpec source sync | PASS | Delta MODIFIED fusionado en `openspec/specs/super-inventory/spec.md`. |
| Archive move | PASS | Cambio movido a `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/`. |
| Active change cleanup | PASS | `openspec/changes/super-inventory-stage9-price-observed-date/` ya no queda activo. |
| PR 3 boundary | PASS | Solo se modificaron/crearon artefactos OpenSpec. |
| OpenSpec CLI | WARNING | `openspec --version` no está disponible en el entorno; validación manual aplicada según convención compartida. |

## Risks

- No se detectan riesgos bloqueantes.
- Advertencia no bloqueante: `openspec/config.yaml` no existe en el workspace actual; se aplicaron las convenciones compartidas y la regla de archivo no destructivo.
- Advertencia no bloqueante: OpenSpec CLI no está instalado/disponible; no se pudo ejecutar validación CLI.

## Publication Boundary

PR 3 debe mantenerse limitado a:

- `openspec/specs/super-inventory/spec.md`
- `openspec/changes/archive/2026-07-18-super-inventory-stage9-price-observed-date/**`

Fuera de alcance para PR 3:

- Backend Java/API/tests.
- UI/static/tests.
- Commits, pushes, merges o PRs automáticos.

## SDD Cycle Complete

El cambio quedó planificado, implementado, verificado, sincronizado en la especificación viva y archivado como audit trail.
