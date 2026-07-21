# Delta for super-inventory

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la Etapa 12 a una presentación default nullable, un precio actual/de referencia nullable en pesos, fuente opcional del producto como `commercialPresentationPriceSourceId` reutilizable activo o `commercialPresentationPriceSourceLabel` libre, fecha observada manual opcional date-only, observaciones manuales append-only y catálogo mínimo de fuentes reutilizable solo mediante el flujo existente de listado/alta inline de Etapa 11. SHALL NOT introducir administración de fuentes, edición/borrado/renombrado/desactivación UI de fuentes, tiendas/comercios, comparación, gráficos, múltiples precios/presentaciones, OCR, barcode, ticket, foto, scraping, automatización, sugerencias persistidas, total sugerido, mezcla con lista manual ni alcance de Etapa 15.
(Previously: Etapa 11 permitía catálogo mínimo solo para observaciones manuales y mantenía la fuente del precio actual del producto como texto libre.)

#### Scenario: Lista sugerida automática limitada
- GIVEN un producto con unidad, objetivo y stock conocido bajo objetivo
- WHEN se consulta la lista sugerida
- THEN el sistema MAY sugerir la diferencia hasta el objetivo
- AND MUST NOT convertirla en compra, consumo, `checked`, stock, movimiento, total por precio ni observación

#### Scenario: Campos fuera de alcance
- GIVEN una solicitud con administración de fuentes, tienda, comercio, comparación, OCR, barcode, ticket, foto o alcance de Etapa 15
- WHEN se procesa en Etapa 12
- THEN el sistema MUST tratarlos fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Reuso acotado al flujo existente
- GIVEN un usuario que necesita una fuente reutilizable para el precio actual del producto
- WHEN crea o elige una fuente
- THEN el sistema MUST reutilizar solo el catálogo y alta inline ya existentes en Etapa 11
- AND MUST NOT abrir una nueva superficie de administración

### Requirement: Presentación comercial default opcional

El sistema MUST permitir una única presentación comercial default nullable. La presentación MUST tener texto visible y MAY tener cantidad positiva. El sistema MAY asociarle un único precio actual/de referencia nullable, positivo, en pesos y no interpretable como precio por unidad. Solo ese precio MAY tener `commercialPresentationPriceSourceId` nullable, `commercialPresentationPriceSourceLabel` nullable, recortada, de hasta 120 caracteres, y `commercialPresentationPriceObservedDate` nullable, manual y date-only/`LocalDate`. La fuente opcional MUST seguir una regla cerrada: `{id only}`, `{label only}` o `{neither}` son válidos; `{id + label}` es inválido. Con `commercialPresentationPriceSourceId`, el sistema MUST resolver una fuente activa y copiar su nombre a `commercialPresentationPriceSourceLabel`. Con texto libre u objetos legacy, `commercialPresentationPriceSourceId` MUST permanecer `null`. Sin fuente, `commercialPresentationPriceSourceId` y `commercialPresentationPriceSourceLabel` MUST permanecer `null`. La fecha MUST existir solo con presentación y precio; el precio MAY existir sin fecha ni fuente. Fechas futuras MUST rechazarse. Al limpiar precio o presentación, `commercialPresentationPriceSourceId`, `commercialPresentationPriceSourceLabel` y `commercialPresentationPriceObservedDate` MUST quedar `null`. Crear, actualizar, cambiar o limpiar precio, fuente, fecha o presentación MUST NOT crear ni mutar observaciones existentes.
(Previously: solo permitía `commercialPresentationPriceSourceLabel` libre para el precio actual del producto.)

#### Scenario: Producto legacy sin backfill
- GIVEN un producto existente con precio actual, `commercialPresentationPriceSourceLabel` libre y `commercialPresentationPriceSourceId=null`
- WHEN se consulta o actualiza sin cambiar la fuente
- THEN el sistema MUST preservarlo como válido sin backfill
- AND MUST mantener `commercialPresentationPriceSourceId=null`

#### Scenario: Precio con fuente reutilizable válida
- GIVEN un producto con presentación default, precio positivo y una fuente activa existente
- WHEN se crea o actualiza con `commercialPresentationPriceSourceId`
- THEN el sistema MUST persistir esa relación nullable y copiar el nombre activo a `commercialPresentationPriceSourceLabel`
- AND MUST NOT requerir texto libre adicional

#### Scenario: Precio con fuente libre válida
- GIVEN un producto con presentación default y precio positivo
- WHEN se crea o actualiza con `commercialPresentationPriceSourceLabel` libre y sin id
- THEN el sistema MUST aceptarlo como texto libre recortado
- AND MUST persistir `commercialPresentationPriceSourceId=null`

#### Scenario: Precio válido sin fuente
- GIVEN un producto con presentación default y precio positivo
- WHEN se crea o actualiza sin `commercialPresentationPriceSourceId` ni `commercialPresentationPriceSourceLabel`
- THEN el sistema MUST aceptarlo como válido dentro de Etapa 12
- AND MUST persistir ambos campos de fuente en `null`

#### Scenario: Fuente inválida por XOR o id inactivo
- GIVEN una solicitud con `commercialPresentationPriceSourceId` junto con `commercialPresentationPriceSourceLabel`, o con un id inexistente/inactivo
- WHEN se procesa la solicitud
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Limpieza por precio o presentación
- GIVEN un producto con presentación, precio, fuente reusable o libre, fecha y observaciones persistidas
- WHEN se elimina el precio o la presentación default
- THEN `commercialPresentationPriceSourceId`, `commercialPresentationPriceSourceLabel` y `commercialPresentationPriceObservedDate` MUST limpiarse a `null`
- AND las observaciones MUST permanecer intactas
