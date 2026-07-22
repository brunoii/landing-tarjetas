# Delta for super-inventory

## ADDED Requirements

### Requirement: Frontera explícita de sincronización de precio actual

El sistema MUST mantener la generación de historial limitada a la acción manual explícita de observación. La sincronización hacia el precio actual/de referencia MAY ocurrir solo como opción explícita dentro de esa creación de observación; crear o actualizar productos MUST NOT crear observaciones automáticas.

#### Scenario: Edición de producto no genera historial
- GIVEN un producto con presentación y precio actual/de referencia
- WHEN se crea o actualiza mediante el flujo de producto
- THEN el sistema MUST NOT crear observaciones de precio
- AND MUST preservar las observaciones existentes sin cambios

#### Scenario: Alcance fuera de frontera
- GIVEN una solicitud o UI que intenta usar administración de fuentes, comparación, múltiples precios, barcode, OCR, ticket, foto o alcance de Etapa 15
- WHEN se procesa dentro de Etapa 13
- THEN el sistema MUST tratarlo fuera de contrato
- AND MUST NOT persistirlo como comportamiento soportado

## MODIFIED Requirements

### Requirement: Observaciones manuales de precio append-only

El sistema MUST permitir crear observaciones manuales de precio solo por acción explícita sobre un `SuperItem` activo con presentación comercial default. Cada observación MUST ser append-only, persistir precio positivo en pesos, fecha observada opcional date-only no futura, snapshot de presentación default, `createdAt`, `priceSourceId` nullable y `sourceLabel` snapshot nullable. La fuente opcional MUST recibirse como `priceSourceId` activo o `sourceLabel` libre recortado hasta 120 caracteres, nunca ambos. Con `priceSourceId`, el sistema MUST copiar el nombre de la fuente a `sourceLabel`; con texto libre u observaciones legacy, `priceSourceId` MUST permanecer `null`. El sistema MUST listar observaciones recientes globalmente y MAY filtrar por producto con límite seguro. La creación manual MAY incluir una opción explícita para sincronizar el mismo precio, fuente resuelta/libre y fecha observada hacia el precio actual/de referencia del producto. Sin esa opción, la operación MUST preservar el comportamiento de Etapas 10-12 y MUST NOT modificar producto, precio actual, stock, movimientos, barcodes ni listas. Con sincronización solicitada, la observación y la actualización del producto MUST confirmarse o revertirse juntas.
(Previously: las observaciones manuales eran siempre observation-only y nunca podían actualizar el precio actual del producto.)

#### Scenario: Alta manual con fuente reutilizable
- GIVEN un `SuperItem` activo con presentación default, precio positivo y una fuente activa
- WHEN se registra una observación manual con `priceSourceId` sin solicitar sincronización
- THEN la observación MUST persistir relación nullable a la fuente y snapshot `sourceLabel` con su nombre
- AND MUST NOT modificar producto, precio actual, stock, movimientos, barcodes ni listas

#### Scenario: Alta manual con fuente libre o sin fuente
- GIVEN un `SuperItem` activo con presentación default y precio positivo
- WHEN se registra una observación con `sourceLabel` libre o sin fuente sin solicitar sincronización
- THEN la observación MUST persistirse con `priceSourceId=null`
- AND el texto libre MUST recortarse sin normalizar catálogos ni mutar la fuente actual del producto

#### Scenario: Alta inválida
- GIVEN producto inexistente, inactivo, sin presentación, precio no positivo, fecha futura, fuente libre mayor a 120 caracteres, `priceSourceId` inactivo/inexistente o `priceSourceId` junto con `sourceLabel`
- WHEN se intenta registrar la observación con o sin sincronización
- THEN el sistema MUST rechazarla sin persistir observación ni mutar producto, inventario o listas

#### Scenario: Listado reciente y legacy seguro
- GIVEN existen observaciones nuevas y legacy con `priceSourceId=null`
- WHEN se consulta el historial global o filtrado por producto con límite ausente o excesivo
- THEN el sistema MUST devolver recientes primero, aplicar límite seguro y preservar observaciones legacy sin backfill ni agrupamiento

#### Scenario: Alta manual con sincronización explícita
- GIVEN un `SuperItem` activo con presentación default, precio positivo, fuente válida o ausente y fecha observada válida o ausente
- WHEN se registra una observación solicitando sincronizar el precio actual/de referencia
- THEN el sistema MUST persistir la observación append-only
- AND MUST actualizar el producto con el mismo precio, fuente resuelta/libre y fecha observada

#### Scenario: Sincronización atómica
- GIVEN una observación válida con sincronización solicitada
- WHEN falla la persistencia de la observación o la actualización del producto
- THEN el sistema MUST revertir ambas mutaciones
- AND MUST preservar el precio previo del producto y el historial de observaciones
