# Archive Report: Fuente manual del precio de referencia Etapa 8

## Status

success

## Change

- Change: `super-inventory-stage8-price-source`
- Proyecto: `landing-tarjetas`
- Artifact store: hybrid (`openspec` + Engram)
- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Work unit: PR 3 OpenSpec archive/spec sync only
- Archived location: `openspec/changes/archive/2026-07-16-super-inventory-stage8-price-source/`

## Executive Summary

Se fusionaron los dos requisitos MODIFIED aceptados de Etapa 8 en `openspec/specs/super-inventory/spec.md` y se archivó el cambio completo bajo la convención `YYYY-MM-DD-{change}`. El cierre mantuvo el límite de publicación de PR 3: solo OpenSpec/spec/archive/reportes, sin tocar backend ni UI.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `super-inventory` | Updated | 0 added, 2 modified, 0 removed requirements. Se actualizaron `Límites explícitos de Etapa 2` y `Presentación comercial default opcional`. |

## Requirements Merged

- `Límites explícitos de Etapa 2`: ahora limita Etapa 8 a una presentación default nullable, un precio actual/de referencia nullable en pesos y una fuente manual opcional para ese precio; mantiene fuera de contrato tiendas/comercios, catálogo de fuentes, historial, múltiples precios/presentaciones, OCR, lookup externo, automatización, persistencia de sugerencias, totales sugeridos, mezcla con lista manual y Producto Base paralelo.
- `Presentación comercial default opcional`: ahora permite `commercialPresentationPriceSourceLabel` nullable solo asociada al precio, recortada, limitada a 120 caracteres y renderizable como etiqueta manual secundaria; fuente sin precio o con precio/presentación inválida debe rechazarse y la fuente debe limpiarse al quitar precio o presentación.

## Archive Contents

- `exploration.md` ✅
- `proposal.md` ✅
- `specs/super-inventory/spec.md` ✅
- `design.md` ✅
- `tasks.md` ✅ (16/16 tareas completas)
- `apply-progress.md` ✅
- `verify-report.md` ✅
- `archive-report.md` ✅

## Source of Truth Updated

- `openspec/specs/super-inventory/spec.md`

## Engram Traceability

| Artifact | Observation ID | Topic key |
|----------|----------------|-----------|
| Exploration | `864` | `sdd/super-inventory-stage8-price-source/explore` |
| Proposal | `865` | `sdd/super-inventory-stage8-price-source/proposal` |
| Spec delta | `866` | `sdd/super-inventory-stage8-price-source/spec` |
| Design | `867` | `sdd/super-inventory-stage8-price-source/design` |
| Verify report | `868` | `sdd/super-inventory-stage8-price-source/verify-report` |
| Tasks | `869` | `sdd/super-inventory-stage8-price-source/tasks` |

## Validation Run

| Check | Result | Evidence |
|-------|--------|----------|
| Task completion gate | PASS | `tasks.md` quedó con 16/16 tareas marcadas `[x]`. |
| Verification gate | PASS | `verify-report.md` declara PASS para PR 2, con PR 1 previamente verificado, y `CRITICAL: None`, `WARNING: None`. |
| OpenSpec source sync | PASS | Delta MODIFIED fusionado en `openspec/specs/super-inventory/spec.md`. |
| Archive move | PASS | Cambio movido a `openspec/changes/archive/2026-07-16-super-inventory-stage8-price-source/`. |
| Active change cleanup | PASS | `openspec/changes/super-inventory-stage8-price-source/` ya no queda activo. |
| PR 3 boundary | PASS | Solo se modificaron/crearon artefactos OpenSpec. |
| OpenSpec CLI | WARNING | `openspec --version` no está disponible en el entorno; validación manual aplicada según convención compartida. |

## Risks

- No se detectan riesgos bloqueantes.
- Advertencia no bloqueante: `openspec/config.yaml` no existe en el workspace actual; se aplicaron las convenciones compartidas y la regla de archivo no destructivo.
- Advertencia no bloqueante: OpenSpec CLI no está instalado/disponible; no se pudo ejecutar validación CLI.

## Publication Boundary

PR 3 debe mantenerse limitado a:

- `openspec/specs/super-inventory/spec.md`
- `openspec/changes/archive/2026-07-16-super-inventory-stage8-price-source/**`

Fuera de alcance para PR 3:

- Backend Java/API/tests.
- UI/static/tests.
- Commits, pushes, merges o PRs automáticos.

## SDD Cycle Complete

El cambio quedó planificado, implementado, verificado, sincronizado en la especificación viva y archivado como audit trail.
