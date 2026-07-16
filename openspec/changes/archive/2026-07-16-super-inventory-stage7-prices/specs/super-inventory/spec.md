# Delta for super-inventory

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 7 a un único precio actual/de referencia nullable, positivo cuando exista y expresado en pesos, asociado solo a la presentación comercial default del `SuperItem`. SHALL NOT introducir OCR, lookup externo, tiendas/comercios, historial de precios, múltiples presentaciones, automatización de compras, automatización de consumo, persistencia de sugerencias, estimación de total de lista sugerida ni mezcla con la lista manual.
(Previously: la Etapa 6A prohibía cualquier precio junto con tiendas, historial, múltiples presentaciones y automatizaciones.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida de Etapa 5
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento ni total estimado por precio

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con OCR, lookup externo, tienda, historial de precios, múltiples presentaciones, compra automática, consumo automático, sugerencia persistida o total estimado de lista sugerida
- WHEN se procesa en Etapa 7
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro producto elegible para sugerencia
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos
- AND MUST NOT mezclar sugerencias dentro de la lista manual

### Requirement: Presentación comercial default opcional

El sistema MUST permitir metadatos opcionales y nullable de una única presentación comercial default sobre el `SuperItem` existente. Cuando la presentación exista, MUST incluir texto visible y MAY incluir cantidad contenida; si la cantidad existe, MUST ser positiva y expresarse en la unidad de inventario del producto. El sistema MAY asociar a esa presentación un único precio actual/de referencia nullable, expresado en pesos y positivo cuando esté presente. Ese precio MUST NOT interpretarse como precio por unidad de inventario. El sistema MUST NOT crear tiendas, historial de precios, múltiples presentaciones, catálogo paralelo ni mutaciones colaterales de inventario.
(Previously: la presentación default no admitía precios.)

#### Scenario: Producto legacy sin presentación ni precio
- GIVEN un producto existente sin datos de presentación ni precio
- WHEN se crea, actualiza o consulta el producto
- THEN la operación MUST seguir siendo válida y retrocompatible
- AND la presentación y el precio MUST exponerse como ausentes o `null`

#### Scenario: Presentación default con precio válido
- GIVEN un producto con presentación default y precio positivo en pesos
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST persistir y exponer esos metadatos
- AND el precio MUST quedar asociado a la presentación default, no a la unidad de inventario

#### Scenario: Presentación o precio inválido
- GIVEN una solicitud con texto de presentación vacío, cantidad no positiva, cantidad sin unidad de inventario, precio cero, precio negativo o precio sin presentación default
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Precio sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, sugerencias, movimientos, barcodes y lista manual existentes
- WHEN se crea, edita, elimina o consulta el precio actual/de referencia
- THEN esos datos MUST permanecer sin cambios
- AND MUST NOT crearse movimiento, sugerencia persistida, alias de barcode ni elemento de lista manual
