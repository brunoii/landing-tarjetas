# Delta for super-inventory

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Snapshot de inventario Etapa 2

El sistema MUST exponer `currentStock` nullable; `null` MUST significar stock desconocido. El snapshot SHALL cambiar solo por ajuste no negativo, compra o consumo.
(Previously: el stock solo podía cambiar mediante ajuste enfocado.)

#### Scenario: Producto migrado con stock desconocido
- GIVEN un producto existente sin dato de stock
- WHEN se consulta el producto
- THEN `currentStock` MUST ser `null`
- AND MUST NOT interpretarse como cero

#### Scenario: Update genérico no modifica stock
- GIVEN un producto con cualquier `currentStock`
- WHEN se crea o actualiza mediante contrato genérico con dato de stock
- THEN el sistema MUST rechazar esa mutación
- AND MUST preservar el stock persistido

#### Scenario: Ajuste enfocado de stock
- GIVEN un producto existente y nuevo stock válido no negativo
- WHEN se ejecuta ajuste enfocado de stock
- THEN `currentStock` MUST establecerse en ese valor absoluto
- AND MUST registrar atómicamente `ADJUSTMENT`

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar Etapa 3 a movimientos, historial reciente, snapshot, cantidad rápida y ajuste auditable. SHALL NOT introducir precios, presentaciones, barcode, OCR ni lista sugerida automática.
(Previously: Etapa 2 prohibía compras, consumo e historial visible.)

#### Scenario: Sin lista sugerida automática
- GIVEN un producto con unidad, objetivo y stock cargado
- WHEN se consulta o genera la lista
- THEN el sistema MUST NOT sugerir compras automáticamente
- AND MUST NOT derivar compras desde objetivo o stock

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true`
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`
- AND MUST NOT crear movimientos

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con precio, presentación, barcode, OCR o lista sugerida automática
- WHEN se procesa en Etapa 3
- THEN MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado
