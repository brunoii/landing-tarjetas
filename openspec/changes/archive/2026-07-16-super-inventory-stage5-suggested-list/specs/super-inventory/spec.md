# Delta for super-inventory

## ADDED Requirements

### Requirement: Lista sugerida read-only separada

El sistema MUST exponer una lista sugerida de compras derivada y read-only, separada de la lista manual basada en `checked`. Una sugerencia MUST existir solo para productos activos, configurados con `unit` y `habitualObjective`, con `currentStock` conocido y menor al objetivo; la cantidad sugerida SHALL ser `habitualObjective - currentStock`.

#### Scenario: Sugerencia elegible
- GIVEN un producto activo con `unit`, `habitualObjective=5` y `currentStock=2`
- WHEN se consulta la lista sugerida
- THEN el producto MUST aparecer con cantidad sugerida `3` y su unidad

#### Scenario: Stock desconocido excluido
- GIVEN un producto activo configurado con `currentStock=null`
- WHEN se consulta la lista sugerida
- THEN el producto MUST NOT aparecer en las sugerencias
- AND `null` MUST NOT interpretarse como cero

#### Scenario: Elegibilidad activa y configurada
- GIVEN productos inactivos, sin `unit`, sin `habitualObjective`, en objetivo o sobre objetivo
- WHEN se consulta la lista sugerida
- THEN ninguno de esos productos MUST aparecer

#### Scenario: Consulta sin mutaciones
- GIVEN productos con `checked`, `currentStock` e historial de movimientos existentes
- WHEN se consulta o renderiza la lista sugerida
- THEN el sistema MUST NOT modificar `checked`, `currentStock` ni movimientos

#### Scenario: Separación de lista manual
- GIVEN productos sugeridos y productos con `checked=true`
- WHEN se genera la lista manual y se consulta la lista sugerida
- THEN la lista manual MUST depender solo de `checked`
- AND las sugerencias MUST permanecer en una salida o sección separada

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 5 a una lista sugerida read-only y derivada desde datos locales existentes. SHALL NOT introducir OCR, lookup externo, precios, tiendas, presentaciones comerciales, automatización de compras, automatización de consumo, persistencia de sugerencias ni mezcla con la lista manual.
(Previously: la primera slice de Etapa 4 permitía alias locales de barcode y prohibía cualquier lista sugerida automática.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida de Etapa 5
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock ni movimiento

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con OCR, lookup externo, precio, tienda, presentación comercial, compra automática, consumo automático o sugerencia persistida
- WHEN se procesa en Etapa 5
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro producto elegible para sugerencia
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos
- AND MUST NOT mezclar sugerencias dentro de la lista manual
