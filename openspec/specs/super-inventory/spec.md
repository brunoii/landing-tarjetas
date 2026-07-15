# Especificación super-inventory

## Propósito

Define la evolución de la Lista del Super hacia inventario doméstico configurable, preservando productos, categorías, endpoints y la lista manual actual mientras incorpora snapshot de stock, cantidad rápida y movimientos operativos auditables.

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

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar Etapa 3 a movimientos, historial reciente, snapshot, cantidad rápida y ajuste auditable. SHALL NOT introducir precios, presentaciones, barcode, OCR ni lista sugerida automática.
(Previously: Etapa 2 prohibía compras, consumo e historial visible.)

#### Scenario: Sin lista sugerida automática
- GIVEN un producto con unidad, objetivo y stock cargado
- WHEN se consulta o genera la lista
- THEN el sistema MUST NOT sugerir compras automáticamente
- AND MUST NOT derivar compras desde objetivo o stock

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con precio, presentación, barcode, OCR o lista sugerida automática
- WHEN se procesa en Etapa 3
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true`
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`
- AND MUST NOT crear movimientos
