# Delta para super-inventory

## ADDED Requirements

### Requirement: Snapshot de inventario Etapa 2

El sistema MUST exponer `currentStock` nullable como snapshot de stock; `null` MUST significar stock desconocido. El sistema SHALL reutilizar `unit` y `habitualObjective` existentes. El stock SHALL cambiar solo mediante un comando enfocado de ajuste.

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
- GIVEN un producto existente y un nuevo stock actual válido
- WHEN se ejecuta el comando enfocado de ajuste de stock
- THEN `currentStock` MUST establecerse en ese nuevo valor absoluto
- AND el sistema MUST registrar internamente un hecho auditable con forma de movimiento

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

## MODIFIED Requirements

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

El sistema MUST limitar Etapa 2 a snapshot de stock, cantidad rápida y ajuste interno auditable; SHALL NOT introducir compras, consumo, historial visible de movimientos, precios, presentaciones, barcode, OCR ni lista sugerida automática.
(Previously: Etapa 1 prohibía stock real y movimientos por completo.)

#### Scenario: Sin lista sugerida automática
- GIVEN un producto con unidad, objetivo y stock cargado
- WHEN se consulta o genera la lista
- THEN el sistema MUST NOT sugerir compras automáticamente
- AND MUST NOT derivar compras desde objetivo o stock

#### Scenario: Campos fuera de alcance
- GIVEN una solicitud que intenta gestionar consumo, precio, presentación, barcode, OCR o historial visible
- WHEN se procesa la solicitud de Etapa 2
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado
