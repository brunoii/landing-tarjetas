# Archive Report: Historial manual de observaciones de precio Etapa 10

## Status

success

## Change

- Change: `super-inventory-stage10-price-history`
- Proyecto: `landing-tarjetas`
- Artifact store: hybrid (`openspec` + Engram)
- Delivery strategy: force-chained
- Chain strategy: stacked-to-main
- Work unit: PR 3 OpenSpec archive/spec sync only
- Archived location: `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/`

## Executive Summary

Se fusionó el requisito ADDED y los dos requisitos MODIFIED aceptados de Etapa 10 en `openspec/specs/super-inventory/spec.md` y se archivó el cambio completo bajo la convención `YYYY-MM-DD-{change}`. El cierre mantuvo el límite de publicación de PR 3: solo OpenSpec/spec/archive/reportes, sin tocar backend, UI, tests productivos ni assets.

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| `super-inventory` | Updated | 1 added, 2 modified, 0 removed requirements. Se agregó `Observaciones manuales de precio append-only` y se actualizaron `Límites explícitos de Etapa 2` y `Presentación comercial default opcional`. |

## Requirements Merged

- `Observaciones manuales de precio append-only`: habilita alta manual explícita sobre `SuperItem` activo con presentación default, precio positivo, fuente opcional recortada, fecha observada date-only no futura, snapshot de presentación y `createdAt`; lista observaciones recientes globales o filtradas por producto con límite seguro.
- `Límites explícitos de Etapa 2`: ahora limita Etapa 10 a precio actual/de referencia, fuente/fecha manuales y observaciones manuales append-only del mismo precio; mantiene fuera de contrato edición/borrado de observaciones, tiendas/comercios, fuentes normalizadas, comparación, gráficos, múltiples precios/presentaciones, OCR, lookup externo, scraping, automatización, sugerencias persistidas, totales sugeridos, mezcla con lista manual y Producto Base/catálogo paralelo.
- `Presentación comercial default opcional`: mantiene la presentación única nullable y el precio actual/de referencia, pero aclara que crear, actualizar, cambiar o limpiar precio/fuente/fecha/presentación no crea ni muta observaciones existentes.

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
| Exploration | `931` | `sdd/super-inventory-stage10-price-history/explore` |
| Proposal | `932` | `sdd/super-inventory-stage10-price-history/proposal` |
| Spec delta | `933` | `sdd/super-inventory-stage10-price-history/spec` |
| Design | `934` | `sdd/super-inventory-stage10-price-history/design` |
| Tasks | `936` | `sdd/super-inventory-stage10-price-history/tasks` |
| Apply progress | `938` | `sdd/super-inventory-stage10-price-history/apply-progress` |
| Verify report | `941` | `sdd/super-inventory-stage10-price-history/verify-report` |

## Validation Run

| Check | Result | Evidence |
|-------|--------|----------|
| Task completion gate | PASS | `tasks.md` quedó con 16/16 tareas marcadas `[x]`; las tareas 3.1 y 3.2 se completaron en este cierre documental autorizado por el orquestador. |
| Verification gate | PASS | `verify-report.md` declara PASS para PR 1 backend/API y PR 2 UI/static, con `CRITICAL: None`, `WARNING: None`; este archive completa PR 3 OpenSpec-only. |
| OpenSpec source sync | PASS | Delta ADDED/MODIFIED fusionado en `openspec/specs/super-inventory/spec.md`. |
| Archive move | PASS | Cambio movido a `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/`. |
| Active change cleanup | PASS | `openspec/changes/super-inventory-stage10-price-history/` ya no queda activo. |
| PR 3 boundary | PASS | Solo se modificaron/crearon/movieron artefactos OpenSpec; no se modificó backend ni UI durante el archive. |
| Markdown diff check | PASS | `git diff --check -- openspec/specs/super-inventory/spec.md openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history` no reportó errores; Git emitió solo advertencia LF→CRLF para la spec viva. |
| OpenSpec CLI | WARNING | `openspec --version` no está disponible en el entorno; validación manual aplicada según convención compartida. |

## Risks

- No se detectan riesgos bloqueantes.
- Advertencia no bloqueante: `openspec/config.yaml` no existe en el workspace actual; se aplicaron las convenciones compartidas y la regla de archivo no destructivo.
- Advertencia no bloqueante: OpenSpec CLI no está instalado/disponible; no se pudo ejecutar validación CLI.

## Publication Boundary

PR 3 debe mantenerse limitado a:

- `openspec/specs/super-inventory/spec.md`
- `openspec/changes/archive/2026-07-18-super-inventory-stage10-price-history/**`

Fuera de alcance para PR 3:

- Backend Java/API/tests.
- UI/static/tests.
- Commits, pushes, merges o PRs automáticos.

## SDD Cycle Complete

El cambio quedó planificado, implementado, verificado, sincronizado en la especificación viva y archivado como audit trail.
