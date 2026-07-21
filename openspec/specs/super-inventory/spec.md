# Especificación super-inventory

## Propósito

Define la evolución de la Lista del Super hacia inventario doméstico configurable, preservando productos, categorías, endpoints y la lista manual actual mientras incorpora snapshot de stock, cantidad rápida, movimientos operativos auditables y alias locales de barcode sobre productos existentes.

## Requirements

### Requirement: Evolución compatible del producto existente

El sistema MUST tratar los productos existentes del super como la base progresiva del futuro Producto Base, sin crear una base paralela ni exigir reconstrucción funcional.

#### Scenario: Producto existente sigue disponible
- GIVEN un producto existente con categoría, nombre, notas y estado `checked`
- WHEN se consulta la lista de productos
- THEN el producto MUST aparecer con sus datos actuales preservados
- AND su identidad funcional MUST seguir siendo la misma

#### Scenario: Sin Producto Base paralelo
- GIVEN la Etapa 1 está activa
- WHEN se registra o actualiza un producto del super
- THEN el sistema MUST usar el modelo evolutivo existente
- AND MUST NOT requerir una tabla o catálogo paralelo de Producto Base

### Requirement: Estado de configuración de producto

El sistema MUST representar si un producto está configurado para inventario mediante unidad y objetivo opcionales.

#### Scenario: Producto configurado
- GIVEN un producto con unidad y objetivo válidos
- WHEN se consulta el producto
- THEN su estado de configuración MUST indicar que está configurado

#### Scenario: Producto pendiente por datos faltantes
- GIVEN un producto sin unidad o sin objetivo
- WHEN se consulta el producto
- THEN su estado MUST indicar configuración pendiente
- AND el sistema MUST NOT inventar unidad ni objetivo por defecto

#### Scenario: Objetivo inválido
- GIVEN una solicitud con objetivo inválido para configuración
- WHEN se crea o actualiza el producto
- THEN el sistema MUST rechazar la solicitud con error de validación
- AND MUST NOT modificar el producto persistido

### Requirement: Contratos retrocompatibles de productos

El sistema MUST aceptar contratos actuales sin unidad/objetivo y MAY aceptar contratos extendidos con esos datos opcionales.

#### Scenario: Payload anterior sigue funcionando
- GIVEN una solicitud actual con nombre, categoría, notas y `checked`
- WHEN se crea o actualiza un producto
- THEN la operación MUST conservar el comportamiento actual
- AND el producto resultante MUST quedar pendiente si falta unidad u objetivo

#### Scenario: Respuesta extendida compatible
- GIVEN productos con y sin configuración completa
- WHEN se consulta `/api/super/items`
- THEN la respuesta MUST incluir los datos actuales
- AND SHOULD exponer unidad, objetivo y estado cuando existan o apliquen

### Requirement: Snapshot de inventario Etapa 2

El sistema MUST exponer `currentStock` nullable; `null` MUST significar stock desconocido. El snapshot SHALL cambiar solo por ajuste no negativo, compra o consumo.

#### Scenario: Producto migrado con stock desconocido
- GIVEN un producto existente sin dato de stock
- WHEN se consulta el producto
- THEN `currentStock` MUST ser `null`
- AND MUST NOT interpretarse como cero

#### Scenario: Update genérico no modifica stock
- GIVEN un producto con cualquier `currentStock`
- WHEN se crea o actualiza mediante el contrato genérico de producto con dato de stock
- THEN el sistema MUST rechazar esa mutación de stock
- AND MUST preservar el stock persistido

#### Scenario: Ajuste enfocado de stock
- GIVEN un producto existente y nuevo stock válido no negativo
- WHEN se ejecuta el comando enfocado de ajuste de stock
- THEN `currentStock` MUST establecerse en ese nuevo valor absoluto
- AND MUST registrar atómicamente `ADJUSTMENT`

### Requirement: Movimientos de stock

El sistema MUST aplicar compras y consumos como comandos atómicos: snapshot y movimiento MUST confirmarse o revertirse juntos.

#### Scenario: Compra
- GIVEN producto con `currentStock` conocido
- WHEN se registra compra con cantidad positiva
- THEN el stock MUST aumentar y registrar `PURCHASE`

#### Scenario: Consumo manual
- GIVEN producto con `currentStock` conocido
- WHEN se registra consumo manual con cantidad positiva
- THEN el stock MUST disminuir y registrar `CONSUMPTION`

#### Scenario: Consumo rápido
- GIVEN producto con `currentStock` conocido y `quickQuantity` positiva
- WHEN se ejecuta consumo rápido
- THEN el stock MUST bajar por `quickQuantity` y registrar `QUICK_CONSUMPTION`

#### Scenario: Atomicidad
- GIVEN un comando de stock
- WHEN falla el snapshot o el movimiento
- THEN el sistema MUST revertir ambos

### Requirement: Validaciones delta

El sistema MUST bloquear stock desconocido o datos inválidos; consumos MAY dejar negativo solo con confirmación.

#### Scenario: Stock desconocido
- GIVEN producto con `currentStock=null`
- WHEN se solicita compra, consumo manual o consumo rápido
- THEN el sistema MUST rechazar con error que pida ajuste

#### Scenario: Consumo rápido sin cantidad
- GIVEN producto sin `quickQuantity`
- WHEN se solicita consumo rápido
- THEN el sistema MUST rechazar con error claro

#### Scenario: Negativo sin confirmar
- GIVEN consumo que dejaría stock negativo
- WHEN falta confirmación explícita
- THEN el sistema MUST responder conflicto sin cambiar stock ni registrar movimiento

#### Scenario: Negativo confirmado
- GIVEN consumo que dejaría stock negativo
- WHEN existe confirmación explícita
- THEN el sistema MUST permitirlo y registrar el movimiento

### Requirement: Historial reciente

El sistema MUST exponer historial reciente y SHOULD aceptar filtro por producto.

#### Scenario: Consulta de historial
- GIVEN existen movimientos de stock
- WHEN se consulta historial con o sin filtro por producto
- THEN MUST mostrar recientes primero y filtrar por producto cuando aplique
- AND MUST incluir ajustes, compras y consumos

### Requirement: Cantidad rápida opcional

El sistema MUST aceptar `quickQuantity` como dato nullable/opcional y MUST validar que sea positivo cuando esté presente.

#### Scenario: Cantidad rápida válida
- GIVEN una solicitud con `quickQuantity` positiva
- WHEN se crea o actualiza el producto
- THEN el valor MUST persistirse y exponerse en la respuesta

#### Scenario: Cantidad rápida ausente o inválida
- GIVEN una solicitud sin `quickQuantity` o con valor no positivo
- WHEN se procesa la solicitud
- THEN la ausencia MUST aceptarse como `null`
- AND el valor no positivo MUST rechazarse con error de validación

### Requirement: `checked` como intención manual de compra

El sistema MUST preservar `checked` exclusivamente como intención manual o excepcional de compra pendiente, sin mutar stock.
(Previously: `checked` era solo intención manual y no podía convertirse en cantidad, stock ni movimiento.)

#### Scenario: Configurar producto no cambia intención manual
- GIVEN un producto con `checked=true`
- WHEN se completa o modifica unidad, objetivo o cantidad rápida
- THEN `checked` MUST permanecer sin cambios
- AND MUST NOT modificar `currentStock` ni crear movimientos

#### Scenario: Desmarcado masivo conserva su alcance
- GIVEN productos marcados manualmente para comprar
- WHEN se ejecuta el desmarcado masivo
- THEN el sistema MUST cambiar solo la intención manual `checked`
- AND MUST NOT alterar unidad, objetivo, cantidad rápida, stock ni movimientos

### Requirement: Lista manual y categorías preservadas

El sistema MUST mantener categorías, productos, generación manual de lista y endpoints actuales de forma compatible. La lista MAY mostrar pistas de cantidad solo cuando existan datos explícitos, y MUST NOT derivarlas desde stock u objetivo.
(Previously: la lista manual se basaba en `checked` y no usaba stock real ni sugerencias automáticas.)

#### Scenario: Lista manual se genera igual
- GIVEN productos configurados y pendientes con `checked=true`
- WHEN se genera la lista manual
- THEN la lista MUST incluirlos según la regla actual basada en `checked`
- AND MAY mostrar cantidad si existe `quickQuantity` y unidad
- AND MUST NOT derivar compras desde `currentStock` ni `habitualObjective`

#### Scenario: Categorías sin cambio funcional
- GIVEN categorías existentes activas
- WHEN se crean, editan, listan o usan en productos
- THEN el comportamiento actual MUST preservarse

### Requirement: Alias locales de barcode sobre SuperItem existente

El sistema MUST persistir códigos de barcode como texto asociado solo a un `SuperItem` existente. MUST preservar ceros iniciales y MUST NOT crear Producto Base, catálogo paralelo ni identidad de producto alternativa.

#### Scenario: Lookup con alias activo
- GIVEN un alias activo con código `0075012345678` asociado a un `SuperItem`
- WHEN se busca exactamente ese código
- THEN el sistema MUST devolver el `SuperItem` asociado
- AND MUST preservar el código como texto sin perder ceros iniciales

#### Scenario: Lookup sin alias activo
- GIVEN no existe alias activo para un código ingresado
- WHEN se busca ese código
- THEN el sistema MUST responder como no encontrado
- AND MAY permitir asociarlo manualmente a un `SuperItem` existente

#### Scenario: Adjuntar alias a producto existente
- GIVEN un `SuperItem` existente y un código sin alias activo
- WHEN el usuario adjunta el código al producto
- THEN el sistema MUST crear un alias activo para ese `SuperItem`

#### Scenario: Rechazo de duplicado activo
- GIVEN un código ya existe como alias activo
- WHEN se intenta adjuntar el mismo código a cualquier producto
- THEN el sistema MUST rechazar la operación por duplicado
- AND MUST NOT crear otro alias activo

#### Scenario: Remover alias
- GIVEN un alias activo asociado a un producto
- WHEN el usuario lo remueve
- THEN el sistema MUST desactivarlo o removerlo
- AND el lookup futuro MUST ignorar ese alias

### Requirement: Barcode manual-first sin impacto en inventario

Las operaciones de barcode MUST NOT modificar `currentStock`, `checked` ni movimientos. La entrada manual o pegado MUST estar disponible; cámara o `BarcodeDetector` MAY existir solo como mejora progresiva. El comportamiento de Etapas 1, 2 y 3 MUST permanecer intacto.

#### Scenario: Estado de inventario preservado
- GIVEN un producto con `currentStock`, `checked` e historial de movimientos
- WHEN se busca, adjunta o remueve un barcode
- THEN esos valores MUST permanecer sin cambios
- AND MUST NOT registrarse movimiento de stock

#### Scenario: Fallback manual obligatorio
- GIVEN el navegador no soporta cámara o deniega permisos
- WHEN el usuario ingresa o pega un código manualmente
- THEN el lookup y la asociación manual MUST seguir disponibles

#### Scenario: Etapas previas intactas
- GIVEN categorías, lista manual, snapshot, cantidad rápida y movimientos existentes
- WHEN se usa o no se usa barcode
- THEN los contratos de Etapas 1, 2 y 3 MUST conservar su comportamiento

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
- THEN el sistema MUST rechazarla con error de validación
- AND MUST NOT modificar el producto persistido

#### Scenario: Limpieza por precio o presentación
- GIVEN un producto con presentación, precio, fuente, fecha y observaciones persistidas
- WHEN se elimina el precio o la presentación default
- THEN fuente y fecha MUST limpiarse a `null` y las observaciones MUST permanecer intactas

#### Scenario: Fuente y fecha sin mutaciones colaterales
- GIVEN un producto con `checked`, `currentStock`, movimientos, barcodes, lista manual, lista sugerida y observaciones existentes
- WHEN se crea, edita, elimina o consulta la fuente, fecha, precio o presentación
- THEN esos datos MUST permanecer sin cambios y MUST NOT crearse movimiento, barcode, sugerencia persistida, elemento manual ni observación

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
