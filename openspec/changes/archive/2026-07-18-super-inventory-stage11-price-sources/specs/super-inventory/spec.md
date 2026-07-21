# Delta for super-inventory

## ADDED Requirements

### Requirement: Fuentes de precio reutilizables mínimas

El sistema MUST permitir crear y listar fuentes de precio activas reutilizables para observaciones manuales. Cada fuente MUST tener nombre visible no vacío y nombre normalizado único. Una fuente de precio MUST NOT representar tienda, comercio, sucursal, ubicación, dirección, ranking, comparación ni historial externo.

#### Scenario: Crear fuente válida
- GIVEN un nombre con espacios externos y una variante de mayúsculas/minúsculas no existente
- WHEN se crea la fuente de precio
- THEN el sistema MUST persistirla activa con nombre visible recortado
- AND MUST impedir otra fuente activa o existente con el mismo nombre normalizado

#### Scenario: Listar solo fuentes activas sin semántica comercial
- GIVEN existen fuentes de precio activas
- WHEN se consulta el catálogo mínimo de fuentes
- THEN el sistema MUST devolver solo fuentes activas reutilizables
- AND MUST NOT exponer dirección, ubicación, tienda, comercio, comparación ni métricas de precio

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 11 a una presentación default nullable, un precio actual/de referencia nullable en pesos, fuente manual opcional del producto como texto libre, fecha observada manual opcional date-only, observaciones manuales append-only y catálogo mínimo de fuentes solo para esas observaciones. SHALL NOT introducir edición/borrado/renombrado/desactivación UI de fuentes, tiendas/comercios, comparación, gráficos, múltiples precios/presentaciones, OCR, lookup externo, scraping, automatización, sugerencias persistidas, total sugerido, mezcla con lista manual ni Producto Base/catálogo.
(Previously: Etapa 10 prohibía catálogo de fuentes; Etapa 11 permite solo un catálogo mínimo para observaciones manuales.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento, total por precio ni observación

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con administración de fuentes, tienda, comercio, dirección, ubicación, comparación, múltiples precios/presentaciones, OCR, lookup externo, scraping, automatización, total sugerido o Producto Base
- WHEN se procesa en Etapa 11
- THEN el sistema MUST tratarlos fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro elegible para sugerencia
- WHEN se genera la lista manual, cambia `checked`, o se crea/lista una fuente u observación
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos
- AND MUST NOT mezclar sugerencias en la lista manual

### Requirement: Observaciones manuales de precio append-only

El sistema MUST permitir crear observaciones manuales de precio solo por acción explícita sobre un `SuperItem` activo con presentación comercial default. Cada observación MUST ser append-only, persistir precio positivo en pesos, fecha observada opcional date-only no futura, snapshot de presentación default, `createdAt`, `priceSourceId` nullable y `sourceLabel` snapshot nullable. La fuente opcional MUST recibirse como `priceSourceId` activo o `sourceLabel` libre recortado hasta 120 caracteres, nunca ambos. Con `priceSourceId`, el sistema MUST copiar el nombre de la fuente a `sourceLabel`; con texto libre u observaciones legacy, `priceSourceId` MUST permanecer `null`. El sistema MUST listar observaciones recientes globalmente y MAY filtrar por producto con límite seguro.
(Previously: las observaciones solo tenían `sourceLabel` libre sin relación nullable a fuente reutilizable.)

#### Scenario: Alta manual con fuente reutilizable
- GIVEN un `SuperItem` activo con presentación default, precio positivo y una fuente activa
- WHEN se registra una observación manual con `priceSourceId`
- THEN la observación MUST persistir relación nullable a la fuente y snapshot `sourceLabel` con su nombre
- AND MUST NOT modificar producto, precio actual, stock, movimientos, barcodes ni listas

#### Scenario: Alta manual con fuente libre o sin fuente
- GIVEN un `SuperItem` activo con presentación default y precio positivo
- WHEN se registra una observación con `sourceLabel` libre o sin fuente
- THEN la observación MUST persistirse con `priceSourceId=null`
- AND el texto libre MUST recortarse sin normalizar catálogos ni mutar la fuente actual del producto

#### Scenario: Alta inválida
- GIVEN producto inexistente, inactivo, sin presentación, precio no positivo, fecha futura, fuente libre mayor a 120 caracteres, `priceSourceId` inactivo/inexistente o `priceSourceId` junto con `sourceLabel`
- WHEN se intenta registrar la observación
- THEN el sistema MUST rechazarla sin persistir observación ni mutar producto, inventario o listas

#### Scenario: Listado reciente y legacy seguro
- GIVEN existen observaciones nuevas y legacy con `priceSourceId=null`
- WHEN se consulta el historial global o filtrado por producto con límite ausente o excesivo
- THEN el sistema MUST devolver recientes primero, aplicar límite seguro y preservar observaciones legacy sin backfill ni agrupamiento
