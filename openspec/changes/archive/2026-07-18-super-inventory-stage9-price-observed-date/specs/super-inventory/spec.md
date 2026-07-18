# Delta para super-inventory

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 9 a una presentación default nullable, un precio actual/de referencia nullable en pesos, una fuente manual opcional y una fecha observada manual opcional date-only, ambas asociadas solo a ese precio. SHALL NOT introducir datetime/timestamp, tienda/comercio, catálogo de fuentes, historial, múltiples precios/presentaciones, OCR, lookup externo, automatización, sugerencias persistidas, total sugerido, mezcla con lista manual ni Producto Base/catálogo.
(Previously: Etapa 8 admitía fuente manual, pero no fecha date-only.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida
- THEN el sistema MAY sugerir la diferencia hasta el objetivo y MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento ni total por precio o fecha

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con datetime/timestamp, tienda, historial de precios, múltiples precios/presentaciones, OCR, lookup externo, automatización, sugerencia persistida, total sugerido o Producto Base
- WHEN se procesa en Etapa 9
- THEN el sistema MUST tratarlos fuera de contrato y MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro elegible para sugerencia
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos ni mezclar sugerencias en la lista manual

### Requirement: Presentación comercial default opcional

El sistema MUST permitir una única presentación comercial default nullable. La presentación MUST tener texto visible y MAY tener cantidad positiva. El sistema MAY asociarle un único precio actual/de referencia nullable, positivo, en pesos y no interpretable como precio por unidad. Solo ese precio MAY tener `commercialPresentationPriceSourceLabel` nullable, recortada, de hasta 120 caracteres, y `commercialPresentationPriceObservedDate` nullable, manual y date-only/`LocalDate`. La fecha MUST existir solo con presentación y precio; el precio MAY existir sin fecha. Fechas futuras MUST rechazarse. Al limpiar precio o presentación, fuente y fecha MUST quedar `null`. El sistema MUST NOT crear tiendas, historial, múltiples presentaciones ni mutaciones colaterales.
(Previously: admitía precio con fuente manual, pero no fecha.)

#### Scenario: Producto legacy sin datos comerciales
- GIVEN un producto existente sin esos datos o un payload legacy que los omite
- WHEN se crea, actualiza o consulta el producto
- THEN la operación MUST ser retrocompatible y presentación, precio, fuente y fecha MUST exponerse ausentes o `null`

#### Scenario: Precio, fuente y fecha válida
- GIVEN un producto con presentación default, precio positivo, fuente con espacios externos y fecha observada no futura
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST persistir y exponer precio, fuente recortada y fecha date-only asociadas solo a `commercialPresentationPricePesos`

#### Scenario: Precio sin fuente ni fecha
- GIVEN un producto con presentación default y precio positivo sin fuente ni fecha
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST aceptar el precio y fuente y fecha MUST ser `null` o ausentes

#### Scenario: Datos comerciales inválidos
- GIVEN una solicitud con fuente sin precio, fecha sin presentación o precio, fecha futura, fuente sobre 120 caracteres, precio sin presentación o precio no positivo
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Limpieza por precio o presentación
- GIVEN un producto con presentación, precio, fuente y fecha persistidos
- WHEN se elimina el precio o se elimina la presentación default
- THEN fuente y fecha MUST limpiarse a `null` y MUST NOT quedar metadata huérfana persistida

#### Scenario: Fuente y fecha sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, movimientos, barcodes, lista manual y lista sugerida existentes
- WHEN se crea, edita, elimina o consulta la fuente o fecha del precio
- THEN esos datos MUST permanecer sin cambios y MUST NOT crearse movimiento, barcode, sugerencia persistida ni elemento manual
