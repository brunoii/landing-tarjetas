# Delta for super-inventory

## ADDED Requirements

### Requirement: Presentación comercial default opcional

El sistema MUST permitir metadatos opcionales y nullable de una única presentación comercial default sobre el `SuperItem` existente. Cuando la presentación exista, MUST incluir texto visible de presentación y MAY incluir cantidad contenida; si la cantidad existe, MUST ser positiva y expresarse en la unidad de inventario del producto. El sistema MUST NOT crear precios, tiendas, historial de precios, múltiples presentaciones, catálogo paralelo ni mutaciones colaterales de inventario.

#### Scenario: Producto legacy sin presentación
- GIVEN un producto existente sin datos de presentación
- WHEN se crea, actualiza o consulta el producto
- THEN la operación MUST seguir siendo válida y retrocompatible
- AND la presentación MUST exponerse como ausente o `null`

#### Scenario: Presentación default válida
- GIVEN un producto con unidad de inventario y presentación con texto visible y cantidad positiva
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST persistir y exponer esos metadatos
- AND la cantidad MUST interpretarse en la unidad de inventario del producto

#### Scenario: Presentación o cantidad inválida
- GIVEN una solicitud con texto de presentación vacío, cantidad no positiva o cantidad sin unidad de inventario
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Presentación sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, sugerencias, movimientos y barcodes existentes
- WHEN se crea, edita, elimina o consulta su presentación default
- THEN esos datos MUST permanecer sin cambios
- AND MUST NOT crearse movimiento, sugerencia persistida ni alias de barcode

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 6A a una presentación comercial default opcional sobre `SuperItem`. SHALL NOT introducir OCR, lookup externo, precios, tiendas, historial de precios, múltiples presentaciones, automatización de compras, automatización de consumo, persistencia de sugerencias ni mezcla con la lista manual.
(Previously: la Etapa 5 prohibía cualquier presentación comercial además de precios, tiendas y automatizaciones.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida de Etapa 5
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock ni movimiento

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con OCR, lookup externo, precio, tienda, historial de precios, múltiples presentaciones, compra automática, consumo automático o sugerencia persistida
- WHEN se procesa en Etapa 6A
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro producto elegible para sugerencia
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos
- AND MUST NOT mezclar sugerencias dentro de la lista manual
