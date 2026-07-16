# Archive Report: Etapa 4 — Alias local de barcode

## Resultado

La Etapa 4 `super-inventory-stage4-barcode-ocr` fue archivada como cambio completo. El delta aceptado se sincronizó en la especificación vigente de `super-inventory`, preservando los contratos de Etapas 1, 2 y 3.

## Resumen ejecutivo

El sistema ahora reconoce alias locales de barcode manual-first sobre `SuperItem` existente. Barcode queda como identificación auxiliar: no crea Producto Base paralelo, no modifica `currentStock`, no cambia `checked` y no registra movimientos. OCR, lookup externo, cámara obligatoria, precios, tiendas, presentaciones, compras automáticas y lista sugerida automática siguen fuera de contrato.

## Spec sync

| Dominio | Acción | Detalle |
|---------|--------|---------|
| `super-inventory` | Updated | Se agregaron 2 requirements: alias locales de barcode y barcode manual-first sin impacto en inventario. |
| `super-inventory` | Modified | Se reemplazó `Límites explícitos de Etapa 2` para permitir solo alias locales de barcode en Etapa 4 y mantener fuera OCR/automatización/catálogos externos. |

## Evidencia de verificación preservada

| Evidencia | Resultado |
|-----------|-----------|
| `mvn -Dtest=SupermarketControllerTests test` | PASS — 39 tests, 0 failures, 0 errors, 0 skipped. |
| `mvn -Dtest=StaticUiContractTests test` | PASS — 26 tests, 0 failures, 0 errors, 0 skipped. |
| `node src/test/resources/static-ui-contract-tests.mjs` | PASS — exit 0. |
| `mvn test` | PASS — 217 tests, 0 failures, 0 errors, 0 skipped. |
| `git diff --check` | PASS con advertencias LF→CRLF únicamente. |
| Validación manual del usuario | PASS — asociación, lookup/resaltado y remoción de barcode sin mutar `checked` ni `currentStock`. |

## Gate de archivo

- `tasks.md`: 17/17 tareas completas.
- `verify-report.md`: PASS WITH WARNINGS, sin issues CRITICAL.
- Warnings no bloqueantes preservados: advertencias LF→CRLF en `git diff --check`, logging H2 esperado en prueba de constraint único y limitación de `git diff --stat` para archivos sin trackear.
- `openspec/config.yaml`: no existe en el workspace; no había reglas `rules.archive` adicionales para aplicar.

## Trazabilidad Engram

| Artifact | Observation |
|----------|-------------|
| Exploration | `#747` — `sdd/super-inventory-stage4-barcode-ocr/explore` |
| Proposal | `#748` — `sdd/super-inventory-stage4-barcode-ocr/proposal` |
| Delta spec | `#749` — `sdd/super-inventory-stage4-barcode-ocr/spec` |
| Design | `#750` — `sdd/super-inventory-stage4-barcode-ocr/design` |
| Tasks | `#752` — `sdd/super-inventory-stage4-barcode-ocr/tasks` |
| Apply progress | `#753` — `sdd/super-inventory-stage4-barcode-ocr/apply-progress` |
| Verify report | `#755` — `sdd/super-inventory-stage4-barcode-ocr/verify-report` |
| User manual validation | `#762` — `sdd/super-inventory-stage4-barcode-ocr/manual-validation` |

## Archival decision

Archive status: `success`.

El cambio queda cerrado porque la especificación principal refleja el comportamiento aceptado, el folder de cambio se preserva como audit trail en `openspec/changes/archive/2026-07-15-super-inventory-stage4-barcode-ocr/`, y el reporte de verificación no contiene bloqueantes críticos.
