# Propuesta: Super Inventory Etapa 11 - fuentes de precio

## Intento

Reducir la repetición y ambigüedad de fuentes de precio en observaciones manuales, habilitando un catálogo mínimo reutilizable sin introducir semántica de tienda, comercio, comparación ni automatización.

## Alcance

### Incluido
- Catálogo mínimo `SuperPriceSource` para crear y listar fuentes activas.
- Relación nullable desde nuevas observaciones de precio hacia una fuente.
- Preservar `sourceLabel` como snapshot en observaciones; copiar el nombre de la fuente cuando se use `priceSourceId`.
- Validar que una nueva observación acepte `priceSourceId` o `sourceLabel` libre, no ambos.
- Mantener observaciones legacy con `priceSourceId=null` y `sourceLabel` textual.

### Fuera de alcance
- Backfill legacy, edición/borrado/renombrado de fuentes, tiendas/comercios, comparación, gráficos, múltiples presentaciones, OCR, lookup/scraping, automatización, totales, Product Base/catálogo y mutaciones de inventario/listas.
- Normalizar `SuperItem.commercialPresentationPriceSourceLabel`; continúa como texto libre.

## Capacidades

### Nuevas capacidades
- Ninguna.

### Capacidades modificadas
- `super-inventory`: agrega catálogo mínimo de fuentes para observaciones manuales y ajusta límites de Etapa 10 sobre catálogo de fuentes.

## Enfoque

Usar una entidad mínima `SuperPriceSource` con nombre, clave normalizada, estado activo y timestamps. Exponer `GET/POST /api/super/price-sources`. Al crear observación, resolver fuente activa si llega `priceSourceId`; guardar FK nullable y snapshot `sourceLabel`. Si llega texto libre, guardar `priceSourceId=null`. La UI agrega selector/alta mínima inline sin administración completa.

## Áreas afectadas

| Área | Impacto | Descripción |
|------|---------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modificado | Reglas de fuente normalizada y límites explícitos. |
| `src/main/java/.../supermarket` | Nuevo/Modificado | Entidad, repositorio, DTOs, controller y servicio. |
| `src/main/resources/static` | Modificado | API JS y UI mínima para seleccionar/crear fuentes. |
| `src/test/java/...` y `src/test/resources/...` | Modificado | Tests API/UI y guards de alcance. |

## Riesgos

| Riesgo | Probabilidad | Mitigación |
|--------|--------------|------------|
| Deriva hacia tiendas/comparación | Media | Naming `price-source`, endpoints mínimos y guards estáticos. |
| Duplicados por mayúsculas/espacios | Media | Clave normalizada `trim().toLowerCase(Locale.ROOT)`. |
| Esquema sin migraciones versionadas | Baja | FK nullable, sin backfill y tests con legacy. |

## Plan de rollback

Revertir código, UI, tests y delta OpenSpec de la etapa. Como no hay backfill ni mutación de producto, las observaciones previas siguen válidas; las nuevas fuentes/relaciones pueden ignorarse al volver al contrato de texto libre.

## Dependencias

- Estado Etapa 10 archivado: observaciones manuales append-only con `sourceLabel` libre.

## Criterios de éxito

- [ ] Crear/listar fuentes activas reutilizables funciona sin semántica de comercio.
- [ ] Observaciones nuevas aceptan exactamente una fuente: `priceSourceId` o `sourceLabel`.
- [ ] Legacy permanece con `priceSourceId=null` y sin backfill.
- [ ] Producto, stock, movimientos, barcodes, listas y precio actual no se mutan.
