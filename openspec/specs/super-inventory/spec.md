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
