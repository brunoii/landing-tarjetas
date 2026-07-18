# Delta for super-inventory

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 8 a una única presentación comercial default nullable, un único precio actual/de referencia nullable en pesos y una única fuente manual opcional para ese precio. SHALL NOT introducir entidad tienda/comercio, catálogo de fuentes, historial de precios, múltiples precios, múltiples presentaciones, OCR, lookup externo, automatización de compras o consumo, persistencia de sugerencias, estimación de total de lista sugerida, mezcla con lista manual ni Producto Base/catálogo paralelo.
(Previously: la Etapa 7 permitía un único precio actual/de referencia sin fuente manual.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento ni total estimado por precio

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con tienda, historial, múltiples precios/presentaciones, OCR, lookup externo, automatización, sugerencia persistida, total sugerido o Producto Base
- WHEN se procesa en Etapa 8
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true` y otro elegible para sugerencia
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`, sugerencias ni movimientos
- AND MUST NOT mezclar sugerencias dentro de la lista manual

### Requirement: Presentación comercial default opcional

El sistema MUST permitir metadatos opcionales y nullable de una única presentación comercial default sobre el `SuperItem` existente. Cuando la presentación exista, MUST incluir texto visible y MAY incluir cantidad contenida positiva en la unidad de inventario. El sistema MAY asociar a esa presentación un único precio actual/de referencia nullable, positivo, en pesos y no interpretable como precio por unidad. El sistema MAY asociar solo a ese precio `commercialPresentationPriceSourceLabel` nullable; cuando exista, MUST guardarse recortada, limitarse a 120 caracteres y exponerse como etiqueta manual secundaria. La fuente MUST existir solo cuando exista precio; el precio MAY existir sin fuente. Al limpiar precio o presentación, la fuente MUST quedar `null`. El sistema MUST NOT crear tiendas, historial, múltiples presentaciones ni mutaciones colaterales.
(Previously: la presentación default admitía un único precio actual/de referencia, pero no fuente manual.)

#### Scenario: Producto legacy sin presentación, precio ni fuente
- GIVEN un producto existente sin esos datos o un payload legacy que los omite
- WHEN se crea, actualiza o consulta el producto
- THEN la operación MUST seguir siendo válida y retrocompatible
- AND presentación, precio y fuente MUST exponerse ausentes o `null`

#### Scenario: Presentación default con precio y fuente válida
- GIVEN un producto con presentación default, precio positivo y fuente con espacios externos
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST persistir y exponer precio y fuente recortada
- AND la fuente MUST quedar asociada solo a `commercialPresentationPricePesos`

#### Scenario: Precio válido sin fuente
- GIVEN un producto con presentación default y precio positivo sin fuente
- WHEN se crea, actualiza o consulta el producto
- THEN el sistema MUST aceptar el precio
- AND `commercialPresentationPriceSourceLabel` MUST ser `null` o ausente

#### Scenario: Fuente inválida o huérfana
- GIVEN una solicitud con fuente sin precio, fuente sobre 120 caracteres, precio sin presentación o precio no positivo
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Limpieza por precio o presentación
- GIVEN un producto con presentación, precio y fuente persistidos
- WHEN se elimina el precio o se elimina la presentación default
- THEN la fuente MUST limpiarse a `null`
- AND MUST NOT quedar fuente huérfana persistida

#### Scenario: Fuente sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, movimientos, barcodes, lista manual y lista sugerida existentes
- WHEN se crea, edita, elimina o consulta la fuente del precio
- THEN esos datos MUST permanecer sin cambios
- AND MUST NOT crearse movimiento, barcode, sugerencia persistida ni elemento de lista manual
