# Delta for super-inventory

## ADDED Requirements

### Requirement: Observaciones manuales de precio append-only

El sistema MUST permitir crear observaciones manuales de precio solo por acción explícita sobre un `SuperItem` activo con presentación comercial default. Cada observación MUST ser append-only, persistir precio positivo en pesos, fuente opcional recortada hasta 120 caracteres, fecha observada opcional date-only no futura, snapshot de presentación default y `createdAt`. El sistema MUST listar observaciones recientes globalmente y MAY filtrar por producto con límite seguro.

#### Scenario: Alta manual válida
- GIVEN un `SuperItem` activo con presentación default y precio positivo
- WHEN se registra una observación manual con fuente y fecha no futura
- THEN la observación MUST persistirse con snapshot de presentación y `createdAt`
- AND MUST NOT modificar producto, stock, movimientos, barcodes ni listas

#### Scenario: Alta inválida
- GIVEN un producto inexistente, inactivo, sin presentación, precio no positivo, fuente mayor a 120 caracteres o fecha futura
- WHEN se intenta registrar la observación
- THEN el sistema MUST rechazarla sin persistir observación ni mutar el producto

#### Scenario: Listado reciente seguro
- GIVEN existen observaciones de varios productos
- WHEN se consulta el historial global o filtrado por producto con límite ausente o excesivo
- THEN el sistema MUST devolver recientes primero y aplicar un límite seguro

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 10 a una presentación default nullable, un precio actual/de referencia nullable en pesos, fuente manual opcional, fecha observada manual opcional date-only y observaciones manuales append-only de ese precio. SHALL NOT introducir edición/borrado de observaciones, datetime/timestamp, tienda/comercio, catálogo de fuentes, comparación, gráficos, múltiples precios/presentaciones, OCR, lookup externo, scraping, automatización, sugerencias persistidas, total sugerido, mezcla con lista manual ni Producto Base/catálogo.
(Previously: Etapa 9 prohibía todo historial; Etapa 10 permite solo observaciones manuales append-only.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida
- THEN el sistema MAY sugerir la diferencia hasta el objetivo y MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento, total por precio ni observación

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con edición/borrado de observaciones, datetime/timestamp, tienda, comparación, múltiples precios/presentaciones, OCR, lookup externo, scraping, automatización, sugerencia persistida, total sugerido o Producto Base
- WHEN se procesa en Etapa 10
- THEN el sistema MUST tratarlos fuera de contrato y MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro elegible para sugerencia
- WHEN se genera la lista manual, cambia `checked` o se crea/lista una observación
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos ni mezclar sugerencias en la lista manual

### Requirement: Presentación comercial default opcional

El sistema MUST permitir una única presentación comercial default nullable. La presentación MUST tener texto visible y MAY tener cantidad positiva. El sistema MAY asociarle un único precio actual/de referencia nullable, positivo, en pesos y no interpretable como precio por unidad. Solo ese precio MAY tener `commercialPresentationPriceSourceLabel` nullable, recortada, de hasta 120 caracteres, y `commercialPresentationPriceObservedDate` nullable, manual y date-only/`LocalDate`. La fecha MUST existir solo con presentación y precio; el precio MAY existir sin fecha. Fechas futuras MUST rechazarse. Al limpiar precio o presentación, fuente y fecha MUST quedar `null`. Crear, actualizar, cambiar o limpiar precio, fuente, fecha o presentación MUST NOT crear ni mutar observaciones existentes.
(Previously: prohibía cualquier historial y no distinguía observaciones manuales append-only.)

#### Scenario: Producto legacy sin datos comerciales
- GIVEN un producto existente sin esos datos o un payload legacy que los omite
- WHEN se crea, actualiza o consulta el producto
- THEN la operación MUST ser retrocompatible y presentación, precio, fuente y fecha MUST exponerse ausentes o `null`

#### Scenario: Precio, fuente y fecha válida
- GIVEN un producto con presentación default, precio positivo, fuente con espacios externos y fecha observada no futura
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST persistir y exponer precio, fuente recortada y fecha date-only sin crear observación

#### Scenario: Precio sin fuente ni fecha
- GIVEN un producto con presentación default y precio positivo sin fuente ni fecha
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST aceptar el precio y fuente y fecha MUST ser `null` o ausentes

#### Scenario: Datos comerciales inválidos
- GIVEN una solicitud con fuente sin precio, fecha sin presentación o precio, fecha futura, fuente sobre 120 caracteres, precio sin presentación o precio no positivo
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación y MUST NOT modificar el producto persistido

#### Scenario: Limpieza por precio o presentación
- GIVEN un producto con presentación, precio, fuente, fecha y observaciones persistidas
- WHEN se elimina el precio o la presentación default
- THEN fuente y fecha MUST limpiarse a `null` y las observaciones MUST permanecer intactas

#### Scenario: Fuente y fecha sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, movimientos, barcodes, lista manual, lista sugerida y observaciones existentes
- WHEN se crea, edita, elimina o consulta la fuente, fecha, precio o presentación
- THEN esos datos MUST permanecer sin cambios y MUST NOT crearse movimiento, barcode, sugerencia persistida, elemento manual ni observación
